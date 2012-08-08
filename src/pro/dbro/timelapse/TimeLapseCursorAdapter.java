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
            
        	view_cache.headline_col = cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_NAME);
        	view_cache.body_col = cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_DESCRIPTION);
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_THUMBNAIL_PATH);
        	view_cache.timelapse_id_col = cursor.getColumnIndexOrThrow(SQLiteWrapper.COLUMN_TIMELAPSE_ID);
            view.setTag(R.id.view_children_cache, view_cache);
            //tag view with timelapse id
            view.setTag(R.id.view_related_timelapse, cursor.getInt(view_cache.timelapse_id_col));
        }
        //Log.d("bindView","yeah");
        view_cache.headline.setText(cursor.getString(view_cache.headline_col));
        view_cache.body.setText(cursor.getString(view_cache.body_col));
        view_cache.thumbnail.setImageBitmap(BitmapFactory.decodeFile(cursor.getString(view_cache.thumbnail_col)));
        view.setTag(R.id.view_related_timelapse, cursor.getInt(view_cache.timelapse_id_col));
    }
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView headline;
        TextView body;
        ImageView thumbnail;
        
        int headline_col; 
        int body_col;
        int thumbnail_col;
        int timelapse_id_col;
    }
}

