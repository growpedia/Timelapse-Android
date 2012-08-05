package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class TimeLapseApplication extends Application {
	// id -> TimeLapse
	public HashMap<Integer,TimeLapse> time_lapse_map = new HashMap<Integer,TimeLapse>();
	public int nextTimeLapseId = 0;
	
	// Singleton
	private static TimeLapseApplication instance;

    public TimeLapseApplication()
    {
        instance = this;
    }

    public static Context getContext()
    {
        return instance;
    }

	
	public void setTimeLapses(ArrayList<TimeLapse> list){
		// Transfer list items into map
		for(int x = 0; x < list.size(); x++){
			time_lapse_map.put(((TimeLapse)list.get(x)).id, list.get(x));
		}
		setNextTimeLapseId();
	}
	
	public void setTimeLapseTitleAndDescription(int timelapse_id, String title, String description){
		
		((TimeLapse)time_lapse_map.get(timelapse_id)).setTitleAndDescription(title, description);
	}
	
	public void createTimeLapse(String title, String description){
		time_lapse_map.put(nextTimeLapseId, new TimeLapse(title, description, nextTimeLapseId));
		Log.d("TimeLapseApplication","created TimeLapse " + String.valueOf(nextTimeLapseId));
		nextTimeLapseId ++;
		
	}
	
	private void setNextTimeLapseId(){
		Object[] keys = (Object[]) time_lapse_map.keySet().toArray();
		// find highest TimeLapse.id
		for(int x = 0; x < keys.length; x++){
			if(((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id > nextTimeLapseId)
				nextTimeLapseId = ((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id;
		}
		// add a 1 to it
		nextTimeLapseId++;
		Log.d("TimeLapseApplication","nextID: " + String.valueOf(nextTimeLapseId));
	}
	
	
	/**
	 * Content Resolver Wrapper methods
	 */
	
	public Cursor getTimeLapseById(int timelapse_id, String[] columns){
		// Query ContentResolver for related timelapse
        String SelectionClause = SQLiteWrapper.COLUMN_TIMELAPSE_ID + " = ?";
        String[] SelectionArgs = {String.valueOf(timelapse_id)};
        
        // If no columns provided, return all
        if(columns == null)
        	columns = SQLiteWrapper.COLUMNS;
        
       return getContentResolver().query(
        	    TimeLapseContentProvider.CONTENT_URI,  // The content URI of the words table
        	    columns,                // The columns to return for each row
        	    SelectionClause,                    // Selection criteria
        	    SelectionArgs,                     // Selection criteria
        	    null);                        	   // The sort order for the returned rows
	}
	
	public boolean updateTimeLapseById(int timelapse_id, String[] columns, String[] values){
		//public int update(Uri uri, ContentValues values, String selection,
		//		String[] selectionArgs) {
		String selectionClause = SQLiteWrapper.COLUMN_TIMELAPSE_ID + " = ?";
		String[] selectionArgs = {String.valueOf(timelapse_id)};
		
		ContentValues contentValues = new ContentValues();
		for(int x=0;x<columns.length;x++){
			contentValues.put(columns[x], values[x]);
		}
		
		int numUpdated = getContentResolver().update(
				TimeLapseContentProvider.CONTENT_URI, 
				contentValues, 
				selectionClause, 
				selectionArgs);
		
		if(numUpdated > 0)
			return true;
		else
			return false;
	}
	
	public boolean createTimeLapse(String[] columns, String[] values){
		ContentValues contentValues = new ContentValues();
		for(int x=0;x<columns.length;x++){
			contentValues.put(columns[x], values[x]);
		}
		
		int next_timelapse_id = 1;
		Cursor cursor = getContentResolver().query(TimeLapseContentProvider.CONTENT_URI, new String[] {SQLiteWrapper.COLUMN_ID}, null, null, SQLiteWrapper.COLUMN_TIMELAPSE_ID + "DESC");
		if(cursor.moveToFirst()){
			next_timelapse_id = cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_TIMELAPSE_ID)) + 1;
		}

		contentValues.put(SQLiteWrapper.COLUMN_TIMELAPSE_ID, String.valueOf(next_timelapse_id));
		getContentResolver().insert(TimeLapseContentProvider.CONTENT_URI, contentValues);
		return true;
	}
}
