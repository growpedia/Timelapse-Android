package pro.dbro.timelapse;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// This guide rules: http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html

public class DBHelper extends SQLiteOpenHelper {
    private SQLiteDatabase db;
    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "timelapse.db";
    private static final String TABLE_NAME = "timelapses";
    
    //Schema: Number, Name, Datetime [YYYYMMDDKKMMSS]

    /**
     * Constructor
     * @param context the application context
     */
    public DBHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }
    
    /**
     * Called at the time to create the DB.
     * The create DB statement
     * @param the SQLite DB
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table " + TABLE_NAME + " (_id integer primary key autoincrement, " 
        + " name text not null, description text not null, " 
        + "creation_date text not null, modified_date text not null, " 
        + "image_count integer not null, directory_path text not null " 
        + "id integer not null, last_image_path text not null" 
        + "thumbnail_path text not null ) ");
    }
    
    /**
     * The Insert DB statement
     * @param number the callers number to insert
     * @param name the friend's name to insert
     */
    public void insert(String name, String description, String creation_date, String modified_date,
    				   int image_count, String directory_path, int id, String last_image_path,
    				   String thumbnail_path) {
        db.execSQL("INSERT INTO "+ TABLE_NAME + "('name', 'description', 'creation_date', 'modified_date'," 
    			   +"'image_count', 'directory_path', 'id', 'last_image_path') values ('"
                + name + "', '"
                + description + "', '"
                + creation_date + "', '"
                + modified_date + "', '"
                + image_count + "', '"
                + directory_path + "', '"
                + id + "', '"
                + last_image_path + "', '"
                + thumbnail_path + "')");
    }
    
    /**
     * The Delete DB statement
     * @param number the callers number to delete
     * @param name the friend's name to delete
     */
    public void delete(long id) {
    	int i = db.delete(TABLE_NAME, "_id=" + id, null);
    }
    
    /**
     * Select All returns a cursor
     * @return the cursor for the DB selection
     */
    public Cursor cursorSelectAll() {
        Cursor cursor = this.db.query(
                TABLE_NAME, // Table Name
                new String[] { "_id", "number", "name", "time", "success" }, // Columns to return
                null,       // SQL WHERE
                null,       // Selection Args
                null,       // SQL GROUP BY 
                null,       // SQL HAVING
                "time DESC");    // SQL ORDER BY
        return cursor;
    }
    
    /**
     * Wipe out the DB
     */
    public void clearAll() {
        db.delete(TABLE_NAME, null, null);
    }
    
    /**
     * Invoked if a DB upgrade (version change) has been detected
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, 
       int oldVersion, int newVersion) {
        // Migration stuff goes here
}

	public void update(String number, String result, String time, int success,
			long id) {
		// this is a key value pair holder used by android's SQLite functions
		ContentValues values = new ContentValues();
		values.put("number", number);
		values.put("name", result);
		values.put("success", success);
	 
		// ask the database object to update the database row of given rowID
		try {db.update(TABLE_NAME, values,  "_id=" + id, null);}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}