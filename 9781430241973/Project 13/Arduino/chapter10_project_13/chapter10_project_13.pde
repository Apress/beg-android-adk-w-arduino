#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define LED_OUTPUT_PIN 2
#define PIEZO_OUTPUT_PIN 3
#define BUTTON_INPUT_PIN 4
#define IR_LIGHT_BARRIER_INPUT_PIN A0 

#define IR_LIGHT_BARRIER_THRESHOLD 511
#define NOTE_C7 2100

#define COMMAND_ALARM 0x9
#define ALARM_TYPE_IR_LIGHT_BARRIER 0x2
#define ALARM_OFF 0x0
#define ALARM_ON 0x1

int irLightBarrierValue;
int buttonValue;
int ledBrightness = 0;
int fadeSteps = 5;

boolean alarm = false;

AndroidAccessory acc("Manufacturer",
		     "Project13",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[3];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_ALARM;
  sntmsg[1] = ALARM_TYPE_IR_LIGHT_BARRIER;
}

void loop() {
  acc.isConnected();
  irLightBarrierValue = analogRead(IR_LIGHT_BARRIER_INPUT_PIN);
  Serial.println(irLightBarrierValue, DEC);
  if((irLightBarrierValue > IR_LIGHT_BARRIER_THRESHOLD) && !alarm) {
    startAlarm();
  }
  buttonValue = digitalRead(BUTTON_INPUT_PIN);
  if((buttonValue == LOW) && alarm) {
    stopAlarm();
  }
  if(alarm) {
    fadeLED();
  }
  delay(10);
}

void startAlarm() {
  alarm = true;
  tone(PIEZO_OUTPUT_PIN, NOTE_C7);
  ledBrightness = 0;
  //inform Android device
  sntmsg[2] = ALARM_ON;
  sendAlarmStateMessage();
}

void stopAlarm() {
  alarm = false;
  //turn off piezo buzzer
  noTone(PIEZO_OUTPUT_PIN);
  //turn off LED
  digitalWrite(LED_OUTPUT_PIN, LOW);
  //inform Android device
  sntmsg[2] = ALARM_OFF;
  sendAlarmStateMessage();
}

void sendAlarmStateMessage() {
  if (acc.isConnected()) {
    acc.write(sntmsg, 3);  
  }
}

void fadeLED() {
  analogWrite(LED_OUTPUT_PIN, ledBrightness); 
  //increase or decrease brightness  
  ledBrightness = ledBrightness + fadeSteps;
  //change fade direction when reaching max or min of analog values
  if (ledBrightness < 0 || ledBrightness > 255) {
    fadeSteps = -fadeSteps ;
  } 
}
