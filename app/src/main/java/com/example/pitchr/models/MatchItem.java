package com.example.pitchr.models;

import com.parse.ParseUser;

public class MatchItem {

    public ParseUser user;
    public double percent;

    public MatchItem(ParseUser user, double percent) {
        this.user = user;
        this.percent = percent;
    }
}
