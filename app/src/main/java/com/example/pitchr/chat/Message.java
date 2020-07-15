package com.example.pitchr.chat;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Message")
public class Message extends ParseObject {

    public static final String KEY_MESSAGE = "message";
    public static final String KEY_SENDER = "sender";
    public static final String KEY_RECEIVER = "receiver";
    public static final String KEY_CREATED_AT = "createdAt";

    public String getMessage() {
        return getString(KEY_MESSAGE);
    }

    public void setMessage(String description) {
        put(KEY_MESSAGE, description);
    }

    public ParseUser getSender() {
        return getParseUser(KEY_SENDER);
    }

    public void setSender(ParseUser parseUser) {
        put(KEY_SENDER, parseUser);
    }

    public ParseUser getReceiver() {
        return getParseUser(KEY_RECEIVER);
    }

    public void setReceiver(ParseUser parseUser) {
        put(KEY_RECEIVER, parseUser);
    }
}