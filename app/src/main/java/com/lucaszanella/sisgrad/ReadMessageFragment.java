package com.lucaszanella.sisgrad;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ReadMessageFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int URL_LOADER = 0;
    private static final String LOG_TAG = "ReadMFrag";//name for the Log.d function

    private Cursor actualCursor = null;//cursor that's gonna be used to load the message
    private String[] mProjection = {
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

    //Ui elements
    ProgressBar progress;
    TextView title;
    TextView author;
    TextView message;
    TextView time;
    ImageView authorImageView;
    View buttons;
    View messageView;
    String messageId;
    String provisoryTitle;

    //View attachments;
    TextView attachmentsName;
    ViewGroup attachmentPoint;


    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        /*
         * Takes action based on the ID of the Loader that's being created
         */

        //gets the intent from the other activity
        Intent intent = getActivity().getIntent();
        messageId = intent.getStringExtra("id");
        provisoryTitle = intent.getStringExtra("provisoryTitle");//title that came from the other activity

        String messageIdArray[] = {messageId};
        String selection = DataProviderContract.MESSAGES.MESSAGE_ID+"=?";

        Log.d(LOG_TAG, "reading message of messageid: "+messageId);
        Log.d(LOG_TAG, "the messageId string is: "+messageIdArray[0]);
        Log.d(LOG_TAG, "selection clause is: "+selection);

        switch (loaderID) {
            case URL_LOADER:
                Log.d(LOG_TAG, "URL_LOADER chosen");
                // Returns a new CursorLoader
                return new CursorLoader(
                        getContext(),   // Parent activity context
                        DataProviderContract.MESSAGES_URI,        // Table to query
                        mProjection,     // Projection to return
                        selection,            // Selection clause
                        messageIdArray,            // Selection arguments
                        null             // Default sort order
                );
            default:
                Log.d(LOG_TAG, "NULL chosen");
                // An invalid id was passed in
                return null;
        }
    }
    //Using cursor loader as taught by google https://developer.android.com/training/load-data-background/setup-loader.html
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
        /*
         *  Changes the adapter's Cursor to be the results of the load. This forces the View to
         *  redraw.
         */
        if (returnCursor.getCount() > 0) {
            progress.setVisibility(View.GONE);
            Log.d(LOG_TAG, "cursor size is "+returnCursor.getCount());
        }
        actualCursor = returnCursor;
        actualCursor.moveToFirst();//just in case there are two equal messageIds (which should never happen, but who knows...)
        showMessage();
    }

    /*
    * Invoked when the CursorLoader is being reset. For example, this is called if the
    * data in the provider changes and the Cursor becomes stale.
    */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Sets the Adapter's backing data to null. This prevents memory leaks.
        // Here we don't use adapters, so there's nothing to do
    }
    public void showMessage() {
        //Loads the message components from the database
        String messageTitle = actualCursor.getString(actualCursor.getColumnIndex(DataProviderContract.MESSAGES.TITLE));
        String messageAuthor = actualCursor.getString(actualCursor.getColumnIndex(DataProviderContract.MESSAGES.AUTHOR));
        String rawMessage = actualCursor.getString(actualCursor.getColumnIndex(DataProviderContract.MESSAGES.MESSAGE));
        String attachmentsJson = actualCursor.getString(actualCursor.getColumnIndex(DataProviderContract.MESSAGES.ATTACHMENTS));
        String poorlyProcessedMessage = "";
        if (rawMessage!=null) {
            poorlyProcessedMessage = Html.fromHtml(rawMessage).toString();
        }

        //Sets the loaded data to the ui elements
        title.setText(messageTitle);
        author.setText(messageAuthor);

        Bitmap image = ImageManagement.loadImageFromStorage(messageAuthor.toLowerCase(), getContext());
        if (image!=null) {
            authorImageView.setImageBitmap(image);
        }
        message.setText(poorlyProcessedMessage);//writes the message to the view
        messageView.setVisibility(View.VISIBLE);//makes it visible, finally
        progress.setVisibility(View.GONE);//hide progress bar

        actualCursor.close();//always close the cursor

        //Iterate through json string. Got here: http://stackoverflow.com/a/4149555/5884503
        // Assume you have a Map<String, String> in JSONObject jdata
        if (attachmentsJson!=null && attachmentsJson.length()>2) {//when empty, json can be {} but I think I eliminated this behavior from the code that writes to the database, leaving it null
            @SuppressWarnings("unchecked")
            JSONObject jdata = null;
            try {
                jdata = new JSONObject(attachmentsJson);//transforms the string into a json object
                Iterator<String> nameItr = jdata.keys();
                Map<String, String> outMap = new HashMap<>();
                while(nameItr.hasNext()) {
                    String name = nameItr.next();
                    outMap.put(name, jdata.getString(name));
                }
                //for each string, inflate a view for the attachment
                for (Map.Entry<String, String> entry : outMap.entrySet())
                {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View attachments = inflater.inflate(R.layout.attachment, null);
                    attachmentsName = (TextView) attachments.findViewById(R.id.attachmentName);
                    attachmentsName.setText(entry.getKey());
                    attachmentPoint.addView(attachments, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_read_message, container, false);

        //just finds the ui components
        progress = (ProgressBar) view.findViewById(R.id.loading);
        progress.setVisibility(View.VISIBLE);
        provisoryTitle = getActivity().getIntent().getStringExtra("provisoryTitle");
        title = (TextView) view.findViewById(R.id.title);
        messageView = (View) view.findViewById(R.id.messageView);
        messageView.setVisibility(View.INVISIBLE);
        author = (TextView) view.findViewById(R.id.author);
        message = (TextView) view.findViewById(R.id.message);
        time = (TextView) view.findViewById(R.id.time);
        attachmentPoint = (ViewGroup) view.findViewById(R.id.attachments);
        authorImageView = (ImageView) view.findViewById(R.id.authorImage);

        return view;//returns the mounted view to be attached to the activity
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "created read messages fragment");

        getLoaderManager().initLoader(URL_LOADER, null, this);//calls the cursorLoader
    }
}
