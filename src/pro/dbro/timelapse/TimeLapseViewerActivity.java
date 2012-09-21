package pro.dbro.timelapse;

import pro.dbro.timelapse.service.GifExportService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
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
	
	private static TimeLapseApplication tla;
	
	// Store the title when the activity starts
	// compare to title.getText() during onPause()
	String originalTitle = "";
	private EditText title;
	//private EditText description;
	private ImageButton cameraButton;
	private ImageButton exportButton;
	private ImageView preview;
	private SeekBar seekBar;
	
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

        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
        title = (EditText) findViewById(R.id.create_timelapse_title);
        cameraButton = (ImageButton) findViewById(R.id.camera_button);
        exportButton =  (ImageButton) findViewById(R.id.export_button);
        cameraButton.setOnClickListener(cameraButtonListener);
		exportButton.setOnClickListener(exportButtonListener);
        
        preview = (ImageView) findViewById(R.id.previewImage);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tla = (TimeLapseApplication)this.getApplicationContext();
        
        Intent intent = getIntent();
        _id = intent.getExtras().getInt("_id");

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
        			int seekBarMax = 0;
        			seekBarMax = cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))-1;
        			Log.d("seekBarMax", String.valueOf(seekBarMax));
        			seekBar.setMax(cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))-1);
        			if(seekBar.getMax() > 0){
        				Log.v("progressMAX", String.valueOf(seekBar.getMax()));
        				seekBar.setVisibility(View.VISIBLE);
        				seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        			}
        			timelapse_dir = cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
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
    	if( (title.getText().toString().compareTo("") == 0 ) 
    			|| ( title.getText().toString().compareTo(originalTitle) == 0) ){
			return;
		}
		else{
			tla.updateTimeLapseById(_id, new String[] {SQLiteWrapper.COLUMN_NAME}, 
												  new String[] {title.getText().toString()});
			//Intent intent = new Intent(TimeLapseViewerActivity.this, BrowserActivity.class);
            //startActivity(intent);
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
	        			seekBar.setMax(cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))-1);
	        			if(seekBar.getMax() > 1){
	        				seekBar.setVisibility(View.VISIBLE);
	        				seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
	        			}
	        		}
	        	}
	        }
	    	preview_is_fresh = true;
	    	cursor.close();
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
            		Intent i = new Intent(BrowserActivity.getContext(), GifExportService.class);
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
				
			Log.v("Seek",String.valueOf(progress));
			//Log.d("preview size", String.valueOf(width)+ "x" + String.valueOf(height));
			//Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(timelapse_dir + "/" + String.valueOf(progress+1)+".jpeg", preview_width, preview_height);
			
			//Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(timelapse_dir + File.pathSeparator + String.valueOf(progress)+".jpeg", 640, 480);
			//preview.setImageBitmap(optimal_bitmap);
			
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
			Intent i = new Intent(BrowserActivity.getContext(), GifExportService.class);
        	i.putExtra("_id", _id);
        	Log.d("SERVICE","Starting");
        	startService(i);
        	exportButton.setEnabled(false);
		}
    };
}
