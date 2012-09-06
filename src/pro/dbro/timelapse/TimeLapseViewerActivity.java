package pro.dbro.timelapse;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;

public class TimeLapseViewerActivity extends Activity {
	
	private TimeLapseApplication tla;
	
	// Store the title when the activity starts
	// compare to title.getText() during onPause()
	String originalTitle = "";
	private EditText title;
	//private EditText description;
	//private Button actionButton;
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
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
        title = (EditText) findViewById(R.id.create_timelapse_title);
        //description = (EditText) findViewById(R.id.create_timelapse_description);
        //actionButton =  (Button) findViewById(R.id.create_timelapse_button);
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
    	if(cursor.moveToFirst()){
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
        			seekBar.setMax(cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT))-1);
        			if(seekBar.getMax() > 1){
        				Log.v("progressMAX", String.valueOf(seekBar.getMax()));
        				seekBar.setVisibility(View.VISIBLE);
        				seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        			}
        			timelapse_dir = cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
        		}
        	}
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
    			&& ( title.getText().toString().compareTo(originalTitle) != 0) ){
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
    
    // Handle ActionBar Events
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_camera:
            	// Go to TimelapseViewer with new TimeLapse
            	Intent intent = new Intent(TimeLapseViewerActivity.this, CameraActivity.class);
            	intent.putExtra("_id", _id); // indicate TimeLapseViewerActivity to create a new TimeLapse
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /*
    // Save changes to current timelapse
    private OnClickListener saveTimeLapseListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(title.getText().toString().compareTo("") == 0){
				return;
			}
			else{
				tla.updateTimeLapseById(_id, new String[] {SQLiteWrapper.COLUMN_NAME}, 
													  new String[] {title.getText().toString()});
				Intent intent = new Intent(TimeLapseViewerActivity.this, BrowserActivity.class);
                startActivity(intent);
			}	
		}
    };
    */
    
    // Create a new timelapse
    private OnClickListener createTimeLapseListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(title.getText().toString().compareTo("") == 0){
				return;
			}
			else{
				Uri new_timelapse = tla.createTimeLapse(new String[]{SQLiteWrapper.COLUMN_NAME },
									new String[]{title.getText().toString() });
				Uri new_timelapse_uri = Uri.parse(TimeLapseContentProvider.AUTHORITY_URI.toString() + new_timelapse.toString());
				Cursor cursor = getContentResolver().query(new_timelapse_uri, null, null, null, null);
				if(cursor.moveToFirst())
					Log.d("TimeLapseCreated","name: " + cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_NAME)));
				//Log.d("TimeLapseCreated","passing id to camera: " + new_timelapse.getLastPathSegment().toString());
				Intent intent = new Intent(TimeLapseViewerActivity.this, CameraActivity.class);
				if(new_timelapse != null){
					//timelapse_id = cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_TIMELAPSE_ID));
					_id = Integer.parseInt(new_timelapse.getLastPathSegment().toString());
					Log.d("TimeLapseCreated","passing id to camera: " + new_timelapse.getLastPathSegment().toString());
					intent.putExtra("_id", _id);
				}
				// Intent.FLAG_ACTIVITY_CLEAR_TOP will allow gallery to live-load images just taken on newly created TimeLapse
				intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                startActivity(intent);
                cursor.close();
			}
		}
    };
    
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
}
