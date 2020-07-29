package com.example.pitchr.models;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.ArrayList;

public class PostItem {

    public Post post;
    public int type;
    public ArrayList<Song> recSongs;
    public UnifiedNativeAd ad;

    public static final int TYPE_POST = 0;
    public static final int TYPE_REC = 1;
    public static final int TYPE_AD = 2;

    public PostItem(Post post, int type, ArrayList<Song> recSongs) {
        this.type = type;
        if (this.type == TYPE_REC) {
            this.recSongs = recSongs;
            this.post = null;
        } else if (this.type == TYPE_POST){
            this.recSongs = null;
            this.post = post;
        } else {
            this.recSongs = null;
            this.post = null;
        }
    }

    public PostItem(Post post, int type, ArrayList<Song> recSongs, UnifiedNativeAd ad) {
        this.type = type;
        if (this.type == TYPE_REC) {
            this.recSongs = recSongs;
            this.post = null;
            this.ad = null;
        } else if (this.type == TYPE_POST){
            this.recSongs = null;
            this.post = post;
            this.ad = null;
        } else {
            this.recSongs = null;
            this.post = null;
            this.ad = ad;
        }
    }
}
