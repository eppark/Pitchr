package com.example.pitchr.models;

import java.util.ArrayList;

public class PostItem {

    public Post post;
    public boolean isRec;
    public ArrayList<Song> recSongs;

    public PostItem(Post post, boolean isRec, ArrayList<Song> recSongs) {
        this.isRec = isRec;
        if (isRec) {
            this.recSongs = recSongs;
            this.post = null;
        } else {
            this.recSongs = null;
            this.post = post;
        }
    }
}
