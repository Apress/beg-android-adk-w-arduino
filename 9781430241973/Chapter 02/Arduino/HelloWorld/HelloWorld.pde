#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define ARRAY_SIZE 25
#define COMMAND_TEXT 0xF
#define TARGET_DEFAULT 0xF

AndroidAccessory acc("Manufacturer",
		     "Model",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

char hello[ARRAY_SIZE] = {'H','e','l','l','o',' ',
'W','o','r','l','d',' ', 'f', 'r', 'o', 'm', ' ',
'A', 'r', 'd', 'u', 'i', 'n', 'o', '!'};

byte rcvmsg[255];
byte sntmsg[3 + ARRAY_SIZE];

void setup() {
  Serial.begin(115200);
  acc.powerOn();
}

void loop() {
  if (acc.isConnected()) {
    //read the sent text message into the byte array 
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      if (rcvmsg[0] == COMMAND_TEXT) {
        if (rcvmsg[1] == TARGET_DEFAULT){
          //get the textLength from the checksum byte
	  byte textLength = rcvmsg[2];
          int textEndIndex = 3 + textLength;
          //print each character to the serial output
          for(int x = 3; x < textEndIndex; x++) {
            Serial.print((char)rcvmsg[x]);
            delay(250);
          }
          Serial.println();
          delay(250);
        }
      }
    }
    
    sntmsg[0] = COMMAND_TEXT;
    sntmsg[1] = TARGET_DEFAULT;
    sntmsg[2] = ARRAY_SIZE;
    for(int x = 0; x < ARRAY_SIZE; x++) {
      sntmsg[3 + x] = hello[x];
    }
    acc.write(sntmsg, 3 + ARRAY_SIZE);
    delay(250);
  }
}

