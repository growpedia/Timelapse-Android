Timelapse-Android
=================

A timelapse tool for Android devices running Android 2.2 or later.

Requirements
------------
All requirements are included with the project in `./libs`

+ Gson 2.2.1
  + JSON translation
+ android-support-library-v4
  + CursorLoader for automatic, asynchronous db-listview interaction in Android 2.2

Configuration (Eclipse w/ADT)
------------
	
1. Ensure android-support-library-v4 and gson are in the project build path

		project properties -> Java Build Path -> Libraries (Tab)
	
	Ensure **android-support-v4.jar** and **gson-2.2.1.jar** are present. If they are not, click  "Add JARs" and select them from <TimeLapse root>/libs


Architecture Overview
---------------------
### TimeLapseApplication ###
+ Global application state
+ HashMap of [timelapse_id] -> [TimeLapse Object]

### BrowserActivity ###
+ The main activity which begins the application. Presents ListView of TimeLapses after reading application state from storage.

### TimeLapseViewerActivity ###
+ Activity which manages viewing, modifying, and creating TimeLapses.

### CameraActivity ###
+ The camera view controller. It safely obtains, manages, and releases
an instance of the system Camera. 
+ Preview imagery is displayed via CameraPreview, which is passed the instance of the system Camera
+ The shutter listener (shutterListener() ) is attached to the root RelativeLayout described in main.xml 
+ Shutter feedback via showShutterFeedback()
+ Overlay the previously taken photo on the live camera preview via setCameraOverlay()
  
### CameraPreview ###
+ The view displaying live camera data. Passed system Camera instance.
   
### CameraUtils ###
+ Camera callback methods which reference TimeLapseActivity methods (i.e: Camera shutter callback - > TimeLapseActivity.showShutterFeedback())
   
### FileUtils ###
+ Generic photo/video writing to sdcard
+ Read/Write application state from/to external storage




TODO
----
+ Delete images within TimeLapse
+ Video/.gif export via ffmpeg
+ Improved TimeLapse viewing UI
+ Stability - proper camera releasing?
+ Improved performance of camera overlay setting
