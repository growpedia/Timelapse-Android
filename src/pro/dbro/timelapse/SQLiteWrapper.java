package pro.dbro.timelapse;

import android.content.ContentValues;
import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * 
 * @author davidbrodsky
 * @description SQLiteWrapper is written to be application agnostic.
 * requires Strings: DATABASE_NAME, DATABASE_VERSION,
 * CREATE_TABLE_STATEMENT, TABLE_NAME
 */
class SQLiteWrapper extends SQLiteOpenHelper {
	
	//DATABASE INFO
    public static final String DATABASE_NAME = "timelapse.db";
    public static final int DATABASE_VERSION = 1;

    //TABLE INFO
    public static final String TABLE_NAME = "timelapses";
    
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CREATION_DATE = "creation_date";
    public static final String COLUMN_MODIFIED_DATE = "modified_date";
    public static final String COLUMN_IMAGE_COUNT = "image_count";
    public static final String COLUMN_DIRECTORY_PATH = "directory_path";
    public static final String COLUMN_TIMELAPSE_ID = "id";
    public static final String COLUMN_LAST_IMAGE_PATH = "last_image_path";
    public static final String COLUMN_THUMBNAIL_PATH = "thumbnail_path";
    
    public static final String[] COLUMNS = {COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_CREATION_DATE, COLUMN_MODIFIED_DATE,
    										COLUMN_IMAGE_COUNT, COLUMN_DIRECTORY_PATH, COLUMN_TIMELAPSE_ID, COLUMN_LAST_IMAGE_PATH, COLUMN_THUMBNAIL_PATH};
    
    public static final String CREATE_TABLE_STATEMENT = "create table " + TABLE_NAME + " ("+ COLUMN_ID +" integer primary key autoincrement, " 
	        +  COLUMN_NAME + " text not null, "+ COLUMN_DESCRIPTION + " text, " 
	        +  COLUMN_CREATION_DATE + " text not null, "+ COLUMN_MODIFIED_DATE +" text not null, " 
	        +  COLUMN_IMAGE_COUNT + " integer not null , " + COLUMN_DIRECTORY_PATH +" text not null, " 
	        +  COLUMN_TIMELAPSE_ID + " integer not null, " + COLUMN_LAST_IMAGE_PATH +" text," 
	        +  COLUMN_THUMBNAIL_PATH + " text, "
	        + "unique(id) on conflict replace);";
   
    //Schema: Number, Name, Datetime [YYYYMMDDKKMMSS]
    /**
     * Constructor
     * @param context the application context
     */
    public SQLiteWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * Called at the time to create the DB.
     * The create DB statement
     * @param the SQLite DB
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_STATEMENT);
    }
    
    /**
     * Invoked if a DB upgrade (version change) has been detected
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, 
       int oldVersion, int newVersion) {
        // Drop old table and re-create
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
    }
    
    public static ContentValues cursorRowToContentValues(Cursor cursor){
    	ContentValues values = new ContentValues();
    	
    	if(cursor == null || !cursor.moveToFirst())
    		return values;
    	
    	AbstractWindowedCursor awc =
                (cursor instanceof AbstractWindowedCursor) ? (AbstractWindowedCursor) cursor : null;

        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
        	//Log.d("cursorRowToContentValues",columns[i] + " null: " + String.valueOf(cursor.isNull(i)));
        	if(cursor.isNull(i)){
        		// Don't insert null table records into ContentValues
        		continue;
        	}
        	else{
	            if (awc != null && awc.isBlob(i)) {
	                values.put(columns[i], cursor.getBlob(i));
	            } else {
	                values.put(columns[i], cursor.getString(i));
	            }
        	}
        }
        
    	return values;
    }
    
    
    
   
    
}