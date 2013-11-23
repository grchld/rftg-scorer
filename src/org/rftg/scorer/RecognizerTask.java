package org.rftg.scorer;

/**
* @author gc
*/
abstract class RecognizerTask implements Runnable {

    abstract void execute() throws Exception;

    @Override
    public final void run() {
        try {
            execute();
        } catch (RuntimeException e) {
            Rftg.e(e);
            throw e;
        } catch (Error e) {
            Rftg.e(e);
            throw e;
        } catch (Throwable t) {
            Rftg.e(t);
            throw new RuntimeException(t);
        }
    }
}
