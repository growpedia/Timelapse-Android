package pro.dbro.timelapse;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

public class TimeLapseArrayAdapter extends ArrayAdapter<TimeLapse> {
	private static final int LAYOUT_ID = R.layout.browser_list_item;
	
	// Cache of re-used objects in getView
	LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
	TimeLapse timelapse;
	
	public TimeLapseArrayAdapter(Context context, ArrayList<TimeLapse> objects) {
		super(context, LAYOUT_ID, objects);
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewCache view_cache = null;
		
		if(convertView == null){
			convertView = inflater.inflate(LAYOUT_ID, parent, false);
			view_cache = new ViewCache();
			view_cache.thumbnail = (ImageView)convertView.findViewById(R.id.list_item_image);
			view_cache.title = (TextView)convertView.findViewById(R.id.list_item_headline);
			view_cache.body = (TextView)convertView.findViewById(R.id.list_item_body);
			convertView.setTag(R.id.view_cache, view_cache);
		}
		else{
			view_cache = (ViewCache)convertView.getTag(R.id.view_cache);
		}
		timelapse = getItem(position);
		
		// Set thumbnail 
		view_cache.thumbnail.setImageBitmap(BitmapFactory.decodeFile(timelapse.thumbnail_path));
		// Set name
		view_cache.title.setText(timelapse.name);
		// Set description
		view_cache.body.setText(timelapse.description);
		// Tag ListView item root view with timelapse id
		convertView.setTag(R.id.view_related_timelapse, timelapse.id);
		// Set ListView item id = timelapse_id
		// TODO: remove this
		convertView.setId(timelapse.id);
		
		return convertView;
	}
	
	public boolean editItemTitle(int position, String newName){
		TimeLapse tl = getItem(position);
		tl.name = newName;
		notifyDataSetChanged();
		return true;
	}
	
	// Cache views used in each ListView item
	// to avoid repeated calls to findViewById(...)
	static class ViewCache
	{
		ImageView thumbnail;
		TextView title;
		TextView body;
	}

}
