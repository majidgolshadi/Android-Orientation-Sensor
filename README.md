[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android--Orientation--Sensor-green.svg?style=flat)](https://android-arsenal.com/details/1/2107)
[![The MIT License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/majidgolshadi/Android-Orientation-Sensor/blob/master/LICENSE)

Android-Orientation-Sensor
==========================

Android Sensor Orientation Library helps you to get more accurate vector values of orientation, using all available device sensors.

Overview
========
In one of my project (parallax library for android application) ,i need to use orientaion sensor values but they are too noisy so I decided to create a native library for android plateform to return these values with less noise.
My purpose for sharing this code in public is to solve anybody who has this problem like me.
To do this I use Accelerometer, Gyroscope, Magnetic and Orientation sensors values.

Usage
=====
to use this library you can add codes to your project and easily use it.

Create it's object
------------------
```java
Orientation orientationSensor = new Orientation(this.getApplicationContext(), this);
```
`this` stands for a class who implements OrientationSensorInterface class

Turn sensor ON
--------------
```java
//------Turn Orientation sensor ON-------
// set tolerance for any directions
orientationSensor.init(1.0, 1.0, 1.0);

// set output speed and turn initialized sensor on
// 0 Normal
// 1 UI
// 2 GAME
// 3 FASTEST
orientationSensor.on(0);
```
first it must be initialized. You can set delta value for any direction you want(e.g 1.0).    
you can control your sensor pooling speed when you turn your sensor on


Turn sensor OFF
---------------
```java
orientationSensor.off();
```

if my introduction was not completely you can see TestActivity class that i put it in this project as a library usage sample.    
I hope to this library helpful for any body that use it.    
I'm realy happy to people who can help me to improve this code.
