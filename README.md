## Description
**sphero-android** is a library I am working on to control sphero devices from an Android phone.

I started this project because I wanted to study Bluetooth LE communication and control the sphero toy without the manufacturer's kind-of-bloated app. I also wanted to explore other ways of controlling sphero devices (such as using a video game controller).

Much of the Bluetooth communication protocol code is ported from this Python sphero library: https://github.com/MProx/Sphero_mini

This repository is very much a work in progress and may not work at any given time.

## Documentation
- *MainActivity*: Entry point for the control application.
- *SpheroMiniActivity*: Front-end for communicating with the sphero library to connect to and control the ball.
- *SpheroMiniController*: Android Java library for controlling a sphero mini.

## Goals
- Customizable Sphero Mini control on an Android device (in progress)
  - Robust API that accounts for dropped packets and commands being called too quickly
  - Sensor feedback
- Bluetooth gamepad integration with Nintendo Switch Pro Controller
- Full Sphero Bolt control
