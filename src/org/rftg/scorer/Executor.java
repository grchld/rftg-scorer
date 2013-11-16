package org.rftg.scorer;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author gc
 */
class Executor {

    private ExecutorService executorService;
    private boolean running;
    private final Object stateLock = new Object();

/*
    private final List<Future> addedFutures = new ArrayList<Future>();
    private final List<Future> synchronizingFutures = new ArrayList<Future>();
    private final List<Future> tmpFutures = new ArrayList<Future>();

    private AtomicInteger waitSize = new AtomicInteger(0);
*/

    void start() {
        synchronized (stateLock) {
            if (!running) {
                int availableProcessors = Runtime.getRuntime().availableProcessors();
                this.executorService = Executors.newFixedThreadPool(availableProcessors);
                running = true;
            }
        }
    }

    void stop() {
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
                                      */
    Future submit(Runnable task) {
        synchronized (stateLock) {
            return running ? this.executorService.submit(task) : null;
        }
    }

    <T> Future<T> submit(Callable<T> task) {
        synchronized (stateLock) {
            return running ? this.executorService.submit(task) : null;
        }
    }

    @SuppressWarnings("unchecked")
    CheckedTask submitChecked(Runnable task) {
        synchronized (stateLock) {
            Future future = submit(task);
            return future == null ? null : new CheckedTask(this.executorService, future);
        }
    }

    <T> CheckedTask<T> submitChecked(Callable<T> task) {
        synchronized (stateLock) {
            Future<T> future = submit(task);
            return future == null ? null : new CheckedTask<T>(this.executorService, future);
        }
    }

    static class CheckedTask<T> {

        final ExecutorService executorService;
        final Future<T> future;

        CheckedTask(ExecutorService executorService, Future<T> future) {
            this.executorService = executorService;
            this.future = future;
        }

        /**
         * @return true if this task either is done, or is still working, or is scheduled
         * false if this task was interrupted or if its executorService is already terminated
         */
        boolean isTrusty() {
            if (future.isDone()) {
                try {
                    future.get();
                    return true;
                } catch (InterruptedException e) {
                    return false;
                } catch (ExecutionException e) {
                    return !(e.getCause() instanceof InterruptedException);
                }
            } else {
                return !executorService.isShutdown();
            }
        }
    }

}
