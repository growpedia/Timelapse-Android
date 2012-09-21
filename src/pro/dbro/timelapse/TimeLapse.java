/** Represent a TimeLapse */
package pro.dbro.timelapse;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import android.content.ContentValues;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class TimeLapse implements Serializable{
	
	public String name = "";
	public String description = "";
	public Date creation_date;
	public Date modified_date;
	public int image_count = 0;
	
	@NotForExport
	public String directory_path;	// absolute directory path
	@NotForExport
	public int id; 					// directory name
	@NotForExport
	public String last_image_path;	// path to last image. Set by FileUtils.findOrGenerateThumbnail. Used by CameraActivity for overlay
	@NotForExport
	public String thumbnail_path;	// path to current thumbnail. Set by FileUtils.findOrGenerateThumbnail
	@NotForDatabase
	@NotForExport
	public static final String thumbnail_dir = "thumbnails";
	@NotForDatabase
	@NotForExport
	public static final String thumbnail_suffix = "_thumb";
	@NotForDatabase
	@NotForExport
	public static int thumbnail_height = 240;	// pixels
	@NotForDatabase
	@NotForExport
	public static int thumbnail_width = 320;	// pixels
		
	// List of filename Strings within directoryPath
	//@NotForExport
	//public ArrayList<String> images;
	
	public TimeLapse(String name, String description, int id){
		super();
		this.name = name;
		this.description = description;
		this.id = id;
		this.modified_date = new Date();
		this.creation_date = new Date();
		
		// Create the filesystem representation of this TimeLapse in another thread
		new FileUtils.SaveTimeLapsesOnFilesystem().execute(this.toContentValues());
	}
	
	public String toString(){
		return String.valueOf(this.id) + "-" + this.name;
	}
	
	public ContentValues toContentValues(){
		ContentValues result = new ContentValues();
		result.put(SQLiteWrapper.COLUMN_CREATION_DATE, creation_date.toString());
		result.put(SQLiteWrapper.COLUMN_DESCRIPTION, description);
		result.put(SQLiteWrapper.COLUMN_DIRECTORY_PATH, directory_path);
		result.put(SQLiteWrapper.COLUMN_IMAGE_COUNT, String.valueOf(image_count));
		result.put(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH, last_image_path);
		result.put(SQLiteWrapper.COLUMN_MODIFIED_DATE, modified_date.toString());
		result.put(SQLiteWrapper.COLUMN_NAME, name);
		result.put(SQLiteWrapper.COLUMN_THUMBNAIL_PATH, thumbnail_path);
		result.put(SQLiteWrapper.COLUMN_TIMELAPSE_ID, String.valueOf(id));
		
		return result;
	}
	/* Deprecated: FileUtils.generateThumbnail sets field on successful thumbnail generation
	public String getThumbnailPath(){

		if(image_count != 0){
			if(directory_path != null){
				return directory_path + File.separator + thumbnail_dir + File.separator + String.valueOf(image_count)+".jpeg";
			}
		}
		return "";
	}
	*/
	
	// Set title and description, and change representation on filesystem
	public void setTitleAndDescription(String title, String description){
		this.name = title;
		this.description = description;
		
		// update the TimeLapse's representation on external storage
		new FileUtils.SaveTimeLapsesOnFilesystem().execute(this.toContentValues());
	}
	
	 // Excludes any field from JSON serializer that is tagged with an "@NotForExport"
	 public static class JsonExclusionStrategy implements ExclusionStrategy {
		 public boolean shouldSkipClass(Class<?> clazz) {
		     return clazz.getAnnotation(NotForExport.class) != null;
		   }
	 
		 public boolean shouldSkipField(FieldAttributes f) {
		     return f.getAnnotation(NotForExport.class) != null;
		 }
	 }
	 
	 // Determines how Lists of TimeLapse objects are sorted
	 public static class TimeLapseComparator implements Comparator<TimeLapse> {
		    public int compare(TimeLapse object1, TimeLapse object2) {
		        return object1.modified_date.compareTo(object2.modified_date);
		    }
	}

}
