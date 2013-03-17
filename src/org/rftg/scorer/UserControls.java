package org.rftg.scorer;

import android.view.MotionEvent;
import android.view.View;

/**
 * @author gc
 */
class UserControls {

    UserControls(RecognizerResources recognizerResources) {
    }

    void release() {

    }

    boolean onTouch(View view, MotionEvent motionEvent, State state) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (x < 200 && y < 200) {
                state.player.chips = 0;
                state.player.cards.clear();
            }

            return true;
        } else {
            return false;
        }
    }
}
