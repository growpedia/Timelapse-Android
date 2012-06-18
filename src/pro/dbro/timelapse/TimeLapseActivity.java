package pro.dbro.timelapse;

import java.io.IOException;

import pro.dbro.timelapse.R.id;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class TimeLapseActivity extends Activity {
	private Camera mCamera;
	private CameraPreview mCameraPreview;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        // TEST: make imageview transparent
        ImageView myImageView = (ImageView) findViewById(id.camera_overlay);
        myImageView.setAlpha(100);

        // Obtain camera instance
        mCamera = getCameraInstance();
       
        // Obtain SurfaceView for camera preview
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(id.camera_preview);
        preview.addView(mCameraPreview);
       
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
    	if(mCamera != null){
    		Log.d("OnPause","Releasing camera");
    		mCamera.stopPreview();
    		mCamera.release();
    	}
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	// If there is no camera instance, create one
    	if(mCamera == null){
    		Log.d("OnResume","mCamera null");
    	}
    	else{
    		Log.d("OnResume","mCamera not null");
    	}
    	mCamera = getCameraInstance();
    }
   
}