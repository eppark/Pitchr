package com.example.pitchr.chat;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseRelation;

@ParseClassName("DM")
public class DM extends ParseObject {

    public static final String KEY_USERS = "users";
    public static final String KEY_MESSAGES = "messages";
    public static final String KEY_CREATED_AT = "createdAt";

    public ParseRelation<Message> getMessages() {
        return getRelation(KEY_MESSAGES);
    }

}