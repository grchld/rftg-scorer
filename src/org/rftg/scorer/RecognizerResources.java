package org.rftg.scorer;

import android.content.Context;

/**
 * @author gc
 */
class RecognizerResources {

    final MainContext mainContext;

//    UserControls userControls;
//    final ScreenProperties screenProperties;

    /*
    private volatile boolean loaded;
    private volatile boolean loadingStarted;
    private int startCount;
    */

    RecognizerResources(MainContext mainContext) {
        this.mainContext = mainContext;
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
