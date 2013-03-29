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

    private volatile boolean loaded;
    private final int startCount;

    RecognizerResources(Context resourceContext, CardInfo cardInfo, Settings settings, ScreenProperties screenProperties) {
        this.screenProperties = screenProperties;
        this.cardInfo = cardInfo;
        this.maxCardNum = settings.gameType.maxCardNum;
        this.resourceContext = resourceContext;
        this.executor = new Executor();
        this.customNativeTools = new CustomNativeTools();
        this.userControls = new UserControls(this);
        this.cardPatterns = new CardPatterns(this);

        startCount = executor.getWaitSize();
        if (startCount == 0) {
            loaded = true;
        }

        // Loading is not completed!!! Check isLoaded
    }

    public void release() {
        this.executor.shutdown();
        this.cardPatterns.release();
        this.userControls.release();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getLoadingPercent() {
        int waitSize = this.executor.getWaitSize();
        if (startCount == 0 || waitSize == 0 ) {
            loaded = true;
            return 100;
        } else {
            return (startCount - waitSize) * 100 / startCount;
        }
    }


}
