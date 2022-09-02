## Description
**sphero-android** is a program and library I am working on to control sphero devices from an Android phone.

I started this project because I wanted to study Bluetooth LE communication and control the sphero toy without a custom interface. I also wanted to explore other ways of controlling sphero devices (such as using a video game controller). It has also been a method for me to learn Android development.

Much of the Bluetooth communication protocol code is ported from this Python sphero library: https://github.com/MProx/Sphero_mini

## Features
- Connection to spheros via MAC address
- Sphero Mini controller
  - Touchscreen joystick for movement
  - LED color and brightness configuration
  - Gamepad controller (Nintendo Switch Pro Controller. Other bluetooth controllers will work, but the button mappings will be wrong.)
  - Queue-based structure for processing commands with options for waiting for acknoweldgements, resending missed commands, and discarding similar queued commands when too many are sent

## Files
- *MainActivity*: Entry point for the control application.
- *SpheroMiniActivity*: Front-end for communicating with the sphero library to connect to and control the ball.
- *SpheroController*: Android Java class for controlling a sphero mini.

## Goals
- Options for reading sensor data from a sphero
- Sphero Bolt control
