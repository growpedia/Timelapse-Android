package pro.dbro.timelapse;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TimeLapseCursorAdapter extends SimpleCursorAdapter {
	
	public TimeLapseCursorAdapter(Context context, Cursor c){
		super(context, R.layout.browser_list_item, c, new String[]{} , new int[]{}, 0);
	}

	
	@Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewCache view_cache = (ViewCache) view.getTag(R.id.view_children_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.headline = (TextView) view.findViewById(R.id.list_item_headline);
        	view_cache.body = (TextView) view.findViewById(R.id.list_item_body);
        	view_cache.thumbnail = (ImageView) view.findViewById(R.id.list_item_image);
            
        	view_cache.headline_col = cursor.getColumnIndexOrThrow("name");
        	view_cache.body_col = cursor.getColumnIndexOrThrow("description");
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow("thumbnail_path");
            view.setTag(R.id.view_children_cache, view_cache);
            
            //attach timelapse_id to view
            int db_index = cursor.getColumnIndex(SQLiteWrapper.COLUMN_TIMELAPSE_ID);
            int timelapse_id = cursor.getInt(db_index);
            view.setTag(R.id.view_related_timelapse, timelapse_id);
        }
        //Log.d("bindView","yeah");
        view_cache.headline.setText(cursor.getString(view_cache.headline_col));
        view_cache.body.setText(cursor.getString(view_cache.body_col));
        view_cache.thumbnail.setImageBitmap(BitmapFactory.decodeFile(cursor.getString(view_cache.thumbnail_col)));
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView headline;
        TextView body;
        ImageView thumbnail;
        
        int headline_col; 
        int body_col;
        int thumbnail_col;
    }
}

