Timelapse-Android
=================

A timelapse tool for Android devices running Android 2.2 or later.

Requirements
------------
+ Gson 2.2.1 for JSON translation
+ android-support-library-v4
+ ActionBarSherlock 4.1.0

Installation (Eclipse w/ADT)
------------

1. Download ActionBarSherlock
		
		http://actionbarsherlock.com/download.html
		
2. Create a library project in Eclipse (for ActionBarSherlock)
		
		File -> New -> Project… -> Android Project from Existing Code
   Select ActionBarSherlock's **/library** folder as the "Root Directory".

3. Add the ActionBarSherlock library project to the TimeLapse project build path

		project properties -> Android -> Add (In Library Pane)
	Select the ActionBarSherlock library project.
	
4. Ensure android-support-library-v4 and gson are in the project build path

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
+ Video export via ffmpeg
+ Stability - proper camera releasing?
+ Improved performance of camera overlay setting
+ b/c BrowserActivity is now launchmode:singleTask, implement ActionBar Home behavior for TimeLapseViewer
+ Remove TimeLapseApplication for thread safety?


? Does editing the TimeLapse title/desc change the modified_date?