// Manages the music in the main menu

package com.burningflower.pokemonquiz.model;

import android.content.Context;
import android.media.MediaPlayer;

import com.burningflower.pokemonquiz.R;

import java.io.IOException;

/**
 * Created by User on 02/01/2017.
 */

public class MusicManager {
    private static MusicManager theInstance = null;
    private MediaPlayer mainSong;
    private boolean ignoreNextStopMusicV;

    public static MusicManager getInstance (Context context) {
        if (theInstance == null)
            theInstance = new MusicManager(context);

        return theInstance;
    }

    private MusicManager (Context context) {
        mainSong = MediaPlayer.create (context, R.raw.main_song);
        mainSong.setLooping (true);
    }

    public void playMusic () {
        mainSong.start ();
    }

    public void stopMusic () throws IOException {
        if (ignoreNextStopMusicV)
            ignoreNextStopMusicV = false;
        else {
            mainSong.stop();
            mainSong.prepare();
            mainSong.seekTo(0);
        }
    }

    public void pause () {
        if (mainSong.isPlaying ())
            mainSong.pause ();
    }

    public boolean isPlaying () {
        return mainSong.isPlaying ();
    }

    public void ignoreNextStopMusic () {
        if (ignoreNextStopMusicV)
            ignoreNextStopMusicV = false;
        else {
            this.ignoreNextStopMusicV = true;
        }
    }
}
