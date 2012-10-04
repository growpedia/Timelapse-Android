package pro.dbro.timelapse;

import pro.dbro.timelapse.service.GifExportService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TimeLapseViewerActivity extends Activity {
	
	public static TimeLapseApplication tla;
	private Context c;
	// Store the title when the activity starts
	// compare to title.getText() during onPause()
	String originalTitle = "";
	private EditText title;
	//private EditText description;
	private ImageButton cameraButton;
	private ImageButton exportButton;
	private ImageView preview;
	private SeekBar seekBar;
	
	AnimationTimer animation;
	
	private int preview_width = 0;
	private int preview_height = 0;
	private String timelapse_dir;
		
	private int _id = -1;
	
	BitmapFactory bmf = new BitmapFactory();
	
	private static boolean preview_is_fresh = false;
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = this;
        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
        title = (EditText) findViewById(R.id.create_timelapse_title);
        cameraButton = (ImageButton) findViewById(R.id.camera_button);
        exportButton =  (ImageButton) findViewById(R.id.export_button);
        cameraButton.setOnClickListener(cameraButtonListener);
		// set exportButton listener pending gif state
        
        preview = (ImageView) findViewById(R.id.previewImage);
        preview.setOnClickListener(animationToggleListener);
        
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tla = (TimeLapseApplication)this.getApplicationContext();
        
        Intent intent = getIntent();
        _id = intent.getExtras().getInt("_id");
        
        // LocalBroadCast Stuff
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStateMessageReceiver,
        	      new IntentFilter("service_status_change"));

        Log.d("TimeLapseViewerActivity", "TL id : " + String.valueOf(_id));
        
        Display display = getWindowManager().getDefaultDisplay();
        preview_width = display.getWidth();
		preview_height = (int) (((double)preview_width * 3)/4);

            
        Cursor cursor = tla.getTimeLapseById(_id, null);
    	if(cursor != null && cursor.moveToFirst()){
    		title.setText(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_NAME)));
    		originalTitle = title.getText().toString();
    		//description.setText(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_DESCRIPTION)));
        	if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_THUMBNAIL_PATH))){
        		//Log.d("optimal_width",String.valueOf(seekBar.getWidth()));
        		//Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)),preview_width, preview_height);
        		bmf = new BitmapFactory();
        		preview.setImageBitmap(bmf.decodeFile(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_THUMBNAIL_PATH))));
        		preview.setVisibility(View.VISIBLE);
        		if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))){
        			int image_count = 0;
        			image_count = cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
        			Log.d("seekBarMax", String.valueOf(image_count-1));
        			seekBar.setMax(image_count-1);
        			//seekBar.setSecondaryProgress(0);
        			if(seekBar.getMax() > 0){
        				Log.v("progressMAX", String.valueOf(seekBar.getMax()));
        				seekBar.setVisibility(View.VISIBLE);
        				seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        			}
        			timelapse_dir = cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
        			
        			// Change export button to Share button if .gif exists
        			if(cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_GIF_STATE)) == image_count){
        				setupShareButton();
        			}
        			else{
        				setupExportButton();
        			}
        		}
        	}
        }
    	// Else this timelapse is not yet created
    	// We shouldn't encounter this state
    	else{
    		exportButton.setEnabled(false);
    		cameraButton.setEnabled(false);
    	}
        preview_is_fresh = true;
        cursor.close();
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	
    	// User may be switching to cameraActivity, so signal the preview must be refreshed onResume
    	preview_is_fresh = false;
    	Log.d("TimeLapseViewer onPause","TL id: " + _id);
    	
    	// Save title if changed
    	// if new nonblank title input
    	validateAndUpdateTitle();
    	
    	// Cancel animation
    	if(animation != null){
			animation.cancel();
			animation = null;
		}
    	
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	Display display = getWindowManager().getDefaultDisplay();
        //Point size = new Point();
        //Requires API 13+
        //display.getSize(size);

		if(!preview_is_fresh){
			preview_width = display.getWidth();
			preview_height = (int) (((double)preview_width * 3)/4);
			Cursor cursor = tla.getTimeLapseById(_id, null);
	    	if(cursor.moveToFirst()){
	        	if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))){
	        		//Log.d("optimal_width",String.valueOf(seekBar.getWidth()));
	        		//Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)),preview_width, preview_height);
	        		//preview.setImageBitmap(optimal_bitmap);
	        		preview.setImageBitmap(bmf.decodeFile(cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_THUMBNAIL_PATH))));
	        		preview.setVisibility(View.VISIBLE);
	        		if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))){
	        			int image_count = cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
	        			seekBar.setMax(image_count-1);
	        			if(seekBar.getMax() > 1){
	        				seekBar.setVisibility(View.VISIBLE);
	        				seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
	        			}
	        			// Change export button to Share button if .gif exists
	        			if(cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_GIF_STATE)) == image_count){
	        				setupShareButton();
	        			}
	        			else{
	        				setupExportButton();
	        			}
	        		}
	        		
	        	}
	        }
	    	preview_is_fresh = true;
	    	cursor.close();
		}
        
    }
    
    public void validateAndUpdateTitle(){
    	if( (title.getText().toString().compareTo("") == 0 ) 
    			|| ( title.getText().toString().compareTo(originalTitle) == 0) ){
			return;
		}
		else{
			tla.updateTimeLapseById(_id, new String[] {SQLiteWrapper.COLUMN_NAME}, 
												  new String[] {title.getText().toString()});
		}
    }
    
    /*
    // Populate ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Log.d("OnCreateOptionsMenu","yes");
    	if(_id != -1){
    		MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.layout.timelapse_viewer_menu, menu);
    	}
    	return true;
        
    }
    */
    /*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	if(tla.serviceIsRunning())
    		menu.removeItem(R.id.menu_export);
    	
    	return true;
    }
    */
    
    /*
    // Handle ActionBar Events
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_camera:
            	// Go to TimelapseViewer with new TimeLapse
            	Intent intent = new Intent(TimeLapseViewerActivity.this, CameraActivity.class);
            	intent.putExtra("_id", _id); // indicate TimeLapseViewerActivity to create a new TimeLapse
                startActivity(intent);
            case R.id.menu_export:
            	// make GIF
            	if(_id != -1 && !tla.serviceIsRunning()){
            		Intent i = new Intent(TimeLapseApplication.applicationContext, GifExportService.class);
                	i.putExtra("_id", _id);
                	Log.d("SERVICE","Starting");
                	startService(i);
            	}
            	
            	
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */
    
    
    private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			//Log.d("Seek",String.valueOf(progress));
			if(!fromUser)
				return;

			if(animation != null){
				animation.cancel();
				animation = null;
			}
			
			preview.setImageBitmap(bmf.decodeFile(timelapse_dir + "/" + TimeLapse.thumbnail_dir + "/" + String.valueOf(progress+1)+TimeLapse.thumbnail_suffix + ".jpeg"));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
    	
    };
    
    public static void hideSoftKeyboard (View view) {
        InputMethodManager imm = (InputMethodManager)tla.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
      }
    
    OnClickListener cameraButtonListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(TimeLapseViewerActivity.this, CameraActivity.class);
        	intent.putExtra("_id", _id); // indicate TimeLapseViewerActivity to create a new TimeLapse
            startActivity(intent);
		}
    };
    
    OnClickListener exportButtonListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			validateAndUpdateTitle();

			new AlertDialog.Builder(c)
            .setTitle("Export .GIF")
            .setIcon(R.drawable.ic_launcher)
            .setMessage("Choose an output resolution")
            .setPositiveButton("640x480", new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                	Intent i = new Intent(TimeLapseViewerActivity.this, GifExportService.class);
                	i.putExtra("_id", _id);
                	i.putExtra("resolution", 480);
                	Log.d("SERVICE","Starting");
                	startService(i);
                	exportButton.setEnabled(false);
                }

			 })
            .setNeutralButton("320x240", new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                	Intent i = new Intent(TimeLapseViewerActivity.this, GifExportService.class);
                	i.putExtra("_id", _id);
                	i.putExtra("resolution", 240);
                	Log.d("SERVICE","Starting");
                	startService(i);
                	exportButton.setEnabled(false);
                }

			 })
            .show();
			
        	
		}
    };
    
    OnClickListener shareButtonListener = new OnClickListener(){

		@Override
		public void onClick(View v) {

			Cursor result = tla.getTimeLapseById(_id, new String[]{SQLiteWrapper.COLUMN_GIF_PATH});
			String gifPath = null;
			if(result != null && result.moveToFirst())
				gifPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_GIF_PATH));
			
			if(gifPath != null){
				// Intent to have system share gif
				Intent notificationIntent = new Intent(Intent.ACTION_SEND);
				notificationIntent.setType("image/gif");
				notificationIntent.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + gifPath));
				startActivity(notificationIntent);
				
				//PendingIntent contentIntent = PendingIntent.getActivity(GifExportService.this, 0, Intent.createChooser(notificationIntent, "Share .GIF"),0);
			}
			result.close();
		}
    };
    
    OnClickListener animationToggleListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(animation != null){
				animation.cancel();
				animation = null;
			}
			else
				prepareAnimation();
		}
    	
    };

    
 // Called when message received
    private BroadcastReceiver serviceStateMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    // Get extra data included in the Intent
    	    int status = intent.getIntExtra("status", -1);
    	    if(status == 1){ // gif export successful
    	    	Log.d("TimeLapseViewerActivity-BroadcastReceived", "service complete");
    	    	setupShareButton();
    	    }
    	}
    };
    
    public void setupExportButton(){
    	exportButton.setOnClickListener(exportButtonListener);
    	exportButton.setImageResource(R.drawable.film_lg);
    	exportButton.setEnabled(true);
    }
    
    public void setupShareButton(){
    	exportButton.setOnClickListener(shareButtonListener);
    	exportButton.setImageResource(R.drawable.share);
    	exportButton.setEnabled(true);
    }
    
    public void prepareAnimation(){
    	final int FRAME_DURATION_MS = 100;
    	Cursor result = tla.getTimeLapseById(_id, new String[]{SQLiteWrapper.COLUMN_IMAGE_COUNT});
    	int image_count = -1;
    	if(result != null && result.moveToFirst())
    		image_count = result.getInt(result.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
    	
    	if(image_count == -1)
    		return;
    	animation = new AnimationTimer(FRAME_DURATION_MS * image_count, FRAME_DURATION_MS, preview, seekBar, timelapse_dir);
    	animation.start();
    	
    	result.close();
    	
    }
    
    
}
