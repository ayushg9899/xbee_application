# xbee_application
ANDROID APPLICATION TO RECEIVE AND TRANSMIT DATA THROUGH XBEE MODULE.

# INTRODUCTION

The android application is made to connect an Android device to Xbee module and transmit given data through Xbee series 2 router module to coordinator module. The smatphone is connected to a coordinator module which receives and transmits data to other xbee (router) modules.
The app contains a simple GUI which provide step by step instruction to establish  connection to the module. The basic layout of application include :-
    • connecting to the xbee module
    • scanning for all the devices nearby and connecting to one of those devices’ PAN ID
    • scanning devices with same PAN ID
    • storing the MAC address and node identifier of selected device on same PAN ID to send data.

The app is made on Android Studio IDE using JAVA language. The minimum SDK version is set as 26 but it can be changed from build.gradle file.


# PREREQUISITES
One requires an android smartphone, an OTG cable and at least two xbee module to establish a connection between them and transfer data.
The xbee devices need to be set on same network.
You can use XCTU application to modify your xbee settings (This software is available for windows, linux and macOS).


# LIBRARIES USED

The two most important libraries used are android.hardware.usb library and github.felHR85.UsbSerial library. The former library is used to give android device the permission to connect to an USB Peripheral device while the latter one is used for serial communication between android device and connected USB peripheral device.
Various steps involved in establishing connection with xbee module to read and write data for communication with other xbee modules are:
    • Adding dependancies and repo to build.gradle file.
    • Using CreateUsbSerialDevice method to handle all serial port operations.
    • Checking if device is supported, device is connected and creating appropriate message for them if not.
The  github.felHR85.UsbSerial library library provide many features and options to user which you can find on its github page.

To know more about these libraries, refer the below links.

Link: https://developer.android.com/guide/topics/connectivity/usb?hl=en

Link: https://github.com/felHR85/UsbSerial

# Layout and Code

The GUI contains different layout for different tasks which are switched dynamically in java code.A single java file is used instead of creating different activity for all the layouts.
The first layout has a connect button to establish a connection between android device and xbee module. The 2nd layout has a single button to scan all the nearby devices and get their PAN IDs. The 3rd layout displays all the PAN IDs of devices present nearby. The user selects one of the displayed PAN IDs to change the connected xbee module’s PAN ID to selected PAN ID and so on.
The JAVA code has different functions for all the layouts. It also has an onreceive function to read data from the peripheral device. This feature was initially added to receive data from arduino but can be used with xbee modules too. Number of functions are made for different purposes and to keep the code clean.


# TRANSMISSION OF DATA

One cannot send data to xbee and expect to receive it on coordinator end normally. Data is transmitted through xbee in frames. 
 Reading data
Data received in xbee device connected to smartphones are in parts of frames(in frames of one or two hexadecimal byte).
To read these data as a whole frame I used the property of xbee data frame to store a single frame of data received in byte by byte.
I used an byte array b[] to store this data. The storing of data will start only when frame delimiter(7E in most cases) is received. Next two bytes of frame is size(length) of actual data. I used this feature to determine the  the length of frame and stop storing the data when frame’s checksum is received at position b[2]+4.
Now I decode this frame to information I need and use it. I also used the length of data(2nd and 3rd byte of data) to determine what kind of data i’ve received made different functions to handle different data.                                                                                                                                                                                                              
Sending data                                                                                                                          
To send data I created different frames manually for each type of command and sent it using serialport.write() funtion. I also  used XCTU software to help me create the frames.                                                                                                                                  
#.Refer Xbee module Documentation to know more about data transmission.
 
