#include <Arduino_LSM9DS1.h>

unsigned long microTime_old, microTime, microTime_delta;

float gx, gy, gz;
float vx, vy, vz;
float vx_add, vy_add, vz_add;
float pos_vx, pos_vy, pos_vz;
float x, y, z;
int n;
float ax, ay, az;
float ax_offset, ay_offset, az_offset;
float ax_temp, ay_temp, az_temp;
const float r2d = 180.0 / PI;
const float d2r = PI / 180.0;

float accAngleX = 0, accAngleY = 0, gyroAngleX = 0, gyroAngleY = 0, gyroAngleZ = 0;
float roll = 0, pitch = 0, yaw = 0; 
float p_roll = 0, p_pitch = 0, p_yaw = 0;
float e_roll = 0, e_pitch = 0, e_yaw = 0;
float p_ax = 0, p_ay = 0, p_az = 0;
float e_ax = 0, e_ay = 0, e_az = 0;

/**** calibration ****/
float gyro_x_const = 0;
float gyro_y_const = 0;
float gyro_z_const = 0;

int start_flag = 3;

void setup() {
  
  // put your setup code here, to run once:
  Serial.begin(9600);
  while(!Serial)
  Serial.println("Serial ON");
  
  if (!IMU.begin())
  {
    Serial.println("IMU failed");
    while (1);
  }

  IMU.setContinuousMode();


  // Initialize Variables
  vx = 0;
  vy = 0;
  vz = 0;
  x = 0;
  y = 0;
  z = 0;
  while (!IMU.accelerationAvailable())
    IMU.readAcceleration(ax_offset, ay_offset, az_offset);

  microTime_old = micros();
}

void loop() {

      n = IMU.accelerationAvailable();
      while(n < 5){
        n = IMU.accelerationAvailable();
      }
      microTime = micros();
      microTime_delta = microTime - microTime_old;
      microTime_old = microTime;

      IMU.readAccelerationGyroMultiple(ax, ay, az, gx, gy, gz, n);

      if(start_flag){
        x=0; y=0; z=0; roll = 0; yaw = 0; pitch = 0;
        start_flag--;
        return;
      }

      gx = gx - gyro_x_const;
      gy = gy - gyro_y_const;
      gz = gz - gyro_z_const;
      
      ax = -ax;
      gx = -gx;
    
      float thres = gx*gx + gy*gy + gz*gz;
      if (thres < 15)
        { 
          gy = 0;
          gx = 0;
          gz = 0;
        }
        
      yaw =  yaw + (gz * microTime_delta / 1000000.0) * d2r;
      roll = roll + (gx * microTime_delta / 1000000.0) * d2r;
      pitch = pitch + (gy * microTime_delta / 1000000.0) * d2r;

    e_pitch = (pitch + p_pitch)/2.0;
    e_roll = (roll + p_roll) / 2.0;
    e_yaw = (yaw + p_yaw)/2.0;
    
    // float e_rotated_gravity[3] = {-sin(e_pitch), cos(e_pitch)*sin(e_roll), cos(e_pitch)*cos(e_roll)};
    float e_rotated_gravity[3] = {-cos(e_yaw)*sin(e_pitch)*cos(e_roll) + sin(e_yaw)*sin(e_roll), 
                                  sin(e_yaw)*sin(e_pitch)*cos(e_roll) + cos(e_yaw)*sin(e_roll),
                                  cos(e_pitch)*cos(e_roll)};

     p_pitch = pitch;
     p_roll = roll;
     p_yaw = yaw;
    
     ax -= e_rotated_gravity[0];
     ay -= e_rotated_gravity[1];
     az -= e_rotated_gravity[2];

     if (thres < 15)
        { 
          ax_offset = ax;
          ay_offset = ay;
          az_offset = az;
        }

        ax -= ax_offset;
        ay -= ay_offset;
        az -= az_offset;

        e_ax = p_ax + (ax-p_ax)/2.0;
        e_ay = p_ay + (ay-p_ay)/2.0;
        e_az = p_az + (az-p_az)/2.0;

        p_ax = ax;
        p_ay = ay;
        p_az = az;

      vx_add = 4.9 * e_ax * microTime_delta / 1000000.0;
      vy_add = 4.9 * e_ay * microTime_delta / 1000000.0;
      vz_add = 4.9 * e_az * microTime_delta / 1000000.0;
    
      pos_vx = vx + vx_add;
      pos_vy = vy + vy_add;
      pos_vz = vz + vz_add; 
    
      vx = pos_vx + vx_add;
      vy = pos_vy + vy_add;
      vz = pos_vz + vz_add;

      if (thres < 15)
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

      Serial.println(sqrt(x*x + y*y + z*z)*100);

    //String result = String(x*100) + "\t" + String(y*100) + "\t" + String(z*100);
    // Serial.println(result);
  
//    Serial.print(yaw * r2d);
//    Serial.print('\t');
//    Serial.print(roll * r2d);
//    Serial.print('\t');
//    Serial.println(pitch * r2d);
//    Serial.print('\t');
//    Serial.print(ax);
//    Serial.print('\t');
//    Serial.print(ay);
//    Serial.print('\t');
//    Serial.println(az);
//
//    Serial.print(x*100);
//    Serial.print('\t');
//    Serial.print(y*100);
//    Serial.print('\t');
//    Serial.println(z*100);

}
