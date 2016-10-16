#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_ANALOG 0x3
#define INPUT_PIN_0 0x0

AndroidAccessory acc("Manufacturer",
		     "Project06",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[4];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_ANALOG;
  sntmsg[1] = INPUT_PIN_0;
}

void loop() {
  if (acc.isConnected()) {
    int currentValue = analogRead(INPUT_PIN_0);
    sntmsg[2] = (byte) (currentValue >> 8);
    sntmsg[3] = (byte) currentValue;
    acc.write(sntmsg, 4);
    delay(100);
  }
}


