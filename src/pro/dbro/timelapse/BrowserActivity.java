package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class BrowserActivity extends SherlockListActivity {
	
	private static final String[] BROWSER_LIST_ITEM_KEYS = {"title", "body", "timelapse"};
	private static final int[] BROWSER_LIST_ITEM_VALUES = {R.id.list_item_headline, R.id.list_item_body, R.id.list_item_container};
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        
        // (Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
        SimpleAdapter browserAdapter = new SimpleAdapter(this.getApplicationContext(), loadItems(), R.layout.browser_list_item, BROWSER_LIST_ITEM_KEYS, BROWSER_LIST_ITEM_VALUES);
        browserAdapter.setViewBinder(new BrowserViewBinder());
        setListAdapter(browserAdapter);
    }
    
    // Handle listview item select
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(((String)v.getTag(R.id.view_onclick_action)).equals("camera")){
        	//  launch CameraActivity
        	Intent intent = new Intent(BrowserActivity.this, CameraActivity.class);
            startActivity(intent);
        }
        else if(((String)v.getTag(R.id.view_onclick_action)).equals("view")){
        	Intent intent = new Intent(BrowserActivity.this, TimeLapseViewerActivity.class);
            startActivity(intent);
        }
    }
    
    // Create Map describing ListView contents. Fed as "data" to SimpleAdapter constructor
    private List<Map<String, String>> loadItems(){
    	// TODO: Load items from storage
    	HashMap<String, String> itemMap = new HashMap<String, String>();
    	itemMap.put("title", "Title Uno is too long for stuff and I hope it doesn't break");
    	itemMap.put("body", "Body uno har har har har i am a pirate on the high seas with cheese I do please");
    	itemMap.put("timelapse", "13");
    	List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
    	mapList.add(itemMap);
    	return mapList;
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
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
