package pro.dbro.timelapse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
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

	public static TimeLapseApplication c;
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crittercism.init(getApplicationContext(), Secrets.CRITTERCISM_ID);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.browser);
        
        c = (TimeLapseApplication)getApplicationContext();
        
        list = (ListView) findViewById(android.R.id.list);
        list.addHeaderView(this.getLayoutInflater().inflate(R.layout.browser_list_header, null));
        empty = (TextView) findViewById(android.R.id.empty);
        
        // Establish LocalBroadcastManager for communication with other Classes
        //LocalBroadcastManager.getInstance(this).registerReceiver(browserActivityMessageReceiver,
      	//      new IntentFilter(String.valueOf(R.id.browserActivity_message)));
        
        // Load Timelapses from external storage
        //Log.d("OnCreate","Beginning filesystem read");
        new FileUtils.ParseTimeLapsesFromFilesystem().execute("");
 
        getSupportLoaderManager().initLoader(0, null, this);
        adapter = new TimeLapseCursorAdapter(this, null);
		list.setAdapter(adapter);
		list.setOnItemClickListener(listItemClickListener);

    }
    
    public static TimeLapseApplication getContext() {
        return c;
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
    /*
    // Populate ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.browser_menu, menu);
        return true;
    }
    
    // Handle ActionBar Events
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
            	// Go to TimelapseViewer with new TimeLapse
            	Intent intent = new Intent(BrowserActivity.this, TimeLapseViewerActivity.class);
            	intent.putExtra("_id", -1); // indicate TimeLapseViewerActivity to create a new TimeLapse
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */
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
    
 
    private void populateListView(ArrayList<TimeLapse> data){
    	// Populate ListView
        // (Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
    	/*
        browserAdapter = new SimpleAdapter(this.getApplicationContext(), loadItems(data), R.layout.browser_list_item, BROWSER_LIST_ITEM_KEYS, BROWSER_LIST_ITEM_VALUES);
        browserAdapter.setViewBinder(new BrowserViewBinder());
        setListAdapter(browserAdapter);
        */
		/*
    	Cursor cursor = sqliteManager.cursorSelectAll();
		//getLoaderManager().initLoader(0, null, (LoaderCallbacks<Cursor>) this);
		getSupportLoaderManager().initLoader(0, null, this);
		adapter = new TimeLapseCursorAdapter((Context)c,
                R.layout.browser_list_item, cursor);
		this.list.setAdapter(adapter);
		*/

    }
    
    // Create Map describing ListView contents. Fed as "data" to SimpleAdapter constructor
    private List<Map<String, String>> loadItems(ArrayList<TimeLapse> list){
    	// Sort TimeLapse list by modified date
    	Collections.sort(list, new TimeLapse.TimeLapseComparator());
    	List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
    	
    	// Relate ListView row element identifiers to TimeLapse fields
    	for(int x = 0; x < list.size();x++){
    		HashMap<String, String> itemMap = new HashMap<String, String>();
    		itemMap.put("title", list.get(x).name);
    		itemMap.put("body", list.get(x).description);
    		itemMap.put("timelapse", String.valueOf(list.get(x).id));
    		if(list.get(x).image_count > 0){
    			File thumb_dir = new File(FileUtils.getOutputMediaDir(list.get(x).id), "thumbnails");
    			File thumb_image = new File(thumb_dir, String.valueOf(list.get(x).image_count)+"_thumb.jpeg");
    			if(thumb_image.exists())
    				itemMap.put("thumbnail", thumb_image.getAbsolutePath());
    		}
    		mapList.add(itemMap);
    	}
    	//Log.d("maplist_in",list.toString());
    	//Log.d("maplist_out",mapList.toString());
    	return mapList;
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
 		if(data.getCount() != 0){
 			empty.setVisibility(View.GONE);
 		}
 		else{
 			empty.setText(R.string.no_timelapses_found);
 		}
 		adapter.swapCursor(data);
 	}

 	@Override
 	public void onLoaderReset(Loader<Cursor> loader) {
 		// data is not available anymore, delete reference
 		adapter.swapCursor(null);
 	}

}
