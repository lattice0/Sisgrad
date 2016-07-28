package com.lucaszanella.sisgrad;

/**
 * Created by lucaszanella on 5/3/16.
 */

import android.net.Uri;
import android.provider.BaseColumns;


public class DataProviderContract implements BaseColumns{

    private DataProviderContract() { }


    public static final int DATABASE_VERSION = 68;//Changing database version makes it delete and recreate the entire database
    // Database table
    public static final String DATABASE_NAME = "sisgrad.db";

    public class MESSAGES {//messages table (stores each message of Sisgrad's server
        public static final String TABLE_NAME = "messages";
        public static final String ID = "_id";
        public static final String TITLE = "title";
        public static final String AUTHOR = "author";
        public static final String MESSAGE_ID = "messageId";
        public static final String SENT_DATE = "sentDate";
        public static final String SENT_DATE_UNIX = "sentDateUnix";
        public static final String READ_DATE = "readDate";
        public static final String MESSAGE = "message";
        public static final String ATTACHMENTS = "attachments";
        public static final String ACESSED_DATE = "acessedDate";
        public static final String DID_READ = "didRead";
    }

    public class STORAGE {//storage table (stores the path of archives stored in internal memory)
        public static final String TABLE_NAME = "storage";
        public static final String ID = "_id";
        public static final String TYPE = "type";//0 means Bitmap
        public static final String PATH = "path";
        public static final String NAME = "name";
        public static final String HASHNAME = "hashname";
        public static final String TRIES = "tries";
        public static final String DATE = "date";
    }

    public class CLASSES {//classes table (stores the classes that a user has in Unesp
        public static final String TABLE_NAME = "messages";
        public static final String ID = "_id";
        public static final String DAY = "day";
        public static final String CLASS = "class";
        public static final String TEACHER = "teacher";
        public static final String LOADED_DATE = "loadedDate";
    }


    //The URI scheme used for content URIs
    public static final String SCHEME = "content";

    public static final String AUTHORITY = "com.lucaszanella.sisgrad";

    /**
     * The DataProvider content URI
     */
    public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY);

    /**
     *  The MIME type for a content URI that would return multiple rows
     *  <P>Type: TEXT</P>
     */
    public static final String MIME_TYPE_ROWS =
            "vnd.android.cursor.dir/vnd.com.example.android.threadsample";

    /**
     * The MIME type for a content URI that would return a single row
     *  <P>Type: TEXT</P>
     *
     */
    public static final String MIME_TYPE_SINGLE_ROW =
            "vnd.android.cursor.item/vnd.com.example.android.threadsample";

    /**
     * Picture table primary key column name
     */
    public static final String ROW_ID = BaseColumns._ID;


    /**
     * Picture table content URI
     */
    public static final Uri MESSAGES_URI =
            Uri.withAppendedPath(CONTENT_URI, MESSAGES.TABLE_NAME);

    public static final Uri CLASSES_URI =
            Uri.withAppendedPath(CONTENT_URI, CLASSES.TABLE_NAME);

    public static final Uri STORAGE_URI =
            Uri.withAppendedPath(CONTENT_URI, STORAGE.TABLE_NAME);
}
