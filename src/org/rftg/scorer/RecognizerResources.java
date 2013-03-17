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

    RecognizerResources(Context resourceContext, int maxCardNum) {
        this.maxCardNum = maxCardNum;
        this.resourceContext = resourceContext;
        this.executor = new Executor();
        this.customNativeTools = new CustomNativeTools();
        cardPatterns = new CardPatterns(this);
        cardInfo = new CardInfo(resourceContext.getAssets());
    }

    public void release() {
        this.executor.shutdown();
        this.cardPatterns.release();
    }

}
