package pro.dbro.timelapse;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.crittercism.app.Crittercism;

public class BrowserActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	// The real loader:
	SimpleCursorAdapter adapter;
	TextView empty;
	ListView list;

	public static final String PREFS_NAME = "TimeLapseStoragePrefs";
	public static final String PREFS_STORAGE_LOCATION = "storage_location";

	public static TimeLapseApplication tla;
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crittercism.init(getApplicationContext(), Secrets.CRITTERCISM_ID);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.browser);
        
        tla = (TimeLapseApplication)getApplicationContext();
        final Context c = this;
        
        list = (ListView) findViewById(android.R.id.list);
        list.addHeaderView(this.getLayoutInflater().inflate(R.layout.browser_list_header, null));
        empty = (TextView) findViewById(android.R.id.empty);
        
        // Load Timelapses from external storage
        //Log.d("OnCreate","Beginning filesystem read");
        if(!tla.filesystemParsed)
        	new FileUtils.ParseTimeLapsesFromFilesystem().execute("");
 
        getSupportLoaderManager().initLoader(0, null, this);
        adapter = new TimeLapseCursorAdapter(this, null);
		list.setAdapter(adapter);
		list.setOnItemClickListener(listItemClickListener);
		list.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final int _id = (Integer) view.getTag(R.id.view_related_timelapse);
				new AlertDialog.Builder(c)
	            .setTitle("Delete Timelapse?")
	            .setIcon(R.drawable.ic_launcher)
	            .setMessage("This will permanently delete this timelapse and all it's raw photo files.")
	            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
	                
	                public void onClick(DialogInterface dialog, int which) {
	                	new FileUtils.deleteTimeLapse().execute(_id);
	                }

				 })
	            .setNeutralButton("Cancel", null)
	            .show();
				return false;
			}

	    });

    }
    
    public static TimeLapseApplication getContext() {
        return tla;
    }
    
    // Handle listview item select
	public OnItemClickListener listItemClickListener = new OnItemClickListener(){

		@SuppressLint("NewApi")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d("OnItemClick",String.valueOf(position));

			// Create Timelapse header list item
        	if(position == 0){
        		Intent intent = new Intent(BrowserActivity.this, CameraActivity.class);
        		intent.putExtra("_id", -1);
        		startActivity(intent);
        	}
        	// Camera icon within list item
        	else if(((String)view.getTag(R.id.view_onclick_action)).equals("camera")){
	        	//  launch CameraActivity
	        	Intent intent = new Intent(BrowserActivity.this, CameraActivity.class);
	        	intent.putExtra("_id", (Integer)view.getTag(R.id.view_related_timelapse));
	            startActivity(intent);
	        }
        	// Elsewhere within list item
	        else if(((String)view.getTag(R.id.view_onclick_action)).equals("view")){
	        	Intent intent = new Intent(BrowserActivity.this, TimeLapseViewerActivity.class);
	        	intent.putExtra("_id", (Integer)view.getTag(R.id.view_related_timelapse));
	            startActivity(intent);
	        }
	        
		}
    	
    };

  
    @Override
    protected void onResume(){
    	super.onResume();

    }
     
    @Override
    protected void onPause(){
    	super.onPause();
    	Log.d("BrowserActivity","onPause");
    }
    
    @Override
	protected void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  //LocalBroadcastManager.getInstance(this).unregisterReceiver(browserActivityMessageReceiver);
	  super.onDestroy();
	}
    
 
    
    /**
     * Welcome to Loader Town
     */
    
 // Creates a new loader after the initLoader () call
 	@Override
 	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
 		String[] projection = { SQLiteWrapper.COLUMN_ID, SQLiteWrapper.COLUMN_TIMELAPSE_ID, SQLiteWrapper.COLUMN_NAME, SQLiteWrapper.COLUMN_DESCRIPTION, SQLiteWrapper.COLUMN_THUMBNAIL_PATH };
 		CursorLoader cursorLoader = new CursorLoader(this,
 				TimeLapseContentProvider.CONTENT_URI, projection, null, null, "modified_date desc");
 		return cursorLoader;
 	}

 	@Override
 	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
 		Log.d("onLoadFinished", String.valueOf(data.getCount()));
 		empty.setVisibility(View.GONE);
 		// even if search returns nothing, we add new timelapses from the ListView header now
 		adapter.swapCursor(data);
 	}

 	@Override
 	public void onLoaderReset(Loader<Cursor> loader) {
 		// data is not available anymore, delete reference
 		adapter.swapCursor(null);
 	}

}
