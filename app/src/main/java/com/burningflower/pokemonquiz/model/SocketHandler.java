// Class that manages a Socket.io socket to communicate with the game server

package com.burningflower.pokemonquiz.model;

import android.content.Context;
import android.util.Log;

import com.burningflower.pokemonquiz.R;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by User on 11/05/2017.
 */

public class SocketHandler {
    private static Socket socket = null;

    public static Socket getSocket (Context context) {
        Log.d("SocketHandler", "getSocket()");

        // If the socket is not created yet
        if (socket == null) {
            try {
                // Create a socket and connect to the server
                socket = IO.socket(context.getString(R.string.serverURL));
                Log.d("SocketHandler", "getSocket() -> socket.connect");
                socket.connect();
                socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d("SocketHandler", "Socket disconnected");
                        socket.connect();
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        /*socket.on(Socket.EVENT_CONNECT,onConnect);
        socket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);*/

        return socket;
    }
}
