package pro.dbro.timelapse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;

public class CameraUtils {
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "CameraUtils";
	
	// Called by Camera when a picture's data is ready for processing
	// Restart Camera preview after snapping, and set just-captured photo as overlay
	public static PictureCallback mSavePicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {

	        File pictureFile = FileUtils.getOutputMediaFile(FileUtils.MEDIA_TYPE_IMAGE);
	        if (pictureFile == null){
	            Log.d(TAG, "Error creating media file, check storage permissions");
	            return;
	        }

	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	        } catch (FileNotFoundException e) {
	            Log.d(TAG, "File not found: " + e.getMessage());
	        } catch (IOException e) {
	            Log.d(TAG, "Error accessing file: " + e.getMessage());
	        }
	        Log.d(TAG,"Picture saved: " + pictureFile.getAbsolutePath());
	        TimeLapseActivity.setCameraOverlay(pictureFile.getAbsolutePath());
	        
	    }
	};
	
	// Called by Camera on shutter action, but before picture's data is ready
	public static ShutterCallback mShutterFeedback = new ShutterCallback(){

		@Override
		public void onShutter() {
			// Yield to theh activity to display shutter feedback
			TimeLapseActivity.showShutterFeedback();
		}
		
	};
	
	

}
