// First activity that shows the first screen and connects to the server

package com.burningflower.pokemonquiz.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.burningflower.pokemonquiz.model.MusicManager;
import com.burningflower.pokemonquiz.R;
import com.burningflower.pokemonquiz.model.SocketHandler;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class StartActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {
    private Context context = this;
    private GoogleApiClient mGoogleApiClient;
    private Socket socket;

    private static final int RC_SIGN_IN = 49404;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Starts playing music
        (MusicManager.getInstance (this)).playMusic ();

        // Gets a socket connecting to the server
        socket = SocketHandler.getSocket(context);

        // Authenticates via Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.googleAuthToken))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signIn();
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    // Google authentication resolved
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("StartActivity", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully
            GoogleSignInAccount acct = result.getSignInAccount();
            String idToken = acct.getIdToken();

            // Requests the server to sign in using the obtained token from Google
            socket.emit("sign-in", idToken);
            socket.on("sign-in-result", onServerSiginInResult);
        } else {
            Toast.makeText(getApplicationContext(), R.string.problemGoogleAuthentication,
                    Toast.LENGTH_LONG).show();
        }
    }

    // Server authentication resolved
    private Emitter.Listener onServerSiginInResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("StartActivity", "onServerSiginInResult()");
            JSONObject data = (JSONObject) args[0];

            String result = null;
            String error = null;

            try {
                result = data.getString("result");
                error = null;
                if (data.has("error"))
                    error = data.getString("error");
            } catch (JSONException e) {
                Log.d("StartActivity", "JSON", e);
                return;
            }

            final String resultF = result, errorF = error;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Successfully connected
                    if (resultF.equals("connected")) {
                        goToMainMenu();
                    // First time connecting with the given email, asks the user for a nick
                    } else if (resultF.equals("choose-username")) {
                        showUsernameInput();
                    // An error occurred during sign in
                    } else if (resultF.equals("error")) {
                        Toast.makeText(getApplicationContext(), errorF, Toast.LENGTH_LONG).show();
                        showUsernameInput();
                    // New player successfully created
                    } else if (resultF.equals("player-created")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.trainerCreated);
                        builder.setPositiveButton(R.string.enterGame, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                goToMainMenu();
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    }
                }
            });
        }
    };

    // Asks the player for a nick
    private void showUsernameInput(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.welcome);
        builder.setMessage(R.string.insertTrainerName);

        // Input filter to enable only the allowed characters in the username
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String allowedCharacterSet = getString(R.string.allowedCharactersInName);
                if (source != null && source.length() != 0) {
                    if (!allowedCharacterSet.contains(("" + source.charAt(source.length() - 1))))
                        return "";
                    else
                        return null;
                }
                return null;
            }
        };

        // Set up the input
        final EditText input = new EditText(context);
        input.setFilters(new InputFilter[] {
                filter,
                new InputFilter.LengthFilter(16)
        });

        // Sets a new view to ask the player for a nickname
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = input.getText().toString();
                socket.emit("username-chosen", username);
                dialog.cancel();
            }
        });

        builder.setCancelable(false);

        builder.show();
    }

    // Authentication successful, moving to the MainActivity
    private void goToMainMenu () {
        socket.off("sign-in-result", onServerSiginInResult);
        Intent intent = new Intent(context, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("StartActivity", "onConnectionFailed:" + connectionResult);
    }
}
