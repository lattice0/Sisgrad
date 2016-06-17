package com.lucaszanella.sisgrad;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by lucaszanella on 5/2/16.
 */

public class MessagesAdapter extends CursorAdapter {
    public MessagesAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, 0);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.message_displayer, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView author = (TextView) view.findViewById(R.id.author);
        TextView message = (TextView) view.findViewById(R.id.message);
        TextView time = (TextView) view.findViewById(R.id.time);
        View progress = (ProgressBar) view.findViewById(R.id.progress);
        //progress.setVisibility(View.VISIBLE);
        // Extract properties from cursor
        String titleString = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.TITLE));
        String authorString = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.AUTHOR));
        String timeString = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.SENT_DATE_UNIX));
        String hasAttachment = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.ATTACHMENTS));
        ViewGroup attachmentPoint = (ViewGroup) view.findViewById(R.id.attachmentIcon);

        /*
        for (int i=0; i<3; i++) {
            View attachments = inflater.inflate(R.layout.attachment, null);
            TextView attachmentsName = (TextView) attachments.findViewById(R.id.attachmentName);
            attachmentsName.setText("Lista_"+i+"_Analise.pdf");
            attachmentPoint.addView(attachments, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        }
        */
        java.util.Date timeDate =new java.util.Date(Long.parseLong(timeString)*1000);
        String messageString = "";
        String a = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.MESSAGE));
        //Log.d("CURSOR", "message is "+a);
        if (a==null) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
            messageString = a;
        }
        // Populate fields with extracted properties
        title.setText(titleString);
        author.setText(authorString);
        message.setText(Html.fromHtml(messageString).toString().replace("\n", " ").replace("\r", ""));
        if (hasAttachment!=null) {
            //Log.d("A", "fucking hasAttachment"+hasAttachment);
            if (hasAttachment.length()>2) {
                /*
                LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View attachments = inflater.inflate(R.layout.attachment_icon, null);
                //View someView = attachments.findViewById(R.id.icon_container);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) attachments.getLayoutParams();
                params.width = attachmentPoint.getHeight();
                attachments.setLayoutParams(params);
                attachmentPoint.addView(attachments, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                */
            }
        }
        time.setText("08:20");
    }
}
