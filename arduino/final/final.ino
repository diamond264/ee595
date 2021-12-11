#include <Arduino_LSM9DS1.h>
#include <ArduinoBLE.h>
/* Bluetooth */
typedef struct LocationMeasurement {
  byte flag = 0b00000000; // Refer to the "GATT Specification Supplement" for more detail
  byte location[12]; // Byte-array used instead of float, to resolve 4-byte aligning issue
} LocationMeasurement;
LocationMeasurement loc;

typedef struct OrientationMeasurement {
  byte flag = 0b00000000; // Refer to the "GATT Specification Supplement" for more detail
  byte orientation[12]; // Byte-array used instead of float, to resolve 4-byte aligning issue
} OrientationMeasurement;
OrientationMeasurement orm;

BLEService YoutubeService("1809");

BLECharacteristic LocationMeasurementChar("2A1C", BLERead | BLENotify, sizeof(loc), true);
byte cccd_value[2]= {0b00000000, 0b00000010}; 
BLEDescriptor cccd("2902", cccd_value, 2); // UUID of CCCD is 2902

BLECharacteristic OrientationMeasurementChar("2A1E", BLERead | BLENotify, sizeof(orm), true);
byte cccd_value_2[2]= {0b00000000, 0b00000001};
BLEDescriptor cccd_2("2902", cccd_value_2, 2);

void blePeripheralConnectHandler(BLEDevice central) {
  // central connected event handler
  Serial.print("Connected event, central: ");
  Serial.println(central.address());
}

// Simple handler for the disconnect event
void blePeripheralDisconnectHandler(BLEDevice central) {
  // central disconnected event handler
  Serial.print("Disconnected event, central: ");
  Serial.println(central.address());
}

unsigned long microTime_old, microTime, microTime_delta;

float gx, gy, gz;
float rotate_x, rotate_y, rotate_z;
double vx, vy, vz;
double vx_add, vy_add, vz_add;
double pos_vx, pos_vy, pos_vz;
double x, y, z;
double accum_x, accum_y, accum_z;
double accum_gx, accum_gy, accum_gz;
int n;
float ax, ay, az;
float ax_offset, ay_offset, az_offset;
double ax_temp, ay_temp, az_temp;
const double rotate_coeff = 1000000 * (2*PI/360);

float accAngleX = 0, accAngleY = 0, gyroAngleX = 0, gyroAngleY = 0, gyroAngleZ = 0;
float roll, pitch, yaw; 

bool start_flag;

void setup() {
  pinMode(22, OUTPUT); // RED
  pinMode(23, OUTPUT); // GREEN
  pinMode(24, OUTPUT); // BLUE
  pinMode(25, OUTPUT);
  
  // put your setup code here, to run once:
  Serial.begin(9600);
  while (!Serial)
    Serial.println("Serial ON");

  if(!BLE.begin())
  {
    Serial.println("BLE start failed?");
  }

   BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);
  
  // Set "Generic Access Profile" characteristics  
  BLE.setLocalName("EE595_Arduino");
  BLE.setDeviceName("EE595_Arduino"); // UUID 2A00
  BLE.setAppearance(0x0543); // UUID 2A01

  
  // Characteristics with "Indicate" property has a CCCD descriptor already installed. To change the value, the only possible way is to completely replace it
  LocationMeasurementChar.descriptor("2902") = cccd;
  // Characteristics with "Notify" property has a CCCD descriptor already installed. To change the value, the only possible way is to completely replace it
  OrientationMeasurementChar.descriptor("2902") = cccd_2;

  YoutubeService.addCharacteristic(LocationMeasurementChar); // Mandatory. Refer to the "Health Thermometer Service" document for more detail
  YoutubeService.addCharacteristic(OrientationMeasurementChar); // Optional. Refer to the "Health Thermometer Service" document for more detail

  BLE.addService(YoutubeService);
  BLE.setAdvertisedService(YoutubeService);
  BLE.advertise();
  
  if (!IMU.begin())
  {
    Serial.println("IMU failed");
    while (1);
  }

  IMU.setContinuousMode();


  // Initialize Variables
  rotate_x = 0;
  rotate_y = 0;
  rotate_z = 0;
  vx = 0;
  vy = 0;
  vz = 0;
  x = 0;
  y = 0;
  z = 0;
  start_flag = true;
  while (!IMU.accelerationAvailable())
    IMU.readAcceleration(ax_offset, ay_offset, az_offset);

  microTime_old = micros();
}

void loop() {

  BLEDevice central = BLE.central();

  if (central){
    while(central.connected()){
      digitalWrite(22, HIGH);
      digitalWrite(23, LOW);
      digitalWrite(24, LOW);
      
      n = IMU.accelerationAvailable();
      while(n < 10){
        n = IMU.accelerationAvailable();
      }
      
      microTime = micros();
      microTime_delta = microTime - microTime_old;
      microTime_old = microTime;

      if(start_flag){
        start_flag = false;
        continue;
      }
      
      IMU.readAccelerationGyroMultiple(ax, ay, az, gx, gy, gz, n);

      ax = -ax;
      gx = -gx;
      accAngleX = (atan(ay / sqrt(pow(ax, 2) + pow(az, 2))) * 180 / PI);
      accAngleY = (atan(-1 * ax / sqrt(pow(ay, 2) + pow(az, 2))) * 180 / PI);

      gyroAngleX = gyroAngleX + gx * microTime_delta / rotate_coeff;
      gyroAngleY = gyroAngleY + gy * microTime_delta / rotate_coeff;
      
      
    
    //  rotate_x = rotate_x + gx * microTime_delta / rotate_coeff;
    //  rotate_y = rotate_y + gy * microTime_delta / rotate_coeff;
    //  rotate_z = rotate_z + gz * microTime_delta / rotate_coeff;
    //
    //  // Remove roll
    //  ax = -ax;
    //  ax_temp = ax;
    //  ay_temp = ay * cos(rotate_x) - az * sin(rotate_x);
    //  az_temp = ay * sin(rotate_x) + az * cos(rotate_x);
    //  ax = ax_temp;
    //  ay = ay_temp;
    //  az = az_temp;
    //  
    //  // Remove pitch
    //  ax_temp = ax * cos(rotate_y) + az * sin(rotate_y);
    //  ay_temp = ay;
    //  az_temp = - ax * sin(rotate_y) + az * cos(rotate_y);
    //  ax = ax_temp;
    //  ay = ay_temp;
    //  az = az_temp;
    //
    //  // Remove yaw
    //  ax_temp = ax * cos(rotate_z) - ay * sin(rotate_z);
    //  ay_temp = ax * sin(rotate_z) + ay * cos(rotate_z);
    //  az_temp = az;
    //  ax = ax_temp;
    //  ay = ay_temp;
    //  az = az_temp;
    
      
      ax -= ax_offset;
      ay -= ay_offset;
      az -= az_offset;
      
    
      vx_add = 4.9 * ax * microTime_delta / 1000000.0;
      vy_add = 4.9 * ay * microTime_delta / 1000000.0;
      vz_add = 4.9 * az * microTime_delta / 1000000.0;
    
      pos_vx = vx + vx_add;
      pos_vy = vy + vy_add;
      pos_vz = vz + vz_add; 
    
      vx = pos_vx + vx_add;
      vy = pos_vy + vy_add;
      vz = pos_vz + vz_add;
    
      float thres = gx*gx + gy*gy + gz*gz;
      if (thres < 200)
        {
          
          ax_offset = ax_offset + ax;
          ay_offset = ay_offset + ay;
          az_offset = az_offset + az;
          
          pos_vx = 0;
          pos_vy = 0;
          pos_vz = 0;
    
          vx = 0;
          vy = 0;
          vz = 0;
    
          rotate_x = 0;
          rotate_y = 0;
          rotate_z = 0;

          gyroAngleX = 0;
          accAngleX = 0;
          gyroAngleY = 0;
          accAngleY = 0;
          gz = 0;
        }
      else if (thres > 30000)
      {
          pos_vx = 0;
          pos_vy = 0;
          pos_vz = 0;
    
          vx = 0;
          vy = 0;
          vz = 0;
    
          rotate_x = 0;
          rotate_y = 0;
          rotate_z = 0;
      }

      yaw =  yaw + gz * microTime_delta / rotate_coeff;
      roll = 0.96 * gyroAngleX + 0.04 * accAngleX;
      pitch = 0.96 * gyroAngleY + 0.04 * accAngleY;
    
      x = x + pos_vx * microTime_delta / 1000000.0;
      y = y + pos_vy * microTime_delta / 1000000.0;
      z = z + pos_vz * microTime_delta / 1000000.0;

      *(float*)&orm.orientation[0] = (float)yaw;
      *(float*)&orm.orientation[4] = (float)roll;
      *(float*)&orm.orientation[8] = (float)pitch;

      *(float*)&loc.location[0] = (float)x;
      *(float*)&loc.location[4] = (float)y;
      *(float*)&loc.location[8] = (float)z;

      LocationMeasurementChar.writeValue((uint8_t*)&loc, sizeof(loc));
      OrientationMeasurementChar.writeValue((uint8_t*)&orm, sizeof(orm));
  
  
    Serial.print(yaw);
    Serial.print('\t');
    Serial.print(roll);
    Serial.print('\t');
    Serial.println(pitch);
  
      // Serial.println(y*100);
    }
  }else {
    digitalWrite(22, LOW);
    digitalWrite(23, HIGH);
    digitalWrite(24, HIGH);
  }

}
