package pro.dbro.timelapse.viewgroup;

import pro.dbro.timelapse.TimeLapseViewerActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class FocusControlRelativeLayout extends RelativeLayout {

	public FocusControlRelativeLayout(Context context) {
		super(context);
	}
	
	public FocusControlRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public FocusControlRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public boolean onInterceptTouchEvent(MotionEvent me){
		// On  any layout touch outside of the text inputs, set focus to layout (thus hiding soft keyboard)
		// and pass the touch event downstream
		// Thus, the text inputs can still respond and take focus if needed
		// BUT touches with no clear intent remove focus from the text views and remove keyboard
		if(me.getAction() == me.ACTION_DOWN && findViewById(pro.dbro.timelapse.R.id.create_timelapse_title).hasFocus()){
			// If the touch doesn't occur in the area of text input
			if(!isPointInsideView(me.getRawX(), me.getRawY(), (findViewById(pro.dbro.timelapse.R.id.create_timelapse_title)))){
				TimeLapseViewerActivity.hideSoftKeyboard(this);
				this.requestFocus();
				//if(isPointInsideView(me.getRawX(), me.getRawY(), findViewById(R.id.map)) || isPointInsideView(me.getRawX(), me.getRawY(), (findViewById(R.id.reverse))) )
				//	return false; // allow direct touch of map and reverse buttons from text editing
				return false; // pass all touches downstream
			}
		}
		
		// each following event (up to and including the final up) 
		// will be delivered first here and then to the target's onTouchEvent().
		//return false - tablelayout views animate
		// return true - no touches get passed
		return false;
	}
	
	public boolean onTouchEvent(MotionEvent me){
		return false;
		
	}
	
	/**
	 * Determines if given points are inside view
	 * @param x - x coordinate of point
	 * @param y - y coordinate of point
	 * @param view - view object to compare
	 * @return true if the points are within view bounds, false otherwise
	 */
	private boolean isPointInsideView(float x, float y, View view){
	    int location[] = new int[2];
	    view.getLocationOnScreen(location);
	    int viewX = location[0];
	    int viewY = location[1];

	    //point is inside view bounds
	    if(( x > viewX && x < (viewX + view.getWidth())) &&
	            ( y > viewY && y < (viewY + view.getHeight()))){
	        return true;
	    } else {
	        return false;
	    }
	}

}
