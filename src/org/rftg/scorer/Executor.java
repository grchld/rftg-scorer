package org.rftg.scorer;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author gc
 */
public class Executor {

    private ExecutorService executorService;
    private boolean running;
    private final Object stateLock = new Object();

/*
    private final List<Future> addedFutures = new ArrayList<Future>();
    private final List<Future> synchronizingFutures = new ArrayList<Future>();
    private final List<Future> tmpFutures = new ArrayList<Future>();

    private AtomicInteger waitSize = new AtomicInteger(0);
*/

    public void start() {
        synchronized (stateLock) {
            if (!running) {
                int availableProcessors = Runtime.getRuntime().availableProcessors();
                this.executorService = Executors.newFixedThreadPool(availableProcessors);
                running = true;
            }
        }
    }

    public void stop() {
        synchronized (stateLock) {
            if (running) {
                this.executorService.shutdown();
                boolean terminated = false;
                try {
                    terminated = this.executorService.awaitTermination(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Rftg.w("Unexpected interrupt request");
                }
                if (!terminated) {
                    this.executorService.shutdownNow();
                }
                running = false;
            }
        }
    }

/*
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
    */
}
