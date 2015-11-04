package com.example.yi.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Remember that we are going to continue playback even when the user navigates away from the app.
 * In order to facilitate this, we will display a notification showing the title of the track being played.
 * Clicking the notification will take the user back into the app.
 * <p/>
 * Created by yi on 11/3/15.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    // media player
    private MediaPlayer player;
    // song list
    private ArrayList<Song> songs;
    // list that records the songs that has been played
    private Set<Integer> songsHasBeenPlayed;
    // current position
    private int songPosn;

    private final IBinder musicBind = new MusicBinder();

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = false;
    private Random rand;

    private AudioManager audioManager;

    @Override
    public void onCreate() {
        // create the service
        super.onCreate();
        // initialize position
        songPosn = 0;
        // create player
        player = new MediaPlayer();

        initMusicPlayer();

        rand = new Random();

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    /**
     * Stop the notification
     */
    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    public void initMusicPlayer() {
        // set player properties
        // the wake lock will let playback continue when the device becomes idle
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        // set the stream type to music
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // when the MediaPlayer instance is prepared
        player.setOnPreparedListener(this);
        // when a song has completed playback
        player.setOnCompletionListener(this);
        // when an error is thrown
        player.setOnErrorListener(this);
    }

    /**
     * Return the Binder object
     *
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    /**
     * This will execute when the user exists the app,
     * at which point we will stop the service.
     *
     * @param intent
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    /**
     * Will fire when a track ends, including cases where the user has chosen a new track
     * or skipped to the next/previous tracks as well as when the track reaches the end of its playback.
     * In the latter case, we want to continue playback by playing the next track.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    /**
     * Callback from the player.prepareAsync() in playSong() method
     *
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        // start playback
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
        // queue of songs and preventing any song from being repeated
        // until all songs have been played
        this.songsHasBeenPlayed = new HashSet<>(songs.size());
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Toast.makeText(getApplicationContext(), "AUDIO FOCUS CHANGED!!!!!!!", Toast.LENGTH_LONG).show();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {
        // play a song
        // do reset: since we will also use this code
        // when the user is playing subsequent songs
        player.reset();



        // get song
        Song playSong = songs.get(songPosn);
        // set the title used in notification bar
        songTitle = playSong.getTitle();
        // get id
        long currSong = playSong.getId();
        // set uri
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        try {
            // now try setting this URI as the data source for the MediaPlayer instance,
            // but an exception may be thrown if an error pops up
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        // complete the playSong method by calling the asynchronous method to prepare it
        player.prepareAsync();
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
    }

    /**
     * Go to prev song
     */
    public void playPrev() {

        // decrement the song index
        songPosn--;
        // check that we haven't gone outside the range of the list
        if (songPosn < 0) {
            songPosn = songs.size() - 1;
        }
        playSong();
    }

    /**
     * Skip to next
     */
    public void playNext() {

        boolean isTheNewSongHasBeenPlayed = true;


        if (shuffle) {

            // record the playing history (Set will not add duplicate by default)
            songsHasBeenPlayed.add(songPosn);
            do {

                // select a song randomly
                int newSong = songPosn;
                while (newSong == songPosn) {
                    newSong = rand.nextInt(songs.size());
                }
                songPosn = newSong;

                // check if the new song ever been played before
                if (songsHasBeenPlayed.contains(songPosn)) {
                    if (songsHasBeenPlayed.size() == songs.size()) {
                        songsHasBeenPlayed.clear();
                        Toast.makeText(getApplicationContext(), "All songs has been played!! Start over.", Toast.LENGTH_LONG).show();
                        player.reset();
                        return;
                    }
                } else {
                    isTheNewSongHasBeenPlayed = false;
                }
            } while (isTheNewSongHasBeenPlayed);

        } else {
            // play the next song
            songPosn++;
            if (songPosn >= songs.size()) {
                songPosn = 0;
            }
        }

        playSong();
    }

    public void setShuffle() {
        if (shuffle) {
            shuffle = false;
        } else {
            shuffle = true;
        }
    }

}
