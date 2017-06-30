// Service that checks periodically if the trainer title has changed, sending a notification in that case

package com.burningflower.pokemonquiz.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.burningflower.pokemonquiz.R;
import com.burningflower.pokemonquiz.view.MainActivity;
import com.burningflower.pokemonquiz.view.StartActivity;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by User on 19/06/2017.
 */

public class TitleChangeService extends GcmTaskService {
    public static final String TAG = "TITLE_CHANGE_SERVICE";
    public static final String PLAYER_ID = "CALL";
    public static final String SERVER_URL = "SERVER_URL";
    public static String serverURL;
    public static final String extraPath = "/getTitle?playerID=";

    private RequestQueue mRequestQueue = null;
    private String playerID = null;

    @Override
    public int onRunTask(TaskParams taskParams) {
        if (mRequestQueue == null)
            initRequestQueue();

        playerID = taskParams.getExtras().getString(PLAYER_ID);
        serverURL = taskParams.getExtras().getString(SERVER_URL);

        Log.i("TitleChangeService", "onRunTask; id: "+playerID+", url: "+serverURL);

        checkTitleChange();

        return GcmNetworkManager.RESULT_SUCCESS;
    }

    // Needed to do the call to the API
    private void initRequestQueue () {
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();
    }

    // Check if the title has changed
    private void checkTitleChange() {
        Log.i("TitleChangeService", "checkTitleChange(); id: "+playerID+", url: "+serverURL+extraPath+playerID);
        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, serverURL+extraPath+playerID,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Gets the new title
                        int newTitle = Integer.parseInt(response);
                        // Gets the old title
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        int oldTitle = sharedPref.getInt ("playerTitle", -1);

                        // If the old title was initialized
                        if (oldTitle != -1) {
                            Log.i("TitleChangeService", "Response: " + newTitle + " (" + oldTitle + ")");

                            // If the new title is different than before
                            if (newTitle != oldTitle) {
                                // Send the notification
                                sendTitleChangeNotification(newTitle, oldTitle);
                                Log.i("TitleChangeService", "Título cambiado de " +
                                        getResources().getStringArray(R.array.titles)[oldTitle] +
                                        " a " + getResources().getStringArray(R.array.titles)[newTitle]);

                                // Remember the new title
                                sharedPref.edit().putInt("playerTitle", newTitle).apply();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("TitleChangeService", error.toString());
                    }
                });

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    // Notify the user that his title has changed
    private void sendTitleChangeNotification (int newTitle, int oldTitle) {
        String message;

        message = getString(R.string.titleChangeInit)+((oldTitle < newTitle)?getString(R.string.titleUp):getString(R.string.titleDown))+getString(R.string.titleChangeFrom)+
                getResources().getStringArray(R.array.titles)[oldTitle].toUpperCase()+getString(R.string.titleChangeTo)+
                getResources().getStringArray(R.array.titles)[newTitle].toUpperCase();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.pokeball)
                        .setContentTitle(getString(R.string.titleChange))
                        .setContentText(message);

        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);

        Intent resultIntent = new Intent(this, StartActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
