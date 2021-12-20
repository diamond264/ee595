# 1. Trajectory tracking
Arduino code for trajectory tracking is in /trajectory_tracking.

## 1.1 Change of the Arduino LSM9DS1 library
To utilize more sampling frequency, we modified the LSM9DS1 library.
We attached _LSM9DS1.cpp_ and _LSM9DS1.h_ with the Arduino source file.
File path for library file is *Documents/Arduino/libraries/Arduino_LSM9DS1/src*.
Please replace the existing library files with our files to test the code.

## 1.2 Gyroscope calibration
We calibrate the gyroscope using /trajectory_tracking/calibration.
This code measures the gyroscope error. 
Please put Arduino in a steady state and execute the code.
Then, the measured error is displayed in the Serial monitor.
Put the error to the float variables in arduino code at
/trajectory_tracking/trajectory_tracking_arduino.
There are three float variables *gyro_x_const, gyro_y_const, gyro_z_const*.

## 1.3 Running trajectory tracking
Finally, you can run trajectory tracking using /trajectory_tracking/trajectory_tracking_arduino.
We display the distance with sqrt(x^2+y^2+z^2).

# 2. Mobile Service
We put the code for the mobile service at /mobile service.
There are codes for arduino, smartphone, and the laptop.

##2.1 Arduino code
Please use the same LSM9DS1 library file mentioned at 1.1.
And the calibration procedure for the mobile service is same with 1.2.
Please calibrate the gyroscope constants in final.ino

##2.2 Android code
APK for android code is /mobile service/apk-release.apk.
We tested on Galaxy S21 Ultra 5G. 
If the APk is not working, please use the android code at /mobile service/EE595Android.

## 2.3. Setting physical devices for use
In the in-the-wild environment, the trajectory inferred through the IMU sensor value is very unstable. We assume that the positions of the phone and monitor are fixed in order to minimize the instability. In the current code, it is assumed that the monitor is positioned 40cm in front of the Arduino, and the phone is positioned 40cm to the right from the Arduino. Before executing the Python script, all devices should be fixed in the mentioned locations.
## 2.4. Firebase token
We use Firebase for communication between Python scripts and Chrome extension scripts. We placed the api key in the directory and implemented the code to automatically generate the api token in the script. The api key will not be destroyed until the end of the course, but if there is a security issue with the firebase api key or the key automatically expires, communication between the chrome extension and the python script becomes impossible. There is a part about the firebase setting in the Python script and chrome extension script, so please find and fix it if necessary.
## 2.5. Installing & executing chrome extension script
To install our chrome extension script in your chrome, please go to chrome://extensions/. Click on Load Unpacked and select web/chrome_extension folder. Then it will be installed as a local extension in your chrome. Enable it by switching the button in the installed extension box. If the current page of your chrome starts with "https://www.youtube.com/watch?", the script will be automatically executed. *If the script does not work, please refresh your link.*
## 2.6. Running android and arduino code
Before running the python script, please run the android and arduino code beforehand.
They will broadcast themselves with the bluetooth.

## 2.7. Executing python script
Our python script is implemented assuming Python 3.9. 
To execute our script, first install all required pip packages in requirements.txt. 
By executing web/restAPIConnector.py, you can execute the python client which communicates with the sensors and generates user context summary based on the transmitted trajectory data.
If the python script is connected to arduino and android, it will print 'connected to - device'.
Since the bluetooth connection is not stable, we tried multiple times to connect with android and arduino at the same time.
If the python script is not connected to both devices for 10 seconds, please re-run the python script.
