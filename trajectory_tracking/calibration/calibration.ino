#include <Arduino_LSM9DS1.h>

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  while(!Serial)
  Serial.println("Serial ON");

  if(!IMU.begin())
  {
    Serial.println("IMU failed");
    while(1);
  }

  IMU.setContinuousMode();
}

void loop() {
  float GyroX, GyroY, GyroZ, AccX, AccY, AccZ;
  int c;
  int loop_cnt = 500;
  c=0;
  float GyroErrorX = 0, GyroErrorY = 0, GyroErrorZ = 0;
  // put your main code here, to run repeatedly:
  while (c < loop_cnt) {
     int n = IMU.accelerationAvailable();
      while(n < 5){
        n = IMU.accelerationAvailable();
      }
      

    IMU.readAccelerationGyroMultiple(AccX, AccY, AccZ, GyroX, GyroY, GyroZ, n);
    GyroErrorX = GyroErrorX + GyroX;
    GyroErrorY = GyroErrorY + GyroY;
    GyroErrorZ = GyroErrorZ + GyroZ;
    c++;
  }
  //Divide the sum by 200 to get the error value
  GyroErrorX = GyroErrorX / loop_cnt;
  GyroErrorY = GyroErrorY / loop_cnt;
  GyroErrorZ = GyroErrorZ / loop_cnt;

  Serial.print("GyroErrorX: ");
  Serial.println(GyroErrorX);
  Serial.print("GyroErrorY: ");
  Serial.println(GyroErrorY);
  Serial.print("GyroErrorZ: ");
  Serial.println(GyroErrorZ);
}
