#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_DC_MOTOR 0x8
#define DC_MOTOR_ID_1 0x1
#define DC_MOTOR_ID_1_PIN 2

AndroidAccessory acc("Manufacturer",
		     "Project11",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte rcvmsg[3];

void setup() {
  Serial.begin(19200);
  pinMode(DC_MOTOR_ID_1_PIN, OUTPUT);
  acc.powerOn();
}

void loop() {
  if (acc.isConnected()) {
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      if (rcvmsg[0] == COMMAND_DC_MOTOR) {
        if(rcvmsg[1] == DC_MOTOR_ID_1) {
          int motorSpeed = rcvmsg[2] & 0xFF;
          motorSpeed = map(motorSpeed, 0, 100, 0, 255);  
          analogWrite(DC_MOTOR_ID_1_PIN, motorSpeed);
        }
      }
    }
  }
}

