/*************************************************************************
* File Name          : Plotter.ino
* Author             : Evan
* Updated            : Ander
* Version            : V0.1.2
* Date               : 02/15/2014
* Description        : Data transmission format (7 byte):
                       =======================================
                        [6]   [5]   [4]   [3]   [2]   [1]   [0]
                        End   X_H   X_L   Y_H   Y_L   0xFE  0xFF
                        
                       End:0xFD for Data,0xFC for Reset
                       =======================================
* License            : CC-BY-SA 3.0
* Copyright (C) 2011-2014 Maker works Technology Co., Ltd. All right reserved.
* http://www.makeblock.cc/
**************************************************************************/

#include <AccelStepper.h>
#include <SoftwareSerial.h>
#include <Makeblock.h>
#include <Wire.h>

// Define a stepper and the pins it will use
AccelStepper stepperX(AccelStepper::DRIVER, 13, 12); // 13-PUL, 12-DIR PORT 3
AccelStepper stepperY(AccelStepper::DRIVER, 2, 8); // 2-PUL, 8-DIR, PORT 4
MeBluetooth bluetooth(PORT_6);
int motorDrc = 4; //M1
int motorPwm = 5; //M1
int limitSW_X = A0; //PORT 7
int limitSW_Y = A1; //PORT 8


byte rxBuf;
unsigned int timer = 0;
int x=0,y=0,xLast=0,yLast=0;
int inByte;
unsigned char dataBuf[8] = {0};
int xPos[40]={0};int yPos[40]={0};

char stateMachine = 0,t=0; 
boolean isMoving = false;int minIndex = 10,posIndex = 0;

void initMotor(){
  stepperX.setMaxSpeed(500);stepperX.setAcceleration(6000); // set X stepper speed and acceleration
  stepperY.setMaxSpeed(500);stepperY.setAcceleration(6000); // set Y stepper speed and acceleration
  stepperX.moveTo(-4000);stepperY.moveTo(-4000);// move XY to origin

  while(digitalRead(limitSW_X))stepperX.run();
  while(digitalRead(limitSW_Y))stepperY.run();// scanning stepper motor
  stepperX.setCurrentPosition(0);stepperY.setCurrentPosition(0); // reset XY position
  stepperX.setMaxSpeed(6000);stepperY.setMaxSpeed(6000);// set XY working speed
}

void setup(){  
  pinMode(limitSW_X, INPUT);
  pinMode(limitSW_Y, INPUT);
  
  pinMode(motorDrc, OUTPUT);
  digitalWrite(motorDrc, HIGH);
  pinMode(motorPwm, OUTPUT);
  analogWrite(motorPwm, 0);
  
  initMotor();
  Serial.begin(9600);
  bluetooth.begin(9600);
}

void loop() {
  minIndex = isMoving?5:20;//buffer limit
  if(posIndex<minIndex){
    while(bluetooth.available()){
        rxBuf = bluetooth.read();
        if(stateMachine == 0)// check state machine
        {
          if(rxBuf == 0xff) stateMachine = 1;
          else stateMachine = 0;
        }
        else if(stateMachine == 1)
        {
          if(rxBuf == 0xfe) stateMachine = 2;
          else stateMachine = 0;
        }
        else if(stateMachine == 2)// receive data
        {
          dataBuf[t++] = rxBuf&0xff;
          if(t>4){// when receive all of data, reset stateMachine
            if(dataBuf[4]==0xfd){
              posIndex++;
              xPos[posIndex] = (dataBuf[2] + (dataBuf[3]<<8)); yPos[posIndex] = (dataBuf[0] + (dataBuf[1]<<8));//push data into buffer.
              if(yPos[posIndex]<0||yPos[posIndex]>3200)yPos[posIndex]=yPos[posIndex-1]; //limit Y postion
              if(xPos[posIndex]<0||xPos[posIndex]>3200)xPos[posIndex]=xPos[posIndex-1]; //limit X postion
              
            }else if(dataBuf[4]==0xfc){
              //when finish or reset
              analogWrite(motorPwm, 0);
              posIndex = 0;
              delay(1000);
            }
          t=0;// reset 
          stateMachine=0;// reset stateMachine
          }
        }
      }
    }
    stepperX.run(); stepperY.run(); // scanning stepper motor
    isMoving = posIndex>15;
    if(posIndex>0){
      readPosition();
    }
  //************************************
  //    [3]     [2]     [1]     [0]
  //    Y-H     Y-L     X-H     X-L
  //************************************
}
void readPosition(){
  int i=0;
  if(stepperX.currentPosition() == xLast && stepperY.currentPosition() ==  yLast ){  
        for(i=0;i<posIndex;i++){
          xPos[i]=xPos[i+1]; yPos[i]=yPos[i+1]; //update buffer
        }
        if(posIndex>0){
          posIndex--;//update buffer index 
        }
        int dx = xLast-xPos[0];
        int dy = yLast-yPos[0];
        unsigned long dist = dx*dx+dy*dy;//calculate distance between points.
        if(dist>25){
           analogWrite(motorPwm, 0); //pen up
        }else{
          if(posIndex!=0) analogWrite(motorPwm, 200);//pen down
        }
        stepperX.moveTo(xPos[0]); stepperY.moveTo(yPos[0]);// move to target
        xLast = xPos[0]; yLast = yPos[0];// save last position
    }
}

