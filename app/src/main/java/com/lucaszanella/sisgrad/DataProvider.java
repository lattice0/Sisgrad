package com.lucaszanella.sisgrad;

/**
 * Created by lucaszanella on 5/3/16.
 */

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

/**
 * Defines a ContentProvider that delivers data about everything needed in this app.
 */
public class DataProvider extends ContentProvider {
    // Identifies log statements issued by this component
    public static final String LOG_TAG = "DataProvider";

    // Indicates that the incoming query is for a Sisgrad Message
    public static final int MESSAGES_QUERY = 1;

    // Indicates that the incoming query is for a Mail Message
    public static final int MAIL_QUERY = 2;

    // Indicates that the incoming query is for RU information
    public static final int RU_QUERY = 3;

    // Indicates that the incoming query is for storage information (where are the images stored)
    public static final int STORAGE_QUERY = 4;

    // Indicates an invalid content URI
    public static final int INVALID_URI = -1;

    //IMPORTANT: MUST BE SET HERE AND NOT IN DataProviderContract
    public static final String AUTHORITY = "com.lucaszanella.sisgrad";


    // Defines an SQLite statement that builds the Sisgrad 'Messages' table
    private static final String CREATE_MESSAGES_TABLE = "create table "
            + DataProviderContract.MESSAGES.TABLE_NAME
            + "("
            + DataProviderContract.MESSAGES.ID + " integer primary key autoincrement, "
            + DataProviderContract.MESSAGES.TITLE + " text not null, "
            + DataProviderContract.MESSAGES.AUTHOR + " text not null,"
            + DataProviderContract.MESSAGES.MESSAGE_ID + " text not null,"
            + DataProviderContract.MESSAGES.SENT_DATE + " text not null,"
            + DataProviderContract.MESSAGES.SENT_DATE_UNIX + " text not null,"
            + DataProviderContract.MESSAGES.READ_DATE+ " text not null,"
            + DataProviderContract.MESSAGES.MESSAGE+ " text,"
            + DataProviderContract.MESSAGES.ATTACHMENTS+ " text,"
            + DataProviderContract.MESSAGES.ACESSED_DATE + " text not null,"
            + DataProviderContract.MESSAGES.DID_READ + " integer not null"
            + ");";

    private static final String CREATE_STORAGE_TABLE = "create table "
            + DataProviderContract.STORAGE.TABLE_NAME
            + "("
            + DataProviderContract.STORAGE.ID + " integer primary key autoincrement, "
            + DataProviderContract.STORAGE.TYPE + " integer not null,"//0 is for Bitmap
            + DataProviderContract.STORAGE.PATH + " text not null, "
            + DataProviderContract.STORAGE.NAME + " text not null,"
            + DataProviderContract.STORAGE.TRIED + " integer default 0,"
            + DataProviderContract.STORAGE.DIDLOAD + " integer default 0,"
            + DataProviderContract.STORAGE.DATE + " text not null"
            + ");";

    // Defines an helper object for the backing database
    private SQLiteOpenHelper mHelper;

    // Defines a helper object that matches content URIs to table-specific parameters
    private static final UriMatcher sUriMatcher;

    // Stores the MIME types served by this provider
    private static final SparseArray<String> sMimeTypes;

    /*
     * Initializes meta-data used by the content provider:
     * - UriMatcher that maps content URIs to codes
     * - MimeType array that returns the custom MIME type of a table
     */
    static {

        // Creates an object that associates content URIs with numeric codes
        sUriMatcher = new UriMatcher(0);

        /*
         * Sets up an array that maps content URIs to MIME types, via a mapping between the
         * URIs and an integer code. These are custom MIME types that apply to tables and rows
         * in this particular provider.
         */
        sMimeTypes = new SparseArray<String>();

        // Adds a URI "match" entry that maps messages URL content to a specific message
        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.MESSAGES.TABLE_NAME,
                MESSAGES_QUERY);

        // Adds a URI "match" entry that maps files (images specifically) URL content to a specific file
        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.STORAGE.TABLE_NAME,
                STORAGE_QUERY);


        // Specifies a custom MIME type for the message URL table
        sMimeTypes.put(
                MESSAGES_QUERY,
                "vnd.android.cursor.dir/sisgrad." +
                        DataProviderContract.AUTHORITY + "." +
                        DataProviderContract.MESSAGES.TABLE_NAME);

        // Specifies a custom MIME type for the image URL table
        sMimeTypes.put(
                MESSAGES_QUERY,
                "vnd.android.cursor.dir/sisgrad." +
                        DataProviderContract.AUTHORITY + "." +
                        DataProviderContract.STORAGE.TABLE_NAME);
    }

    // Closes the SQLite database helper class, to avoid memory leaks
    public void close() {
        mHelper.close();
    }

    /**
     * Defines a helper class that opens the SQLite database for this provider when a request is
     * received. If the database doesn't yet exist, the helper creates it.
     */
    public class DataProviderHelper extends SQLiteOpenHelper {
        /**
         * Instantiates a new SQLite database using the supplied database name and version
         *
         * @param context The current context
         */
        DataProviderHelper(Context context) {
            super(context,
                    DataProviderContract.DATABASE_NAME,
                    null,
                    DataProviderContract.DATABASE_VERSION);
        }


        /**
         * Executes the queries to drop all of the tables from the database.
         *
         * @param db A handle to the provider's backing database.
         */
        private void dropTables(SQLiteDatabase db) {
            // If the table doesn't exist, don't throw an error
            db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.MESSAGES.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.STORAGE.TABLE_NAME);
        }

        /**
         * Does setup of the database. The system automatically invokes this method when
         * SQLiteDatabase.getWriteableDatabase() or SQLiteDatabase.getReadableDatabase() are
         * invoked and no db instance is available.
         *
         * @param db the database instance in which to create the tables.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Creates the tables in the backing database for this provider
            db.execSQL(CREATE_MESSAGES_TABLE);
            db.execSQL(CREATE_STORAGE_TABLE);
        }

        /**
         * Handles upgrading the database from a previous version. Drops the old tables and creates
         * new ones.
         *
         * @param db The database to upgrade
         * @param version1 The old database version
         * @param version2 The new database version
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(),
                    "Upgrading database from version " + version1 + " to "
                            + version2 + ", which will destroy all the existing data");

            // Drops all the existing tables in the database
            dropTables(db);

            // Invokes the onCreate callback to build new tables
            onCreate(db);
        }
        /**
         * Handles downgrading the database from a new to a previous version. Drops the old tables
         * and creates new ones.
         * @param db The database object to downgrade
         * @param version1 The old database version
         * @param version2 The new database version
         */
        @Override
        public void onDowngrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(),
                    "Downgrading database from version " + version1 + " to "
                            + version2 + ", which will destroy all the existing data");

            // Drops all the existing tables in the database
            dropTables(db);

            // Invokes the onCreate callback to build new tables
            onCreate(db);

        }
    }
    /**
     * Initializes the content provider. Notice that this method simply creates a
     * the SQLiteOpenHelper instance and returns. You should do most of the initialization of a
     * content provider in its static initialization block or in SQLiteDatabase.onCreate().
     */
    @Override
    public boolean onCreate() {

        // Creates a new database helper object
        mHelper = new DataProviderHelper(getContext());

        return true;
    }
    /**
     * Returns the result of querying the chosen table.
     * @see android.content.ContentProvider#query(Uri, String[], String, String[], String)
     * @param uri The content URI of the table
     * @param projection The names of the columns to return in the cursor
     * @param selection The selection clause for the query
     * @param selectionArgs An array of Strings containing search criteria
     * @param sortOrder A clause defining the order in which the retrieved rows should be sorted
     * @return The query results, as a {@link android.database.Cursor} of rows and columns
     */
    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase db = mHelper.getReadableDatabase();
        //Log.d(LOG_TAG, "reached matcher chooser");
        Cursor returnCursor;
        // Decodes the content URI and maps it to a code
        switch (sUriMatcher.match(uri)) {
            // If the query is for a message
            case MESSAGES_QUERY:
                // Does the query against a read-only version of the database
                returnCursor = db.query(
                        DataProviderContract.MESSAGES.TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, sortOrder);

                // Sets the ContentResolver to watch this content URI for data changes
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;
            case STORAGE_QUERY:
                // Does the query against a read-only version of the database
                returnCursor = db.query(
                        DataProviderContract.STORAGE.TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, sortOrder);

                // Sets the ContentResolver to watch this content URI for data changes
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;
            case INVALID_URI:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }
        return null;
    }

    /**
     * Returns the mimeType associated with the Uri (query).
     * @see android.content.ContentProvider#getType(Uri)
     * @param uri the content URI to be checked
     * @return the corresponding MIMEtype
     */
    @Override
    public String getType(Uri uri) {

        return sMimeTypes.get(sUriMatcher.match(uri));
    }
    /**
     *
     * Insert a single row into a table
     * @see android.content.ContentProvider#insert(Uri, ContentValues)
     * @param uri the content URI of the table
     * @param values a {@link android.content.ContentValues} object containing the row to insert
     * @return the content URI of the new row
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase localSQLiteDatabase;
        long id;
        // Decode the URI to choose which action to take
        String TABLE = "";
        switch (sUriMatcher.match(uri)) {
            // For the modification messages table
            case MESSAGES_QUERY:
                // Creates a writeable database or gets one from cache
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                id = localSQLiteDatabase.insert(
                        DataProviderContract.MESSAGES.TABLE_NAME,
                        null,
                        values
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }
            case STORAGE_QUERY:
                // Creates a writeable database or gets one from cache
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                id = localSQLiteDatabase.insert(
                        DataProviderContract.STORAGE.TABLE_NAME,
                        null,
                        values
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }
            case INVALID_URI:
                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }

        return null;
    }

    /**
     * Returns an UnsupportedOperationException if delete is called
     * @see android.content.ContentProvider#delete(Uri, String, String[])
     * @param uri The content URI
     * @param selection The SQL WHERE string. Use "?" to mark places that should be substituted by
     * values in selectionArgs.
     * @param selectionArgs An array of values that are mapped to each "?" in selection. If no "?"
     * are used, set this to NULL.
     *
     * @return the number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase localSQLiteDatabase;
        long id;
        switch (sUriMatcher.match(uri)) {
            // For the modification messages table
            case MESSAGES_QUERY:
                Log.d("DATA", "erasing messages");
                // Creates a writeable database or gets one from cache
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                id = localSQLiteDatabase.delete(DataProviderContract.MESSAGES.TABLE_NAME, null, null);
            case STORAGE_QUERY:
                Log.d("DATA", "erasing messages");
                // Creates a writeable database or gets one from cache
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                id = localSQLiteDatabase.delete(DataProviderContract.STORAGE.TABLE_NAME, null, null);            case INVALID_URI:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }

        return 0;
    }

    /**
     * Updates one or more rows in a table.
     * @see android.content.ContentProvider#update(Uri, ContentValues, String, String[])
     * @param uri The content URI for the table
     * @param values The values to use to update the row or rows. You only need to specify column
     * names for the columns you want to change. To clear the contents of a column, specify the
     * column name and NULL for its value.
     * @param selection An SQL WHERE clause (without the WHERE keyword) specifying the rows to
     * update. Use "?" to mark places that should be substituted by values in selectionArgs.
     * @param selectionArgs An array of values that are mapped in order to each "?" in selection.
     * If no "?" are used, set this to NULL.
     *
     * @return int The number of rows updated.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase localSQLiteDatabase;
        int rows;
        // Decodes the content URI and choose which insert to use
        switch (sUriMatcher.match(uri)) {

            // A picture URL content URI
            case MESSAGES_QUERY:
                // Creats a new writeable database or retrieves a cached one
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Updates the table
                rows = localSQLiteDatabase.update(
                        DataProviderContract.MESSAGES.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (0 != rows) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {

                    throw new SQLiteException("Update error:" + uri);
                }
            case STORAGE_QUERY:
                // Creats a new writeable database or retrieves a cached one
                localSQLiteDatabase = mHelper.getWritableDatabase();

                // Updates the table
                rows = localSQLiteDatabase.update(
                        DataProviderContract.STORAGE.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (0 != rows) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {

                    throw new SQLiteException("Update error:" + uri);
                }
            case INVALID_URI:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }
        return -1;
    }
}

