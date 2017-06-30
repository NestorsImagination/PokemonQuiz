// Activity that shows the results obtained in the finished game (it will be improved in the future)

package com.burningflower.pokemonquiz.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.burningflower.pokemonquiz.model.Question;
import com.burningflower.pokemonquiz.R;

import java.io.IOException;
import java.util.ArrayList;

public class FinishActivity extends AppCompatActivity {
    private MediaPlayer mpResult, mpSong;
    private int score, numQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);

        Intent intent = getIntent();

        score = intent.getIntExtra ("SCORE", 0);
        numQuestions = intent.getIntExtra ("NUM_QUESTIONS", 0);

        setFinishScreen ();
    }

    // Sets the GUI so that it shows the player the statistics of the finished game
    private void setFinishScreen () {
        final TextView scoreText = (TextView) findViewById(R.id.score);
        scoreText.setText (Integer.toString (score) + "/" + Integer.toString (numQuestions));

        final TextView scorePercentText = (TextView) findViewById(R.id.score_percent);
        int percent = (score * 100) / numQuestions;
        scorePercentText.setText (Integer.toString (percent)+"%");

        mpResult = MediaPlayer.create (this, R.raw.results);
        mpSong = MediaPlayer.create (this, R.raw.finish);

        // Animation of the GUI
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                scoreText.setVisibility (View.VISIBLE);
                mpResult.start ();

                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scorePercentText.setVisibility (View.VISIBLE);
                        mpResult.stop ();
                        try {
                            mpResult.prepare ();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mpResult.start ();

                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scoreText.setVisibility (View.VISIBLE);
                                mpSong.start ();

                                Handler h = new Handler();
                                h.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        (findViewById(R.id.button_menu)).setVisibility(View.VISIBLE);
                                    }
                                },2000);
                            }
                        },500);
                    }
                },1000);
            }
        },700);
    }

    // Go back to the Main Menu
    public void goToMainMenu (View v) {
        MediaPlayer click = MediaPlayer.create (this, R.raw.click);
        click.start ();

        click.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.stop ();
                mp.release();
            }
        });

        mpSong.stop ();
        mpSong.release ();
        mpResult.release ();
        finish ();
    }
}