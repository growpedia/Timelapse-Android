package pro.dbro.timelapse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// Good articles:
// http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html
// http://stackoverflow.com/questions/5786206/to-to-implement-a-sqlite-manager-for-thread-safe-read-write-access

/**
 * 
 * @author davidbrodsky
 * SQLiteManager includes all application-specific database code
 * The SQLiteWrapper static inner class remains application agnostic
 * for re-usability
 */
public class SQLiteManager{
	
    //for logging
    private final String TAG = this.getClass().getSimpleName();

    //DATABASE
    private static final String DATABASE_NAME = "timelapse.db";
    private static final int DATABASE_VERSION = 1;

    //TABLE NAMES
    private static final String TABLE_NAME = "timelapses";
    
    private static final String CREATE_TABLE_STATEMENT = "create table " + TABLE_NAME + " (_id integer primary key autoincrement, " 
	        + "name text not null, description text, " 
	        + "creation_date text not null, modified_date text not null, " 
	        + "image_count integer not null, directory_path text not null, " 
	        + "id integer not null, last_image_path text," 
	        + "thumbnail_path text, "
	        + "unique(id) on conflict replace);";
    
    // this is mostly good, except the _id integer increases by the total # of timelapses every app load...
    // why sqlite don't you have on conflict UPDATE

    //MEMBER VARIABLES
    //private SQLiteWrapper mSQLiteWrapper;
    private SQLiteDatabase mDB;
    
    //Fields describing application-specific Java Object corresponding to a Table row
    private static Field[] object_fields;

    //SINGLETON
    private static final SQLiteManager instance = new SQLiteManager();


    private SQLiteManager()
    {
        final SQLiteWrapper sqliteWrapper = new SQLiteWrapper(TimeLapseApplication.getContext());

        //open the DB for read and write
        mDB = sqliteWrapper.getWritableDatabase();
    }


    public static SQLiteManager getInstance()
    {
        return instance;
    }

    /**
     *  INSERT FUNCTIONS consisting of "synchronized" methods 
     */
    public synchronized long insertTableA(String myName, int myAge)
    {
        Long lValueToReturn;

        //organize the data to store as key/value pairs
        ContentValues kvPairs = new ContentValues();
        kvPairs.put("ColumnOne", myName);
        kvPairs.put("ColumnTwo", myAge);

        lValueToReturn = mDB.insert(TABLE_NAME, null, kvPairs);

        return lValueToReturn;
    }
    
    /**
     * 
     * @param keys Column names
     * @param values Column values
     */
    public synchronized void insert(Field[] keys, String[] values) {
    	
    	String sql = "INSERT INTO " + TABLE_NAME + "(";
    	
    	for(int x=0;x<keys.length;x++){
    		sql += "'" + keys[x].getName() + "'";
    		if(x != keys.length - 1)
    			sql += ", ";
    	}
    	
    	sql += ") values (";
    	
    	for(int x=0;x<values.length;x++){
    		sql += "'" + values[x] + "'";
    		if(x != values.length - 1)
    			sql += ", ";
    	}
    	
    	sql += ")";
    	try{
    	//Log.d("DBHelper","Insert SQL Generated: " + sql);
    	mDB.execSQL(sql);
    	Log.d("DBHelper","'INSERT Success'");
    	}
    	catch(Throwable t){
    		Log.d("DBHelper","fail: "+t.toString());
    	}
        
    }
    
    public synchronized void insertTimeLapse(TimeLapse tl){
    	//TODO: Multiple sqllite insert
    	// http://stackoverflow.com/questions/1609637/is-it-possible-to-insert-multiple-rows-at-a-time-in-an-sqlite-database

    	// Generic object field introspection ignore @NotForDatabase annotated fields
    	try{
    	if(object_fields == null){
    		Field[] all_object_fields = tl.getClass().getDeclaredFields();
    		ArrayList<Field> object_fields_list = new ArrayList<Field>();
    		for(int x=0;x<all_object_fields.length;x++){
    			Annotation[] annotations = all_object_fields[x].getDeclaredAnnotations();
    			if( all_object_fields[x].isAnnotationPresent(NotForDatabase.class) ){
    				Log.d("insertTimeLapse","skipping NotForExport field");
    				continue;
    			}
        		object_fields_list.add(all_object_fields[x]);
        	}
    		//object_fields = (Field[]) object_fields_list.toArray(contents);
    		object_fields = new Field[object_fields_list.size()];
    		object_fields_list.toArray(object_fields);
    	}
    	
    	String[] object_values = new String[object_fields.length];
    	for(int x=0;x<object_fields.length;x++){
    		try {
				object_values[x] = object_fields[x].get(tl).toString();
			} catch (NullPointerException e){
				object_values[x] = null;
				
			} catch (IllegalArgumentException e) {
				Log.d("Insert Error", e.toString());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				Log.d("Insert Error", e.toString());
			}
    	}
    	insert(object_fields, object_values);
    	
    	}catch(NullPointerException exception){
    		Log.d("insertTimeLapse",""+exception.toString());
    	}
    	

    }
    
    /**
     *  Update TimeLapse given parallel String[]s of key/values
     */
	public synchronized void updateTimeLapse(String[] keys, String[] values, long id) {
		// this is a key value pair holder used by android's SQLite functions
		if (keys.length != values.length)
			return;
		
		ContentValues content_values = new ContentValues();
		
		for(int x=0; x< keys.length;x++){
			content_values.put(keys[x], values[x]);
		}
	
		// ask the database object to update the database row of given rowID
		try {mDB.update(TABLE_NAME, content_values,  "_id=" + id, null);}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	/**
     * The Delete DB statement
     * @param number the callers number to delete
     * @param name the friend's name to delete
     */
    public void delete(long id) {
    	int i = mDB.delete(TABLE_NAME, "_id=" + id, null);
    }
    
    /**
     * Select All returns a cursor
     * @return the cursor for the DB selection
     */
    public Cursor cursorSelectAll() {
        Cursor cursor = mDB.query(
                TABLE_NAME, // Table Name
                null, // Columns to return. Null returns all
                null,       // SQL WHERE
                null,       // Selection Args
                null,       // SQL GROUP BY 
                null,       // SQL HAVING
                "id DESC");    // SQL ORDER BY
        return cursor;
    }
    
    /**
     * Wipe out the DB
     */
    public void clearAll() {
        mDB.delete(TABLE_NAME, null, null);
    }
    

    /**
     * 
     * @author davidbrodsky
     * SQLiteWrapper is written to be application agnostic.
     * requires Strings: DATABASE_NAME, DATABASE_VERSION,
     * CREATE_TABLE_STATEMENT, TABLE_NAME
     */
	static class SQLiteWrapper extends SQLiteOpenHelper {
	    
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
}