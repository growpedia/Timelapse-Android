package pro.dbro.timelapse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FileUtils {
	
	public static final String METADATA_FILENAME = "metadata.json";
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	// Directory within /sdcard where pictures are saved
	private static final String MEDIA_DIRECTORY = "TimeLapse";
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "FileUtils";

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	// Read application state from filesystem in a separate thread
	// Argument: String root filepath to search
	// Returns: ArrayList of TimeLapses corresponding to filepath contents
	public static class ParseTimeLapsesFromFilesystem extends AsyncTask<String, Void, ArrayList<TimeLapse>>{
		private String TAG = "ParseTimeLapseFromFilesystem"; //  for debug

		// This method is executed in a separate thread
		@Override
		protected ArrayList<TimeLapse> doInBackground(String... filePath) {
			
			ArrayList<TimeLapse> result = new ArrayList<TimeLapse>();
			// For now, hardcode filePath directory
			//File dir = new File(filePath[0]);
			File dir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
			if(!dir.isDirectory())
				return result;
			
			TimeLapse temp;
			for (File child : dir.listFiles()) {
				if (!child.isDirectory() || ".".equals(child.getName()) || "..".equals(child.getName())) {
					continue;  // Ignore the self and parent aliases. Also non-directories
				}
				// if the child is a directory, attempt to parse the expected metadata.json
				File metadata = new File(child, METADATA_FILENAME);
				Log.d(TAG,"Checking for : " + metadata.getAbsolutePath());
				if (metadata.exists()){
					try{
						Gson gson = new Gson();
						// automatically deserialize JSON attributes matching TimeLapse fields
						temp = gson.fromJson(fileToString(metadata), TimeLapse.class);
						// manually assign other attributes
						
						// count images in directory
						temp.image_count = child.listFiles(new imageFilter()).length;
						Log.d(TAG, String.valueOf(temp.image_count) + " images found");
						// set directory path
						temp.directory_path = child.getPath();
						// assign id based on dir name
						temp.id = Integer.parseInt(child.getName());
						result.add(temp);
						Log.d(TAG,"Successfully parsed timelapse");
					}
					catch(Throwable t){
						Log.d(TAG,t.getLocalizedMessage());
					}
				}
				else
					continue;
		   
		    }
			return result;
		}
		
		@Override
	    protected void onPostExecute(ArrayList<TimeLapse> result) {
			sendMessage(result);
	        super.onPostExecute(result);
	    }
		
		private void sendMessage(ArrayList<TimeLapse> result) {
		  	  Intent intent = new Intent(String.valueOf(R.id.filesystem_parse_complete));
		  	  intent.putExtra("result", result);
		  	  LocalBroadcastManager.getInstance(BrowserActivity.c).sendBroadcast(intent);
		}
		// Check that file has extension = ".jpeg"
		public class imageFilter implements FileFilter{

			@Override
			public boolean accept(File pathname) {
				String[] pathArray = pathname.getPath().split(pathname.separator);
				String extension = pathArray[pathArray.length-1].split(".")[1];
				if (extension.compareTo("jpeg") == 0){
					return true;
				}
				else{
					return false;
				}
			}
			
		}
		
	}
	
	// Efficiently convert a File object to a String
	public static String fileToString(File file) throws IOException{
		BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder file_content = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    file_content.append(line);
		}
		
		return file_content.toString();
	}


}
