Original App Design Project
===

# Pitchr

## Table of Contents
1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)
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

### [BONUS] Interactive Prototype

## Schema 
[This section will be completed in Unit 9]
### Models
[Add table of models]
### Networking
- [Add list of network requests by screen ]
- [Create basic snippets for each Parse network request]
- [OPTIONAL: List endpoints if using existing API such as Yelp]
