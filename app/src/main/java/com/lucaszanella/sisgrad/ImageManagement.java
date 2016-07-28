package com.lucaszanella.sisgrad;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Class to load images from the web and store them at the internal storage (if such image were not loaded yed),
 * or to load the given image from internal storage as a Bitmap. Uses the STORAGE table to keep track of the files.
 * Remember that the name of the file in the storage is hex(sha256(realFileName));
 */
public class ImageManagement extends AppCompatActivity{
    private static final String LOG_TAG = "ImgManagement";//name for the Log.d function

    //Simple function to load a bitmap from the internet using insecure HTTP :(. TODO: add support for cookies
    public static Bitmap getBitmapHTTP(URL url) {
        try {
            Log.d(LOG_TAG, "getBitmapFromURl called for "+url.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            Log.d(LOG_TAG, "getBitmapFromURl called but exception catched");

            // Log exception
            return null;
        }
    }
    public static void DumpCursor (Context context) {
        Log.d(LOG_TAG, "dump cursor called");
        String[] projection = {
                DataProviderContract.STORAGE.NAME,
                DataProviderContract.STORAGE.HASHNAME,
                DataProviderContract.STORAGE.PATH,
        };

        Cursor cursorOfFiles = context.getContentResolver().query(DataProviderContract.STORAGE_URI, projection, null, null, null);
        Log.d(LOG_TAG, DatabaseUtils.dumpCursorToString(cursorOfFiles));
        cursorOfFiles.close();
    }

    //Simple function to load an image from the storage, given a filename and a path
    public static Bitmap loadImageFromStorage(String name, Context context)
    {
        //Log.d(LOG_TAG, "loading name: "+name);
        String[] projection = {
                DataProviderContract.STORAGE.HASHNAME,
                DataProviderContract.STORAGE.PATH,
        };
        String selection = DataProviderContract.STORAGE.HASHNAME+"=?";
        String fileName = Sha256Hex.hash(name.toLowerCase());//IMPORTANT: NEVER FORGET TO PUT IT TO LOWER CASE
        String[] arguments = {fileName};
        Cursor cursorOfFiles = context.getContentResolver().query(DataProviderContract.STORAGE_URI, projection, selection, arguments, null);

        if(cursorOfFiles!=null && cursorOfFiles.getCount()>0) {
            cursorOfFiles.moveToFirst();
            String path = cursorOfFiles.getString(cursorOfFiles.getColumnIndex(DataProviderContract.STORAGE.PATH));
            ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
            File filePath = new File(path, fileName);//create directory and file
            try {
                //File f = new File(context.getFilesDir(), fileName);
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(filePath));
                return b;
            } catch (FileNotFoundException e) {

                //Log.d(LOG_TAG, "file not found for " + name+ " path "+path+ " filename "+fileName);
                cursorOfFiles.close();
                /*
                File f = directory;
                File file[] = f.listFiles();
                Log.d("Files", "Size: "+ file.length);
                for (int i=0; i < file.length; i++)
                {
                    Log.d("Files", "FileName:" + file[i].getName());
                }
                */
                return null;
            }
        } else {
            //Log.d(LOG_TAG, "null or empty cursor");
        }
        cursorOfFiles.close();
        return null;
    }
    //just a simple tuple to return the file name and path when saving it
    public static class IMGData {
        String fileName;
        String hashName;
        String filePath;
        public IMGData (String fileName, String hashName, String filePath) {
            this.fileName = fileName;
            this.hashName = hashName;
            this.filePath = filePath;
        }
    }
    public static IMGData saveImage(Bitmap image, String realFileName, Context context) {
        Log.d(LOG_TAG, "saveImage caled for "+realFileName);
        String fileName = Sha256Hex.hash(realFileName.toLowerCase());//fileName is gonna be hex(sha256(realFileName))
        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);//get the path to the image
        File filePath = new File(directory, fileName);//create directory and file
        FileOutputStream fos = null;
        if (true) {//was: filePath.exists()
            try {
                //open the FileOutputStream for the file path
                fos = new FileOutputStream(filePath);
                // Use the compress method on the BitMap object to write image to the OutputStream
                image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                Log.d(LOG_TAG, "image compressed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(LOG_TAG, "saved image to storage (I guess)");
            }
        } else {
            Log.d(LOG_TAG, "IMPORTANT: DETECTED THAT FILE ALREADY EXISTS FOR "+realFileName);
        }
        return new IMGData(realFileName, fileName, directory.getAbsolutePath());
    }
    public static void registerImage(IMGData data, Context context) {
        Log.d(LOG_TAG, "registering image "+data.fileName);

        String fileName = data.fileName;
        String hashName = data.hashName;
        String filePath = data.filePath;

        String unixTime = Long.toString(new Date().getTime() / 1000);

        ContentValues values = new ContentValues();
        values.put(DataProviderContract.STORAGE.NAME, fileName);
        values.put(DataProviderContract.STORAGE.HASHNAME, hashName);
        values.put(DataProviderContract.STORAGE.PATH, filePath);
        values.put(DataProviderContract.STORAGE.DATE, unixTime);
        values.put(DataProviderContract.STORAGE.TYPE, 0);
        //if we put TRIES 0, it'd try to first load it, so let TRIES=1 and the app will leave it alone until
        //the date times out and we reload the image
        values.put(DataProviderContract.STORAGE.TRIES, 1);
        //TODO: inspect what will happen if the image gets loaded, then timeout and gets a failed load

        context.getContentResolver().insert(DataProviderContract.STORAGE_URI, values);

    }
    //Basically we're gonna call registerFailedAttempt every time just to register the new date we tried
    //this is the same as UPDATing the database, but since we made HASHNAME to be unique, lets just
    //insertWithOnConflict in the database :)
    public static void registerFailedAttempt(IMGData data, Context context) {
        Log.d(LOG_TAG, "registering failed image "+data.fileName);

        String fileName = data.fileName;
        String hashName = data.hashName;
        String filePath = data.filePath;

        String unixTime = Long.toString(new Date().getTime() / 1000);

        ContentValues values = new ContentValues();
        values.put(DataProviderContract.STORAGE.NAME, fileName);
        values.put(DataProviderContract.STORAGE.HASHNAME, hashName);
        values.put(DataProviderContract.STORAGE.PATH, filePath);
        values.put(DataProviderContract.STORAGE.DATE, unixTime);
        values.put(DataProviderContract.STORAGE.TYPE, 0);

        context.getContentResolver().insert(DataProviderContract.STORAGE_URI, values);

        //Increment TRIES by 1 every time we register a new failed attempt, that's the whole point
        SQLiteDatabase db = new DataProvider().new DataProviderHelper(context).getWritableDatabase();
        final SQLiteStatement stmt = db.compileStatement("UPDATE " +
                DataProviderContract.STORAGE.TABLE_NAME + " SET " +
                DataProviderContract.STORAGE.TRIES + " = " +
                DataProviderContract.STORAGE.TRIES + " +1 WHERE " +
                DataProviderContract.STORAGE.HASHNAME + " = ?");
        stmt.bindString(1, hashName);
        final int rows = stmt.executeUpdateDelete();
        Log.d(LOG_TAG, "rows affected: "+rows);

        db.close();
    }
}

