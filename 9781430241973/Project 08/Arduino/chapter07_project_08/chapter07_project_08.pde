#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_TEMPERATURE 0x4
#define INPUT_PIN_0 0x0
//-----
//change those values according to your thermistor's datasheet
long r0 = 4700;
long beta = 3980;
//-----

double t0 = 298.15;
long additional_resistor = 10000;
float v_in = 5.0;
double r_inf;
double currentThermistorResistance;

AndroidAccessory acc("Manufacturer",
		     "Project08",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");

byte sntmsg[6];

void setup() {
  Serial.begin(19200);
  acc.powerOn();
  sntmsg[0] = COMMAND_TEMPERATURE;
  sntmsg[1] = INPUT_PIN_0;
  r_inf = r0 * (exp((-beta) / t0));
}

void loop() {
  if (acc.isConnected()) {
    int currentADCValue = analogRead(INPUT_PIN_0);
    
    float voltageMeasured = getCurrentVoltage(currentADCValue);
    double currentThermistorResistance = getCurrentThermistorResistance(voltageMeasured);
    double currentTemperatureInDegrees = getCurrentTemperatureInDegrees(currentThermistorResistance);
    
    // multiply the float value by 10 to retain one value behind the decimal point before converting
    // to an integer for better value transmission
    int convertedValue = currentTemperatureInDegrees * 10;
    
    sntmsg[2] = (byte) (convertedValue >> 24);  
    sntmsg[3] = (byte) (convertedValue >> 16);  
    sntmsg[4] = (byte) (convertedValue >> 8);  
    sntmsg[5] = (byte) convertedValue;
    acc.write(sntmsg, 6);
    delay(100);
  }
}

// "reverse ADC calculation"
float getCurrentVoltage(int currentADCValue) {
  return v_in * currentADCValue / 1024;
}

// rearranged voltage divider formula for thermistor resistance calculation
double getCurrentThermistorResistance(float voltageMeasured) {
  return ((v_in * additional_resistor) - (voltageMeasured * additional_resistor)) / voltageMeasured; 
}

//Steinhart-Hart B equation for temperature calculation
double getCurrentTemperatureInDegrees(double currentThermistorResistance) {
  return (beta / log(currentThermistorResistance / r_inf)) - 273.15; 
}


