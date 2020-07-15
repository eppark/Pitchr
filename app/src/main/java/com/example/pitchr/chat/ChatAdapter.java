package com.example.pitchr.chat;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<Message> mMessages;
    private Context mContext;
    private ParseFile receiver_pfp;
    private ParseFile sender_pfp;

    public ChatAdapter(Context context, List<Message> messages, ParseFile receive_pfp) {
        mMessages = messages;
        mContext = context;
        receiver_pfp = receive_pfp;
        sender_pfp = ParseUser.getCurrentUser().getParseFile("pfp");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_chat, parent, false);

        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = mMessages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageOther;
        ImageView imageMe;
        TextView body;

        public ViewHolder(View itemView) {
            super(itemView);
            imageOther = (ImageView)itemView.findViewById(R.id.ivProfileOther);
            imageMe = (ImageView)itemView.findViewById(R.id.ivProfileMe);
            body = (TextView)itemView.findViewById(R.id.tvBody);
        }

        public void bind(Message message) {
            final boolean isMe = message.getSender() != null && message.getSender().getObjectId().equals(ParseUser.getCurrentUser().getObjectId());

            if (isMe) {
                imageMe.setVisibility(View.VISIBLE);
                imageOther.setVisibility(View.GONE);
                body.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);

            } else {
                imageOther.setVisibility(View.VISIBLE);
                imageMe.setVisibility(View.GONE);
                body.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            }

            ImageView profileView;
            if (isMe) {
                profileView = imageMe;
                body.setBackgroundResource(R.drawable.outgoing_speech_bubble);
                body.setTextColor(mContext.getResources().getColor(R.color.white));
                body.setPadding(30, 20, 60, 40);
                if (sender_pfp != null) {
                    Glide.with(mContext).load(sender_pfp.getUrl()).circleCrop().into(profileView);
                } else {
                    Glide.with(mContext).load(R.drawable.default_pfp).circleCrop().into(profileView);
                }
            } else {
                profileView = imageOther;
                body.setBackgroundResource(R.drawable.incoming_speech_bubble);
                body.setTextColor(mContext.getResources().getColor(R.color.gray3));
                body.setPadding(60, 20, 30, 40);
                if (receiver_pfp != null) {
                    Glide.with(mContext).load(receiver_pfp.getUrl()).circleCrop().into(profileView);
                } else {
                    Glide.with(mContext).load(R.drawable.default_pfp).circleCrop().into(profileView);
                }
            }

            body.setText(message.getMessage());
        }
    }
}