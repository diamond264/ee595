#Team 9 Project
###1. Change of the Arduino LSM9DS1 library
To utilize more sampling frequency, we modified the LSM9DS1 library. 
We attached _LSM9DS1.cpp_ and _LSM9DS1.h_ with the Arduino source file.
File path for library file is *Documents/Arduino/libraries/Arduino_LSM9DS1/src*. 
Please replace the existing library files with our files to test the code.

###2. Arduino code
Except for the library file, we don't need any callibration steps.
You can just run Arduino source file to test our code.
From the figures from the provided announcements, we thought that just measuring the y-axis value would be sufficient.
So we just print out y-axis measurement to the serial monitor. 
Currently, printing x-axis and z axis are commented out.
If those values are needed for the evaluation, you can use it.