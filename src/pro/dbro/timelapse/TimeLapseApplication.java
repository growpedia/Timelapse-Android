package pro.dbro.timelapse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class TimeLapseApplication extends Application {
	
	/**
	 * Content Resolver Wrapper methods
	 */
	
	public Cursor getTimeLapseById(int _id, String[] columns){
		// Query ContentResolver for related timelapse
		String selectionClause = SQLiteWrapper.COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(_id)};
        
        // If no columns provided, return all
        if(columns == null)
        	columns = SQLiteWrapper.COLUMNS;
       return getContentResolver().query(
        	    TimeLapseContentProvider.CONTENT_URI,  // The content URI of the words table
        	    columns,                // The columns to return for each row
        	    selectionClause,                    // Selection criteria
        	    selectionArgs,                     // Selection criteria
        	    null);                        	   // The sort order for the returned rows
	}
	
	public boolean updateTimeLapseById(int _id, String[] columns, String[] values){
		//public int update(Uri uri, ContentValues values, String selection,
		//		String[] selectionArgs) {		
		ContentValues contentValues = new ContentValues();
		for(int x=0;x<columns.length;x++){
			contentValues.put(columns[x], values[x]);
		}
		
		Date now = new Date();
		SimpleDateFormat iso8601Format = new SimpleDateFormat(
	            "yyyy-MM-dd HH:mm:ss");
		contentValues.put(SQLiteWrapper.COLUMN_MODIFIED_DATE, iso8601Format.format(now));
		
		//make sure timelapse-id is included
		contentValues.put(SQLiteWrapper.COLUMN_ID, _id);
		
		return updateOrInsertTimeLapseByContentValues(contentValues);
	}
	

	/**
	 * Attempt to update a TimeLapse record in the TimeLapseContentProvider.
	 * Failing this, inserts a new record.
	 * @param cv ContentValues representing a timelapse object
	 * @return true if an existing row was updated, false if a new row was created
	 */
	public boolean updateOrInsertTimeLapseByContentValues(ContentValues cv){
		String selectionClause = null;
		String[] selectionArgs = null;
		if(cv.containsKey(SQLiteWrapper.COLUMN_ID)){
			selectionClause = SQLiteWrapper.COLUMN_ID + " = ?";
			selectionArgs = new String[]{cv.getAsString(SQLiteWrapper.COLUMN_ID)};
		} else if(cv.containsKey(SQLiteWrapper.COLUMN_TIMELAPSE_ID)){
			selectionClause = SQLiteWrapper.COLUMN_TIMELAPSE_ID + " = ?";
			selectionArgs = new String[]{cv.getAsString(SQLiteWrapper.COLUMN_TIMELAPSE_ID)};
		}
		//Cursor testQuery = getContentResolver().query(TimeLapseContentProvider.CONTENT_URI, null, selectionClause, selectionArgs, null);
		//Log.d("QueryTest-getCount", String.valueOf(testQuery.getCount()));
		//Log.d("QueryTest-ID", String.valueOf(testQuery.getString(testQuery.getColumnIndex(SQLiteWrapper.COLUMN_TIMELAPSE_ID))));
		
		int numUpdated = getContentResolver().update(
				TimeLapseContentProvider.CONTENT_URI, 
				cv, 
				selectionClause, 
				selectionArgs);
		
		if(numUpdated > 0){
			Log.d("updateOrInsertTimeLapseByContentValue","success");
			return true;
		} else{
			Log.d("updateOrInsertTimeLapseByContentValue","kindly note that this behavior is currently fucked");
			//TODO: Add defaults for not null fields
			getContentResolver().insert(TimeLapseContentProvider.CONTENT_URI, cv);
			return false;
		}
		
	}
	
	/**
	 * Given parallel arrays of columns and values, create a timelapse
	 * in the TimeLapseContentProvider, as well as on the External Filesystem
	 * @param columns
	 * @param values
	 * @return
	 */
	public Uri createTimeLapse(String[] columns, String[] values){
		ContentValues contentValues = new ContentValues();
		if(columns != null && values != null && columns.length == values.length){
			for(int x=0;x<columns.length;x++){
				contentValues.put(columns[x], values[x]);
			}
		}
		// Determine next timelapse_id
		int next_timelapse_id = 1;
		Cursor cursor = getContentResolver().query(TimeLapseContentProvider.CONTENT_URI, new String[] {SQLiteWrapper.COLUMN_TIMELAPSE_ID}, null, null, SQLiteWrapper.COLUMN_TIMELAPSE_ID + " DESC");
		if(cursor.moveToFirst()){
			next_timelapse_id = cursor.getInt(cursor.getColumnIndex(SQLiteWrapper.COLUMN_TIMELAPSE_ID)) + 1;
		}
		//File timelapse_dir = getOutputMediaDir(input[0].getAsInteger(SQLiteWrapper.COLUMN_TIMELAPSE_ID));
		//Log.d("Directory_path", "" + FileUtils.getOutputMediaDir(next_timelapse_id).getAbsolutePath());
		contentValues.put(SQLiteWrapper.COLUMN_DIRECTORY_PATH, FileUtils.getOutputMediaDir(next_timelapse_id).getAbsolutePath());

		// Add other initial data
		Date now = new Date();
		SimpleDateFormat iso8601Format = new SimpleDateFormat(
	            "yyyy-MM-dd HH:mm:ss");
		contentValues.put(SQLiteWrapper.COLUMN_CREATION_DATE, iso8601Format.format(now));
		contentValues.put(SQLiteWrapper.COLUMN_MODIFIED_DATE, iso8601Format.format(now));
		
		contentValues.put(SQLiteWrapper.COLUMN_IMAGE_COUNT, 0);
		contentValues.put(SQLiteWrapper.COLUMN_TIMELAPSE_ID, String.valueOf(next_timelapse_id));
		
		// If no title provided, use a sensible default
		if(columns == null || !Arrays.asList(columns).contains(SQLiteWrapper.COLUMN_NAME)){
			SimpleDateFormat humanFormat = new SimpleDateFormat(
		            "MM/dd/yy H:m");
			contentValues.put(SQLiteWrapper.COLUMN_NAME, humanFormat.format(now));
		}
		

		// Save TimeLapse to filesystem
		new FileUtils.SaveTimeLapsesOnFilesystem().execute(contentValues);
		Log.d("TimeLapseCollision","Writing from CreateTimeLapse");
		cursor.close();
		
		// Save TimeLapse to ContentProvider, returning URI
		return getContentResolver().insert(TimeLapseContentProvider.CONTENT_URI, contentValues);
	}

	
	public boolean serviceIsRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("pro.dbro.timelapse.service.GifExportService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}
