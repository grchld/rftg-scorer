package org.rftg.scorer;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author gc
 */
public class Executor {

    private final ExecutorService executorService;

    private final List<Future> addedFutures = new ArrayList<Future>();
    private final List<Future> synchronizingFutures = new ArrayList<Future>();

    public Executor() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void sync() {
        synchronized (synchronizingFutures) {
            while (true) {
                synchronized (addedFutures) {
                    if (addedFutures.isEmpty()) {
                        return;
                    }
                    synchronizingFutures.addAll(addedFutures);
                    addedFutures.clear();
                }
                for (Future future : synchronizingFutures) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        Log.e("rftg", e.getMessage(), e);
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Log.e("rftg", e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void submit(Runnable task) {
        Future future = this.executorService.submit(task);
        synchronized (addedFutures) {
            addedFutures.add(future);
        }
    }

    public void submit(Callable<?> task) {
        Future future = this.executorService.submit(task);
        synchronized (addedFutures) {
            addedFutures.add(future);
        }
    }

    public void shutdown() {
        this.executorService.shutdown();
    }
}
