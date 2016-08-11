package com.lucaszanella.sisgrad;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by lucaszanella on 5/18/16.
 * Adapted from this answer: http://stackoverflow.com/a/26581876/5884503
 * TODO: substitute MessagesAdapter by this one, which is lighter and more efficient
 */
public class MessagesRecycleCursorAdapter extends RecyclerView.Adapter<MessagesRecycleCursorAdapter.ViewHolder>{//Entender melhor esta declaração

    Cursor messagesCursor;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;

        public final TextView Title;
        public final TextView Author;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mImageView = (ImageView) view.findViewById(R.id.avatar);
            Title = (TextView) view.findViewById(R.id.title);
            Author = (TextView) view.findViewById(R.id.author);
        }
    }



    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (messagesCursor == cursor) {
            return null;
        }
        Cursor oldCursor = messagesCursor;
        this.messagesCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    private Object getItem(int position) {
        return null;
    }

    @Override
    public int getItemCount() {
        return (messagesCursor == null) ? 0 : messagesCursor.getCount();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_list_element, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position>=1) {
            System.out.println("POSITION: " + position);
            messagesCursor.move(position);
            holder.Title.setText(messagesCursor.getString(messagesCursor.getColumnIndexOrThrow("title")));
            holder.Author.setText(messagesCursor.getString(messagesCursor.getColumnIndexOrThrow("author")));
        }
    }


}