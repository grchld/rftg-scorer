package org.rftg.scorer;

import android.content.Context;

/**
 * @author gc
 */
class MainContext {

    final Context resourceContext;
    final Executor executor = new Executor();
    final CardInfo cardInfo;
    final FastCameraView fastCamera;
    final UserInterfaceView userInterface;
    final State state;
    final CardPatterns cardPatterns;
    final Recognizer recognizer;


    MainContext(Context resourceContext, FastCameraView fastCamera, UserInterfaceView userInterface, CardInfo cardInfo, State state) {
        this.resourceContext = resourceContext;
        this.fastCamera = fastCamera;
        this.userInterface = userInterface;
        this.cardInfo = cardInfo;
        this.state = state;
        cardPatterns = new CardPatterns(this);
        recognizer = new Recognizer(this);
    }

}
