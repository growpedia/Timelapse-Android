package pro.dbro.timelapse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	public static final String MEDIA_DIRECTORY = "TimeLapse";
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "FileUtils";

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int timelapse_id, int type){
	      return Uri.fromFile(getOutputMediaFile(timelapse_id, type));
	}
	
	public static File getOutputMediaDir(int timelapse_id){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

		// /mnt/sdcard/TimeLapse
	    File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
	    // /mnt/sdcard/TimeLapse/x
	    mediaStorageDir = new File(mediaStorageDir, String.valueOf(timelapse_id));

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "failed to create directory");
	            return null;
	        }
	    }
	    return mediaStorageDir;
	}

	/** Create a File for saving an image or video 
	 *  Assumes timelapse_id is validated	*/
	public static File getOutputMediaFile(int timelapse_id, int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

		// /mnt/sdcard/TimeLapse
	    File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
	    // /mnt/sdcard/TimeLapse/x
	    mediaStorageDir = new File(mediaStorageDir, String.valueOf(timelapse_id));

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    // get tla.timelapse_map(timelapse_id).image_count + 1
	    
	    // Get next image name from TimeLapse.image_count, and increment image_count
	    TimeLapseApplication tla = BrowserActivity.getContext();
	    int int_image = ((TimeLapse)tla.time_lapse_map.get(timelapse_id)).image_count + 1;
	    
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator + 
	        String.valueOf(int_image) + ".jpeg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        String.valueOf(int_image) + ".mp4");
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
			Log.d(TAG, "reading filesystem. Root: " + dir.getAbsolutePath());
			// A file exists in place of the requested root TimeLapse directory
			// TODO: Prompt user for action
			if(dir.exists() && !dir.isDirectory()){
				Log.d(TAG,"Filename collision with TimeLapse directory");
				return result;
			}
			else if(!dir.exists()){
				// The TimeLapse root directory didn't exist
				Log.d(TAG,"Creating TimeLapse directory");
				dir.mkdir();
				return result;
			}
			else
				Log.d(TAG,"TimeLapse directory found!");
			
			TimeLapse temp;
			for (File child : dir.listFiles()) {
				Log.d(TAG,"Inspecing child: " + child.getAbsolutePath());
				if (!child.isDirectory() || ".".equals(child.getName()) || "..".equals(child.getName())) {
					Log.d(TAG,"Ignoring child");
					continue;  // Ignore the self and parent aliases. Also non-directories
				}
				// if the child is a directory, attempt to parse the expected metadata.json
				File metadata = new File(child, METADATA_FILENAME);
				Log.d(TAG,"Checking for : " + metadata.getAbsolutePath());
				if (metadata.exists()){
					Log.d(TAG,"Metadata found");
					try{
						Gson gson = new Gson();
						// automatically deserialize JSON attributes matching TimeLapse fields
						temp = gson.fromJson(fileToString(metadata), TimeLapse.class);
						// manually assign other attributes
						
						// count images in directory
						if (child.listFiles(new imageFilter()) != null){
							File[] children = child.listFiles(new imageFilter());
							temp.image_count = child.listFiles(new imageFilter()).length;
						}
						else
							temp.image_count = 0;
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
				else{
					Log.d(TAG,"Metadata not found");
					continue;
				}
		   
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
				String extension = pathArray[pathArray.length-1].split("\\.")[1];
				if (extension.compareTo("jpeg") == 0){
					return true;
				}
				else{
					return false;
				}
			}
			
		}
		
	}
	
	// Given a TimeLapse object, save it's representation on the filesystem
	// Returns True if successful, False otherwise
	public static class SaveTimeLapsesOnFilesystem extends AsyncTask<TimeLapse, Void, Boolean>{
	
		// This method is executed in a separate thread
		@Override
		protected Boolean doInBackground(TimeLapse... input) {
			/*
			File timelapse_root = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
			if(!timelapse_root.isDirectory())
				return false;
			File timelapse_dir = new File(timelapse_root, String.valueOf(((TimeLapse)input[0]).id));
			if(!timelapse_dir.exists())
				return false;
				*/
			File timelapse_dir = getOutputMediaDir(input[0].id);
			
			File timelapse_meta = new File(timelapse_dir, METADATA_FILENAME);
			Gson gson = new GsonBuilder().setPrettyPrinting().setExclusionStrategies(new TimeLapse.JsonExclusionStrategy()).create();
			
			try {
				// Overwrite the metadata.json file
				FileWriter writer = new FileWriter(timelapse_meta, false);
				writer.write(gson.toJson(input[0]));
				writer.flush();
		        writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("CreateTimeLapseOnFileSystem", "writing failed:");
			}
			return true;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
			// Don't need to send a message indicating this is complete
			//sendMessage(result);
	        super.onPostExecute(result);
	    }
		
		private void sendMessage(Boolean result) {
		  	  Intent intent = new Intent(String.valueOf(R.id.timelapse_to_filesystem_complete));
		  	  intent.putExtra("result", result);
		  	  LocalBroadcastManager.getInstance(BrowserActivity.c).sendBroadcast(intent);
		}
		
	}
	
		// Save a picture (given as byte[]) to the filesystem
		public static class SavePictureOnFilesystem extends AsyncTask<byte[], Void, String>{
			
			int timelapse_id = -1;
		
			public SavePictureOnFilesystem(int timelapse_id){
				super();
				this.timelapse_id = timelapse_id;
				
			}
			// This method is executed in a separate thread
			@Override
			protected String doInBackground(byte[]... input) {
				if(timelapse_id == -1){
					Log.d(TAG,"Error: no timelapse_id given");
					return "";
				}
				File pictureFile = FileUtils.getOutputMediaFile(timelapse_id, FileUtils.MEDIA_TYPE_IMAGE);
		        if (pictureFile == null){
		            Log.d(TAG, "Error creating media file, check storage permissions");
		            return "";
		        }

		        try {
		            FileOutputStream fos = new FileOutputStream(pictureFile);
		            fos.write(input[0]);
		            fos.close();
		        } catch (FileNotFoundException e) {
		            Log.d(TAG, "File not found: " + e.getMessage());
		        } catch (IOException e) {
		            Log.d(TAG, "Error accessing file: " + e.getMessage());
		        }
		        Log.d(TAG,"Picture saved: " + pictureFile.getAbsolutePath());
		        
		        // Picture is now written to Filesystem
		        // set TimeLapse image_count and modified_date
		        TimeLapseApplication tla = BrowserActivity.getContext();
		        TimeLapse tl = ((TimeLapseApplication)tla).time_lapse_map.get(timelapse_id);
		        tl.modified_date = new Date();
		        tl.image_count ++;
				
		        // Save the new metadata.json reflecting the recently taken picture
		        new FileUtils.SaveTimeLapsesOnFilesystem().execute(tl);
				return pictureFile.getAbsolutePath();
			}
			
			@Override
		    protected void onPostExecute(String result) {
				// Don't need to send a message indicating this is complete
				//sendMessage(result);
				super.onPostExecute(result);
				CameraActivity.setCameraOverlay(result);
		    }
			
			private void sendMessage(Boolean result) {
			  	  Intent intent = new Intent(String.valueOf(R.id.timelapse_to_filesystem_complete));
			  	  intent.putExtra("result", result);
			  	  LocalBroadcastManager.getInstance(BrowserActivity.c).sendBroadcast(intent);
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
