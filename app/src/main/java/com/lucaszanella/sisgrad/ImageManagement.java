package com.lucaszanella.sisgrad;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseUtils;
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
                DataProviderContract.STORAGE.TRIED,
                DataProviderContract.STORAGE.PATH,
                DataProviderContract.STORAGE.DIDLOAD
        };

        Cursor cursorOfFiles = context.getContentResolver().query(DataProviderContract.STORAGE_URI, projection, null, null, null);
        Log.d(LOG_TAG, DatabaseUtils.dumpCursorToString(cursorOfFiles));
        cursorOfFiles.close();
    }

    //Simple function to load an image from the storage, given a filename and a path
    public static Bitmap loadImageFromStorage(String name, Context context)
    {
        Log.d(LOG_TAG, "loading name: "+name);
        String[] projection = {
                DataProviderContract.STORAGE.NAME,
                DataProviderContract.STORAGE.TRIED,
                DataProviderContract.STORAGE.PATH,
                DataProviderContract.STORAGE.DIDLOAD
        };
        String selection = DataProviderContract.STORAGE.NAME+"=?";
        String fileName = Sha256Hex.hash(name.toLowerCase());//IMPORTANT: NEVER FORGET TO PUT IT TO LOWER CASE
        String[] arguments = {fileName};
        Cursor cursorOfFiles = context.getContentResolver().query(DataProviderContract.STORAGE_URI, projection, selection, arguments, null);
        if (true) {
            Cursor a = context.getContentResolver().query(DataProviderContract.STORAGE_URI, null, null, null, null);
            //Log.d(LOG_TAG, "CURSOR IS _--------- : "+DatabaseUtils.dumpCursorToString(a));
        }
        if(cursorOfFiles!=null && cursorOfFiles.getCount()>0) {
            //Log.d(LOG_TAG, "cursor not null for name : "+name);
            cursorOfFiles.moveToFirst();
            String path = cursorOfFiles.getString(cursorOfFiles.getColumnIndex(DataProviderContract.STORAGE.PATH));
            //Log.d(LOG_TAG, "path is: "+path);
            ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);//get the path to the image
            File filePath = new File(path, fileName);//create directory and file
            try {
                //File f = new File(context.getFilesDir(), fileName);
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(filePath));
                return b;
            /*
            ImageView img=(ImageView)findViewById(R.id.authorImage);
            img.setImageBitmap(b);
            */
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "file not found for " + name+ " path "+path+ " filename "+fileName);
                Log.d(LOG_TAG, "directory context is :"+filePath.toString());
                //e.printStackTrace();
                cursorOfFiles.close();

                File f = directory;
                File file[] = f.listFiles();
                Log.d("Files", "Size: "+ file.length);
                for (int i=0; i < file.length; i++)
                {
                    Log.d("Files", "FileName:" + file[i].getName());
                }


                return null;
            }
        } else {
            Log.d(LOG_TAG, "null or empty cursor");
        }
        cursorOfFiles.close();
        return null;
    }
    //just a simple tuple to return the file name and path when saving it
    public static class Tuple {
        String fileName;
        String filePath;
        public Tuple (String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }
    public static Tuple saveImage(Bitmap image, String realFileName, Context context) {
        Log.d(LOG_TAG, "saveImage caled for "+realFileName);
        String fileName = Sha256Hex.hash(realFileName.toLowerCase());//fileName is gonna be hex(sha256(realFileName))
        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);//get the path to the image
        File filePath = new File(directory, fileName);//create directory and file
        FileOutputStream fos = null;
        if (true) {
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
            //Log.d(LOG_TAG, "IMPORTANT: DETECTED THAT FILE ALREADY EXISTS FOR "+realFileName+" OR IS DIRECTORY + !filePath.isDirectory()"+filePath.isDirectory());
        }
        return new Tuple(fileName, directory.getAbsolutePath());
    }
    public static void registerImage(Tuple tuple, Context context) {
        Log.d(LOG_TAG, "registering image "+tuple.fileName);

        String fileName = tuple.fileName;
        String filePath = tuple.filePath;
        ContentValues values = new ContentValues();
        values.put(DataProviderContract.STORAGE.NAME, fileName);
        values.put(DataProviderContract.STORAGE.PATH, filePath);
        values.put(DataProviderContract.STORAGE.DIDLOAD, 1);
        values.put(DataProviderContract.STORAGE.TRIED, 1);
        values.put(DataProviderContract.STORAGE.DATE, 0);
        values.put(DataProviderContract.STORAGE.TYPE, 0);

        context.getContentResolver().insert(DataProviderContract.STORAGE_URI, values);
    }

    public static void registerFailedAttempt(Tuple tuple, Context context) {
        Log.d(LOG_TAG, "registering failed image "+tuple.fileName);

        String fileName = tuple.fileName;
        String filePath = tuple.filePath;
        ContentValues values = new ContentValues();
        values.put(DataProviderContract.STORAGE.NAME, fileName);
        values.put(DataProviderContract.STORAGE.PATH, filePath);
        values.put(DataProviderContract.STORAGE.DIDLOAD, 0);
        values.put(DataProviderContract.STORAGE.TRIED, 1);
        values.put(DataProviderContract.STORAGE.DATE, 0);
        values.put(DataProviderContract.STORAGE.TYPE, 0);

        context.getContentResolver().insert(DataProviderContract.STORAGE_URI, values);
    }
}

