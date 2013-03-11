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
    final int maxCardNum = Card.GameType.EXP1.maxCardNum;

    RecognizerResources(Context resourceContext) {
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
