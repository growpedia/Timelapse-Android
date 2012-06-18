package pro.dbro.timelapse;

import java.io.File;
import java.io.IOException;

import pro.dbro.timelapse.R.id;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class TimeLapseActivity extends Activity {
	private Camera mCamera;
	private static CameraPreview mCameraPreview;
	private static Context c;
	
	// ImageView overlayed on the Camera preview
	private static ImageView cameraOverlay;
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "TimeLapseActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        // Store context for use by static methods
        c = getApplicationContext();
        
        // TEST: make imageview transparent
        cameraOverlay = (ImageView) findViewById(id.camera_overlay);
        cameraOverlay.setAlpha(100);

        // Obtain camera instance
        mCamera = getCameraInstance();
        // Show alert if camera not available
        if (mCamera == null){
        	AlertDialog noCameraAlertDialog = new AlertDialog.Builder(c)
        		.setTitle("Your Title")
    			.setMessage("Click yes to exit!")
				.setNeutralButton("Okay!", new OnClickListener(){
					@Override
					public void onClick(DialogInterface thisDialog, int arg1) {
						// Cancel the dialog
						thisDialog.cancel();	
					}
				}).create();

				noCameraAlertDialog.show();
        }
        // If camera available, proceed
        else{
	        // Obtain SurfaceView for camera preview
	        mCameraPreview = new CameraPreview(this, mCamera);
	        FrameLayout preview = (FrameLayout) findViewById(id.camera_preview);
	        preview.addView(mCameraPreview);
	        
	        // Set shutter touch listener to layout
	        RelativeLayout container = (RelativeLayout) findViewById(id.container_layout);
	        container.setOnTouchListener(shutterListener);
        }
       
    }
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        	Log.d("getCameraInstance",e.toString());
        }
        return c; // returns null if camera is unavailable
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	// Release camera for other applications
    	releaseCamera();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	// If there is no camera instance, create one
    	if(mCamera == null){
    		Log.d("OnResume","mCamera null");
    		mCamera = getCameraInstance();
    	}
    	else{
    		Log.d("OnResume","mCamera not null");
    	}
    	
    }
    
    private OnTouchListener shutterListener = new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if ( (event.getAction()) == event.ACTION_DOWN ) {
				Log.d(TAG,"Taking picture...");
				// takePicture must be called after mCamera.startPreview()
				mCamera.takePicture(CameraUtils.mShutterFeedback, null, null, CameraUtils.mSavePicture);
				// Consume touch event
				return true;
            }
			return false;
		}
    };
    
    // Display the just-captured picture as the camera overlay
    public static void setCameraOverlay(String filepath){
    	// Decode the just-captured picture from file and display it 
    	// in the cameraOverlay ImageView
    	
    	File imgFile = new  File(filepath);
    	if(imgFile.exists()){
    	    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

    	    cameraOverlay.setImageBitmap(myBitmap);
    	    // Ensure camera_overlay is visible
    	    // visibility: 0 : visible, 1 : invisible, 2 : gone
        	cameraOverlay.setVisibility(0);
    	}
    	
    	mCameraPreview.restartPreview();
    }

	public static void showShutterFeedback() {
		CharSequence text = "Snap!";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(c, text, duration);
		toast.show();
		
	}
	
	// Release Camera when application is finished 
	private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
   
}