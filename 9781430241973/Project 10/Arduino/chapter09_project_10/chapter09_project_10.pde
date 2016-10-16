#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_SERVO 0x7
#define SERVO_ID_1 0x1
#define SERVO_ID_1_PIN 2

int highSignalTime;
float microSecondsPerDegree;
// default boundaries, change them for your specific servo
int leftBoundaryInMicroSeconds = 1000;
int rightBoundaryInMicroSeconds = 2000;

AndroidAccessory acc("Manufacturer",
		     "Project10",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte rcvmsg[6];

void setup() {
  Serial.begin(19200);
  pinMode(SERVO_ID_1_PIN, OUTPUT);
  acc.powerOn();
  microSecondsPerDegree = (rightBoundaryInMicroSeconds - leftBoundaryInMicroSeconds) / 180.0;
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
          moveServo(SERVO_ID_1_PIN, posInDegrees);
        }
      }
    }
  }
}

void moveServo(int servoPulsePin, int pos){
  // calculate time for high signal
  highSignalTime = leftBoundaryInMicroSeconds + (pos * microSecondsPerDegree);
  // set Servo to HIGH
  digitalWrite(servoPulsePin, HIGH);
  // wait for calculated amount of microseconds
  delayMicroseconds(highSignalTime);
  // set Servo to LOW
  digitalWrite(servoPulsePin, LOW);
  // delay to complete waveform
  delayMicroseconds(20000 - highSignalTime);
}

