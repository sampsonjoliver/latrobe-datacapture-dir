latrobe-datacapture-dir
=======================

# Flexion Angle Analysis using Android and Inertial Sensors

Flexion angle data capture system on Android, supported by an analytic web application.

This project is written in collaboration with La Trobe University as a part a research project conducted by the Health Sciences and Computer Sciences. The project aims to provide proof-of-concept of using commercially available Android devices, equipped with inertial and positioning sensors, in order to determine differences in relative positioning and orientation between each device. Specifically, the project is interested in determining angular differences between the orientation of each device in order to calculate the angle of the "bend" (flexion/extension) of a selected skeletal joint, by attaching a device on either side of the joint.

This repository includes the source files used in implementing a prototype for the project. The files are broken into two components: the client sensor node, which is an Android client application, and a webserver, which is an Express js web application server.

The project presents a complete outline of the research, design, and implementation of the system in the accompanying documentation, including a detailed specification of each component.

## INSTRUCTIONS FOR USE

### Client Sensor Network Application.

The client sensor application targets Android API level 14 and above. Deployment requires that the Android SDK is installed and the relevant SDK Platform (API level 14 or above) is installed. For more instructions on this, see the relevant Android documentation (https://developer.android.com/sdk/installing/index.html)

Included are the project files for use in IntelliJ IDEA Studio or Android Studio.

The project may then be built and deployed to any Android device. Running the application requires two devices to have the application installed. One device must run the Data Collection Client app and another must run the Data Collection Master app.

### Web Application Server

The webserver is built in Express js and requires the use of Node.js. Deployment requires that both Node.js runtime environment and the Node Package Manager (NPM) are installed. For more information on this, see the relevant Node.js documentation (http://nodejs.org/). The project defines its further package dependencies in the package.json file within the project root directory. These may be installed using a platform toolkit such as IntelliJ automatically (the relevant .idea files for launching the project in IntelliJ IDEA are included), or else installed manually via npm using the following command from the root directory of the project:
> `npm install`

Before deployment, the webserver requires a MongoDB database instance to connect to. For more information on how to install and run a MongoDB instance, see the relevant documentation (http://www.mongodb.org/). Once an instance is configured, the server may be setup to use the database by performing the following steps:

1. Open the file config/config.js

2. Under module.exports = { ... }, set the username and password fields of both the test and development variables

3. Modify the db field of both the test and development vars to match the mongodb connection url of your mongodb instance

The web application may be deployed either via IntelliJ IDEA, or else manually via the commandline by executing the following command from the root directory of the project:
> `node bin\www`

The server is now running. The server UI environment can be reached by connecting to localhost:3000/
