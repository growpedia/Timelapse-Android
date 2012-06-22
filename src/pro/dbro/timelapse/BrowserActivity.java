package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class BrowserActivity extends SherlockListActivity {
	
	private static final String[] BROWSER_LIST_ITEM_KEYS = {"title", "body", "timelapse"};
	private static final int[] BROWSER_LIST_ITEM_VALUES = {R.id.list_item_headline, R.id.list_item_body, R.id.list_item_container};
	public static Context c;
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        
        c = this;

        // Establish LocalBroadcastManager for communication with other Classes
        LocalBroadcastManager.getInstance(this).registerReceiver(browserActivityMessageReceiver,
      	      new IntentFilter(String.valueOf(R.id.filesystem_parse_complete)));
        
        // Load Timelapses from external storage
        //Log.d("OnCreate","Beginning filesystem read");
        new FileUtils.ParseTimeLapsesFromFilesystem().execute("");
    }
    
    // Handle listview item select
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(((String)v.getTag(R.id.view_onclick_action)).equals("camera")){
        	//  launch CameraActivity
        	Intent intent = new Intent(BrowserActivity.this, CameraActivity.class);
        	intent.putExtra("timelapse_id", (Integer)v.getTag(R.id.view_related_timelapse));
            startActivity(intent);
        }
        else if(((String)v.getTag(R.id.view_onclick_action)).equals("view")){
        	Intent intent = new Intent(BrowserActivity.this, TimeLapseViewerActivity.class);
        	intent.putExtra("timelapse_id", (Integer)v.getTag(R.id.view_related_timelapse));
            startActivity(intent);
        }
    }
    
    // Populate ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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
            	intent.putExtra("timelapse_id", -1); // indicate TimeLapseViewerActivity to create a new TimeLapse
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	
    	TimeLapseApplication tla = (TimeLapseApplication)getApplicationContext();
    	ArrayList<TimeLapse> valuesList = new ArrayList<TimeLapse>(tla.time_lapse_map.values());
    	Log.d("BrowserActivity","onResume. Populating Listview: " + valuesList.toString());
    	populateListView(valuesList);
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	Log.d("BrowserActivity","onPause");
    }
    
    @Override
	protected void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  LocalBroadcastManager.getInstance(this).unregisterReceiver(browserActivityMessageReceiver);
	  super.onDestroy();
	}
    
    // Receives messages from other components
    // i.e: when the application state is done being read from the filesystem
    private BroadcastReceiver browserActivityMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    // Populate ListView with received data
    		Log.d("Broadcast Receiver", "Received filesystem read result: " + ((ArrayList<TimeLapse>) intent.getSerializableExtra("result")).toString());
    		TimeLapseApplication  tla = (TimeLapseApplication)getApplicationContext();
    		tla.setTimeLapses((ArrayList<TimeLapse>) intent.getSerializableExtra("result"));
    	    populateListView((ArrayList<TimeLapse>) intent.getSerializableExtra("result"));
    	  }
    };
    
    private void populateListView(ArrayList<TimeLapse> data){
    	// Populate ListView
        // (Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
        SimpleAdapter browserAdapter = new SimpleAdapter(this.getApplicationContext(), loadItems(data), R.layout.browser_list_item, BROWSER_LIST_ITEM_KEYS, BROWSER_LIST_ITEM_VALUES);
        browserAdapter.setViewBinder(new BrowserViewBinder());
        setListAdapter(browserAdapter);
    }
    
    // Create Map describing ListView contents. Fed as "data" to SimpleAdapter constructor
    private List<Map<String, String>> loadItems(ArrayList<TimeLapse> list){
    	
    	List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
    	
    	// Relate ListView row element identifiers to TimeLapse fields
    	for(int x = 0; x < list.size();x++){
    		HashMap<String, String> itemMap = new HashMap<String, String>();
    		itemMap.put("title", ((TimeLapse)list.get(x)).name);
    		itemMap.put("body", ((TimeLapse)list.get(x)).description);
    		itemMap.put("timelapse", String.valueOf(((TimeLapse)list.get(x)).id));
    		mapList.add(itemMap);
    	}
    	Log.d("maplist_in",list.toString());
    	Log.d("maplist_out",mapList.toString());
    	return mapList;
    }

}
