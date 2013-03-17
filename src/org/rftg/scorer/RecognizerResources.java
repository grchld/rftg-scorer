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

    RecognizerResources(Context resourceContext, CardInfo cardInfo, Settings settings) {
        this.cardInfo = cardInfo;
        this.maxCardNum = settings.gameType.maxCardNum;
        this.resourceContext = resourceContext;
        this.executor = new Executor();
        this.customNativeTools = new CustomNativeTools();
        cardPatterns = new CardPatterns(this);
    }

    public void release() {
        this.executor.shutdown();
        this.cardPatterns.release();
    }

}
