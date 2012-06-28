Timelapse-Android
=================

A timelapse tool for Android devices running Android 2.2 or later.

Architecture Overview
---------------------

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
+ Custom ListView SimpleAdapter with notifyDataSetChanged implemented
+ b/c BrowserActivity is now launchmode:singleTask, implement ActionBar Home behavior for TimeLapseViewer


? Does editing the TimeLapse title/desc change the modified_date?