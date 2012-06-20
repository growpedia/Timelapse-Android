/** Represent a TimeLapse */
package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.Date;

public class TimeLapse {
	
	public String name;
	public String description;
	public Date creationDate;
	public Date modifiedDate;
	public String directoryPath;
	// List of filename Strings within directoryPath
	public ArrayList<String> images;

}
