package pro.dbro.timelapse;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class TimeLapseViewerActivity extends SherlockActivity {
	
	private TimeLapseApplication tla;
	private EditText title;
	private EditText description;
	private Button createButton;
	
	private int timelapse_id = -1;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
        title = (EditText) findViewById(R.id.create_timelapse_title);
        description = (EditText) findViewById(R.id.create_timelapse_description);
        createButton =  (Button) findViewById(R.id.create_timelapse_button);
        
        tla = (TimeLapseApplication)getApplicationContext();
        
        Intent intent = getIntent();
        
        timelapse_id = intent.getExtras().getInt("timelapse_id");
        Log.d("TimeLapseViewerActivity", "TL id : " + String.valueOf(timelapse_id));
        if(timelapse_id == -1){
        	// new timelapse
        	createButton.setVisibility(View.VISIBLE);
        	createButton.setOnClickListener(createTimeLapseListener);
        }
        else{
        	if(tla.time_lapse_map.containsKey(timelapse_id)){
        		title.setText(((TimeLapse)tla.time_lapse_map.get(timelapse_id)).name);
            	description.setText(((TimeLapse)tla.time_lapse_map.get(timelapse_id)).description);
        	}
        }
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	// Save title / description
    	Log.d("TimeLapseViewer onPause","TL id: " + timelapse_id);
    	if(tla.time_lapse_map.containsKey(timelapse_id)){
    		Log.d("TimeLapseViewer OnPause","saving..");
    		((TimeLapse)tla.time_lapse_map.get(timelapse_id)).name = title.getText().toString();
    		((TimeLapse)tla.time_lapse_map.get(timelapse_id)).description = description.getText().toString();
    	}
    	
    }
    
    // Populate ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.layout.timelapse_viewer_menu, menu);
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
    
    private OnClickListener createTimeLapseListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(title.getText().toString().compareTo("") == 0){
				return;
			}
			else{
				tla.createTimeLapse(title.getText().toString(), description.getText().toString());
				//findViewById(R.id.create_timelapse_button).setVisibility(View.GONE);
				Log.d("TimeLapse Created",title.getText().toString() + " " + description.getText().toString());
				Intent intent = new Intent(TimeLapseViewerActivity.this, BrowserActivity.class);
                startActivity(intent);
			}
			
		}
    	
    };
}
