// Activity where the games are played

package com.burningflower.pokemonquiz.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.burningflower.pokemonquiz.model.Question;
import com.burningflower.pokemonquiz.model.SocketHandler;
import com.burningflower.pokemonquiz.R;
import com.burningflower.pokemonquiz.widgets.AutoResizeButtonText;
import com.burningflower.pokemonquiz.widgets.AutoResizeTextView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class QuestionActivity extends AppCompatActivity {
    private AutoResizeTextView questionText;
    private AutoResizeButtonText [] answerButtons = new AutoResizeButtonText [4];   // The answer buttons
    private ImageView image;
    private ImageButton playButton;
    private TextView questionIndicator, timeIndicator;
    private Context context = this;

    Socket socket = SocketHandler.getSocket(context);

    private Question currentQuestion = null;
    private MediaPlayer sound;              // The sound to play if this is a sound question
    private int numAnimated = 0;
    private boolean firstQuestion = true;
    private LinearLayout layout;
    private ProgressBar progressBar;
    private int countdown, numQuestions, currentQuestionIndex = 0;

    private int currentSelectedAnswer;

    private boolean waitingForNextQuestion = true, readyForNextQuestion = true,
            nextQuestionLoaded = false, isSoundQuestion = false, finished = false;

    // Timer to update the countdown timer that shows the remaining time of the game
    private Timer countdownTimer;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        Log.d("QuestionActivity", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        getUI ();
        setButtonEvents ();

        sound = new MediaPlayer();

        socket.emit("get-first-question");
        socket.on("next-question", onNextQuestion);
        socket.on("time-out", onTimeOut);
    }

    private void getUI () {
        questionText = ((AutoResizeTextView) findViewById(R.id.Question));
        answerButtons [0] = (AutoResizeButtonText) findViewById(R.id.Answer1);
        answerButtons [1] = (AutoResizeButtonText) findViewById(R.id.Answer2);
        answerButtons [2] = (AutoResizeButtonText) findViewById(R.id.Answer3);
        answerButtons [3] = (AutoResizeButtonText) findViewById(R.id.Answer4);
        image = (ImageView) findViewById (R.id.questionImage);
        layout = (LinearLayout)findViewById(R.id.activity_question);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        playButton = (ImageButton) findViewById (R.id.ButtonPlaySound);
        questionIndicator = (TextView) findViewById(R.id.questionIndicator);
        timeIndicator = (TextView) findViewById(R.id.timeIndicator);
    }

    private void setButtonEvents () {
        for (int i = 0; i < 4; i++) {
            final int finalI = i;
            (answerButtons [i]).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    answer(finalI);
                }
            });
        }
    }

    // Show a question to the player to exit the game when the Back Button is pressed
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder (this);
        builder.setTitle(R.string.Text_GameExit);
        builder.setPositiveButton(R.string.Text_Exit, new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog, int id) {
                socket.emit("exit-game");
                socket.off("next-question", onNextQuestion);
                socket.off("time-out", onTimeOut);
                sound.release();
                finish ();

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.Text_KeepPlaying, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // When the countdown timer reaches 0 in the server, it emits a socket message to tell
    // the player that the game has finished, finishing the game automatically
    private Emitter.Listener onTimeOut = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 4; i++)
                        (answerButtons[i]).setClickable(false);
                    playButton.setClickable(false);

                    // Shows the time-out message with an animation
                    TextView timeOutText =  (TextView) findViewById(R.id.timeOutText);
                    final int layoutWidth = layout.getWidth();
                    slideView(timeOutText, layoutWidth, false, null);
                    timeOutText.setVisibility(View.VISIBLE);

                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 2 seconds later the game finishes and the activity changes to the Main Menu
                            socket.off("next-question", onNextQuestion);
                            socket.off("time-out", onTimeOut);
                            sound.release();
                            finish ();
                        }
                    }, 2000);
                }
            });
        }
    };

    // When the server sends a question, set the next question
    private Emitter.Listener onNextQuestion = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("QuestionActivity", "onNextQuestion()");

            // Obtain the question
            JSONObject data = (JSONObject) args[0];

            try {
                currentQuestion = new Question(data.getString("question"), data.getString("answer1"),
                        data.getString("answer2"), data.getString("answer3"), data.getString("answer4"),
                        data.getString("image"), data.getBoolean("specialImage"), data.getString("sound"));

                // If it is the first question, get the number of questions for the game and the maximum
                // time to complete it
                if (firstQuestion) {
                    numQuestions = data.getInt("numQuestions");
                    countdown = data.getInt("time");
                }
            } catch (JSONException e) {
                Log.d("QuestionActivity", "JSON", e);
                return;
            }

            Log.d("QuestionActivity", "onNextQuestion2()");

            nextQuestionLoaded = true;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);

                    // If there was a question before and there has been a short delay,
                    // set the question
                    if (readyForNextQuestion) {
                        // If it is the first question, set the time indicator and set the question
                        if (firstQuestion) {
                            firstQuestion = false;
                            timeIndicator.setText("Tiempo: "+countdown);

                            setQuestion();
                            layout.setVisibility(View.VISIBLE);

                            getUIBack();
                        } else {
                            // ...else, change the question with an animation
                            changeQuestion();
                        }
                    }
                }
            });
        }
    };

    // Increment the index of the current question
    private void incrementQuestionIndex () {
        questionIndicator.setText((currentQuestionIndex+1)+" / "+numQuestions);
        currentQuestionIndex++;
    }

    // Decrement the timer
    private void decrementTimer () {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (countdown != 0) {
                    countdown--;
                    timeIndicator.setText("Tiempo: " + countdown);
                }
            }
        });
    }

    // Change the question to the next with an animation
    private void changeQuestion () {
        numAnimated = 0;

        // Animation listener that sets the question and returns the UI back when this animation has finished
        final Animator.AnimatorListener slideButtonsOutListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                numAnimated++;
                if (numAnimated == 4) {
                    for (int i = 0; i < 4; i++)
                        answerButtons[i].setBackground(ContextCompat.getDrawable(context, R.drawable.answer_button));
                    setQuestion();
                    getUIBack();
                }
            }

            @Override
            public void onAnimationStart(Animator animator) {}
            @Override
            public void onAnimationCancel(Animator animator) {}
            @Override
            public void onAnimationRepeat(Animator animator) {}
        };

        // Animation to slide the buttons and other elements out the screen

        final int layoutWidth = layout.getWidth();
        final int layoutHeight = layout.getHeight();

        Handler h = new Handler();

        slideView(questionText, layoutWidth, true, null);

        int delay = 50;

        if (isSoundQuestion) {
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slideView(playButton, layoutWidth, true, null);
                }
            }, delay);

            delay+=50;
        }

        for (int i = 0; i < 4; i++) {
            final int final_i = i;
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slideView(answerButtons[final_i], layoutWidth, true, slideButtonsOutListener);
                }
            }, delay);

            delay += 50;
        }

        ObjectAnimator slideImageOutAnim = ObjectAnimator.ofFloat(image,
                "translationY", 0, -layoutHeight/2);
        slideImageOutAnim.setDuration(300);
        slideImageOutAnim.start();

        /*ObjectAnimator fadeOutImage = ObjectAnimator.ofFloat(image,
                "alpha", 1, 0);
        fadeOutImage.setDuration(300);
        fadeOutImage.start();*/
    }

    // General function to slide an element in/out the screen from/to the right
    private void slideView (View view, int layoutWidth, boolean out, Animator.AnimatorListener listener) {
        ObjectAnimator slideButtonAnim;

        if (out)
            slideButtonAnim = ObjectAnimator.ofFloat(view, "translationX", 0, layoutWidth);
        else
            slideButtonAnim = ObjectAnimator.ofFloat(view, "translationX", layoutWidth, 0);

        slideButtonAnim.setDuration(300);
        if (listener != null)
            slideButtonAnim.addListener(listener);
        slideButtonAnim.start();
    }

    // Returns the UI elements to their original positions
    private void getUIBack () {
        numAnimated = 0;

        // Listener that makes the game playable when all the UI elements are right in their positions
        final Animator.AnimatorListener slideButtonsInListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                numAnimated++;
                if (numAnimated == 4) {
                    waitingForNextQuestion = false;
                    // Makes all buttons clickable
                    for (int i = 0; i < 4; i++)
                        (answerButtons[i]).setClickable(true);

                    // If this is a sound question, makes the button to play the sound clickable and plays the sound
                    if (isSoundQuestion) {
                        playButton.setClickable(true);
                        playSound(playButton);
                    }

                    // If this is the first question, starts the timer shown in the UI
                    if (currentQuestionIndex == 1) {
                        countdownTimer = new Timer();
                        countdownTimer.scheduleAtFixedRate(new TimerTask()
                        {
                            public void run()
                            {
                                decrementTimer();
                            }
                        }, 200, 1000);
                    }
                }
            }

            @Override
            public void onAnimationStart(Animator animator) {}
            @Override
            public void onAnimationCancel(Animator animator) {}
            @Override
            public void onAnimationRepeat(Animator animator) {}
        };

        // Make an animation to slide in all the UI elements

        final int layoutWidth = layout.getWidth();
        final int layoutHeight = layout.getHeight();

        Handler h = new Handler();

        slideView(questionText, layoutWidth, false, null);

        int delay = 50;

        if (isSoundQuestion) {
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playButton.setVisibility (View.VISIBLE);
                    slideView(playButton, layoutWidth, false, null);
                }
            }, delay);

            delay+=50;
        }

        for (int i = 0; i < 4; i++) {
            final int final_i = i;
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slideView(answerButtons[final_i], layoutWidth, false, slideButtonsInListener);
                }
            }, delay);

            delay += 50;
        }

        ObjectAnimator slideImageInAnim = ObjectAnimator.ofFloat(image,
                "translationY", -layoutHeight/2, 0);
        slideImageInAnim.setDuration(300);
        slideImageInAnim.start();

        /*ObjectAnimator fadeInImage = ObjectAnimator.ofFloat(image,
                "alpha", 0, 1);
        fadeInImage.setDuration(200);
        fadeInImage.start();*/
    }

    // Sets the current question
    private void setQuestion () {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextQuestionLoaded = false;
                readyForNextQuestion = false;

                // Sets the question text in the UI
                Log.d("QuestionActivity", "setQuestion()");
                Log.d("QuestionActivity", "setQuestion() -> image:"+currentQuestion.getImage());
                Log.d("QuestionActivity", "setQuestion() -> audio:"+currentQuestion.getAudio());
                questionText.setText(currentQuestion.getQuestion());
                answerButtons[0].setText(currentQuestion.getAnswer1());
                answerButtons[1].setText(currentQuestion.getAnswer2());
                answerButtons[2].setText(currentQuestion.getAnswer3());
                answerButtons[3].setText(currentQuestion.getAnswer4());
                incrementQuestionIndex ();

                // If the questions is a sound question
                if (!currentQuestion.getAudio().equals("")) {
                    isSoundQuestion = true;
                    image.setImageResource(R.drawable.sound);

                    playButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            playSound(playButton);
                        }
                    });

                    // Gets the sound from the given URL
                    try {
                        sound.stop();
                        sound.reset();
                        sound.setDataSource(currentQuestion.getAudio());
                        sound.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    isSoundQuestion = false;
                    playButton.setVisibility (View.GONE);

                    // If the question has a generic image
                    if (!currentQuestion.isSpecialImage()) {
                        Log.d("QuestionActivity", "setQuestion() -> Special image");
                        // Set the image to one stored in the app drawable folder
                        image.setImageResource(getResources().getIdentifier("drawable/" + currentQuestion.getImage(), null, context.getPackageName()));
                    // If it is a special image, not stored in the application
                    } else {
                        Log.d("QuestionActivity", "setQuestion() -> Standard image");
                        // Get the image from the given URL
                        Picasso.with(context).load(currentQuestion.getImage()).into(image);
                    }

                    image.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // Plays the question's sound
    private void playSound (final View v) {
        v.setClickable (false);

        sound.start ();

        sound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if (!waitingForNextQuestion)
                    v.setClickable (true);

                sound.stop ();
                try {
                    sound.prepare ();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // The player presses a button (indicated by the buttonID parameter)
    private void answer (int buttonID) {
        // Stop the timer if it is the last question
        if (currentQuestionIndex == numQuestions)
            countdownTimer.cancel();

        waitingForNextQuestion = true;
        progressBar.setVisibility(View.VISIBLE);

        // Send the chosen answer index to the server and wait for the feedback
        currentSelectedAnswer = buttonID;
        socket.emit("answer", buttonID);
        socket.on("answer-result", onAnswerResponse);

        // Make all buttons unclickable
        playButton.setClickable(false);
        for (int i = 0; i < 4; i++)
            (answerButtons[i]).setClickable(false);
    }

    // Feedback from the server for the chosen answer
    private Emitter.Listener onAnswerResponse = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

            socket.off("answer-result", onAnswerResponse);

            Log.d("QuestionActivity", "onAnswerResponse()");
            JSONObject data = (JSONObject) args[0];

            int correctAnswer, gameScore = -1, gainedScore = -1, totalScore = -1;

            try {
                // Get the correct answer and check if the game has finished
                correctAnswer = data.getInt("correctAnswer");
                finished = data.getBoolean("finished");

                // If it's the last question, get the game results
                if (finished) {
                    gameScore = data.getInt("gameScore");
                    gainedScore = data.getInt("gainedScore");
                    totalScore = data.getInt("totalScore");
                }
            } catch (JSONException e) {
                Log.d("QuestionActivity", "JSON", e);
                return;
            }

            Log.d("QuestionActivity", "onAnswerResponse2()");

            if (!finished)
                questionResults(correctAnswer, true);
            else
                questionResults (correctAnswer, false, gameScore, gainedScore, totalScore);
        }
    };

    // Show the player the results for the chosen answer
    private void questionResults (final int correctAnswer, final boolean loadNextQuestion, final int... finishData) {
        final int correctAnswerF = correctAnswer;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MediaPlayer mp;
                /*Toast message;
                TextView toastMessage;*/

                // Wrong answer
                if (currentSelectedAnswer != correctAnswerF) {
                    // The chosen answer is tinted red and a "wrong sound" is played
                    answerButtons[currentSelectedAnswer].setBackground(ContextCompat.getDrawable(context, R.drawable.answer_button_wrong));

                    mp = MediaPlayer.create(context, R.raw.wrong_answer);
                    /*message = Toast.makeText(getApplicationContext(), R.string.Text_WrongAnswer,
                            Toast.LENGTH_SHORT);
                    toastMessage = (TextView) message.getView().findViewById(android.R.id.message);
                    toastMessage.setTextColor(Color.RED);*/

                // Correct answer
                } else {
                    // A "correct sound" is played
                    mp = MediaPlayer.create(context, R.raw.correct_answer);
                    /*message = Toast.makeText(getApplicationContext(), R.string.Text_CorrectAnswer,
                            Toast.LENGTH_SHORT);
                    toastMessage = (TextView) message.getView().findViewById(android.R.id.message);
                    toastMessage.setTextColor(Color.GREEN);*/
                }

                // Tint the correct answer green in any case
                answerButtons[correctAnswer].setBackground(ContextCompat.getDrawable(context, R.drawable.answer_button_correct));

                //message.show();
                mp.start();

                Handler h = new Handler();

                // Goes to the next question or finish the game in 2 seconds
                h.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        readyForNextQuestion = true;

                        if (loadNextQuestion) {
                            // If the next question has been received, change the question, wait until
                            // it has been received otherwise
                            if (nextQuestionLoaded)
                                changeQuestion();
                            else
                                progressBar.setVisibility(View.VISIBLE);
                        } else
                            goToFinishActivity(finishData[0]);
                    }

                }, 2000);
            }
        });
    }

    // Got to the activity that shows the results of the game, passing it the results
    private void goToFinishActivity (int score) {
        socket.off("next-question", onNextQuestion);
        socket.off("time-out", onTimeOut);
        sound.release();

        Intent intent = new Intent(context, FinishActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("NUM_QUESTIONS", numQuestions);
        startActivity(intent);
        this.finish();
    }
}