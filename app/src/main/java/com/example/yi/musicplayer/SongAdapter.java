package com.example.yi.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by yi on 11/3/15.
 */
public class SongAdapter extends BaseAdapter{

    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> songs) {
        this.songs = songs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // map to song layout
        LinearLayout songLay = (LinearLayout) songInf.inflate(R.layout.song, parent, false);
        // get title and artsit views
        TextView songView = (TextView) songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLay.findViewById(R.id.song_artist);
        // get song using position
        Song currSong = songs.get(position);
        // get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        // set position as tag
        songLay.setTag(position);
        return songLay;
    }
}
