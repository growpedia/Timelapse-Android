package pro.dbro.timelapse;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    
    private final String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            tryAutoFocus();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        
        List supportedSizes = mCamera.getParameters().getSupportedPictureSizes();
        Camera.Parameters parameters = mCamera.getParameters();
        // Set autoFocus mode
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        // Attempt to set preview resolution
        parameters.setPreviewSize(((Camera.Size)supportedSizes.get(8)).width, ((Camera.Size)supportedSizes.get(8)).height);
        Log.d("onSurfaceChanged","width: "+ String.valueOf(((Camera.Size)parameters.getPreviewSize()).width) + " x "+ String.valueOf(((Camera.Size)parameters.getPreviewSize()).height));
        mCamera.setParameters(parameters);
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            tryAutoFocus();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    
    private void tryAutoFocus(){
    	if(mCamera.getParameters().getFocusMode().compareTo("FOCUS_MODE_AUTO") == 0 ||  
    			mCamera.getParameters().getFocusMode().compareTo("FOCUS_MODE_MACRO") == 0){
    		mCamera.autoFocus(null);
    	}
    }
}
