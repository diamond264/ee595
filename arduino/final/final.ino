#include <Arduino_LSM9DS1.h>
#include <ArduinoBLE.h>
/* Bluetooth */
typedef struct LocationMeasurement {
  byte flag = 0b00000000; // Refer to the "GATT Specification Supplement" for more detail
  byte location[24]; // Byte-array used instead of float, to resolve 4-byte aligning issue
} LocationMeasurement;
LocationMeasurement loc;

BLEService YoutubeService("1809");

BLECharacteristic LocationMeasurementChar("2A1C", BLERead | BLENotify, sizeof(loc), true);
byte cccd_value[2]= {0b00000000, 0b00000010}; 
BLEDescriptor cccd("2902", cccd_value, 2); // UUID of CCCD is 2902


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
float vx, vy, vz;
float vx_add, vy_add, vz_add;
float pos_vx, pos_vy, pos_vz;
float x, y, z;
int n;
float ax, ay, az;
float ax_offset, ay_offset, az_offset;
float ax_temp, ay_temp, az_temp;
const float rotate_coeff = 1000000.0 * (2.0*PI/360.0);

float accAngleX = 0, accAngleY = 0, gyroAngleX = 0, gyroAngleY = 0, gyroAngleZ = 0;
float roll = 0, pitch = 0, yaw = 0; 

bool start_flag;

void setup() {
  pinMode(22, OUTPUT); // RED
  pinMode(23, OUTPUT); // GREEN
  pinMode(24, OUTPUT); // BLUE
  pinMode(25, OUTPUT);
  
  // put your setup code here, to run once:
  Serial.begin(9600);

  if(!BLE.begin())
  {
    Serial.println("BLE start failed?");
  }

   BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);
  
  // Set "Generic Access Profile" characteristics  
  BLE.setLocalName("EE595B_Arduino");
  BLE.setDeviceName("EE595B_Arduino"); // UUID 2A00
  BLE.setAppearance(0x0543); // UUID 2A01

  
  // Characteristics with "Indicate" property has a CCCD descriptor already installed. To change the value, the only possible way is to completely replace it
  LocationMeasurementChar.descriptor("2902") = cccd;

  YoutubeService.addCharacteristic(LocationMeasurementChar); // Mandatory. Refer to the "Health Thermometer Service" document for more detail

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

      
      IMU.readAccelerationGyroMultiple(ax, ay, az, gx, gy, gz, n);

      if(start_flag){
        start_flag = false;
        continue;
      }

      gx = gx - 2.5;
      gy = gy + 13.25;
      gz = gz - 0.75;
      
      ax = -ax;
      gx = -gx;
    
      float thres = gx*gx + gy*gy + gz*gz;
      if (thres < 200)
        { 
          gy = 0;
          gx = 0;
          gz = 0;
        }
        
      yaw =  yaw + gz * microTime_delta / rotate_coeff;
      roll = roll + gx * microTime_delta / rotate_coeff;
      pitch = pitch + gy * microTime_delta / rotate_coeff;

//      pitch, yaw, roll
//      float rotation_matrix[3][3] = {{cos(pitch)*cos(yaw), cos(pitch)*sin(yaw)*sin(roll)-sin(pitch)*cos(roll), cos(pitch)*sin(yaw)*cos(roll) + sin(pitch)*sin(roll)},
//                                      {sin(pitch)*cos(yaw), sin(pitch)*sin(yaw)*sin(roll)-cos(pitch)*cos(roll), sin(pitch)*sin(yaw)*cos(roll) + cos(pitch)*sin(roll)},
//                                         {-sin(yaw), cos(yaw)*sin(roll), cos(yaw)*cos(roll)}};

     float d2r = PI/180.0;
     float rotated_gravity[3] = {-sin(pitch*d2r/56), cos(pitch*d2r/56)*sin(roll*d2r/56), cos(pitch*d2r/56)*cos(roll*d2r/56)};

     ax -= rotated_gravity[0];
     ay -= rotated_gravity[1];
     az -= rotated_gravity[2];

     if (thres < 200)
        { 
          ax_offset = ax;
          ay_offset = ay;
          az_offset = az;
        }

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

      if (thres < 200)
        { 
          pos_vx = 0;
          pos_vy = 0;
          pos_vz = 0;
    
          vx = 0;
          vy = 0;
          vz = 0;
        }
    
      x = x + pos_vx * microTime_delta / 1000000.0;
      y = y + pos_vy * microTime_delta / 1000000.0;
      z = z + pos_vz * microTime_delta / 1000000.0;

      *(float*)&loc.location[0] = (float)x*100;
      *(float*)&loc.location[4] = (float)y*100;
      *(float*)&loc.location[8] = (float)z*100;
      *(float*)&loc.location[12] = (float)pitch / 56;
      *(float*)&loc.location[16] = (float)yaw / 56;
      *(float*)&loc.location[20] = (float)roll / 56;

      LocationMeasurementChar.writeValue((uint8_t*)&loc, sizeof(loc));
  
  
//    Serial.print(yaw/56);
//    Serial.print('\t');
//    Serial.print(roll/56);
//    Serial.print('\t');
//    Serial.print(pitch/56);
//    Serial.print('\t');
//    Serial.print(ax);
//    Serial.print('\t');
//    Serial.print(ay);
//    Serial.print('\t');
//    Serial.println(az);

    Serial.print(x*100);
    Serial.print('\t');
    Serial.print(y*100);
    Serial.print('\t');
    Serial.println(z*100);
    }
  }else {
    digitalWrite(22, LOW);
    digitalWrite(23, HIGH);
    digitalWrite(24, HIGH);
  }

}
