package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;

public class TimeLapseApplication extends Application {
	// id -> TimeLapse
	public HashMap<Integer,TimeLapse> time_lapse_map = new HashMap<Integer,TimeLapse>();
	public int nextTimeLapseId = 1;

	
	public void setTimeLapses(ArrayList<TimeLapse> list){
		// Transfer list items into map
		for(int x = 0; x < list.size(); x++){
			time_lapse_map.put(((TimeLapse)list.get(x)).id, list.get(x));
		}
		setNextTimeLapseId();
	}
	
	public void createTimeLapse(String title, String description){
		time_lapse_map.put(nextTimeLapseId, new TimeLapse(title, description, nextTimeLapseId));
		nextTimeLapseId ++;
	}
	
	private void setNextTimeLapseId(){
		Object[] keys = (Object[]) time_lapse_map.keySet().toArray();
		for(int x = 0; x < keys.length; x++){
			if(((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id > nextTimeLapseId)
				nextTimeLapseId = ((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id;
		}
		nextTimeLapseId++;
	}
}
