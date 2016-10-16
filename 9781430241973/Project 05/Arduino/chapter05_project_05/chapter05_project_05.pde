#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_ANALOG 0x3
#define TARGET_PIN_2 0x2

AndroidAccessory acc("Manufacturer",
		     "Project05",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte rcvmsg[6];

void setup() {
  Serial.begin(19200);
  pinMode(TARGET_PIN_2, OUTPUT);
  acc.powerOn();
}

void loop() {
  if (acc.isConnected()) {
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      if (rcvmsg[0] == COMMAND_ANALOG) {
        if (rcvmsg[1] == TARGET_PIN_2){
          int output = ((rcvmsg[2] & 0xFF) << 24)
                     + ((rcvmsg[3] & 0xFF) << 16)
                     + ((rcvmsg[4] & 0xFF) << 8)
                     + (rcvmsg[5] & 0xFF);
          //set the frequency for the desired tone in Hz
          tone(TARGET_PIN_2, output);
        }
      }
    }
  }
}


