#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_BUTTON 0x1
#define TARGET_BUTTON 0x1
#define VALUE_ON 0x1
#define VALUE_OFF 0x0
#define INPUT_PIN 2

AndroidAccessory acc("Manufacturer",
		     "Project03",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[3];
int lastButtonState;
int currentButtonState;

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_BUTTON;
  sntmsg[1] = TARGET_BUTTON;
}

void loop() {
  if (acc.isConnected()) {
    currentButtonState = digitalRead(INPUT_PIN);
    if(currentButtonState != lastButtonState) {
      if(currentButtonState == LOW) {
        sntmsg[2] = VALUE_ON;
      } else {
        sntmsg[2] = VALUE_OFF;
      }
      acc.write(sntmsg, 3);
      lastButtonState = currentButtonState;
    }
    delay(100);
  }
}

