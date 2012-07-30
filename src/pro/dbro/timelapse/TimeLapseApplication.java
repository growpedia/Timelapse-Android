package pro.dbro.timelapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class TimeLapseApplication extends Application {
	// id -> TimeLapse
	public HashMap<Integer,TimeLapse> time_lapse_map = new HashMap<Integer,TimeLapse>();
	public int nextTimeLapseId = 0;
	
	// Singleton
	private static TimeLapseApplication instance;

    public TimeLapseApplication()
    {
        instance = this;
    }

    public static Context getContext()
    {
        return instance;
    }

	
	public void setTimeLapses(ArrayList<TimeLapse> list){
		// Transfer list items into map
		for(int x = 0; x < list.size(); x++){
			time_lapse_map.put(((TimeLapse)list.get(x)).id, list.get(x));
		}
		setNextTimeLapseId();
	}
	
	public void setTimeLapseTitleAndDescription(int timelapse_id, String title, String description){
		
		((TimeLapse)time_lapse_map.get(timelapse_id)).setTitleAndDescription(title, description);
	}
	
	public void createTimeLapse(String title, String description){
		time_lapse_map.put(nextTimeLapseId, new TimeLapse(title, description, nextTimeLapseId));
		Log.d("TimeLapseApplication","created TimeLapse " + String.valueOf(nextTimeLapseId));
		nextTimeLapseId ++;
		
	}
	
	private void setNextTimeLapseId(){
		Object[] keys = (Object[]) time_lapse_map.keySet().toArray();
		// find highest TimeLapse.id
		for(int x = 0; x < keys.length; x++){
			if(((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id > nextTimeLapseId)
				nextTimeLapseId = ((TimeLapse)time_lapse_map.get(Integer.parseInt(keys[x].toString()))).id;
		}
		// add a 1 to it
		nextTimeLapseId++;
		Log.d("TimeLapseApplication","nextID: " + String.valueOf(nextTimeLapseId));
	}
}
