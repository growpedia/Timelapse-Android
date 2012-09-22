package pro.dbro.timelapse.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import pro.dbro.timelapse.AnimatedGifEncoder;
import pro.dbro.timelapse.BrowserActivity;
import pro.dbro.timelapse.FileUtils;
import pro.dbro.timelapse.R;
import pro.dbro.timelapse.SQLiteWrapper;
import pro.dbro.timelapse.TimeLapse;
import pro.dbro.timelapse.TimeLapseApplication;
import pro.dbro.timelapse.TimeLapseViewerActivity;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class GifExportService extends IntentService {
	
	private Context c;
	
	private NotificationManager mNM;
	private Notification notification; // keep an instance of the notification to update time text
	private int EXPORTING_NOTIFICATION = R.id.export_service_notification;
	private int COMPLETE_NOTIFICATION = R.id.export_service_notification_complete;
	
	BitmapFactory bmf = new BitmapFactory();
	
	private PendingIntent contentIntent = null; // The intent to fire when notification clicked

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
		mNM.cancel(EXPORTING_NOTIFICATION);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int _id = intent.getExtras().getInt("_id");
		int resolution = intent.getExtras().getInt("resolution");
		generateGif(_id, resolution);
	}
	
	@SuppressLint("NewApi")
	private void showNotification(String name){
		// in ICS+, Progress bar notification is pre-rolled
		if(Build.VERSION.SDK_INT >= 14){
			Notification.Builder builder = new Notification.Builder(c);
			if(contentIntent != null)
				builder.setContentIntent(contentIntent);
			else
				builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
			builder.setSmallIcon(R.drawable.ic_stat_timelapse)
			.setTicker("Exporting " + name + ".GIF")
			.setWhen(0)
			.setContentTitle("Exporting " + name + ".GIF")
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
			else
				builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
			builder.setSmallIcon(R.drawable.ic_stat_timelapse)
			.setTicker(getString(R.string.notification_export_ticker))
			.setContentTitle("Exporting " + name + ".GIF")
			.setWhen(0)
			//.setProgress(100,0,false)
			.setOngoing(true).setOnlyAlertOnce(true);
			notification = builder.getNotification();
		}
		
		mNM.notify(EXPORTING_NOTIFICATION, notification);
	}
	
	@SuppressLint("NewApi")
	private void updateNotificationProgress(int progress){
		// in ICS+, Progress bar notification is pre-rolled
				if(Build.VERSION.SDK_INT >= 14 && progress <= image_count){
					Notification.Builder builder = new Notification.Builder(c);
					if(contentIntent != null)
						builder.setContentIntent(contentIntent);
					else
						builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
					
					builder.setSmallIcon(R.drawable.ic_stat_timelapse)
					.setProgress(image_count,progress,false)
					.setOngoing(true)
					.setWhen(0);
					
					if(contentIntent != null)
						builder.setContentIntent(contentIntent);
					else
						builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
					notification = builder.getNotification();
				}
				// Pre-ICS I'd have to set a custom notification contentView
				// not doing that atm
				else{
					NotificationCompat.Builder builder = new NotificationCompat.Builder(c);
					if(contentIntent != null)
						builder.setContentIntent(contentIntent);
					else
						builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
					
					builder.setSmallIcon(R.drawable.ic_stat_timelapse)
					.setWhen(0)
					.setOngoing(true)
					.setContentText("Processing frame " + String.valueOf(progress) + " of " + String.valueOf(image_count));
					//.setProgress(100,0,false)
					if(contentIntent != null)
						builder.setContentIntent(contentIntent);
					else
						builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
					notification = builder.getNotification();
				}
				mNM.notify(EXPORTING_NOTIFICATION, notification);
		
	}
	
	@SuppressLint("NewApi")
	private void showCompleteNotification(File result){
		NotificationCompat.Builder builder = new NotificationCompat.Builder(c);
		
		// Intent to have system view gif
		/*
		Uri file_uri = Uri.parse("file://" + result.getAbsolutePath());
		Intent notificationIntent = new Intent(Intent.ACTION_VIEW, file_uri);
		notificationIntent.setDataAndType(file_uri, "image/gif");
		PendingIntent contentIntent = PendingIntent.getActivity(GifExportService.this, 0, notificationIntent,0);
		*/
		
		// Intent to have system share gif
		Intent notificationIntent = new Intent(Intent.ACTION_SEND);
		notificationIntent.setType("image/gif");

		notificationIntent.putExtra(Intent.EXTRA_STREAM,
		  Uri.parse("file://" + result.getAbsolutePath()));

		PendingIntent contentIntent = PendingIntent.getActivity(GifExportService.this, 0, Intent.createChooser(notificationIntent, "Share .GIF"),0);
		
		if(isIntentAvailable(c, notificationIntent)){
			builder.setContentIntent(contentIntent)
			.setContentText("Touch to share");
		}else{
			builder.setContentText("Please install an app that can share .gifs");
		}
		
		builder.setSmallIcon(R.drawable.ic_stat_timelapse)
		.setTicker(result.getName() + " Exported!")
		.setWhen(0)
		.setAutoCancel(true)
		.setContentTitle(result.getName() + " Exported!");
		
		if(contentIntent != null)
			builder.setContentIntent(contentIntent);
		else
			builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0)); // blank intent
		
		notification = builder.getNotification();
		
		mNM.notify(COMPLETE_NOTIFICATION, notification);
	}
	
	public void generateGif(int _id, int resolution){
		if(_id == -1){
			Log.d("SERVICE","Error: no _id given");
			return;
		}
		String gifPath;
		TimeLapseApplication tla = BrowserActivity.getContext();
		
		Cursor result = tla.getTimeLapseById(_id, null);
		if(result.moveToFirst()){
			image_count = result.getInt(result.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
			
			String tlPath = null;
			// if generating gif from full-size images:
			if(resolution == 480){
				tlPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
			}
			else{
			// if generating from thumbnails:
				tlPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH)) + File.separator + TimeLapse.thumbnail_dir;
			}
			
			String name = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_NAME));
			//TODO: Do this proper
			try {
				name = java.net.URLEncoder.encode(name, "UTF-8");
			} catch (UnsupportedEncodingException e2) {
				e2.printStackTrace();
			}
			showNotification(name);
			FileOutputStream bos;
			try {
				File resultFile = new File(tlPath, name+".gif");
				gifPath = resultFile.getAbsolutePath();
				bos = new FileOutputStream(resultFile);
				Log.d("gif","output gif: " + String.valueOf(resultFile.getAbsolutePath()));
				AnimatedGifEncoder encoder = new AnimatedGifEncoder();
				encoder.start(bos);
				
				for(int x = 1; x <= image_count; x++){

					if(resolution == 480)
						encoder.addFrame(bmf.decodeFile(tlPath + "/" + String.valueOf(x) + ".jpeg"));
					else
						encoder.addFrame(bmf.decodeFile(tlPath + "/" + String.valueOf(x) + TimeLapse.thumbnail_suffix + ".jpeg"));		
					
					// If downsampling images for giff:
					//encoder.addFrame(FileUtils.decodeSampledBitmapFromResource(tlPath + "/" + String.valueOf(x)+".jpeg", 320, 240));
					
					Log.d("gif","adding frame " + String.valueOf(x));
					updateNotificationProgress(x);
					//encoder.addFrame
				}
				Log.d("gif","gif'n complete");
				encoder.finish();
				//out = new BufferedOutputStream(new FileOutputStream(file));
				bos.write(_id);
				// show notification offering to open file
				showCompleteNotification(resultFile);
				// record new gif state
				tla.updateTimeLapseById(_id, new String[]{SQLiteWrapper.COLUMN_GIF_STATE, SQLiteWrapper.COLUMN_GIF_PATH}, 
						new String[]{String.valueOf(image_count), resultFile.getAbsolutePath()});
				
				// notify TimeLapseViewerActivity that export is complete
				sendExportCompleteBroadcast();
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
	
	public static boolean isIntentAvailable(Context ctx, Intent intent) {
	   final PackageManager mgr = ctx.getPackageManager();
	   List<ResolveInfo> list =
	      mgr.queryIntentActivities(intent, 
	         PackageManager.MATCH_DEFAULT_ONLY);
	   return list.size() > 0;
	} 
	
	public void sendExportCompleteBroadcast(){
		Intent intent = new Intent("service_status_change");
	  	  // You can also include some extra data.
	  	  intent.putExtra("status", 1);
	  	  LocalBroadcastManager.getInstance(TimeLapseViewerActivity.tla).sendBroadcast(intent);
	}

}
