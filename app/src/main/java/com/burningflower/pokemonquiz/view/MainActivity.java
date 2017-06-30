// Activity for the Main Menu of the game

package com.burningflower.pokemonquiz.view;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.burningflower.pokemonquiz.model.MusicManager;
import com.burningflower.pokemonquiz.model.Question;
import com.burningflower.pokemonquiz.R;
import com.burningflower.pokemonquiz.model.SocketHandler;
import com.burningflower.pokemonquiz.model.TitleChangeServiceManager;
import com.burningflower.pokemonquiz.widgets.AutoResizeTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private Context context = this;

    private Socket socket;

    private String playerID, playerName;
    private int score, position, totalPlayers, playerTitle;
    private float rankingPercentage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonEvents ();

        socket = SocketHandler.getSocket(context);
    }

    // Ask the socket for the player's data
    private void updatePlayerData () {
        socket.emit("get-player-data");
        socket.on("player-data", onPlayerDataReceived);
    }

    // Method called when the server sends the data of the player via Socket.io
    private Emitter.Listener onPlayerDataReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("MainActivity", "onPlayerDataReceived");
            socket.off("player-data", onPlayerDataReceived);

            JSONObject data = (JSONObject) args[0];
            try {
                playerID = data.getString("_id");
                playerName = data.getString("username");
                playerTitle = data.getInt("title");
                // Remember the current title
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt ("playerTitle", playerTitle).apply();
                score = data.getInt("score");
                position = data.getJSONObject("ranking").getInt("position");
                totalPlayers = data.getJSONObject("ranking").getInt("total");
                rankingPercentage = (position*1.0f)/totalPlayers;
            } catch (JSONException e) {
                Log.d("StartActivity", "JSON", e);
                return;
            }

            Log.d("MainActivity", "Name: "+playerName);
            Log.d("MainActivity", "Title: "+playerTitle);

            setPlayerData();
        }
    };

    private void setButtonEvents () {
        (findViewById(R.id.buttonPlay)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    playGame();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Set the data of the player in the UI
    private void setPlayerData () {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoResizeTextView) findViewById(R.id.playerName)).setText(playerName);
                ((AutoResizeTextView) findViewById(R.id.playerScore)).setText(getString(R.string.score)+score);
                ((AutoResizeTextView) findViewById(R.id.playerTitle)).setText(getResources().getStringArray(R.array.titles)[playerTitle]);
            }
        });

        TitleChangeServiceManager.startService(context, playerID);
    }

    // On resume, play music and update player data
    @Override
    protected void onResume () {
        super.onResume();

        MusicManager mm = MusicManager.getInstance (this);
        if (!mm.isPlaying ()) {
            mm.playMusic();
        }

        updatePlayerData();
    }

    // On pause, pause the music
    @Override
    protected void onPause () {
        super.onPause();

        (MusicManager.getInstance (this)).pause ();
    }

    // Make a "Pokémon click" sound
    private void clickSound () {
        MediaPlayer click = MediaPlayer.create (this, R.raw.click);
        click.start ();

        click.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.stop ();
                mp.release();
            }
        });
    }

    // Tell the server to init a game
    public void playGame () throws IOException {
        Log.d("MainActivity", "playGame()");
        clickSound ();

        socket.emit("start-game");

        socket.on("game-started", onGameStarted);
    }

    // When the server is ready to start a game, go to the QuestionActivity activity to play the game
    private Emitter.Listener onGameStarted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            SocketHandler.getSocket(context).off("game-started", onGameStarted);
            Log.d("MainActivity", "onGameStarted()");
            Intent intent = new Intent(context, QuestionActivity.class);
            startActivity (intent);
        }
    };
}