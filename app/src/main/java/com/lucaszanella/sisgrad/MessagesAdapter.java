package com.lucaszanella.sisgrad;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lucaszanella on 5/2/16.
 */

public class MessagesAdapter extends CursorAdapter {
    //values in seconds
    private static final long ONE_HOUR = 60*60;
    private static final long ONE_DAY = ONE_HOUR*24;
    private static final long ONE_WEEK = ONE_DAY*7;
    private static final long ONE_MONTH = ONE_DAY*30;

    public MessagesAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, 0);
    }

    public Map<String, Bitmap> authorImages = new HashMap<>();

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
        ImageView image = (ImageView) view.findViewById(R.id.person);
        View progress = (ProgressBar) view.findViewById(R.id.progress);
        //progress.setVisibility(View.VISIBLE);

        // Extract properties from cursor
        String titleString = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.TITLE));
        String authorString = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.AUTHOR));
        long unixTime = Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.SENT_DATE_UNIX)));
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
        /*
        if (authorImages!=null) {
            //Log.d("mAdapter","log tag for "+cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.AUTHOR)).toLowerCase());
            Bitmap photo = authorImages.get(cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.AUTHOR)).toLowerCase());
            if (photo != null) {
                image.setImageBitmap(photo);
            }
        }
        */
        String authorName = cursor.getString(cursor.getColumnIndexOrThrow(DataProviderContract.MESSAGES.AUTHOR)).toLowerCase();
        //loads each bitmap ONCE to authorImages, if they're already loaded, just set them as the image source in 'image' views
        if (!authorImages.containsKey(authorName)) {
            try {
                Bitmap img = ImageManagement.loadImageFromStorage(authorName, context);
                if (img!=null) {
                    authorImages.put(authorName, img);
                    image.setImageBitmap(img);
                } else {
                    image.setImageResource(R.mipmap.genericavatar);

                }
            } catch (Exception e) {

            }
        } else {
            try {
                image.setImageBitmap(authorImages.get(authorName));
            } catch (Exception e) {

            }
        }
        Date date = new Date();
        long currentTime = date.getTime()/1000;

        String timeText = "";
        //Log.d("adapter", "name: "+authorName+" currentTime: "+currentTime+" unixTime: "+unixTime+" difference: "+difference);
        timeText = TimeAgo.TimeAgo(currentTime, unixTime);

        //java.util.Date timeDate =new java.util.Date(Long.parseLong(unixTime)*1000);
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
        time.setText(timeText);
        //cursor.close();
    }
}
