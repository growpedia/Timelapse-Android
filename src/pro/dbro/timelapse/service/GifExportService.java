package pro.dbro.timelapse.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pro.dbro.timelapse.AnimatedGifEncoder;
import pro.dbro.timelapse.BrowserActivity;
import pro.dbro.timelapse.FileUtils;
import pro.dbro.timelapse.R;
import pro.dbro.timelapse.SQLiteWrapper;
import pro.dbro.timelapse.TimeLapseApplication;
import pro.dbro.timelapse.TimeLapseViewerActivity;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GifExportService extends IntentService {
	
	private Context c;
	
	private NotificationManager mNM;
	private Notification notification; // keep an instance of the notification to update time text
	private int NOTIFICATION = R.id.export_service_notification;
	
	private PendingIntent contentIntent; // The intent to fire when notification clicked

	private int image_count = 0; // frames in gif
	
	public GifExportService() {
		super("GifExportService");
	}
	public GifExportService(String name) {
		super(name);
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		c = this;
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mNM.cancel(NOTIFICATION);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int _id = intent.getExtras().getInt("_id");
		generateGif(_id);
	}
	
	@SuppressLint("NewApi")
	private void showNotification(){
		// in ICS+, Progress bar notification is pre-rolled
		if(Build.VERSION.SDK_INT >= 14){
			Notification.Builder builder = new Notification.Builder(c);
			if(contentIntent != null)
				builder.setContentIntent(contentIntent);
			builder.setSmallIcon(R.drawable.ic_stat_timelapse)
			.setTicker("Exporting .GIF")
			.setWhen(0)
			.setContentTitle("Exporting...")
			.setProgress(image_count,0,false)
			.setOngoing(true).setOnlyAlertOnce(true);
			notification = builder.getNotification();
		}
		// Pre-ICS I'd have to set a custom notification contentView
		// not doing that atm
		else{
			NotificationCompat.Builder builder = new NotificationCompat.Builder(c);
			if(contentIntent != null)
				builder.setContentIntent(contentIntent);
			builder.setSmallIcon(R.drawable.ic_stat_timelapse)
			.setTicker("Exporting .GIF")
			.setContentTitle("Exporting...")
			.setWhen(0)
			//.setProgress(100,0,false)
			.setOngoing(true).setOnlyAlertOnce(true);
			notification = builder.getNotification();
		}
		
		mNM.notify(NOTIFICATION, notification);
	}
	
	@SuppressLint("NewApi")
	private void updateNotificationProgress(int progress){
		// in ICS+, Progress bar notification is pre-rolled
				if(Build.VERSION.SDK_INT >= 14 && progress <= image_count){
					Notification.Builder builder = new Notification.Builder(c)
					.setContentTitle("Exporting...")
					.setSmallIcon(R.drawable.ic_stat_timelapse)
					.setProgress(image_count,progress,false)
					.setWhen(0);
					notification = builder.getNotification();
				}
				// Pre-ICS I'd have to set a custom notification contentView
				// not doing that atm
				else{
					NotificationCompat.Builder builder = new NotificationCompat.Builder(c)
					.setContentTitle("Exporting...")
					.setSmallIcon(R.drawable.ic_stat_timelapse)
					.setWhen(0)
					.setContentText("Processing frame " + String.valueOf(progress) + " of " + String.valueOf(image_count));
					//.setProgress(100,0,false)
					notification = builder.getNotification();
				}
				mNM.notify(NOTIFICATION, notification);
		
	}
	
	public void generateGif(int _id){
		if(_id == -1){
			Log.d("SERVICE","Error: no _id given");
			return;
		}
		String gifPath;
		TimeLapseApplication tla = BrowserActivity.getContext();
		
		Cursor result = tla.getTimeLapseById(_id, null);
		if(result.moveToFirst()){
			image_count = result.getInt(result.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
			showNotification();
			String tlPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
			String name = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_NAME));
			FileOutputStream bos;
			try {
				File resultFile = new File(tlPath, name+".gif");
				gifPath = resultFile.getAbsolutePath();
				bos = new FileOutputStream(resultFile);
				Log.d("gif","output gif: " + String.valueOf(resultFile.getAbsolutePath()));
				AnimatedGifEncoder encoder = new AnimatedGifEncoder();
				encoder.start(bos);
				
				for(int x = 1; x <= image_count; x++){
					encoder.addFrame(FileUtils.decodeSampledBitmapFromResource(tlPath + "/" + String.valueOf(x)+".jpeg", 640, 480));
					Log.d("gif","adding frame " + String.valueOf(x));
					updateNotificationProgress(x);
					//encoder.addFrame
				}
				Log.d("gif","gif'n complete");
				encoder.finish();
				//out = new BufferedOutputStream(new FileOutputStream(file));
				bos.write(_id);
	            try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
					Log.d("gif","IO error: " + e.getLocalizedMessage());
				}
			} catch (FileNotFoundException e1) {
				Log.d("gif","FileNotFound error: " + e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				Log.d("gif","IO error: " + e1.getLocalizedMessage());
				e1.printStackTrace();
			}
			 			
			result.close();
		
		}
	}

}
