package pro.dbro.timelapse;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class TimeLapseContentProvider extends ContentProvider {

	// database
		private SQLiteWrapper database;

		// Used for the UriMacher
		private static final int TIMELAPSES = 10;
		private static final int TIMELAPSE_ID = 20;

		private static final String AUTHORITY = "pro.dbro.timelapse.timelapsecontentprovider";

		private static final String BASE_PATH = "timelapses";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
				+ "/" + BASE_PATH);
		
		public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY + "/");

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ "/timelapses";
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ "/timelapse";

		private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		static {
			sURIMatcher.addURI(AUTHORITY, BASE_PATH, TIMELAPSES);
			sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TIMELAPSE_ID);
		}

		@Override
		public boolean onCreate() {
			database = new SQLiteWrapper(getContext());
			return false;
		}
		

		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			
			// Uisng SQLiteQueryBuilder instead of query() method
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

			// Check if the caller has requested a column which does not exists
			checkColumns(projection);

			// Set the table
			queryBuilder.setTables(SQLiteWrapper.TABLE_NAME);

			int uriType = sURIMatcher.match(uri);
			switch (uriType) {
			case TIMELAPSES:
				break;
			case TIMELAPSE_ID:
				// Adding the ID to the original query
				queryBuilder.appendWhere(SQLiteWrapper.COLUMN_ID + "="
						+ uri.getLastPathSegment());
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}

			SQLiteDatabase db = database.getWritableDatabase();
			Cursor cursor = queryBuilder.query(db, projection, selection,
					selectionArgs, null, null, sortOrder);
			// Make sure that potential listeners are getting notified
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			int len = cursor.getCount();
			return cursor;
		}

		@Override
		public String getType(Uri uri) {
			return null;
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			int rowsDeleted = 0;
			long id = 0;
			switch (uriType) {
			case TIMELAPSES:
				id = sqlDB.insert(SQLiteWrapper.TABLE_NAME, null, values);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(BASE_PATH + "/" + id);
		}

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			int rowsDeleted = 0;
			switch (uriType) {
			case TIMELAPSES:
				rowsDeleted = sqlDB.delete(SQLiteWrapper.TABLE_NAME, selection,
						selectionArgs);
				break;
			case TIMELAPSE_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(SQLiteWrapper.TABLE_NAME,
							SQLiteWrapper.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsDeleted = sqlDB.delete(SQLiteWrapper.TABLE_NAME,
							SQLiteWrapper.COLUMN_ID + "=" + id 
							+ " and " + selection,
							selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsDeleted;
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {

			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			int rowsUpdated = 0;
			switch (uriType) {
			case TIMELAPSES:
				rowsUpdated = sqlDB.update(SQLiteWrapper.TABLE_NAME, 
						values, 
						selection,
						selectionArgs);
				break;
			case TIMELAPSE_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(SQLiteWrapper.TABLE_NAME, 
							values,
							SQLiteWrapper.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsUpdated = sqlDB.update(SQLiteWrapper.TABLE_NAME, 
							values,
							SQLiteWrapper.COLUMN_ID + "=" + id 
							+ " and " 
							+ selection,
							selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsUpdated;
		}

		private void checkColumns(String[] projection) {
			/*
			String[] available = { SQLiteWrapper.COLUMN_ID, SQLiteWrapper.COLUMN_CREATION_DATE, 
					SQLiteWrapper.COLUMN_DESCRIPTION, SQLiteWrapper.COLUMN_DIRECTORY_PATH, 
					SQLiteWrapper.COLUMN_IMAGE_COUNT, SQLiteWrapper.COLUMN_LAST_IMAGE_PATH, 
					SQLiteWrapper.COLUMN_MODIFIED_DATE, SQLiteWrapper.COLUMN_NAME, 
					SQLiteWrapper.COLUMN_THUMBNAIL_PATH, SQLiteWrapper.COLUMN_TIMELAPSE_ID,
					SQLiteWrapper.COLUMN_GIF_PATH, SQLiteWrapper.COLUMN_GIF_STATE};
					*/
			if (projection != null) {
				HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
				HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(SQLiteWrapper.COLUMNS));
				// Check if all columns which are requested are available
				if (!availableColumns.containsAll(requestedColumns)) {
					throw new IllegalArgumentException("Unknown columns in projection");
				}
			}
		}

}
