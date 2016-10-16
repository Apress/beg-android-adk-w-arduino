#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_LIGHT_INTENSITY 0x5
#define INPUT_PIN_0 0x0

AndroidAccessory acc("Manufacturer",
		     "Project07",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[3];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_LIGHT_INTENSITY;
  sntmsg[1] = INPUT_PIN_0;
}

void loop() {
  if (acc.isConnected()) {
    int currentValue = analogRead(INPUT_PIN_0);
    sntmsg[2] = map(currentValue, 0, 1023, 0, 100); 
    acc.write(sntmsg, 3);
    delay(100);
  }
}


