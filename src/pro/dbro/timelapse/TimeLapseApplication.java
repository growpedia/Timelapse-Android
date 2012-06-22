package pro.dbro.timelapse;

import java.util.ArrayList;

import android.app.Application;

public class TimeLapseApplication extends Application {
	
	public ArrayList<TimeLapse> time_lapses = new ArrayList<TimeLapse>();
	public int nextTimeLapseId = -1;
	
	public void setTimeLapses(ArrayList<TimeLapse> list){
		time_lapses = list;
		setNextTimeLapseId();
	}
	
	public void createTimeLapse(String title, String description){
		time_lapses.add(new TimeLapse(title, description, nextTimeLapseId));
		nextTimeLapseId ++;
		
	}
	
	private void setNextTimeLapseId(){
		for(int x = 0; x < time_lapses.size(); x++){
			if(((TimeLapse)time_lapses.get(x)).id > nextTimeLapseId)
				nextTimeLapseId = ((TimeLapse)time_lapses.get(x)).id;
		}
		nextTimeLapseId++;
	}
}
