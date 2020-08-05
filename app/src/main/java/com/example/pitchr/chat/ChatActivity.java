package com.example.pitchr.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivityChatBinding;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = ChatActivity.class.getSimpleName();
    ActivityChatBinding binding;
    ArrayList<Message> mMessages;
    ChatAdapter mAdapter;
    ParseUser receiver;
    public DM currentDm;

    // keep track of initial load to scroll to bottom of the view
    boolean mFirstLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("chatActivity");

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        binding.tvNoMessages.setVisibility(View.GONE);
        binding.pbLoading.setVisibility(View.GONE); // hide loading bar at first
        mFirstLoad = true;

        // Show back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set user and DM info
        receiver = (ParseUser) Parcels.unwrap(getIntent().getParcelableExtra("receiver"));
        currentDm = (DM) Parcels.unwrap(getIntent().getParcelableExtra("dm"));
        getSupportActionBar().setTitle(receiver.getUsername());
        binding.toolbar.setTitle(receiver.getUsername());

        // Setup adapter
        mMessages = new ArrayList<>();
        mAdapter = new ChatAdapter(ChatActivity.this, mMessages, receiver.getParseFile("pfp"));
        binding.rvChat.setAdapter(mAdapter);

        // associate the LayoutManager with the RecylcerView
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setReverseLayout(true);
        binding.rvChat.setLayoutManager(linearLayoutManager);

        // Set up messages and refreshing
        binding.pbLoading.setVisibility(View.VISIBLE);
        setupMessagePosting();
        myHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);
    }

    // Setup button event handler which posts the entered message to Parse
    private void setupMessagePosting() {
        // When send button is clicked, create message object on Parse
        binding.btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description = binding.etMessage.getText().toString();
                if (!description.isEmpty()) {
                    final Message message = new Message();
                    message.setMessage(description);
                    message.setReceiver(receiver);
                    message.setSender(ParseUser.getCurrentUser());
                    message.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                // LOG TO ANALYTICS
                                ParseApplication.logEvent("messageEvent", Arrays.asList("status"), Arrays.asList("failure"));

                                // Let the user know there was an error
                                Log.e(TAG, "Error while saving", e);
                                Toast.makeText(getApplicationContext(), "Error while saving message!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // LOG TO ANALYTICS
                            ParseApplication.logEvent("messageEvent", Arrays.asList("status"), Arrays.asList("success"));

                            // Save to Parse and refresh messages
                            Log.i(TAG, "Message save success!");
                            currentDm.getRelation(DM.KEY_MESSAGES).add(message);
                            currentDm.saveInBackground();
                            refreshMessages();

                            // Notify the other user that they were messaged
                            String topic = String.format("/topics/%s", receiver.getUsername());
                            String notificationTitle = "Pitchr";
                            String notificationMessage = String.format("%s sent you a message: \"%s\"", ParseUser.getCurrentUser().getUsername(), description);
                            String icon = ((ParseFile) ParseUser.getCurrentUser().get("pfp")) != null ? ((ParseFile) ParseUser.getCurrentUser().get("pfp")).getUrl() : "";

                            JSONObject notification = new JSONObject();
                            JSONObject notificationBody = new JSONObject();
                            try {
                                // Set the message
                                notificationBody.put("title", notificationTitle);
                                notificationBody.put("message", notificationMessage);
                                if (!icon.isEmpty()) {
                                    notificationBody.put("icon", icon);
                                } else {
                                    notificationBody.put("icon", getString(R.string.default_app_icon_url));
                                }

                                // Set the topic
                                notification.put("to", topic);
                                notification.put("data", notificationBody);
                            } catch (JSONException ex) {
                                Log.e(TAG, "onCreate error!", ex);
                            }
                            // Send the notification
                            ParseApplication.sendNotification(notification, getApplicationContext());
                        }
                    });
                } else {
                    Toast.makeText(ChatActivity.this, "Can't send an empty message!", Toast.LENGTH_SHORT).show();
                }
                binding.etMessage.setText(null);
            }
        });
    }

    // Query messages from Parse so we can load them into the chat adapter
    private void refreshMessages() {
        // Get messages from this DM
        ParseQuery<ParseObject> query = currentDm.getRelation(DM.KEY_MESSAGES).getQuery();
        query.include(DM.KEY_USERS);
        query.setLimit(20); // Only show 20 messages
        // get the latest 20 messages, order will show up newest to oldest of this group
        query.orderByDescending("createdAt");
        // Execute query to fetch all messages from Parse asynchronously
        // This is equivalent to a SELECT query with SQL
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> messages, ParseException e) {
                if (e == null) {
                    mMessages.clear();
                    mMessages.addAll((List<Message>)(Object) messages);
                    mAdapter.notifyDataSetChanged(); // update adapter
                    // Scroll to the bottom of the list on initial load
                    if (mFirstLoad) {
                        binding.rvChat.scrollToPosition(0);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e("message", "Error Loading Messages" + e);
                }
                if (mMessages.size() == 0) {
                    binding.tvNoMessages.setVisibility(View.VISIBLE);
                } else {
                    binding.tvNoMessages.setVisibility(View.GONE);
                }
                binding.pbLoading.setVisibility(View.GONE);
            }
        });
    }

    // Create a handler which can run code periodically
    static final int POLL_INTERVAL = 1000; // milliseconds
    Handler myHandler = new android.os.Handler();
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            myHandler.postDelayed(this, POLL_INTERVAL);
        }
    };

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}