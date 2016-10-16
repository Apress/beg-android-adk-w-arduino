#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_ANALOG 0x3
#define TARGET_PIN 0x0
#define INPUT_PIN A0

AndroidAccessory acc("Manufacturer",
		     "Project04",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[6];
int analogPinReading;

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_ANALOG;
  sntmsg[1] = TARGET_PIN;
}

void loop() {
  if (acc.isConnected()) {  
    analogPinReading = analogRead(INPUT_PIN);
    sntmsg[2] = (byte) (analogPinReading >> 24);
    sntmsg[3] = (byte) (analogPinReading >> 16);
    sntmsg[4] = (byte) (analogPinReading >> 8);
    sntmsg[5] = (byte) analogPinReading;
    acc.write(sntmsg, 6);
    delay(100);
  }
}

