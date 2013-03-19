package org.rftg.scorer;

import android.content.Context;

/**
 * @author gc
 */
class RecognizerResources {

    final Context resourceContext;
    final Executor executor;
    final CardPatterns cardPatterns;
    final CustomNativeTools customNativeTools;
    final int maxCardNum;
    final CardInfo cardInfo;
    final UserControls userControls;
    final ScreenProperties screenProperties;

    RecognizerResources(Context resourceContext, CardInfo cardInfo, Settings settings, ScreenProperties screenProperties) {
        this.screenProperties = screenProperties;
        this.cardInfo = cardInfo;
        this.maxCardNum = settings.gameType.maxCardNum;
        this.resourceContext = resourceContext;
        this.executor = new Executor();
        this.customNativeTools = new CustomNativeTools();
        this.cardPatterns = new CardPatterns(this);
        this.userControls = new UserControls(this);
    }

    public void release() {
        this.executor.shutdown();
        this.cardPatterns.release();
        this.userControls.release();
    }

}
