#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <CapSense.h>

#define COMMAND_TOUCH_SENSOR 0x6
#define SENSOR_ID 0x0;
#define THRESHOLD 50

CapSense touchSensor = CapSense(4,6);

AndroidAccessory acc("Manufacturer",
		     "Project09",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[3];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  //disables auto calibration
  touchSensor.set_CS_AutocaL_Millis(0xFFFFFFFF);
  sntmsg[0] = COMMAND_TOUCH_SENSOR;
  sntmsg[1] = SENSOR_ID;
}

void loop() {
  if (acc.isConnected()) {
    //takes 30 measurements to reduce false readings and disturbances
    long value =  touchSensor.capSense(30);
    if(value > THRESHOLD) {
      sntmsg[2] = 0x1;
    } 
    else {
      sntmsg[2] = 0x0;
    }
    acc.write(sntmsg, 3);
    delay(100); 
  }
}

