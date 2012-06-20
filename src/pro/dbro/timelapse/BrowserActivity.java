package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class BrowserActivity extends SherlockListActivity {
	
	private static final String[] BROWSER_LIST_ITEM_KEYS = {"title", "body"};
	private static final int[] BROWSER_LIST_ITEM_VALUES = {R.id.list_item_headline, R.id.list_item_body};
	
	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.browser);
        
        // (Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
        ListAdapter browserAdapter = new SimpleAdapter(this.getApplicationContext(), loadItems(), R.layout.browser_list_item, BROWSER_LIST_ITEM_KEYS, BROWSER_LIST_ITEM_VALUES);
        setListAdapter(browserAdapter);
    }
    
    // Create Map describing ListView contents
    private List<Map<String, String>> loadItems(){
    	// TODO: Load items from storage
    	HashMap<String, String> itemMap = new HashMap<String, String>();
    	itemMap.put("title", "Title Uno");
    	itemMap.put("body", "Body uno");
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

}
