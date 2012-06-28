/** Represent a TimeLapse */
package pro.dbro.timelapse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class TimeLapse implements Serializable{
	
	public String name = "";
	public String description = "";
	public Date creation_date;
	public Date modified_date;
	public int image_count = 0;
	
	@NotForExport
	public String directory_path;
	@NotForExport
	public int id; // the directory name
	
	// List of filename Strings within directoryPath
	@NotForExport
	public ArrayList<String> images;
	
	public TimeLapse(String name, String description, int id){
		super();
		this.name = name;
		this.description = description;
		this.id = id;
		this.modified_date = new Date();
		this.creation_date = new Date();
		
		// Create the filesystem representation of this TimeLapse in another thread
		new FileUtils.SaveTimeLapsesOnFilesystem().execute(this);
	}
	
	// Set title and description, and change representation on filesystem
	public void setTitleAndDescription(String title, String description){
		this.name = title;
		this.description = description;
		
		// update the TimeLapse's representation on external storage
		new FileUtils.SaveTimeLapsesOnFilesystem().execute(this);
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
