package com.lucaszanella.sisgrad;

/*
* Lucas Zanella
*
* Fragment that will display the last messages from the Sisgrad's system, and also load
* the image of each author through Lattes system.
* If you're new to development, this fragment uses the following concepts:
* CursorLoader
* ContentProviders
* AsyncTask
* Bitmaps and circular image viewers
*/

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lucaszanella.LattesCrawler.LattesCrawler;
import com.lucaszanella.SisgradCrawler.SisgradCrawler;

import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//My own crawlers

public class MessagesFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "MessagesFragment";//name for the Log.d function for this class

    //WebCrawlers
    private Sisgrad app;//Global login object
    private LattesCrawler lattes = new LattesCrawler();

    View progress;//updating list progress object
    SwipeRefreshLayout mSwipeRefreshLayout;//swipe refresh object for messages list

    //Sets that keep track of things
    Map<String, Boolean> listOfAuthors = new HashMap<>();//Stores the name of each author, one time only, so their photos can be searched at Lattes

    //AsyncTask objects for the first AsyncTask execution (AsyncTasks can only be executed once)
    GetMessages messageUpdaterTask = new GetMessages();//Task that updates messages
    DoOrResumeLogin loginUpdaterTask = new DoOrResumeLogin();//Task that does login (or resume its session cookies)

    private static final int URL_LOADER = 0;//loader code for DataProvider (just one database is loaded here, but let's use this code anyway)

    private static final Integer IMAGES_CACHE_LIMIT = 2* TimeAgo.ONE_DAY;//Time, in seconds, to reload all author images from Lattes, in seconds

    private Cursor actualCursor = null;//cursor for messages list (mAdapter)

    private MessagesAdapter mAdapter;//adapter for messages list

    private String[] mProjection = {//projection: columns to get from MESSAGES table
            DataProviderContract.MESSAGES.ID,
            DataProviderContract.MESSAGES.TITLE,
            DataProviderContract.MESSAGES.AUTHOR,
            DataProviderContract.MESSAGES.MESSAGE,
            DataProviderContract.MESSAGES.ATTACHMENTS,
            DataProviderContract.MESSAGES.MESSAGE_ID,
            DataProviderContract.MESSAGES.READ_DATE,
            DataProviderContract.MESSAGES.SENT_DATE,
            DataProviderContract.MESSAGES.SENT_DATE_UNIX
    };

    //returns an instance of this fragment to be attached in MainActivity
    public static MessagesFragment newInstance(int page, String title) {
        MessagesFragment fragmentFirst = new MessagesFragment();

        Bundle args = new Bundle();
        args.putInt("0", page);//TODO: remove these args
        args.putString("title", title);
        fragmentFirst.setArguments(args);

        return fragmentFirst;
    }

    //UI creation process in order they're executed
    //---------------------------------------------
    onItemSelected mCallback;

    public interface onItemSelected {//interface to be implemented by the main activity to receive the clicks

        void onMessageSelected(String id, String provisoryTitle);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            Activity activity = (Activity) context;
            mCallback = (onItemSelected) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("must implement SellFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        //--------------------------------------------
        //Cursor Tests
        String selection = DataProviderContract.MESSAGES.MESSAGE+"=?";
        String[] arguments = {"null"};//we're gonna get only the null messages, so we can load their content

        String[] projection = {//columns to return from query
                DataProviderContract.MESSAGES.MESSAGE_ID,
                DataProviderContract.MESSAGES.MESSAGE,
                DataProviderContract.MESSAGES.SENT_DATE_UNIX
        };
        Cursor c1 = getContext().getContentResolver().query(
                DataProviderContract.MESSAGES_URI,
                projection,
                null,
                null,
                DataProviderContract.MESSAGES.SENT_DATE_UNIX+" DESC"
        );//load messages
        String id = "20435271";
        Log.d(LOG_TAG, "does cursor contains message id "+id+" ? "+CursorUtils.Contains(c1,id,DataProviderContract.MESSAGES.MESSAGE_ID));
        Log.d(LOG_TAG, "does cursor contains message id "+id+" ? "+CursorUtils.Contains(c1,id,DataProviderContract.MESSAGES.MESSAGE_ID));

        //--------------------------------------------
        */

        //DUMP STORAGE CURSOR-------------------------
        String selection2 = DataProviderContract.STORAGE.TYPE + "=?";
        String[] arguments2 = {"0"};//type 0 means: retrieve all author bitmaps

        String[] projection2 = {//columns to return from query
                DataProviderContract.STORAGE.HASHNAME,
                DataProviderContract.STORAGE.TYPE,
                DataProviderContract.STORAGE.DATE,
                DataProviderContract.STORAGE.TRIES
        };
        Cursor i = getContext().getContentResolver().query(
                DataProviderContract.STORAGE_URI,
                projection2,
                selection2,
                arguments2,
                null//no order
        );

        Log.d(LOG_TAG, "STORAGE DUMP: "+ DatabaseUtils.dumpCursorToString(i));
        //--------------------------------------------
        Log.d(LOG_TAG, "MESSAGES FRAGMENT CREATED");
        app = ((Sisgrad) getContext().getApplicationContext());//Gets the global object that stores login information
        //login = app.getLoginObject();//gets the login object from the global object
        //loadTeacherImagesToAdapter();
        //ImageManagement.DumpCursor(getContext());
        getLoaderManager().initLoader(URL_LOADER, null, this);//calls the cursorLoader
        loginUpdaterTask.execute();//start login process (or resume it)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_messages_display, container, false);

        progress = (ProgressBar) view.findViewById(R.id.loading);
        progress.setVisibility(View.VISIBLE);
        Log.d(LOG_TAG, "getting loader manager");

        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture on the list.
        */
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout");

                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        if (messageUpdaterTask.getStatus() == AsyncTask.Status.PENDING) {
                            //loginUpdaterTask = new DoOrResumeLogin();
                            //loginUpdaterTask.execute(login);
                            //messageUpdaterTask.execute(login);
                            mSwipeRefreshLayout.setRefreshing(true);//app already does login when started, and login implies in getmessages, so just set the progress to true and wait the GetMessages() task to finish

                        }

                        if (messageUpdaterTask.getStatus() == AsyncTask.Status.RUNNING) {
                            // My AsyncTask is currently doing work in doInBackground()
                        }

                        if (messageUpdaterTask.getStatus() == AsyncTask.Status.FINISHED) {
                            messageUpdaterTask = new GetMessages();
                            messageUpdaterTask.execute();
                        }
                    }
                }
        );

        ListView mListView = (ListView) view.findViewById(R.id.messagesList);
        mAdapter =
                new MessagesAdapter(
                        getContext(),   // Current context
                        null,  //No cursor yet
                        0 // No flags
                );//sets the adapter for the list of messages

        //mAdapter.authorImages = listOfTeacherAndBitmaps;
        // Sets the adapter for the view
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (actualCursor != null) {
                    actualCursor.moveToPosition(position);
                    String mId = actualCursor.getString(
                            actualCursor.getColumnIndex(DataProviderContract.MESSAGES.MESSAGE_ID));
                    String provisoryTitle = actualCursor.getString(
                            actualCursor.getColumnIndex(DataProviderContract.MESSAGES.TITLE));
                    Log.d(LOG_TAG, "chose item " + mId);

                    mCallback.onMessageSelected(mId, provisoryTitle);
                }
            }
        });
        Log.d(LOG_TAG, "setted adapter");
        return view;
    }

    //Cursor Loader implementation as taught here by google https://developer.android.com/training/load-data-background/setup-loader.html
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (loaderID) {
            case URL_LOADER:
                Log.d(LOG_TAG, "URL_LOADER chosen");
                // Returns a new CursorLoader
                return new CursorLoader(
                        getContext(),   // Parent activity context
                        DataProviderContract.MESSAGES_URI,        // Table to query
                        mProjection,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        DataProviderContract.MESSAGES.SENT_DATE_UNIX + " DESC"             // Arrange by earliest
                );
            default:
                Log.d(LOG_TAG, "NULL chosen");
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
        /*
         *  Changes the adapter's Cursor to be the results of the load. This forces the View to
         *  redraw.
         */
        //Log.d(LOG_TAG, "cursor loaded");
        if (returnCursor.getCount() > 0) {
            progress.setVisibility(View.GONE);//makes progress bar disapear
        }
        actualCursor = returnCursor;
        mAdapter.changeCursor(returnCursor);//updates the recently loaded cursor
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
        * Invoked when the CursorLoader is being reset. For example, this is called if the
        * data in the provider changes and the Cursor becomes stale.
        */
        // Sets the Adapter's backing data to null. This prevents memory leaks.
        mAdapter.changeCursor(null);
    }

    //AsyncTasks in order they're loaded

    /*
    * AsyncTask to do login or resume the login's cookies
    */
    private class DoOrResumeLogin extends AsyncTask<Boolean, Integer, Integer> {
        protected Integer doInBackground(Boolean... forceRelogin) {
            Log.d(LOG_TAG, "login AsyncTask called");

            //mountListOfMessageIds();
            //mountListOfTeacherNames();
            try {
                return app.doOrResumeLogin(forceRelogin[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return Sisgrad.PAGE_ERROR;
            }
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }
        //TODO: implement each error accordingly
        protected void onPostExecute(Integer loginResult) {
            if (loginResult.equals(Sisgrad.OK)) {
                Log.d(LOG_TAG, "login successful, now getting messages list");
                //Executes the AsyncTask that will update the messages list
                messageUpdaterTask.execute();
                //Starts the chain reaction of message loaders, which are AsyncTasks that recursively call themselves until
                //all message contents are loaded
                new LoadNewMessage().execute();
            } else if (loginResult.equals(Sisgrad.WRONG_PASSWORD)){
                //maybe password was changed during execution of this app. TODO: properly warns the user
            } else if (loginResult.equals(Sisgrad.WRONG_EMAIL)){
                //shouldn't give this error, email never changes
            } else if (loginResult.equals(Sisgrad.NOT_FOUND)) {
                //page not found
            } else if (loginResult.equals(Sisgrad.GENERIC_ERROR)) {
                //alert of this generic error
            }
        }
    }

    private class GetMessages extends AsyncTask<Void, Integer, Integer> {
        private static final int OK = 0;
        private static final int TIMEOUT = 1;
        private static final int EXCEPTION = 2;
        private static final int PAGE_ERROR = 3;

        protected Integer doInBackground(Void... nothing) {
            Log.d(LOG_TAG, "getting messages");
            try {
                SisgradCrawler.GetMessagesResponse getMessagesResponse = app.getLoginObject().getMessages(0);//gets the first page of messages
                Log.d(LOG_TAG, "got messages");
                if (getMessagesResponse.pageError==null) {
                    List<Map<String, String>> messages = getMessagesResponse.messages;//actual messages
                    //Mount cursor of message IDs so we can see if the new message ID is contained in it
                    //If yes, don't do anything, else, the message is a new message and must be included in the database
                    String[] projection = {//columns to return from query
                            DataProviderContract.MESSAGES.MESSAGE_ID,
                            DataProviderContract.MESSAGES.MESSAGE,
                            DataProviderContract.MESSAGES.SENT_DATE_UNIX
                    };
                    //Let's query the messages already in the database
                    Cursor c = getContext().getContentResolver().query(
                            DataProviderContract.MESSAGES_URI,
                            projection,
                            null,
                            null,
                            DataProviderContract.MESSAGES.SENT_DATE_UNIX + " DESC"
                    );
                    for (Map<String, String> message : messages) {//iterates through the messages
                        String id = message.get("messageId");
                        //TODO: substitute Contains by database query
                        if (!CursorUtils.Contains(c, id, DataProviderContract.MESSAGES.MESSAGE_ID)) {
                            //if this message is not included in the setOfMessageIds, add it to the database
                            ContentValues values = new ContentValues();//the method of inserting data into the database is through a ContentProvider
                            //values.put(MessagesTable.COLUMN_ID, id);
                            values.put(DataProviderContract.MESSAGES.TITLE, message.get("title"));
                            values.put(DataProviderContract.MESSAGES.AUTHOR, message.get("author"));
                            values.put(DataProviderContract.MESSAGES.MESSAGE_ID, message.get("messageId"));
                            values.put(DataProviderContract.MESSAGES.SENT_DATE, message.get("sentDate"));
                            values.put(DataProviderContract.MESSAGES.SENT_DATE_UNIX, message.get("sentDateUnix"));
                            values.put(DataProviderContract.MESSAGES.READ_DATE, message.get("readDate"));
                            values.putNull(DataProviderContract.MESSAGES.MESSAGE);
                            values.put(DataProviderContract.MESSAGES.ACESSED_DATE, "");
                            values.put(DataProviderContract.MESSAGES.DID_READ, 0);
                            getContext().getContentResolver().insert(DataProviderContract.MESSAGES_URI, values);//stores message metadata into database
                            Log.d(LOG_TAG, "adding new message of id: " + id);
                            //setOfMessageIds.put(newId, false);//put messageId and 'false' in setOfMessageIds, so the fragment can load it later
                        } else {
                            //Log.d(LOG_TAG, "cursor contains message already for "+id);
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (messages.size() > 0) {
                        return OK;
                    }
                } else {
                    return PAGE_ERROR;//TODO: think in a way to display the error code to the user, that is, pass the error to onPostExecute
                }
            } catch (SisgradCrawler.LoginTimeoutException e) {
                return TIMEOUT;
            } catch (Exception e) {
                e.printStackTrace();
                return EXCEPTION;
            }
            return null;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Integer result) {
            progress.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);
            if (result==OK) {
            /*
            * Now the AsyncTask that loads the first unloaded message from setOfMessageIds will be called.
            * Here, two will be called, so there is always two AsyncTask's running. Not too much but
            * it works fine. Every time one terminates, a new one is called, until all keys of setOfMessageIds
            * are true (that is, all message contents are loaded)
            */
                new LoadNewMessage().execute();
                new LoadNewMessage().execute();

            /*
            * Now the AsyncTask that loads the first unloaded message from setOfTeacherIds will be called.
            * Here, two will be called, so there is always two AsyncTask's running. Not too much but
            * it works fine. Every time one terminates, a new one is called, until all keys of setOfTeacherIds
            * are true (that is, all teacher images are loaded or failed to load)
            */
                new LoadNewAvatar().execute();
                //new loadNewAvatar().execute();
            } else if (result==TIMEOUT) {

            }
        }
    }

    //-------------------------------------------
     /*
    * AsyncTask to finally load the messages from the server (not the content, just the metadata of each message).
    * It loads a cursor of messages with null content, and then load the content for the first message in the cursor.
    * This AsyncTask will recursively call itself until all the messages are loaded. If you can more than one  of this
    * AsyncTask, it'll load more than one message per time. Two or three messages at the same time should do fine.
    */
    private class LoadNewMessage extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... nothing) {
            Log.d(LOG_TAG, "loadMessage called");

            String selection = DataProviderContract.MESSAGES.MESSAGE + " IS NULL ";
            //String[] arguments = {null};//we're gonna get only the null messages, so we can load their content

            String[] projection = {//columns to return from query
                    DataProviderContract.MESSAGES.MESSAGE_ID,
                    DataProviderContract.MESSAGES.MESSAGE,
                    DataProviderContract.MESSAGES.SENT_DATE_UNIX
            };
            Cursor nullMessages = getContext().getContentResolver().query(
                    DataProviderContract.MESSAGES_URI,
                    projection,
                    selection,
                    null,
                    DataProviderContract.MESSAGES.SENT_DATE_UNIX + " DESC"
            );//load messages

            if (nullMessages!=null) {
                //Log.d(LOG_TAG, "NOT NULL");
                if (nullMessages.getCount() > 0) {
                    //we'll always get the first null message and save it to the database
                    nullMessages.moveToFirst();
                    String currentMessageId = nullMessages.getString(nullMessages.getColumnIndex(DataProviderContract.MESSAGES.MESSAGE_ID));
                    getMessageAndSaveToDatabase(currentMessageId);
                    nullMessages.close();
                    return true;//true means 'load again, the cursor was not empty'
                } else {
                    nullMessages.close();
                }
            }//don't close cursor if it's null, remember

            //if this line is reached, then no more messages with null content were found
            //so let's tell onPostExecute to finish calling this AsyncTask recursively.
            return false;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                new LoadNewMessage().execute();//if true, call itself again, until all messages are loaded
            } else {
                Log.d(LOG_TAG, "ended loadNewMessage");
            }
        }
    }

    //makes part of the AsyncTask above
    private void getMessageAndSaveToDatabase(String messageId) {//simply get the message content given an Id and save it to the database
        try {
            Log.d(LOG_TAG, "loading message for " + messageId);
            SisgradCrawler.GetMessageResponse response = app.getLoginObject().getMessage(messageId, true);//true means: get the HTML of message, false is for text-only
            if (response.message.length() > 5) {
                ContentValues values = new ContentValues();
                values.put(DataProviderContract.MESSAGES.MESSAGE, response.message);
                if (response.attachments != null) {
                    String jsonAttachments = new JSONObject(response.attachments).toString();
                    //Log.d(LOG_TAG,"JSON MESSAGE:"+jsonAttachments);
                    values.put(DataProviderContract.MESSAGES.ATTACHMENTS, jsonAttachments);
                }
                getContext().getContentResolver().update(DataProviderContract.MESSAGES_URI, values, DataProviderContract.MESSAGES.MESSAGE_ID + " = ?", new String[]{messageId});
            } else {
                Log.d(LOG_TAG, "error inside AsyncTask for messageId: " + messageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    * This is the AsyncTask that download and save each avatar. It makes a list of message authors
    * with no image, and try to load them.
    */
    private class LoadNewAvatar extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... s) {
            Log.d(LOG_TAG, "loadNewAvatar called");
            //--------------------------------------------------------
            //Cursor that will query the message authors
            String[] projection = {//columns to return from query
                    DataProviderContract.MESSAGES.AUTHOR,
            };
            Cursor authors = getContext().getContentResolver().query(
                    DataProviderContract.MESSAGES_URI,
                    projection,
                    null,//no selection, let's query all messages
                    null,//no arguments for selection, obviously
                    DataProviderContract.MESSAGES.SENT_DATE_UNIX + " DESC"
            );
            //--------------------------------------------------------
            //Create a list of author names, where each one appears only once, so we can check them later at the database
            if (authors!=null) {//TODO: Merge this with the 'for' below
                for (authors.moveToFirst(); !authors.isAfterLast(); authors.moveToNext()) {
                    //Important: we're gonna use lowercase in everything from now, when regarding the author name
                    //TODO: remove .toLowercase of everything else
                    String author = authors.getString(authors.getColumnIndex(DataProviderContract.MESSAGES.AUTHOR)).toLowerCase();
                    if (!listOfAuthors.keySet().contains(author)) {
                        listOfAuthors.put(author, false);//false means we didn't work with this author data yet
                    }
                }
            }
            if (authors!=null) {authors.close();}
            //Log.d(LOG_TAG, "set of authors BEFORE processing: "+listOfAuthors);
            //Now we're gonna see which authors didn't ave any information about its avatar image in the database
            //and create the entries for it, so in the next recursive call of this AsyncTask, it gets loaded
            for (String author : listOfAuthors.keySet()) {
                String authorHash = Sha256Hex.hash(author.toLowerCase());//file name is the author name, but hashed

                //Cursor that will query information about this specific author
                String selection2 = DataProviderContract.STORAGE.HASHNAME + "=?";
                String[] arguments2 = {authorHash};//TODO: add the type of the image

                String[] projection2 = {//columns to return from query
                        DataProviderContract.STORAGE.NAME,
                        DataProviderContract.STORAGE.HASHNAME,
                        DataProviderContract.STORAGE.TRIES,
                        DataProviderContract.STORAGE.DATE
                };
                Cursor authorInformation = getContext().getContentResolver().query(
                        DataProviderContract.STORAGE_URI,
                        projection2,
                        selection2,
                        arguments2,
                        null//no order
                );
                //Log.d(LOG_TAG, "author: "+author+" dump: "+DatabaseUtils.dumpCursorToString(authorInformation));
                //Now let's see if there's no records about this name on the database. If there's none,
                //then we add it to the database
                if (authorInformation!=null && !(authorInformation.getCount()>0)) {
                    String unixTime = Long.toString(new Date().getTime() / 1000);

                    ContentValues values = new ContentValues();
                    values.put(DataProviderContract.STORAGE.NAME, author);
                    values.put(DataProviderContract.STORAGE.HASHNAME, authorHash);
                    values.put(DataProviderContract.STORAGE.PATH, "");
                    values.put(DataProviderContract.STORAGE.DATE, unixTime);
                    values.put(DataProviderContract.STORAGE.TYPE, 0);//type 0 is the type for the files that store author bitmaps
                    values.put(DataProviderContract.STORAGE.TRIES, 0);//means we tried to load this file from database 0 times
                    getContext().getContentResolver().insert(DataProviderContract.STORAGE_URI, values);
                    Log.d(LOG_TAG, "added to storage: "+author);
                    //return true;//search again
                } else {
                    //if name in database, update its status in listOfAuthors
                    Long currentTime = new Date().getTime()/1000;

                    if (authorInformation!=null) {
                        authorInformation.moveToFirst();
                        //Long authorTime = Long.parseLong(CursorUtils.FindString(images, authorHash, DataProviderContract.STORAGE.HASHNAME, DataProviderContract.STORAGE.DATE));
                        Long authorTime = authorInformation.getLong(authorInformation.getColumnIndex(DataProviderContract.STORAGE.DATE));
                        Integer numberOfTries = authorInformation.getInt(authorInformation.getColumnIndex(DataProviderContract.STORAGE.TRIES));//number of times we tried to get the file

                        //if the file is x second old and we tried to load it no more than 5 times, try again, this
                        //prevents the app from trying to load an non-existing image eternally
                        //OR, if numberOfTries==0, it means we just added this file to the database, so let's give our first try
                        if ((currentTime - authorTime > IMAGES_CACHE_LIMIT && numberOfTries <= 5) || numberOfTries == 0) {
                            //Log.d(LOG_TAG, "author: "+author+" currentTime - authorTime = "+ (currentTime - authorTime)+" numberOfTries = "+numberOfTries);
                            //Log.d(LOG_TAG, "file " + authorHash + " is x seconds old, marking to reload...");
                            listOfAuthors.put(author, false);//mark as false so the system will reload it
                        } else {
                            //Log.d(LOG_TAG, "file " + authorHash + " is NOT 1 hour old, marking to NOT reload...");
                            //Log.d(LOG_TAG, "currentTime: " + currentTime);
                            //Log.d(LOG_TAG, "authorTime: " + authorTime);
                            //Log.d(LOG_TAG, "Difference: " + (currentTime - authorTime));
                            listOfAuthors.put(author, true);//mark as true so we don't try to load again, because file is not 3 days old
                        }
                    }
                }
                if (authorInformation!=null){authorInformation.close();}
            }
            Log.d(LOG_TAG, "set of authors AFTER processing: "+listOfAuthors);

            for (String author : listOfAuthors.keySet()) {
                if (!listOfAuthors.get(author)) {//if no work has been done to this author
                    try {
                        Log.d(LOG_TAG, "searching for message author " + author.toLowerCase());
                        LattesCrawler.requestObject response = lattes.search(author.toLowerCase());
                        if (response.teacherId != null && !response.teacherId.isEmpty()) {
                            Log.d(LOG_TAG, "calling image download, save and register for author " + author.toLowerCase());
                            //TODO: make it save while downloads, so no big file is loaded to the memory
                            Bitmap teacherImage = ImageManagement.getBitmapHTTP(new URL(response.teacherURLImage));//HTTP with no s sucks, but that's the only way the server accepts...
                            ImageManagement.IMGData f = ImageManagement.saveImage(teacherImage, author.toLowerCase(), getContext());//already lowercase, I guess
                            ImageManagement.registerImage(f, getContext());
                            listOfAuthors.put(author, true);//true means we tried
                        } else {
                            ImageManagement.registerFailedAttempt(new ImageManagement.IMGData(author.toLowerCase(), Sha256Hex.hash(author.toLowerCase()), ""), getContext());
                            listOfAuthors.put(author, true);//true means we tried
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ImageManagement.registerFailedAttempt(new ImageManagement.IMGData(author.toLowerCase(), Sha256Hex.hash(author.toLowerCase()), ""), getContext());
                        listOfAuthors.put(author, true);//true means we tried
                    }
                    //we're gonna break by returning true when we found our first unloaded element,
                    //and then true will be returned so the AsyncTask will call itself again
                    return true;
                }
            }
            //if none of the 'if's up there got triggered, it means no author needs to be loaded
            //so lets just return false and the AsyncTask will stop calling itself recursively
            return false;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean result) {
            // starts another loadEarliestUnloadedMessageFromMap when this one finished
            // they'll stop loading when all keys in setOfMessageIds are set to false
            //
            //if the set still contains values with 0, load one more teacher image
            if (result) {
                new LoadNewAvatar().execute();
            } else {
                Log.d(LOG_TAG, "loadNewAvatar ended");
            }
        }
    }

    //-------------------------------------------
    /*
    * AsyncTask to finally load the messages from the server (not the content, just the metadata of each message).
    * Each message must be loaded from the message code in this metadata (yeah, webcrawling sucks...)
    */
    private static class CursorUtils {
        public static Boolean Contains(Cursor cursor, String value, String columnName) {
            //cursor.moveToFirst();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndex(columnName)).equals(value)) {
                    return true;
                }
            }
            return false;
        }
        public static String FindString(Cursor cursor, String value, String columnToSearch, String columnToGetValue) {
            //cursor.moveToFirst();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndex(columnToSearch)).equals(value)) {
                    return (cursor.getString(cursor.getColumnIndex(columnToGetValue)));
                }
            }
            return null;
        }
    }
}



