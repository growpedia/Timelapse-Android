package pro.dbro.timelapse;

import java.io.File;
import java.util.List;

import pro.dbro.timelapse.R.id;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressLint("NewApi")
public class CameraActivity extends Activity {

	// For testing: Force use of front camera
	final static boolean TEST_FRONT_CAMERA = false;

	// Is the current camera front facing? Pass this to the picture
	// saving method to compensate for horizontal mirroring
	public static boolean CAMERA_FRONT_FACING = false;

	// Is the location of this activity locked?
	public static boolean ORIENTATION_LOCKED = false;
	public static boolean PORTRAIT = false;

	// Camera size parameters
	final int DESIRED_WIDTH = 640;
	final int DESIRED_HEIGHT = 480;

	private Camera mCamera;
	private static CameraPreview mCameraPreview;
	private static TimeLapseApplication tla;
	private static Context context;
	private int _id;

	// The optimal image size for the device's screen
	// see getOptimalPreviewSize()
	private static Size optimalSize;

	// Determines whether the shutter listener is active
	public static Boolean taking_picture = false;

	// ImageView overlayed on the Camera preview
	private static ImageView cameraOverlay;

	// Undo button
	private static RelativeLayout undoLayout;

	// For undo button animations
	private static ObjectAnimator fadeInAnimator;
	private static ObjectAnimator fadeOutAnimator;

	// Animation time constants
	private final static long FADE_DURATION = 5 * 1000; // ms
	private final static long UNDO_DURATION = 1 * 1000; // ms

	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "TimeLapseActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera);

		// Store context for use by static methods
		tla = (TimeLapseApplication) getApplicationContext();
		context = this;
		cameraOverlay = (ImageView) findViewById(id.camera_overlay);
		cameraOverlay.setAlpha(100);

		undoLayout = (RelativeLayout) findViewById(id.undo_layout);
		undoLayout.setOnClickListener(undoOnClickListener);

		// LocalBroadCast Stuff
		LocalBroadcastManager.getInstance(this).registerReceiver(
				orientationStateMessageReceiver,
				new IntentFilter("orientation_lock"));

	}

	/** End OnCreate() */

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			if (TEST_FRONT_CAMERA) {
				c = Camera.open(1);
				CAMERA_FRONT_FACING = true;
			} else {
				c = Camera.open();

				if (c == null) {
					// Rear Camera is not available (in use or does not exist)
					// Try all other cameras
					int num_cameras = Camera.getNumberOfCameras();
					for (int x = 0; x < num_cameras; x++) {
						c = Camera.open(x);
						if (c != null) {
							CAMERA_FRONT_FACING = true;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			Log.d("getCameraInstance", e.toString());
		}

		return c; // returns null if camera is unavailable
	}

	@Override
	public void onPause() {
		super.onPause();

		// Release camera for other applications
		releaseCamera();
		ORIENTATION_LOCKED = false;
		// this.finish();
	}

	@Override
	public void onResume() {
		super.onResume();

		mCamera = getCameraInstance();
		if (mCamera == null) {
			showCameraErrorDialog();
		} else {
			// onCreate transplant
			// Camera is available. Onward Ho!

			// Assign Camera parameters
			setupCamera();
			// Obtain SurfaceView for displaying camera preview
			mCameraPreview = new CameraPreview(this, mCamera);
			FrameLayout preview = (FrameLayout) findViewById(id.camera_preview);
			preview.removeAllViews();
			preview.addView(mCameraPreview);

			// Set shutter touch listener to layout
			RelativeLayout container = (RelativeLayout) findViewById(id.container_layout);
			container.setOnTouchListener(shutterListener);

			// End onCreate transplant

			Intent intent = getIntent();

			_id = intent.getExtras().getInt("_id");
			Log.d("CameraActivity", "id received: " + String.valueOf(_id));
			if (_id == -1) {
				// create a new timelapse
				//Uri new_timelapse = tla.createTimeLapse(null, null);
				//_id = Integer.parseInt(new_timelapse.getLastPathSegment());

			}
			else{
				Cursor timelapse_cursor = tla.getTimeLapseById(_id,
						new String[] { SQLiteWrapper.COLUMN_LAST_IMAGE_PATH });
				if (timelapse_cursor != null && timelapse_cursor.moveToFirst()) {
					if (!timelapse_cursor.isNull(timelapse_cursor
							.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))) {
						setCameraOverlay(
								timelapse_cursor
										.getString(timelapse_cursor
												.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)),
								false);
						
						timelapse_cursor = checkAndLockScreenOrientation(timelapse_cursor);
					}
	
					// Log.d("CameraActivity",
					// String.valueOf(timelapse_cursor.isNull(timelapse_cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))));
					// Log.d("CameraActivity","lastImagePath: " +
					// timelapse_cursor.getString(timelapse_cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)));
					timelapse_cursor.close();
				}
			}
		}
	}

	private OnTouchListener shutterListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if ((event.getAction()) == event.ACTION_DOWN) {
				Log.d(TAG, "Taking picture...");
				// takePicture must be called after mCamera.startPreview()

				// Called by Camera when a picture's data is ready for
				// processing
				// Restart Camera preview after snapping, and set just-captured
				// photo as overlay
				// TimeLapsePictureCallback tlpc =
				// CameraUtils.TimeLapsePictureCallback(timelapse_id);
				if (!taking_picture) {
					taking_picture = true;
					Log.d("CameraActivity",
							"passing id to Camera " + String.valueOf(_id));
					
					boolean portrait = false;
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
						portrait = true;
					
					checkAndLockScreenOrientation(portrait);
					
					if(_id == -1){
						Uri new_timelapse = tla.createTimeLapse(null, null);
						_id = Integer.parseInt(new_timelapse.getLastPathSegment());
					}

					mCamera.takePicture(CameraUtils.mShutterFeedback, null,
							null, new CameraUtils.TimeLapsePictureCallback(_id,
									CAMERA_FRONT_FACING, portrait));
				}
				// Consume touch event
				return true;
			}
			return false;
		}
	};

	/**
	 * Display the just-captured picture as the camera overlay If a picture
	 * exists in this timelapse, lock the screen orientation to match the
	 * timelapse setting.
	 */
	@SuppressLint("NewApi")
	public static void setCameraOverlay(String filepath, boolean showUndo) {
		// Decode the just-captured picture from file and display it
		// in the cameraOverlay ImageView

		// if null filePath, clear overlay
		if (filepath == null) {
			cameraOverlay.setVisibility(View.GONE);
			return;
		}
		Log.d("setCameraOverlay", "path:" + filepath);
		File imgFile = new File(filepath);
		Bitmap optimal_bitmap;
		if (imgFile.exists()) {
			if (optimalSize != null) {
				optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(
						imgFile.getAbsolutePath(), optimalSize.width,
						optimalSize.height);
			} else
				return;

			// message set and lock screen orientation
			Intent intent = new Intent();
			
			LocalBroadcastManager.getInstance(CameraActivity.context).sendBroadcast(intent);

			cameraOverlay.setImageBitmap(optimal_bitmap);
			// Ensure camera_overlay is visible
			cameraOverlay.setVisibility(View.VISIBLE);

			if (showUndo) {
				// Fade in/out the undo button if sdk allows
				if (Build.VERSION.SDK_INT >= 14) {
					undoLayout.setAlpha(0);
					undoLayout.setVisibility(View.VISIBLE);

					// Unfortunately the new viewproperyanimator doesn't allow
					// chaining
					// undoLayout.animate().alpha(100).setDuration(FADE_DURATION).start();
					// undoLayout.animate().alpha(0).setDuration(FADE_DURATION).setStartDelay(UNDO_DURATION).setListener(hideUndoListener);

					// Handle partially complete animations

					// if a previous fadeOutAnimator is running, cancel it and
					// bring Undo Button to full opacity
					if (fadeOutAnimator != null && fadeOutAnimator.isStarted()) {
						fadeOutAnimator.cancel();
						undoLayout.setAlpha(100);
						undoLayout.setVisibility(View.VISIBLE);
					}
					// If the previous fadeOutAnimator ended, perform fadeIn
					// before fadeOut
					else if (fadeOutAnimator != null
							&& !fadeOutAnimator.isStarted()) {
						if (fadeInAnimator != null)
							fadeInAnimator.cancel();
						fadeInAnimator = ObjectAnimator.ofFloat(undoLayout,
								"alpha", 0, 100);
						fadeInAnimator.setStartDelay(200); // avoids choppiness
															// due to
															// simultaneous
															// image loading
						fadeInAnimator.setDuration(FADE_DURATION);
						fadeInAnimator.start();
					}
					fadeOutAnimator = ObjectAnimator.ofFloat(undoLayout,
							"alpha", 100, 0);
					fadeOutAnimator.setStartDelay(UNDO_DURATION);
					// prevent AnimationListener class to be exposed to
					// incompatible API versions
					fadeOutAnimator.addListener(new AnimatorListener() {

						@Override
						public void onAnimationStart(Animator animation) {
							// TODO Auto-generated method stub

						}

						@SuppressLint("NewApi")
						@Override
						public void onAnimationEnd(Animator animation) {
							undoLayout.setVisibility(View.GONE);

						}

						@SuppressLint("NewApi")
						@Override
						public void onAnimationCancel(Animator animation) {
							// TODO Auto-generated method stub

						}

						@SuppressLint("NewApi")
						@Override
						public void onAnimationRepeat(Animator animation) {
							// TODO Auto-generated method stub

						}

					});
					fadeOutAnimator.setDuration(FADE_DURATION);
					fadeOutAnimator.start();

				} else {
					undoLayout.setVisibility(View.VISIBLE);
				}
			}
		}

		mCameraPreview.restartPreview();
	}

	/**
	 * Handle shutter action feedback CALLED BY: CameraUtils.mShutterFeedback
	 * (callback method passed to mCamera.takePicture(...) via shutterListener)
	 */
	public static void showShutterFeedback() {
		CharSequence text = "Saving Image...";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(tla, text, duration);
		toast.setGravity(Gravity.TOP, 0, 10);
		toast.show();

	}

	/** Release Camera when application is finished */
	private void releaseCamera() {
		if (mCamera != null) {
			Log.d(TAG,"Stopping preview in SurfaceDestroyed().");
	    	mCamera.setPreviewCallback(null);
	    	mCamera.stopPreview();
	    	mCamera.release(); //it is important that this is done so camera is available to other applications.
			//mCamera.release(); // release the camera for other applications
			// release camera when CameraPreview destroyed
			mCamera = null;
		}
	}

	/** Show an AlertDialog corresponding to a Camera Error */
	private void showCameraErrorDialog() {
		AlertDialog noCameraAlertDialog = new AlertDialog.Builder(
				(Context) context)
				.setTitle(
						getResources().getStringArray(
								R.array.camera_error_dialog)[0])
				.setMessage(
						getResources().getStringArray(
								R.array.camera_error_dialog)[1])
				.setNeutralButton(getString(R.string.dialog_ok),
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface thisDialog,
									int arg1) {
								// Cancel the dialog
								thisDialog.cancel();
							}
						}).create();

		noCameraAlertDialog.show();
	}

	private void setupCamera() {
		// set preview size and make any resize, rotate or
		// reformatting changes here

		// List supportedPictureSizes =
		// mCamera.getParameters().getSupportedPictureSizes();
		Camera.Parameters parameters = mCamera.getParameters();
		parameters = setDesiredPictureSize(parameters);
		// Uncomment to use largest possible picture size
		// parameters.setPictureSize(((Camera.Size)supportedPictureSizes.get(0)).width,
		// ((Camera.Size)supportedPictureSizes.get(0)).height);

		// Testing portrait mode
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			mCamera.setDisplayOrientation(90);
		// setCameraDisplayOrientation(this, 0, mCamera);

		// Set autoFocus mode
		List supportedFocusModes = mCamera.getParameters()
				.getSupportedFocusModes();
		if (isSupported(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
				supportedFocusModes))
			parameters
					.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		else if (isSupported(Camera.Parameters.FOCUS_MODE_AUTO,
				supportedFocusModes))
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		// Set Camera preview params
		List<Size> sizes = parameters.getSupportedPreviewSizes();
		// match preview to output picture aspect ratio
		Size size = parameters.getPictureSize();

		optimalSize = CameraUtils.getOptimalPreviewSize(this, sizes,
				(double) size.width / size.height);

		Size original = parameters.getPreviewSize();
		if (!original.equals(optimalSize)) {
			parameters.setPreviewSize(optimalSize.width, optimalSize.height);
		}

		// parameters.setPreviewSize(((Camera.Size)supportedPictureSizes.get(8)).width,
		// ((Camera.Size)supportedPictureSizes.get(8)).height);
		// Log.d("onSurfaceChanged","width: "+
		// String.valueOf(((Camera.Size)parameters.getPreviewSize()).width) +
		// " x "+
		// String.valueOf(((Camera.Size)parameters.getPreviewSize()).height));
		mCamera.setParameters(parameters);
	}

	private static boolean isSupported(String value, List<String> supported) {
		return supported == null ? false : supported.indexOf(value) >= 0;
	}

	private View.OnClickListener undoOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// delete last frame. Calls setCameraOverlay when ready
			new FileUtils.deleteLastFrameFromTimeLapse().execute(_id);
			/*
			 * Cursor timelapse_cursor = c.getTimeLapseById(_id, new
			 * String[]{SQLiteWrapper.COLUMN_LAST_IMAGE_PATH}); if
			 * (timelapse_cursor != null && timelapse_cursor.moveToFirst()) {
			 * if(!timelapse_cursor.isNull(timelapse_cursor.getColumnIndex(
			 * SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))){
			 * setCameraOverlay(timelapse_cursor
			 * .getString(timelapse_cursor.getColumnIndex
			 * (SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)), false); } }
			 */
		}

	};

	public Camera.Parameters setDesiredPictureSize(Camera.Parameters parameters) {
		List<Camera.Size> supportedPictureSizes = parameters
				.getSupportedPictureSizes();

		final double ASPECT_TOLERANCE = .001;
		final double targetRatio = DESIRED_WIDTH / DESIRED_HEIGHT;
		double minDiff = Double.MAX_VALUE;

		// Try to find an size match aspect ratio and size
		for (Size size : supportedPictureSizes) {
			double ratio = (double) size.width / size.height;
			// if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - DESIRED_HEIGHT) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - DESIRED_HEIGHT);
			}
		}
		/*
		 * // Cannot find the one match the aspect ratio. This should not
		 * happen. // Ignore the requirement. if (optimalSize == null) {
		 * Log.w(TAG, "No preview size match the aspect ratio"); minDiff =
		 * Double.MAX_VALUE; for (Size size : supportedPictureSizes) { if
		 * (Math.abs(size.height - DESIRED_HEIGHT) < minDiff) { optimalSize =
		 * size; minDiff = Math.abs(size.height - DESIRED_HEIGHT); } }
		 * 
		 * }
		 */
		Log.d("Camera size set", String.valueOf(optimalSize.width) + "x"
				+ String.valueOf(optimalSize.height));
		parameters.setPictureSize(optimalSize.width, optimalSize.height);

		return parameters;
	}

	private Cursor checkAndLockScreenOrientation(Cursor timelapse_cursor) {
		// If the screen orientation has not been locked, do so now to avoid
		// mixed-orientation timelapses
		if (!ORIENTATION_LOCKED) {
			File imgFile = new File(timelapse_cursor.getString(timelapse_cursor
					.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)));
			Bitmap optimal_bitmap = null;
			if (imgFile.exists()) {
				if (optimalSize != null) {
					optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(
							imgFile.getAbsolutePath(), optimalSize.width,
							optimalSize.height);
					// If this timelapse is in the portrait orientation
					if (optimal_bitmap.getHeight() > optimal_bitmap.getWidth()) {
						// force portrait orientation
						this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
						ORIENTATION_LOCKED = true;
					} else {
						// force landscape orientation
						this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					}
					Log.d("ORIENTATION_LOCKED", "True");
					ORIENTATION_LOCKED = true;
				}
			}
		}
		return timelapse_cursor;
	}
	
	private void checkAndLockScreenOrientation(boolean portrait) {
		// If the screen orientation has not been locked, do so now to avoid
		// mixed-orientation timelapses
		if (!ORIENTATION_LOCKED) {
			if(portrait){
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			ORIENTATION_LOCKED = true;
			Log.d("ORIENTATION_LOCKED", "True");
				
		}
	}

	// Called when orientation lock message received
	private BroadcastReceiver orientationStateMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			int status = intent.getIntExtra("status", -1);
			if (status == 1) { // orientation locked
				Log.d("CameraActivity-Broadcast", "orientation locked");
				
			}
		}

	};

}