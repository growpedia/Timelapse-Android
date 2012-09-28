/*
 *  Copyright (C) 2012  David Brodsky
 *	This file is part of Open BART.
 *
 *  Open BART is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Open BART is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Open BART.  If not, see <http://www.gnu.org/licenses/>.
*/


package pro.dbro.timelapse;

import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

public class AnimationTimer extends CountDownTimer {
	
	private ImageView image_view;
	private SeekBar progress;
	
	BitmapFactory bmf = new BitmapFactory();
	
	long millisInFuture;
	long countDownInterval;
	int image_count;
	int offset; // begin animation at seekBar state
	String timelapse_dir;

	// countDownInterval corresponds to frame rate
	// millisInFuture = countDownInterval * image_count
	public AnimationTimer(long millisInFuture, long countDownInterval) {
		super(millisInFuture, countDownInterval);
		// TODO Auto-generated constructor stub
	}
	public AnimationTimer(long millisInFuture, long countDownInterval, ImageView image_view, SeekBar progress, String timelapse_dir) {
		super(millisInFuture, countDownInterval);
		this.image_view = image_view;
		this.millisInFuture = millisInFuture;
		this.countDownInterval = countDownInterval;
		this.image_count = (int) (millisInFuture / countDownInterval);
		this.progress = progress;
		this.offset = progress.getProgress();
		this.timelapse_dir = timelapse_dir;

	}

	@Override
	public void onFinish() {
		this.offset = 0;
		this.start();
	}

	@Override
	public void onTick(long millisUntilFinished) {
		int frame = 0;

		if(millisUntilFinished != 0){
			frame = image_count - ((int) (millisUntilFinished / countDownInterval )) + offset;
			if(frame >= image_count)
				this.onFinish();
			Log.d("AnimationTimer",String.valueOf(frame));
		}
		progress.setProgress(frame);
		image_view.setImageBitmap(bmf.decodeFile(timelapse_dir + "/" + TimeLapse.thumbnail_dir + "/" + String.valueOf(frame+1)+TimeLapse.thumbnail_suffix + ".jpeg"));
		
		
	}


}
