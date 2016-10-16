#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_LED 0x2
#define TARGET_PIN_2 0x2
#define VALUE_ON 0x1
#define VALUE_OFF 0x0

#define PIN 2

AndroidAccessory acc("Manufacturer",
		     "Project01",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte rcvmsg[3];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  pinMode(PIN, OUTPUT);
}

void loop() {
  if (acc.isConnected()) {
    //read the received data into the byte array 
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      if (rcvmsg[0] == COMMAND_LED) {
        if (rcvmsg[1] == TARGET_PIN_2){
          //get the switch state
	  byte value = rcvmsg[2];
          //set output pin to according state
          if(value == VALUE_ON) {
            digitalWrite(PIN, HIGH);
          } else if(value == VALUE_OFF) {
            digitalWrite(PIN, LOW);
          }
        }
      }
    }
  }
}

