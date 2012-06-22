/** Represent a TimeLapse */
package pro.dbro.timelapse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class TimeLapse implements Serializable{
	
	public String name;
	public String description;
	public Date creation_date;
	public Date modified_date;
	public String directory_path;
	public int id; // the directory name
	public int image_count;
	// List of filename Strings within directoryPath
	public ArrayList<String> images;
	
	public TimeLapse(String name, String description, int id){
		super();
		this.name = name;
		this.description = description;
		this.id = id;
	}

}
