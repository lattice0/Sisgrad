package com.lucaszanella.sisgrad;

//My own crawlers

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class MessagesFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "MessagesFragment";//name for the Log.d function for this class

    //WebCrawlers
    private SisgradCrawler login;
    private LattesCrawler lattes = new LattesCrawler();

    View progress;//updating list progress object
    SwipeRefreshLayout mSwipeRefreshLayout;//swipe refresh object for messages list

    //Sets that keep track of things
    Map<String, Boolean> setOfMessageIds = new HashMap<>();//Stores a map of messageId and a Boolean: true if the message for this id was already loaded, false if not
    Map<String, Integer> setOfTeacherNames = new HashMap<>();//Stores a temporary set of teacher names so each photo can be loaded from memory (name of the photo is hex(sha256(teacherName)))
    Map<String, Bitmap> listOfTeacherAndBitmaps;//Stores the bitmaps of each message author, to be inflated into the messagesAdapter

    //AsyncTask objects for the first AsyncTask execution (AsyncTasks can only be executed once)
    GetMessages messageUpdaterTask = new GetMessages();//Task that updates messages
    DoOrResumeLogin loginUpdaterTask = new DoOrResumeLogin();//Task that does login (or resume its session cookies)

    private static final int URL_LOADER = 0;//loader code for DataProvider (just one database is loaded here, but let's use this code anyway)

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
        Log.d(LOG_TAG, "MESSAGES FRAGMENT CREATED");
        Sisgrad app = ((Sisgrad) getContext().getApplicationContext());//Gets the global object that stores login information
        login = app.getLoginObject();//gets the login object from the global object
        loadTeacherImagesToAdapter();
        ImageManagement.DumpCursor(getContext());
        getLoaderManager().initLoader(URL_LOADER, null, this);//calls the cursorLoader
        loginUpdaterTask.execute(login);//start login process (or resume it)
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
                            messageUpdaterTask.execute(login);
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

        mAdapter.authorImages = listOfTeacherAndBitmaps;
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
                        DataProviderContract.MESSAGES.SENT_DATE_UNIX+" DESC"             // Arrange by earliest
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
    private class DoOrResumeLogin extends AsyncTask<SisgradCrawler, Integer, Boolean> {
        protected Boolean doInBackground(SisgradCrawler... loginObject) {
            Log.d(LOG_TAG, "login AsyncTask called");
            SisgradCrawler login = loginObject[0];
            mountListOfMessageIds();
            mountListOfTeacherNames();
            try {
                login.loginToSentinela();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "login error");
                return false;
            }
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean loginResult) {
            Log.d(LOG_TAG, "login sucessful, now getting messages list");
            messageUpdaterTask.execute(login);
        }
    }
    /*
    * AsyncTask to finally load the messages from the server (not the content, just the metadata of each message).
    * Each message must be loaded from the message code in this metadata (yeah, webcrawling sucks...)
    */
    private class GetMessages extends AsyncTask<SisgradCrawler, Integer, List<Map<String, String>>> {
        protected List<Map<String, String>> doInBackground(SisgradCrawler... login) {
            Log.d(LOG_TAG, "getting messages");
            List<Map<String, String>> messages = null;
            try {
                messages = login[0].getMessages(0);//gets the first page of messages
                Log.d("MAIN", "got messages");
                for (Map<String, String> messageMap : messages) {//iterates through the messages
                    String newId = messageMap.get("messageId");
                    if (!setOfMessageIds.containsKey(newId)) {
                        //if this message is not included in the setOfMessageIds, add it to the database
                        ContentValues values = new ContentValues();//the method of inserting data into the database is through a ContentProvider
                        //values.put(MessagesTable.COLUMN_ID, id);
                        values.put(DataProviderContract.MESSAGES.TITLE, messageMap.get("title"));
                        values.put(DataProviderContract.MESSAGES.AUTHOR, messageMap.get("author"));
                        values.put(DataProviderContract.MESSAGES.MESSAGE_ID, messageMap.get("messageId"));
                        values.put(DataProviderContract.MESSAGES.SENT_DATE, messageMap.get("sentDate"));
                        values.put(DataProviderContract.MESSAGES.SENT_DATE_UNIX, messageMap.get("sentDateUnix"));
                        values.put(DataProviderContract.MESSAGES.READ_DATE, messageMap.get("readDate"));
                        values.putNull(DataProviderContract.MESSAGES.MESSAGE);
                        values.put(DataProviderContract.MESSAGES.ACESSED_DATE, "");
                        values.put(DataProviderContract.MESSAGES.DID_READ, 0);
                        getContext().getContentResolver().insert(DataProviderContract.MESSAGES_URI, values);//stores message metadata into database
                        setOfMessageIds.put(newId, false);//put messageId and 'false' in setOfMessageIds, so the fragment can load it later
                    } else {

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return messages;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(List<Map<String, String>> messages) {
            progress.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);
            /*
            * Now the AsyncTask that loads the first unloaded message from setOfMessageIds will be called.
            * Here, two will be called, so there is always two AsyncTask's running. Not too much but
            * it works fine. Every time one terminates, a new one is called, until all keys of setOfMessageIds
            * are true (that is, all message contents are loaded)
            */
            new loadEarliestUnloadedMessageFromMap().execute();
            new loadEarliestUnloadedMessageFromMap().execute();
            /*
            * Now the AsyncTask that loads the first unloaded message from setOfTeacherIds will be called.
            * Here, two will be called, so there is always two AsyncTask's running. Not too much but
            * it works fine. Every time one terminates, a new one is called, until all keys of setOfTeacherIds
            * are true (that is, all teacher images are loaded or failed to load)
            */
            new loadEarliestTeacherImageFromMap().execute();
            new loadEarliestTeacherImageFromMap().execute();

        }
    }
    /*
    * This is the AsyncTask that loads each message's content. It analyzes setOfMessageIds and the key
    * correspondent to messageId is set to false, then load its message and update into the database through
    * a contentProvider.
    */
    private class loadEarliestUnloadedMessageFromMap extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... s) {
            if (setOfMessageIds != null) {
                for (String id : setOfMessageIds.keySet()) {//Iterates over the key set :)
                    if (!setOfMessageIds.get(id)) {//verifies if value mapped to this  key id is false. If yes, loads a message. (the value of the key itentifies if the key has a message in the database)
                        try {
                            Log.d(LOG_TAG, "loading message for "+id);
                            setOfMessageIds.put(id, true);
                            SisgradCrawler.GetMessageResponse response = login.getMessage(id, true);//true means: get the HTML of message, false is for text-only
                            if (response.message.length() > 5) {
                                ContentValues values = new ContentValues();
                                values.put(DataProviderContract.MESSAGES.MESSAGE, response.message);
                                if (response.attachments!=null) {
                                    String jsonAttachments = new JSONObject(response.attachments).toString();
                                    //Log.d(LOG_TAG,"JSON MESSAGE:"+jsonAttachments);
                                    values.put(DataProviderContract.MESSAGES.ATTACHMENTS, jsonAttachments);
                                }
                                getContext().getContentResolver().update(DataProviderContract.MESSAGES_URI, values, DataProviderContract.MESSAGES.MESSAGE_ID + " = ?", new String[]{id});

                                //map.put(id, true);
                                //Log.d(LOG_TAG, "loaded and updated the following message: " + message.toString() + " of id " + id);
                            } else {
                                Log.d(LOG_TAG, "error inside runnable");
                                //just try once, them let the function recursively call itself if more teachers have number 0
                            }
                        } catch (Exception e) {
                            setOfMessageIds.put(id, false);
                            e.printStackTrace();
                        }
                    } else {
                    }
                }
            }
            return true;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean loadAgain) {
            /* starts another loadEarliestUnloadedMessageFromMap when this one finished
             * they'll stop loading when all keys in setOfMessageIds are set to false
             */
            if (setOfMessageIds.containsValue(false)) {
                new loadEarliestUnloadedMessageFromMap().execute();
            }
        }
    }
    /*
    * This is the AsyncTask that loads each message author image. It analyzes setOfMessageIds and the key
    * correspondent to messageId is set to false, then load its image.
    */
    private class loadEarliestTeacherImageFromMap extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... s) {
            if (setOfTeacherNames != null && lattes != null) {
                for (String teacher : setOfTeacherNames.keySet()) {//Iterates over the key set :)
                    if (setOfTeacherNames.get(teacher) == 0) {//verifies if value mapped to this  key id is 0, that means, a photo needs to be loaded
                        try {
                            Log.d(LOG_TAG, "searching for teacher "+teacher);
                            LattesCrawler.requestObject response = lattes.search(teacher);
                            if (response.teachedId != null && !response.teachedId.isEmpty()) {
                                Log.d(LOG_TAG, "calling image download, save and register for teacher "+teacher);
                                Bitmap teacherImage = ImageManagement.getBitmapHTTP(new URL(response.teacherURLImage));
                                ImageManagement.Tuple f =  ImageManagement.saveImage(teacherImage, teacher, getContext());//already lowercase, I guess
                                ImageManagement.registerImage(f, getContext());
                                setOfTeacherNames.put(teacher, 2);
                            } else {
                                setOfTeacherNames.put(teacher, 1);
                                ImageManagement.registerFailedAttempt(new ImageManagement.Tuple(Sha256Hex.hash(teacher.toLowerCase()),""), getContext());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ImageManagement.registerFailedAttempt(new ImageManagement.Tuple(Sha256Hex.hash(teacher.toLowerCase()),""), getContext());
                            setOfTeacherNames.put(teacher, 1);
                        }
                        loadTeacherImagesToAdapter();
                        break;//just try once, them let the function recursively call itself if more teachers have number 0
                    }
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Void nothing) {
            /* starts another loadEarliestUnloadedMessageFromMap when this one finished
             * they'll stop loading when all keys in setOfMessageIds are set to false
             */
            //if the set still contains values with 0, load one more teacher image
            if (setOfTeacherNames.containsValue(0)) {//TODO: add support for recognizing the number 1 and try to load it again after 3 days or so
                Log.d(LOG_TAG, setOfTeacherNames.toString());
                new loadEarliestTeacherImageFromMap().execute();
            }
        }
    }

    private void mountListOfMessageIds() {
        /*
         * Fills setOfMessageIds with the key/value: messageId/Boolean where Boolean is true if the message was already loaded
         */
        Log.d(LOG_TAG, "mounting message IDs");
        setOfMessageIds.clear();//clears everytime, because this function can be called all time

        String[] projection = {//just intersted in message ID and message content
                DataProviderContract.MESSAGES.MESSAGE_ID,
                DataProviderContract.MESSAGES.MESSAGE,
        };
        Cursor cursorOfMessageIds = getContext().getContentResolver().query(DataProviderContract.MESSAGES_URI, projection, null, null, DataProviderContract.MESSAGES.SENT_DATE_UNIX+" DESC");//load messages

        for (cursorOfMessageIds.moveToFirst(); !cursorOfMessageIds.isAfterLast(); cursorOfMessageIds.moveToNext()) {
            Boolean hasMessage = false;
            if (cursorOfMessageIds.getString(cursorOfMessageIds.getColumnIndex(DataProviderContract.MESSAGES.MESSAGE)) != null) {//see if message is null (if it is, later the fragment will load it)
                hasMessage = true;
            }
            String messageId = cursorOfMessageIds.getString(cursorOfMessageIds.getColumnIndex(DataProviderContract.MESSAGES.MESSAGE_ID));
            setOfMessageIds.put(messageId, hasMessage);//insert the pair messageId/Boolean

        }
        cursorOfMessageIds.close();//always close the cursor!
        Log.d(LOG_TAG, "setOfMessageIds: "+setOfMessageIds.toString());
    }

    private void mountListOfTeacherNames() {
        Log.d(LOG_TAG, "mounting teacher names");
        setOfTeacherNames.clear();

        String[] projection = {
                DataProviderContract.MESSAGES.AUTHOR
        };
        String[] projection2 = {
                DataProviderContract.STORAGE.NAME,
                DataProviderContract.STORAGE.TRIED,
                DataProviderContract.STORAGE.DIDLOAD
        };
        Cursor cursorOfTeacherNames = getContext().getContentResolver().query(DataProviderContract.MESSAGES_URI, projection, null, null, DataProviderContract.MESSAGES.SENT_DATE_UNIX+" DESC");//load me
        for (cursorOfTeacherNames.moveToFirst(); !cursorOfTeacherNames.isAfterLast(); cursorOfTeacherNames.moveToNext()) {
            Integer photoStatus = 0;//0 if did not even try, 1 if did try but couldn't load, 2 if did try and did load.
            String currentTeacher = cursorOfTeacherNames.getString(cursorOfTeacherNames.getColumnIndex(DataProviderContract.MESSAGES.AUTHOR)).toLowerCase();//remember to always put to lowercase
            //cursorOfTeacherNames.getString(cursorOfTeacherNames.getColumnIndex(DataProviderContract.STORAGE.NAME)).toLowerCase();//remember to always put to lowercase
            if (!setOfTeacherNames.containsKey(currentTeacher)) {
                setOfTeacherNames.put(currentTeacher, photoStatus);
            }

        }
        for (String teacherName: setOfTeacherNames.keySet()) {
            Integer photoStatus = 0;//0 if did not even try, 1 if did try but couldn't load, 2 if did try and did load.
            String selection = DataProviderContract.STORAGE.NAME+"=?";
            String[] arguments = {Sha256Hex.hash(teacherName)};
            Cursor cursorOfFiles = getContext().getContentResolver().query(DataProviderContract.STORAGE_URI, projection2, selection, arguments, null);
            if(cursorOfFiles!=null && cursorOfFiles.getCount()>0) {
                cursorOfFiles.moveToFirst();
                Integer didTry = cursorOfFiles.getInt(cursorOfFiles.getColumnIndex(DataProviderContract.STORAGE.TRIED));
                Integer didLoad = cursorOfFiles.getInt(cursorOfFiles.getColumnIndex(DataProviderContract.STORAGE.DIDLOAD));
                if (didTry==0 && didLoad==0) {
                    photoStatus = 0;
                } else if (didTry==1 && didLoad==0) {
                    photoStatus = 1;
                } else if (didTry==1 && didLoad==1) {
                    photoStatus = 2;
                }
            } else {
                photoStatus = 0;
            }
            setOfTeacherNames.put(teacherName, photoStatus);
            cursorOfFiles.close();
        }
        cursorOfTeacherNames.close();
        System.out.println("set of teacher names :" + setOfTeacherNames);
    }

    public void loadTeacherImagesToAdapter() {
        mountListOfTeacherNames();
        listOfTeacherAndBitmaps = new HashMap<>();
        for (String teacher: setOfTeacherNames.keySet()) {
            try {
                Bitmap image = ImageManagement.loadImageFromStorage(teacher, getContext());
                if (image != null) {
                    listOfTeacherAndBitmaps.put(teacher.toLowerCase(), image);
                    //mAdapter.authorImages.put(teacher, image);
                }
            } catch(Exception e) {
                //
            }
        }
        Log.d(LOG_TAG, "listOfTeacherAndBitmaps: +"+listOfTeacherAndBitmaps);
        //mAdapter.authorImages = listOfTeacherAndBitmaps;
    }
}
