Android-Orientation-Sensor
==========================

Android Sensor Orientation Library helps you to get more accurate vector values of orientation, using all available device sensors.

OverView
========
In one of my project (parrallax library for android application) ,i need to use orientaion sensor values but there are too noisy so i decided to create a native library for android plateform to return these values with less noisy.
My purpose for  share this code in public was to solve any body problem like me.
to do this i use Accelerometer, Gyroscope, Magnetic and Orientation sensors values.

Usage
=====
to use this library you can add my package to your project or and after that use it easily

Create it's object
------------------
```java
Orientation orientationSensor = new Orientation(this.getApplicationContext(), this);
```
this stand for a class who implements OrientationSensorInterface class

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
first it must be initialize. you can set after which delta value in any direction you want to get values.    
you can control your sensor pooling speed when you turn your sensor on


Turn sensor OFF
---------------
```java
orientationSensor.off();
```

if my introduction was not completely you can see TestActivity class that i put it in this project as a library usage sample.    
I hope to this library helpful for any body that use it.    
I'm realy happy to people who can help me to improve this code.
