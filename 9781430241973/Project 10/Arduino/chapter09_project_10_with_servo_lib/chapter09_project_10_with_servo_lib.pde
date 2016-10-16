#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <Servo.h> 

#define COMMAND_SERVO 0x7
#define SERVO_ID_1 0x1
#define SERVO_ID_1_PIN 2

Servo servo;

AndroidAccessory acc("Manufacturer",
		     "Project10",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte rcvmsg[6];

void setup() {
  Serial.begin(19200);
  servo.attach(SERVO_ID_1_PIN);
  acc.powerOn();
}

void loop() {
  if (acc.isConnected()) {
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      if (rcvmsg[0] == COMMAND_SERVO) {
        if(rcvmsg[1] == SERVO_ID_1) {
          int posInDegrees = ((rcvmsg[2] & 0xFF) << 24)
                  + ((rcvmsg[3] & 0xFF) << 16)
                  + ((rcvmsg[4] & 0xFF) << 8)
                  + (rcvmsg[5] & 0xFF); 
          posInDegrees = map(posInDegrees, -100, 100, 0, 180);   
          servo.write(posInDegrees);
          // give the servo time to reach its position
          delay(20);
        }
      }
    }
  }
}

