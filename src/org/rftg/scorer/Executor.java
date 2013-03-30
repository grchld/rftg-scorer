package org.rftg.scorer;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gc
 */
public class Executor {

    private final ExecutorService executorService;

    private final List<Future> addedFutures = new ArrayList<Future>();
    private final List<Future> synchronizingFutures = new ArrayList<Future>();
    private final List<Future> tmpFutures = new ArrayList<Future>();

    private AtomicInteger waitSize = new AtomicInteger(0);

    public Executor() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(availableProcessors);
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
                    } finally {
                        waitSize.decrementAndGet();
                    }
                }
            }
        }
    }

    public void checkWait() {
        synchronized (addedFutures) {
            if (addedFutures.isEmpty()) {
                return;
            }
            tmpFutures.clear();
            tmpFutures.addAll(addedFutures);
            addedFutures.clear();
            for (Future future : tmpFutures) {
                if (future.isDone()) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        Log.e("rftg", e.getMessage(), e);
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Log.e("rftg", e.getMessage(), e);
                        throw new RuntimeException(e);
                    } finally {
                        waitSize.decrementAndGet();
                    }
                } else {
                    addedFutures.add(future);
                }
            }
        }
    }

    public int getWaitSize() {
        checkWait();
        return waitSize.get();
    }

    public void submit(Runnable task) {
        Future future = this.executorService.submit(task);
        synchronized (addedFutures) {
            addedFutures.add(future);
            waitSize.incrementAndGet();
        }
    }

    public void submit(Callable<?> task) {
        Future future = this.executorService.submit(task);
        synchronized (addedFutures) {
            addedFutures.add(future);
            waitSize.incrementAndGet();
        }
    }

    public void shutdown() {
        this.executorService.shutdown();
    }
}
