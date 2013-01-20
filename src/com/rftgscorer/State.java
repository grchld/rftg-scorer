package com.rftgscorer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gc
 */
class State implements Serializable {

    private final static String STATE = "state";

    Settings settings = new Settings();
    List<Player> players = new ArrayList<Player>();

    State() {
        Player player = new Player();
        player.name = "Player";
        players.add(player);
    }

    static State loadState(SharedPreferences preferences, String key) {
        String stateString = preferences.getString(key, null);
        if (stateString != null) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode(stateString, Base64.NO_CLOSE | Base64.NO_WRAP | Base64.NO_PADDING)));
                try {
                    return (State)ois.readObject();
                } finally {
                    ois.close();
                }
            } catch (Exception e) {
                Log.e("RftG", "Can't load state", e);
            }
        }
        return null;
    }

    static State loadState(Activity activity) {
        return State.loadState(activity.getPreferences(Context.MODE_PRIVATE), STATE);
    }

    void saveState(SharedPreferences preferences, String key) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bytes);
            oos.writeObject(this);
            oos.close();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, Base64.encodeToString(bytes.toByteArray(), Base64.NO_CLOSE | Base64.NO_WRAP | Base64.NO_PADDING));
            editor.commit();
        } catch (Exception e) {
            Log.e("RftG", "Can't save state", e);
        }
    }

    void saveState(Activity activity) {
        saveState(activity.getPreferences(Context.MODE_PRIVATE), STATE);
    }
}
