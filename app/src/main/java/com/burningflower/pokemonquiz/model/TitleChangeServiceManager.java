// Class that starts the TitleChangeService service

package com.burningflower.pokemonquiz.model;

import android.content.Context;
import android.os.Bundle;

import com.burningflower.pokemonquiz.R;
import com.burningflower.pokemonquiz.services.TitleChangeService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

/**
 * Created by User on 19/06/2017.
 */

public class TitleChangeServiceManager {
    private static GcmNetworkManager gcmNetworkManager = null;
    private static String PLAYER_ID = null;

    // Start the service
    public static boolean startService (Context context, String playerID) {
        // If the service is not started yet
        if (gcmNetworkManager == null || !PLAYER_ID.equals(playerID)) {
            gcmNetworkManager = GcmNetworkManager.getInstance(context);
            PLAYER_ID = playerID;

            // Create a bundle with the ID of the player and the url to the server
            Bundle bundle = new Bundle();
            bundle.putString(TitleChangeService.PLAYER_ID, playerID);
            bundle.putString(TitleChangeService.SERVER_URL, context.getString(R.string.serverURL));

            // Start a periodic task, executing it every ~30 seconds
            Task task = new PeriodicTask.Builder()
                    .setService(TitleChangeService.class)
                    .setPeriod(30)
                    .setFlex(10)
                    .setTag(TitleChangeService.TAG)
                    .setExtras(bundle)
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .build();

            gcmNetworkManager.cancelAllTasks(TitleChangeService.class);
            gcmNetworkManager.schedule(task);

            return true;
        } else
            return false;
    }
}
