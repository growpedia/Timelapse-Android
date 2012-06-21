package pro.dbro.timelapse;

import android.util.Log;
import android.view.View;

public class BrowserViewBinder implements android.widget.SimpleAdapter.ViewBinder {

	@Override
	public boolean setViewValue(View browser_list_item, Object data,
			String textRepresentation) {
		
		Log.d("BrowserViewBinder", browser_list_item.getClass().toString());
		if( browser_list_item.getClass().toString().equals("class pro.dbro.timelapse.ListItemRelativeLayout")){
			browser_list_item.setTag(data);
			return true; // do not ask SimpleAdapter to do binding on this view
		}
		
		return false; // pass binding to SimpleAdapter
	}

}
