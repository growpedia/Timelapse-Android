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
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
	
	// File representing MEDIA_DIRECTORY path
	public static final File mediaStorageDir = getExternalStorage();
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "FileUtils";
	
	public static File getExternalStorage(){
		// Check if storage preference exists:
		SharedPreferences prefs = BrowserActivity.getContext().getSharedPreferences(BrowserActivity.PREFS_NAME, 0);
		if(prefs.contains(BrowserActivity.PREFS_STORAGE_LOCATION)){
			return new File(prefs.getString(BrowserActivity.PREFS_STORAGE_LOCATION, BrowserActivity.getContext().getFilesDir().getAbsolutePath()));
		}
		else{
			SharedPreferences.Editor editor = prefs.edit();
			File result;
			// First, try getting access to the sdcard partition
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				Log.d("MediaDir","Using sdcard");
				result = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
			} else {
			// Else, use the internal storage directory for this application
				Log.d("MediaDir","Using internal storage");
				result = new File(BrowserActivity.getContext().getFilesDir(), MEDIA_DIRECTORY);
			}
			editor.putString(BrowserActivity.PREFS_STORAGE_LOCATION, result.getAbsolutePath());
			editor.commit();
			return result;
		}
	}

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int timelapse_id, int type, int image_count){
	      return Uri.fromFile(getOutputMediaFile(timelapse_id, type, image_count));
	}
	
	public static File getOutputMediaDir(int timelapse_id){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

		
	    // /mnt/sdcard/TimeLapse/x
	    File media_dir = new File(mediaStorageDir, String.valueOf(timelapse_id));

	    // Create the storage directory if it does not exist
	    if (! media_dir.exists()){
	        if (! media_dir.mkdirs()){
	            Log.d(TAG, "failed to create directory");
	            return null;
	        }
	    }
	    Log.d("media_dir",String.valueOf(media_dir.getAbsolutePath()));
	    return media_dir;
	}

	/** Create a File for saving an image or video 
	 *  Assumes timelapse_id is validated	*/
	public static File getOutputMediaFile(int timelapse_id, int type, int image_count){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File timelapse_directory = getOutputMediaDir(timelapse_id);

	    // Create a media file name
	    //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    // get tla.timelapse_map(timelapse_id).image_count + 1
	    
	    //int int_image = ((TimeLapse)tla.time_lapse_map.get(timelapse_id)).image_count + 1;
	    
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(timelapse_directory + File.separator + 
	        String.valueOf(image_count+=1) + ".jpeg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(timelapse_directory + File.separator +
	        String.valueOf(image_count+=1) + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	// Read application state from filesystem in a separate thread
	// Argument: String root filepath to search
	// Returns: ArrayList of TimeLapses corresponding to filepath contents
	public static class ParseTimeLapsesFromFilesystem extends AsyncTask<String, Void, Boolean>{
		private String TAG = "ParseTimeLapseFromFilesystem"; //  for debug
		
		public ParseTimeLapsesFromFilesystem(){
			super();
		}

		// This method is executed in a separate thread
		@Override
		protected Boolean doInBackground(String... filePath) {
			TimeLapseApplication tla = BrowserActivity.getContext();

			// For now, hardcode filePath directory
			//File dir = new File(filePath[0]);
			File dir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
			Log.d(TAG, "reading filesystem. Root: " + dir.getAbsolutePath());
			// A file exists in place of the requested root TimeLapse directory
			// TODO: Prompt user for action
			if(dir.exists() && !dir.isDirectory()){
				Log.d(TAG,"Filename collision with TimeLapse directory");
				return false;
			}
			else if(!dir.exists()){
				// The TimeLapse root directory didn't exist
				Log.d(TAG,"Creating TimeLapse directory");
				dir.mkdir();
				return false;
			}
			else
				Log.d(TAG,"TimeLapse directory found!");
			
			ContentValues file_content;
			Gson gson = new GsonBuilder().registerTypeAdapter(ContentValues.class, new FileUtils.TimeLapseDeserializer()).create();
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
						// automatically deserialize JSON attributes matching TimeLapse fields
						file_content = gson.fromJson(fileToString(metadata), ContentValues.class);
						file_content.put(SQLiteWrapper.COLUMN_DIRECTORY_PATH, child.getAbsolutePath());
						// manually assign other attributes
						
						// count images in directory
						if (child.listFiles(FileUtils.mImageFilter) != null && child.listFiles(FileUtils.mImageFilter).length != 0){
							file_content = findOrGenerateThumbnail(file_content);	
						}
						else
							file_content.put(SQLiteWrapper.COLUMN_IMAGE_COUNT, 0);
						//Log.d(TAG, String.valueOf(file_content.image_count) + " images found");
						// set directory path
						file_content.put(SQLiteWrapper.COLUMN_DIRECTORY_PATH, child.getPath());
						// assign id based on dir name
						file_content.put(SQLiteWrapper.COLUMN_TIMELAPSE_ID, child.getName());

						// Add timelapse to the TimeLapseContentProvider
						tla.updateOrInsertTimeLapseByContentValues(file_content);
						Log.d(TAG,"Successfully parsed timelapse");
					}
					catch(Throwable t){
						Log.d(TAG,""+ t.toString());
						//Log.d(TAG,t.getLocalizedMessage());
					}
				}
				else{
					Log.d(TAG,"Metadata not found");
					continue;
				}
		   
		    }
			return true;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
			
			sendMessage(result);
	        super.onPostExecute(result);
	    }
		
		private void sendMessage(Boolean result) {
			BrowserActivity.getContext().filesystemParsed = true;
		}
		
		
	}
	
	// Given a TimeLapse object, save it's representation on the filesystem
	// Returns True if successful, False otherwise
	/**
	 * Serialize given ContentValues to Json, and write that to the filesystem
	 * @author davidbrodsky
	 *
	 */
	public static class SaveTimeLapsesOnFilesystem extends AsyncTask<ContentValues, Void, ContentValues>{
	
		// This method is executed in a separate thread
		@Override
		protected ContentValues doInBackground(ContentValues... input) {
			/*
			File timelapse_root = new File(Environment.getExternalStorageDirectory(), MEDIA_DIRECTORY);
			if(!timelapse_root.isDirectory())
				return false;
			File timelapse_dir = new File(timelapse_root, String.valueOf(((TimeLapse)input[0]).id));
			if(!timelapse_dir.exists())
				return false;
				*/
			//File timelapse_dir = getOutputMediaDir(input[0].getAsInteger(SQLiteWrapper.COLUMN_TIMELAPSE_ID));
			File timelapse_dir = new File(input[0].getAsString(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
			
			
			File timelapse_meta = new File(timelapse_dir, METADATA_FILENAME);
			//Gson gson = new GsonBuilder().setPrettyPrinting().setExclusionStrategies(new TimeLapse.JsonExclusionStrategy()).create();
			Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ContentValues.class, new FileUtils.TimeLapseSerializer()).create();
			
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
			Log.d("TimeLapse Saved", input[0].getAsString(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
			return input[0];
		}
		
		@Override
	    protected void onPostExecute(ContentValues result) {
			// Don't need to send a message indicating this is complete
			//sendMessage(result);
	        super.onPostExecute(result);
	    }
		
		private void sendMessage(ContentValues result) {
		  	  Intent intent = new Intent(String.valueOf(R.id.browserActivity_message));
		  	  intent.putExtra("timelapse_id", result);
		  	  intent.putExtra("type", R.id.filesystem_modified);
		  	  // TODO: Only update the part of the ListView that is necessary
		  	  LocalBroadcastManager.getInstance(BrowserActivity.getContext()).sendBroadcast(intent);
		}
		
	}
	
		// Delete TimeLapse from ContentProvider and Filesystem
		public static class deleteTimeLapse extends AsyncTask<Integer, Void, Boolean>{
			
			// This method is executed in a separate thread
			@Override
			protected Boolean doInBackground(Integer... input) {
				if(input[0] == -1){
					Log.d(TAG,"Error: no _id given");
					return false;
				}
				
				TimeLapseApplication tla = BrowserActivity.getContext();
				
				Cursor result = tla.getTimeLapseById(input[0], null);
				if(result.moveToFirst()){
					String tlPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
					File timelapse_directory = new File(tlPath);
					DeleteRecursive(timelapse_directory);
					// delete record in ContentProvider
					tla.deleteTimeLapseById(input[0]);

					result.close();
					return true;
				}
				result.close();
				return false;
			}
			
			@Override
		    protected void onPostExecute(Boolean result) {
				// Don't need to send a message indicating this is complete
				//sendMessage(result)
				super.onPostExecute(result);
				
		    }
			
			void DeleteRecursive(File fileOrDirectory) {
			    if (fileOrDirectory.isDirectory())
			        for (File child : fileOrDirectory.listFiles())
			            DeleteRecursive(child);

			    fileOrDirectory.delete();
			}
			
		}
		
		// Delete Last frame from TimeLapse in ContentProvider and Filesystem
		public static class deleteLastFrameFromTimeLapse extends AsyncTask<Integer, Void, String>{
			
			// This method is executed in a separate thread
			@Override
			protected String doInBackground(Integer... input) {
				if(input[0] == -1){
					Log.d(TAG,"Error: no _id given");
					return null;
				}
				
				TimeLapseApplication tla = BrowserActivity.getContext();
				// Check that image_count -1 is available in image and thumb dir
				// delete image and thumb corresponding to image_count
				// updateTimeLapse method to update JSON
				
				Cursor result = tla.getTimeLapseById(input[0], null);
				if(result.moveToFirst()){
					String tlPath = result.getString(result.getColumnIndex(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
					int image_count = result.getInt(result.getColumnIndex(SQLiteWrapper.COLUMN_IMAGE_COUNT));
					
					// If no image exists return
					if(image_count == 0)
						return null;
					
					// Delete image
					File to_delete = new File(tlPath,String.valueOf(image_count) + ".jpeg");
					to_delete.delete();
					
					// Delete corresponding thumbnail
					File thumbnail_dir = new File(tlPath, TimeLapse.thumbnail_dir);
					if(thumbnail_dir.exists() && thumbnail_dir.isDirectory()){
						to_delete = new File(thumbnail_dir, String.valueOf(image_count) + TimeLapse.thumbnail_suffix + ".jpeg");
						to_delete.delete();
					}
					
					String last_image_path = null;
					String thumbnail_path = null;
					if(image_count > 1){
						File new_last_image = new File(tlPath, String.valueOf(image_count-1) + ".jpeg");
						if(new_last_image.exists())
							last_image_path = new_last_image.getAbsolutePath();
						File new_thumbnail = new File(thumbnail_dir, String.valueOf(image_count-1) + TimeLapse.thumbnail_suffix + ".jpeg");
						if(new_thumbnail.exists())
							thumbnail_path = new_thumbnail.getAbsolutePath();
						
					}
					
					// Update record in ContentProvider and filesystem
					tla.updateTimeLapseById(input[0],
							new String[]{SQLiteWrapper.COLUMN_IMAGE_COUNT, SQLiteWrapper.COLUMN_LAST_IMAGE_PATH, SQLiteWrapper.COLUMN_THUMBNAIL_PATH}, 
							new String[]{String.valueOf(image_count-1), last_image_path, thumbnail_path});
					// update TimeLapse
					//tla.getTimeLapseById(input[0], null)
					
					// Reflect changes in TimeLapse .json
					//new FileUtils.SaveTimeLapsesOnFilesystem().execute(SQLiteWrapper.cursorRowToContentValues(tla.getTimeLapseById(input[0], null)));
					
					
					result.close();
					return last_image_path;
				}
				result.close();
				return null;
			}
			
			@Override
		    protected void onPostExecute(String result) {
				// Don't need to send a message indicating this is complete
				//sendMessage(result)
				super.onPostExecute(result);
				if(result != null)
					CameraActivity.setCameraOverlay(result, false);
				
		    }
			
			void DeleteRecursive(File fileOrDirectory) {
			    if (fileOrDirectory.isDirectory())
			        for (File child : fileOrDirectory.listFiles())
			            DeleteRecursive(child);

			    fileOrDirectory.delete();
			}
			
		}
	
		// Save a picture (given as byte[]) to the filesystem
		public static class SavePictureOnFilesystem extends AsyncTask<byte[], Void, String>{
			
			private int _id = -1;
		
			public SavePictureOnFilesystem(int _id){
				super();
				this._id = _id;
				
			}
			// This method is executed in a separate thread
			@Override
			protected String doInBackground(byte[]... input) {
				if(_id == -1){
					Log.d(TAG,"Error: no _id given");
					return "";
				}
				Log.d("SavePicture","Reading contentprovider " + String.valueOf(_id));
				TimeLapseApplication tla = BrowserActivity.getContext();
		        ContentValues tl = SQLiteWrapper.cursorRowToContentValues(tla.getTimeLapseById(_id, null));
		        int timelapse_id = tl.getAsInteger(SQLiteWrapper.COLUMN_TIMELAPSE_ID);
		        if(!tl.containsKey(SQLiteWrapper.COLUMN_DIRECTORY_PATH)){
		        	// If a picture is being saved before the content provider has been updated by FileUtils.SaveTimeLapseonFilesystem
		        	Log.d("SavePictureTimeLapse","Dir_path unexpectedly blank:" +  FileUtils.getOutputMediaFile(timelapse_id, FileUtils.MEDIA_TYPE_IMAGE, 0).getAbsolutePath());
		        	tl.put(SQLiteWrapper.COLUMN_DIRECTORY_PATH, FileUtils.getOutputMediaFile(timelapse_id, FileUtils.MEDIA_TYPE_IMAGE, 0).getAbsolutePath());
		        }
		        Log.d("TimeLapse Retrieved in SavePicture", tl.getAsString(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
		        File pictureFile;
		        if(tl.containsKey(SQLiteWrapper.COLUMN_IMAGE_COUNT))
		        	pictureFile = FileUtils.getOutputMediaFile(timelapse_id, FileUtils.MEDIA_TYPE_IMAGE, tl.getAsInteger(SQLiteWrapper.COLUMN_IMAGE_COUNT));
		        else
		        	pictureFile = FileUtils.getOutputMediaFile(timelapse_id, FileUtils.MEDIA_TYPE_IMAGE, 0);
		        
		        if (pictureFile == null){
		            Log.d(TAG, "Error creating media file, check storage permissions");
		            return "";
		        }

		        try {
		        	Log.d("Writing picture",pictureFile.getAbsolutePath());
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
		        
		        int num_images;
		        if(tl.containsKey(SQLiteWrapper.COLUMN_IMAGE_COUNT))
		        	num_images = tl.getAsInteger(SQLiteWrapper.COLUMN_IMAGE_COUNT);
		        else
		        	num_images = 0;
		        
		        
		        //TimeLapse tl = ((TimeLapseApplication)tla).time_lapse_map.get(timelapse_id);
		        //tl.modified_date = new Date();
		        //tl.image_count ++;
		        
		        // Generate a thumbnail for this new picture
		        tl = findOrGenerateThumbnail(tl);
		        
		        // TODO: Save new thumb, image to db OR do it in SaveTimeLapse
		        Log.d("SavePicture",tl.getAsString(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
		        tla.updateTimeLapseById(_id, new String[]{SQLiteWrapper.COLUMN_IMAGE_COUNT, SQLiteWrapper.COLUMN_LAST_IMAGE_PATH, SQLiteWrapper.COLUMN_THUMBNAIL_PATH},
		        									  new String[]{String.valueOf(num_images+=1), tl.getAsString(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH), tl.getAsString(SQLiteWrapper.COLUMN_THUMBNAIL_PATH)});
				
		        // Save the new metadata.json reflecting the recently taken picture
		        new FileUtils.SaveTimeLapsesOnFilesystem().execute(tl);
				return pictureFile.getAbsolutePath();
			}
			
			@Override
		    protected void onPostExecute(String result) {
				// Don't need to send a message indicating this is complete
				//sendMessage(result);
				super.onPostExecute(result);
				CameraActivity.setCameraOverlay(result, true);
				CameraActivity.taking_picture = false;
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
	
	/** Image Utilities **/
	
	// Calculate Image sample size given target width, height
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
        if (width > height) {
            inSampleSize = Math.round((float)height / (float)reqHeight);
        } else {
            inSampleSize = Math.round((float)width / (float)reqWidth);
        }
    }
    return inSampleSize;
	}
	
	// Given filepath, and required display height, width, loads scaled bitmap without occupying memory == original filesize
	public static Bitmap decodeSampledBitmapFromResource(String path,
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    //BitmapFactory.decodeResource(res, resId, options);
	    BitmapFactory.decodeFile(path, options); 

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    //return BitmapFactory.decodeResource(res, resId, options);
	    return BitmapFactory.decodeFile(path, options);
	}
	
	// Generate a thumbnail given an original, and set TimeLapse.thumbnail_path
	// if thumbnail matching last image in TimeLapse exists, set TimeLapse.thumbnail_path 
	public static ContentValues findOrGenerateThumbnail(ContentValues timelapse){
		
		File timelapse_dir = new File(timelapse.getAsString(SQLiteWrapper.COLUMN_DIRECTORY_PATH));
		Log.d("findOrGenerateThumbnail",timelapse_dir.getAbsolutePath());
		// if the timelapse dir does not exist or is a file,
		// fixing the application state is beyond the scope of this method
		// TODO: for performance, remove this check 
		if(!timelapse_dir.exists() || timelapse_dir.isFile())
			return timelapse;
		
		// Make thumbnail folder if it doesn't exist
		File thumbnail_dir = new File(timelapse_dir, TimeLapse.thumbnail_dir);
	    if (! thumbnail_dir.exists()){
	        if (! thumbnail_dir.mkdirs()){
	            Log.d(TAG, "failed to create thumbnail directory");
	            return timelapse;
	        }
	    }
	    // Determine last image in TimeLapse dir and generate thumbnail if it doesn't exist
		File[] children = timelapse_dir.listFiles(new imageFilter());
		timelapse.put(SQLiteWrapper.COLUMN_IMAGE_COUNT, timelapse_dir.listFiles(new imageFilter()).length);
		// Generate thumbnail of last image and save to storage as "./thumbnail_dir/XXXthumbnail_suffix.jpeg"
		File original = new File(timelapse_dir, timelapse.getAsString(SQLiteWrapper.COLUMN_IMAGE_COUNT) + ".jpeg");
		timelapse.put(SQLiteWrapper.COLUMN_LAST_IMAGE_PATH, original.getAbsolutePath()); 
		Bitmap thumbnail_bitmap = FileUtils.decodeSampledBitmapFromResource(original.getAbsolutePath(), TimeLapse.thumbnail_width, TimeLapse.thumbnail_height);
		File thumbnail_file = new File(thumbnail_dir, timelapse.getAsString(SQLiteWrapper.COLUMN_IMAGE_COUNT)+ TimeLapse.thumbnail_suffix +".jpeg");
		if(!thumbnail_file.exists()){
			FileOutputStream out;
			try {
				out = new FileOutputStream(thumbnail_file);
				thumbnail_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				timelapse.put(SQLiteWrapper.COLUMN_THUMBNAIL_PATH, thumbnail_file.getAbsolutePath()); 
				//Log.d("Thumbnail","TL " + String.valueOf(timelapse.id) + " thumb set to " + timelapse.thumbnail_path);
			} catch (FileNotFoundException e) {
				// Not sure when this would happen...
				// FileOutputStream creates file if it doesn't exist (the intended case)
				// Maybe on permission denied...
				e.printStackTrace();
			}
		}
		else{
			//if thumbail exists, store it with TimeLapse
			timelapse.put(SQLiteWrapper.COLUMN_THUMBNAIL_PATH, thumbnail_file.getAbsolutePath());
		}
		
		return timelapse;
	}
	
	// Check that file has extension = ".jpeg"
	public static class imageFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {
			String[] pathArray = pathname.getPath().split(pathname.separator);
			//Log.d("ImageFilter", pathArray.toString());
			String[] extensionArray = pathArray[pathArray.length-1].split("\\.");
			// if the pathname represents a directory, it won't have extension
			if (extensionArray.length == 1)
				return false;
			String extension = extensionArray[1];
			
			if (extension.compareTo("jpeg") == 0){
				
				return true;
			}
			else{
				return false;
			}
		}
	}
	
	public static imageFilter mImageFilter = new imageFilter();
	
	public static class TimeLapseSerializer implements JsonSerializer<ContentValues> {
		  public JsonElement serialize(ContentValues src, Type typeOfSrc, JsonSerializationContext context) {
			  JsonObject result = new JsonObject();
			  result.addProperty("creation_date", src.getAsString(SQLiteWrapper.COLUMN_CREATION_DATE));
			  result.addProperty("name", src.getAsString(SQLiteWrapper.COLUMN_NAME));
			  result.addProperty("description", src.getAsString(SQLiteWrapper.COLUMN_DESCRIPTION));
			  result.addProperty("modified_date", src.getAsString(SQLiteWrapper.COLUMN_MODIFIED_DATE));
			  result.addProperty("id", src.getAsInteger(SQLiteWrapper.COLUMN_TIMELAPSE_ID));
			  result.addProperty("image_count", src.getAsInteger(SQLiteWrapper.COLUMN_IMAGE_COUNT));
			  
		    return result;
		  }
	}
	
	public static class TimeLapseDeserializer implements JsonDeserializer<ContentValues>{
	
		public ContentValues deserialize(JsonElement json, Type type,
		        JsonDeserializationContext context) throws JsonParseException {
	
		    JsonObject jsonObject = (JsonObject) json;
		    ContentValues result = new ContentValues();
		    try{
			    result.put(SQLiteWrapper.COLUMN_CREATION_DATE, jsonObject.get("creation_date").getAsString());
			    result.put(SQLiteWrapper.COLUMN_NAME, jsonObject.get("name").getAsString());
			    result.put(SQLiteWrapper.COLUMN_DESCRIPTION, jsonObject.get("description").getAsString());
			    result.put(SQLiteWrapper.COLUMN_MODIFIED_DATE, jsonObject.get("modified_date").getAsString());
			    result.put(SQLiteWrapper.COLUMN_IMAGE_COUNT, jsonObject.get("image_count").getAsString());
			    result.put(SQLiteWrapper.COLUMN_TIMELAPSE_ID, jsonObject.get("id").getAsString());
		    }
		    catch(Throwable t){
		    	throw new JsonParseException(t);
		    }
		    return result;
		}
	}


}
