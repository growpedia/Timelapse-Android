package pro.dbro.timelapse;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
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
	
	private EditText title;
	private EditText description;
	private Button actionButton;
	private ImageView preview;
	private SeekBar seekBar;
	
	private int preview_width = 0;
	private int preview_height = 0;
	private String timelapse_dir;
	
	private int timelapse_id = -1;
	
	private static boolean preview_is_fresh = false;
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
        title = (EditText) findViewById(R.id.create_timelapse_title);
        description = (EditText) findViewById(R.id.create_timelapse_description);
        actionButton =  (Button) findViewById(R.id.create_timelapse_button);
        preview = (ImageView) findViewById(R.id.previewImage);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        
        tla = (TimeLapseApplication)this.getApplicationContext();
        
        Intent intent = getIntent();
        timelapse_id = intent.getExtras().getInt("timelapse_id");

        Log.d("TimeLapseViewerActivity", "TL id : " + String.valueOf(timelapse_id));
        
        Display display = getWindowManager().getDefaultDisplay();
        preview_width = display.getWidth();
		preview_height = (int) (((double)preview_width * 3)/4);
        
        if(timelapse_id == -1){
        	// new timelapse
        	actionButton.setVisibility(View.VISIBLE);
        	actionButton.setOnClickListener(createTimeLapseListener);
        }
        else{
            
            Cursor cursor = tla.getTimeLapseById(timelapse_id, null);
        	if(cursor.moveToFirst()){
        		title.setText(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_NAME)));
        		description.setText(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_DESCRIPTION)));
            	actionButton.setText(getString(R.string.save_timelapse_button));
            	actionButton.setVisibility(View.VISIBLE);
            	actionButton.setOnClickListener(saveTimeLapseListener);
            	if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))){
            		//Log.d("optimal_width",String.valueOf(seekBar.getWidth()));
            		Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)),preview_width, preview_height);
            		preview.setImageBitmap(optimal_bitmap);
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
        }
        preview_is_fresh = true;
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	// Save title / description
    	// User may be switching to cameraActivity, so signal the preview must be refreshed onResume
    	preview_is_fresh = false;
    	Log.d("TimeLapseViewer onPause","TL id: " + timelapse_id);
    	
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
			Cursor cursor = tla.getTimeLapseById(timelapse_id, null);
	    	if(cursor.moveToFirst()){
	        	if(!cursor.isNull(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH))){
	        		//Log.d("optimal_width",String.valueOf(seekBar.getWidth()));
	        		Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(cursor.getString(cursor.getColumnIndex(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH)),preview_width, preview_height);
	        		preview.setImageBitmap(optimal_bitmap);
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
		}
        
    }
    
    // Populate ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Log.d("OnCreateOptionsMenu","yes");
    	if(timelapse_id != -1){
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
            	intent.putExtra("timelapse_id", timelapse_id); // indicate TimeLapseViewerActivity to create a new TimeLapse
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    // Save changes to current timelapse
    private OnClickListener saveTimeLapseListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(title.getText().toString().compareTo("") == 0){
				return;
			}
			else{
				tla.updateTimeLapseById(timelapse_id, new String[] {SQLiteWrapper.COLUMN_NAME, SQLiteWrapper.COLUMN_DESCRIPTION }, 
													  new String[] {title.getText().toString(), description.getText().toString()});
				Intent intent = new Intent(TimeLapseViewerActivity.this, BrowserActivity.class);
                startActivity(intent);
				/*
				if(tla.time_lapse_map.containsKey(timelapse_id)){
		    		tla.setTimeLapseTitleAndDescription(timelapse_id, title.getText().toString(), description.getText().toString());
		    		//findViewById(R.id.create_timelapse_button).setVisibility(View.GONE);
					Log.d("TimeLapse Modified",title.getText().toString() + " " + description.getText().toString());
					Intent intent = new Intent(TimeLapseViewerActivity.this, BrowserActivity.class);
					// Signal BrowserActivity to update ListView
					//intent.putExtra("updateListView", true);
	                startActivity(intent);
		    	}*/
			}	
		}
    };
    
    // Create a new timelapse
    private OnClickListener createTimeLapseListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(title.getText().toString().compareTo("") == 0){
				return;
			}
			else{
				Uri new_timelapse = tla.createTimeLapse(new String[]{SQLiteWrapper.COLUMN_NAME, SQLiteWrapper.COLUMN_DESCRIPTION},
									new String[]{title.getText().toString(), description.getText().toString()});
				//Uri new_timelapse_uri = Uri.parse(TimeLapseContentProvider.AUTHORITY_URI.toString() + new_timelapse.toString());
				//Cursor cursor = getContentResolver().query(new_timelapse_uri, null, null, null, null);
				
				//tla.createTimeLapse(title.getText().toString(), description.getText().toString());
				//findViewById(R.id.create_timelapse_button).setVisibility(View.GONE);
				Log.d("TimeLapse Created",title.getText().toString() + " " + new_timelapse.getLastPathSegment().toString());
				Intent intent = new Intent(TimeLapseViewerActivity.this, CameraActivity.class);
				if(new_timelapse != null){
					timelapse_id = Integer.parseInt(new_timelapse.getLastPathSegment().toString());
					intent.putExtra("timelapse_id", timelapse_id);
				}
				// Signal BrowserActivity to update ListView
				//intent.putExtra("updateListView", true);
                startActivity(intent);
			}
		}
    };
    
    private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
				
			Log.v("Seek",String.valueOf(progress));
			//Log.d("preview size", String.valueOf(width)+ "x" + String.valueOf(height));
			Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(timelapse_dir + "/" + String.valueOf(progress+1)+".jpeg", preview_width, preview_height);
			
			//Bitmap optimal_bitmap = FileUtils.decodeSampledBitmapFromResource(timelapse_dir + File.pathSeparator + String.valueOf(progress)+".jpeg", 640, 480);
			preview.setImageBitmap(optimal_bitmap);
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
