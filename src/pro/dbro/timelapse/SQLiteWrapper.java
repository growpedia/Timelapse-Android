package pro.dbro.timelapse;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


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
    
    public static final String CREATE_TABLE_STATEMENT = "create table " + TABLE_NAME + " ("+ COLUMN_ID +"integer primary key autoincrement, " 
	        +  COLUMN_NAME + " text not null, "+ COLUMN_DESCRIPTION+" text, " 
	        +  COLUMN_CREATION_DATE + " text not null, "+ COLUMN_MODIFIED_DATE +" text not null, " 
	        +  COLUMN_IMAGE_COUNT + " integer not null, "+ COLUMN_DIRECTORY_PATH +" text not null, " 
	        +  COLUMN_TIMELAPSE_ID + " integer not null, "+COLUMN_LAST_IMAGE_PATH +" text," 
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
    
   
    
}