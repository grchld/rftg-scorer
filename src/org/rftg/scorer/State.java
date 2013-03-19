package org.rftg.scorer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.*;

/**
 * @author gc
 */
class State {

    private final static String STATE = "state";
    Settings settings = new Settings();
    Player player = new Player();

    static State loadState(SharedPreferences preferences, String key, CardInfo cardInfo) {
        String stateString = preferences.getString(key, null);
        if (stateString != null) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode(stateString, Base64.NO_CLOSE | Base64.NO_WRAP | Base64.NO_PADDING)));
                try {
                    State state = new State();
                    state.load(ois, cardInfo);
                    return state;
                } finally {
                    ois.close();
                }
            } catch (Exception e) {
                Log.e("rftg", "Can't load state", e);
            }
        }
        return null;
    }

    static State loadState(Activity activity, CardInfo cardInfo) {
        return State.loadState(activity.getPreferences(Context.MODE_PRIVATE), STATE, cardInfo);
    }

    void load(ObjectInputStream ois, CardInfo cardInfo) throws IOException, ClassNotFoundException {
        settings.load(ois, cardInfo);
        player.load(ois, cardInfo);
    }

    void save(ObjectOutputStream oos) throws IOException {
        settings.save(oos);
        player.save(oos);
    }

    void saveState(SharedPreferences preferences, String key) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bytes);
            this.save(oos);
            oos.close();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, Base64.encodeToString(bytes.toByteArray(), Base64.NO_CLOSE | Base64.NO_WRAP | Base64.NO_PADDING));
            editor.commit();
        } catch (Exception e) {
            Log.e("rftg", "Can't save state", e);
        }
    }

    void saveState(Activity activity) {
        saveState(activity.getPreferences(Context.MODE_PRIVATE), STATE);
    }
}
