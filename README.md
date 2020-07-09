Original App Design Project
===

# Pitchr

## Table of Contents
1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)
1. [Timeline](#Timeline)
2. [Schema](#Schema)

## Overview
### Description
Pitchr is a music-related social networking / connecting app that focuses on connecting users that have similar music taste with each other.


### App Evaluation
- **Category:** Social networking
- **Mobile:** Matches users to other users of similar music taste. Users can post songs on their feed and follow other users.
- **Market:** Anyone interested in music
- **Habit:** Connect to Spotify to match users with a high similarity rate, but not exact, so that each user can find new music that they'd likely enjoy. Endless scrolling feed for music.
- **Scope:** Primarily be an app that allows users to find new music/friends and post songs.

## Product Spec

### 1. User Stories (Required and Optional)

**Required Must-have Stories**

- [ ] Users can create an account/log in
- [ ] Users can log out
- [ ] Users can connect their Spotify account
- [ ] Users can post Spotify songs
    - [ ] Users can search songs from Spotify
    - [ ] Users can double-tap to like a post
- [ ] Users can take a picture as their profile picture
- [ ] Users can find recommended "matched" users
    - [ ] The app searches through other accounts' favorite songs to find exact song matches
- [ ] Parse is used to keep track of the data

**Optional Nice-to-have Stories**

- [ ] Users can see their own profile
    - [ ] Contains their own song posts
    - [ ] Contains their following list
    - [ ] Contains their follower list
- [ ] Song matches are intentionally not exact so users can find new music to listen to
    - [ ] Song matches also use matching genres
    - [ ] Song matches also use matching artists
    - [ ] Song matches also use recommended songs
    - [ ] Song matches also use recommended artists 
- [ ] Users can tap a song post to hear it on Spotify (embed?)

### 2. Screen Archetypes

* Login Screen
    * Users can create an account/log in
    * Users can connect their Spotify account
* Main feed of songs users have shared
    * Users can post Spotify songs
        * Users can search songs from Spotify
        * Users can double-tap to like a post
* Search screen
    * Users can search songs from Spotify
* Matching view
    * Users can find recommended "matched" users
        * The app searches through other accounts' favorite songs to find exact song matches
* Profile view
    * Users can log out
    * Users can take a picture as their profile picture


### 3. Navigation

**Tab Navigation** (Tab to Screen)

* Main feed
* Matching view
* Profile view

**Flow Navigation** (Screen to Screen)

* Main feed
   => Search screen via search bar
   => Compose post screen via floating button
* Matching view
   => Add songs to favorites screen via button if the user doesn't have any favorite songs
* Profile view
   => Log out button to login screen

## Wireframes
<img src="https://imgur.com/oWOGh5r.jpg" width=600>

### [BONUS] Digital Wireframes & Mockups
<img src="https://imgur.com/W27eazp.jpg" width=600>

## Timeline
Loose timeline idea:

| Week                       	| Goals                                                                                                                                                                                                                                                                                                   	|
|----------------------------	|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|
| FBU week 3, project week 0 	| - Create app idea<br>- Create project plan                                                                                                                                                                                                                                                              	|
| FBU week 4, project week 1 	| - Set up data models (User, Post, Song) and validate they work<br>- Build the navigational skeleton (bottom navigation tabs) of app<br>- Build out skeleton views (Timeline, Matching, Profile) for app<br>- Build basic login/logout (and account creation flow)<br>- Build profile picture camera use 	|
| FBU week 5, project week 2 	| - Build compose post<br>- Build matching algorithm                                                                                                                                                                                                                                                      	|
| FBU week 6, project week 3 	| - Add visuals + add external UI library<br>- Add animations<br>- Add gesture recognizers                                                                                                                                                                                                                	|
| FBU week 7, project week 4 	| - Build following/followers list<br>- Build user posts timeline on profile<br>- Add more complexity to the matching algorithm<br>- Integrate song playback                                                                                                                                              	|
| FBU week 8, project week 5 	| - Project complete                                                                                                                                                                                                                                                                                      	|

## Schema 

### Models

User
| Property  	| Type              	| Description                                               	|
|-----------	|-------------------	|-----------------------------------------------------------	|
| objectId  	| String            	| unique id for the user (default field)                    	|
| pfp       	| File              	| user's profile picture                                    	|
| topSongs  	| Relation of Songs 	| top five songs that the user indicates as their favorites 	|
| followers 	| Relation of Users 	| user's followers                                          	|
| following 	| Relation of Users 	| user's following list                                     	|

Song
| Property   	| Type             	| Description                            	|
|------------	|------------------	|----------------------------------------	|
| objectId   	| String           	| unique ID for the song (default field) 	|
| name       	| String           	| song name                              	|
| artists    	| Array of Strings 	| array of artist names                  	|
| spotifyId 	| String           	| unique Spotify Id for the song        	|
| features   	| Array of floats  	| Spotify audio features for the track   	|
| image      	| File             	| image cover for the song               	|

Post
| Property  	| Type              	| Description                                   	|
|-----------	|-------------------	|-----------------------------------------------	|
| objectId  	| String            	| unique ID for the song (default field)        	|
| createdAt 	| DateTime          	| date when the post is created (default field) 	|
| author    	| Pointer to User   	| author User of the post                       	|
| song      	| Pointer to Song   	| Song content of the post                      	|
| caption   	| String            	| author's caption of the post                  	|
| likes     	| Relation of Users 	| Relation of Users that liked the post         	|

### Networking
<b>List of networking requests by screen</b>

* Main feed
    * (Read/GET) Query all posts where the author is in the following list of the current user
    <pre><code>ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
    query.include(Post.KEY_USER);
    query.whereContainedIn("author", ParseUser.getCurrentUser().getFollowing());
    query.setLimit(20); // Only show 20 posts
    query.addDescendingOrder(Post.KEY_CREATED_AT);
    query.findInBackground(new FindCallback<Post>() {
        @Override
        public void done(List<Post> posts, ParseException e) {
            if (e != null) {
                Log.e(TAG, "Issue with getting posts", e);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Query posts success!");
            allPosts.addAll(posts);
            adapter.notifyDataSetChanged();
        }
    });</pre></code>
    * (Create/POST) Create a new like on a post
    * (Delete) Delete existing like
    * (Create/POST) Create a new post
* Matching view
    * (Read/GET) Query all user's top songs' names ~~and audio features~~
    <pre><code>ParseQuery<ParseUser> query = ParseQuery.getQuery(ParseUser.class);
    query.include(ParseUser.KEY_TOP_SONGS);
    query.setLimit(20); // Only show 20 posts
    query.findInBackground(new FindCallback<ParseUser>() {
        @Override
        public void done(List<ParseUser> users, ParseException e) {
            if (e != null) {
                Log.e(TAG, "Issue with getting songs", e);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Query songs success!");
            List<Song> currentUserSongs = getSongs();
            for (ParseUser user : users) {
                compareSongs(ParseUser.getCurrentUser().getSongs(), user.getSongs());
            }
        }
    });</pre></code>
* Profile view
    * (Read/GET) Query user
    * (Update/PUT) Update user profile picture
    * (Create/POST) Create new top Song
    * (Delete) Delete top Song
    * (Update/PUT) Add to the user's follower/following relations

**Existing API Endpoints**

* Spotify base URL - https://api.spotify.com/v1/

| HTTP Verb 	| Endpoint            	| Description                                      	|
|-----------	|---------------------	|--------------------------------------------------	|
| GET       	| me/top/tracks       	| get top tracks for the current user              	|
| GET       	| audio-features/{id} 	| get a track's audio features with its Spotify Id 	|
| GET       	| tracks/{id}         	| get a track from Spotify                         	|
| GET       	| albums/{id}         	| get an album from Spotify                        	|
