##Packet sniffer

This is a small tool I wrote to parse the log files produced by the nRF Connect BLE sniffer. I looked at the packets sent/received by the official Sphero app to try to reverse engineer some of the commands they used.

This isn't meant to be run or used alongside the rest of the project. Is is just a python script that parses those log files and converts them into sequences of commands to study.

Some of the python code was taken straight from https://github.com/MProx/Sphero_mini 