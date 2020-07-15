package com.example.pitchr.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private Context context;
    private List<ParseUser> allMessages;

    public MessagesAdapter(Context context, List<ParseUser> allMessages) {
        this.context = context;
        this.allMessages = allMessages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new MessagesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParseUser message = allMessages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return allMessages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final String TAG = ViewHolder.class.getSimpleName();
        ParseUser currentMessage;
        ImageView ivPfp;
        TextView tvUsername;
        TextView tvComment;
        DM currentDm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPfp = (ImageView) itemView.findViewById(R.id.ivPfp);
            tvUsername = (TextView) itemView.findViewById(R.id.tvUsername);
            tvComment = (TextView) itemView.findViewById(R.id.tvTime);
            itemView.setOnClickListener(this);
        }

        public void bind(ParseUser message) {
            currentMessage = message;
            getDM();

            // Set the username bold
            SpannableString str =  new SpannableString(message.getUsername());
            str.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), 0);
            tvUsername.setText(str);

            // Put in the image
            ParseFile image = message.getParseFile("pfp");
            if (image != null) {
                Glide.with(context).load(image.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("receiver", Parcels.wrap(currentMessage));
            intent.putExtra("dm", Parcels.wrap(currentDm));
            context.startActivity(intent);
        }

        // Get the current DM
        private void getDM() {
            // Query for DMs that have these exact two users
            ArrayList<String> users = new ArrayList<String>();
            users.add(ParseUser.getCurrentUser().getObjectId());
            users.add(currentMessage.getObjectId());
            // Query
            ParseQuery<DM> dmQuery = ParseQuery.getQuery(DM.class);
            dmQuery.include(DM.KEY_USERS);
            dmQuery.whereContainsAll(DM.KEY_USERS, users);
            dmQuery.findInBackground(new FindCallback<DM>() {

                @Override
                public void done(List<DM> dms, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Issue with getting DM", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d(TAG, "Query DM success!");
                    if (dms.size() > 0) {
                        // If the DM already exists, just use that
                        currentDm = dms.get(0);
                        setComment();
                    } else {
                        // If the DM doesn't exist, create a new DM
                        currentDm = new DM();
                        currentDm.addAllUnique(DM.KEY_USERS, Arrays.asList(ParseUser.getCurrentUser().getObjectId(), currentMessage.getObjectId()));

                        // Save the DM
                        currentDm.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Saving DM failed!", e);
                                    return;
                                }
                                setComment();
                            }
                        });
                    }
                }
            });
        }

        // Set the small comment preview
        private void setComment() {
            // Get messages from this DM
            ParseQuery<ParseObject> query = currentDm.getRelation(DM.KEY_MESSAGES).getQuery();
            query.include(DM.KEY_USERS);
            query.setLimit(1); // Only show 1 message: the newest one
            query.orderByDescending("createdAt");
            // Execute query to fetch all messages from Parse asynchronously
            // This is equivalent to a SELECT query with SQL
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> messages, ParseException e) {
                    if (e == null) {
                        tvComment.setText("");
                        // Set the text preview with "You: " if you were the last sender
                        if (messages.size() > 0) {
                            Message preview = (Message) messages.get(0);
                            if (preview.getSender().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                                tvComment.append("You: ");
                            }
                            tvComment.append(preview.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Error Loading Messages" + e);
                    }
                }
            });
        }
    }

    // Clean all elements of the recycler
    public void clear() {
        allMessages.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<ParseUser> list) {
        allMessages.addAll(list);
        notifyDataSetChanged();
    }
}