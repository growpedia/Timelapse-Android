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
	
	// takes TimeLapse id argument to allow direct writing of picture to proper directory
	public static class TimeLapsePictureCallback implements PictureCallback{
		private int timelapse_id = -1;
		
		public TimeLapsePictureCallback(int timelapse_id){
			this.timelapse_id = timelapse_id;
		}
		
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new FileUtils.SavePictureOnFilesystem(timelapse_id).execute(data);
			
		}
	}
	
	// Called by Camera on shutter action, but before picture's data is ready
	public static ShutterCallback mShutterFeedback = new ShutterCallback(){

		@Override
		public void onShutter() {
			// Yield to theh activity to display shutter feedback
			CameraActivity.showShutterFeedback();
		}
		
	};
	
	

}
