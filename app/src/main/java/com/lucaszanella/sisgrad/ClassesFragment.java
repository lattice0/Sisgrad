package com.lucaszanella.sisgrad;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lucaszanella.SisgradCrawler.SisgradCrawler;

import java.util.List;
import java.util.Map;

//import android.app.LoaderManager;
//import android.content.CursorLoader;
//import android.content.Loader;
//import android.widget.CursorAdapter;

public class ClassesFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "ClassesFragment";
    private Sisgrad app;
    private static final int URL_LOADER = 1;
    private boolean loginProcessTerminatedSucessful = false;
    private Cursor actualCursor = null;

    private ClassesAdapter mAdapter;

    private String[] mProjection = {
            DataProviderContract.CLASSES.ID,
            DataProviderContract.CLASSES.DAY,
            DataProviderContract.CLASSES.CLASS,
            DataProviderContract.CLASSES.TEACHER,
    };
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (loaderID) {
            case URL_LOADER:
                return new CursorLoader(
                        getContext(),   // Parent activity context
                        DataProviderContract.CLASSES_URI,   // Table to query
                        mProjection,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
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
        actualCursor = returnCursor;
        mAdapter.changeCursor(returnCursor);
    }

    /*
     * Invoked when the CursorLoader is being reset. For example, this is called if the
     * data in the provider changes and the Cursor becomes stale.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Sets the Adapter's backing data to null. This prevents memory leaks.
        mAdapter.changeCursor(null);
    }
    /*
     * AsyncTask that loads 'classes' (here 'classes' means classes from the university).
     * TODO: make this asyncTask prettier and implement (and catch) the right errors in the SisgradCrawler class
     */
    private class getClasses extends AsyncTask<Void, Integer, Integer> {
        private static final int OK = 0;
        private static final int TIMEOUT = 1;
        private static final int EXCEPTION = 2;
        private static final int PAGE_ERROR = 3;
        protected Integer doInBackground(Void... nothing) {
            Log.d(LOG_TAG, "getting classes");
            SisgradCrawler.GetClassesResponse  classes = null;
            try {
                classes = app.getLoginObject().getClasses();
                Log.d(LOG_TAG, "got classes: "+classes);
            } catch (Exception e) {
                e.printStackTrace();
                //TODO: display the exception
                return EXCEPTION;
            }
            return OK;
        }

        protected void onProgressUpdate(Integer progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(List<Map<String, String>> classes) {
            //text.setText(messages.get(0).toString());
            //showDialog("Downloaded " + result + " bytes");
        }
    }

    public static Fragment newInstance() {//Returns an instance of this fragment to be attached in the MainActivity
        return new ClassesFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ((Sisgrad) getContext().getApplicationContext());
        //getLoaderManager().initLoader(URL_LOADER, null, this);//starts the loader manager for this class
    }
    //TODO: create the layout inflater for this fragment.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /*// Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.activity_messages_display, container, false);
        Log.d(LOG_TAG, "getting loader manager");
        ListView mListView = (ListView) view.findViewById(R.id.messagesList);
        mAdapter =
                new ClassesAdapter(
                        getContext(),   // Current context
                        null,  //No cursor yet
                        0 // No flags
                );

        mListView.setAdapter(mAdapter);
        Log.d(LOG_TAG, "setted adapter");
        return view;
        */
        return null;
    }

}
