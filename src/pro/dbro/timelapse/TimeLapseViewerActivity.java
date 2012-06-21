package pro.dbro.timelapse;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockActivity;

public class TimeLapseViewerActivity extends SherlockActivity {

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // With the ActionBar, we no longer need to hide the hideous Android Window Title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.timelapse);
        
    }
}
