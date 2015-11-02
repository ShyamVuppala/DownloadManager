package com.shyamv.utilities.downloadmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "DownloadListDataBase";

    // Table Name
    private static final String TABLE_DOWNLOAD_LIST = "DownloadList";

    // Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_FILENAME = "filename";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_PROGRESS = "progress";

    private static DatabaseHandler mInstance = null;

    public static DatabaseHandler getInstance(Context context)
    {
        if(mInstance == null)
            mInstance = new DatabaseHandler(context);

        return mInstance;
    }

    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + TABLE_DOWNLOAD_LIST + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_URL + " TEXT,"
                + KEY_FILENAME + " TEXT,"
                + KEY_PAUSED + " INTEGER,"
                + KEY_PROGRESS + " REAL"
                + ")");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOAD_LIST);

        // Create tables again
        onCreate(db);
    }



    int addItem(Download item) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_FILENAME, item.getFileName());
        values.put(KEY_PAUSED, item.isPaused()?1:0);
        values.put(KEY_PROGRESS, item.getProgress());

        // Inserting Row
        long id = db.insert(TABLE_DOWNLOAD_LIST, null, values);
        db.close(); // Closing database connection

        return (int)id;
    }

    Download getItem(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DOWNLOAD_LIST, new String[] { KEY_ID,
                        KEY_URL, KEY_FILENAME, KEY_PAUSED, KEY_PROGRESS}, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Download item = new Download(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), Integer.parseInt(cursor.getString(3)) == 1, Float.parseFloat(cursor.getString(4)));
        return item;
    }

    public void loadAllItems(List<Download> items) {
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_DOWNLOAD_LIST;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        items.clear();
        // looping through all rows and adding to list
        if (cursor.moveToFirst())
        {
            do
            {
                Download item = new Download(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), Integer.parseInt(cursor.getString(3)) == 1, Float.parseFloat(cursor.getString(4)));
                items.add(item);
            } while (cursor.moveToNext());
        }
    }

    public int updateItem(Download item)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_FILENAME, item.getFileName());
        values.put(KEY_PAUSED, item.isPaused()?1:0);
        values.put(KEY_PROGRESS, item.getProgress());

        // updating row
        return db.update(TABLE_DOWNLOAD_LIST, values, KEY_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
    }

    public void deleteItem(Download item)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOAD_LIST, KEY_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
        db.close();
    }


    public int getItemCount() {
        String countQuery = "SELECT  * FROM " + TABLE_DOWNLOAD_LIST;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

}
