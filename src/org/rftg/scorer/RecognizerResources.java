package org.rftg.scorer;

import android.content.Context;

/**
 * @author gc
 */
class RecognizerResources {

    final Context resourceContext;
    final Executor executor;
//    CardPatterns cardPatterns;
    final CardInfo cardInfo;
//    UserControls userControls;
//    final ScreenProperties screenProperties;

    /*
    private volatile boolean loaded;
    private volatile boolean loadingStarted;
    private int startCount;
    */

    RecognizerResources(Context resourceContext, Executor executor, CardInfo cardInfo) {
        this.cardInfo = cardInfo;
        this.resourceContext = resourceContext;
        this.executor = executor;
    }

    /*
    private void startLoading() {
        this.userControls = new UserControls(this);
        this.cardPatterns = new CardPatterns(this);

        startCount = executor.getWaitSize();
        if (startCount == 0) {
            loaded = true;
        }
    }

    public void release() {
        this.executor.shutdown();
        this.cardPatterns.release();
        this.userControls.release();
    }

    public boolean isLoaded() {
        if (!loaded) {
            synchronized (this) {
                if (!loadingStarted) {
                    startLoading();
                    loadingStarted = true;
                }
            }
        }
        return loaded;
    }
*/

    /**
     * @return value between 0 and 100 if in loading phase, or -1 if resources are ready
     */
    public int getLoadingPercent() {
    /*
        int waitSize = this.executor.getWaitSize();
        if (startCount == 0 || waitSize == 0 ) {
            loaded = true;
            return 100;
        } else {
            return (startCount - waitSize) * 100 / startCount;
        }
        */
        return -1;
    }

}
