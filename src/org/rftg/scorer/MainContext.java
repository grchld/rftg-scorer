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
    final CardPatterns cardPatterns = new CardPatterns(this);
//    final RecognizerResources recognizerResources = new RecognizerResources(this);
    final Recognizer recognizer = new Recognizer(this);


    MainContext(Context resourceContext, FastCameraView fastCamera, UserInterfaceView userInterface, CardInfo cardInfo, State state) {
        this.resourceContext = resourceContext;
        this.fastCamera = fastCamera;
        this.userInterface = userInterface;
        this.cardInfo = cardInfo;
        this.state = state;
    }

}
