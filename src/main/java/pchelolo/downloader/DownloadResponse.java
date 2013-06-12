package pchelolo.downloader;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The class representing a DownloadResponse. Used to get downloaded bytes and to control the download process.
 */
public class DownloadResponse {

    private final Lock LOCK = new ReentrantLock();
    private final Condition STATE_CHANGED = LOCK.newCondition();

    private final DownloadManager manager;
    private final DownloadRequest request;


    private ByteArrayOutputStream downloadResult;
    private Status status = Status.NOT_STARTED;
    private boolean isThreadReleased = false;

    DownloadResponse(DownloadRequest request, DownloadManager manager) {
        this.request = request;
        this.manager = manager;
    }

    ByteArrayOutputStream getStream() {
        return downloadResult;
    }

    void setStream(ByteArrayOutputStream stream) {
        this.downloadResult = stream;
    }

    void setStatus(Status status) {
        LOCK.lock();
        try {
            this.status = status;
            STATE_CHANGED.signalAll();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Checks if the download is paused.
     * <p/>
     * If paused, the method either waits for resume or returns true if the downloading thread should be released
     *
     * @throws InterruptedException
     */
    boolean checkPaused(boolean supportsRangedDownload) throws InterruptedException {
        LOCK.lock();
        try {
            if (this.getStatus() == DownloadResponse.Status.PAUSED) {
                if (supportsRangedDownload) {
                    // Can free the thread and close a connection,
                    // the download will proceed using a ranged download
                    isThreadReleased = true;
                    return true;
                } else {
                    while (this.getStatus() == DownloadResponse.Status.PAUSED) {
                        STATE_CHANGED.await();
                    }
                }
            }
            return false;
        } finally {
            LOCK.unlock();
        }
    }


    // ------------ PUBLIC API --------------- //

    /**
     * Represents a status of the download process
     */
    public static enum Status {

        /**
         * The download is scheduled but not started yet
         */
        NOT_STARTED,

        /**
         * The download is progressing
         */
        IN_PROGRESS,

        /**
         * The download was paused
         */
        PAUSED,

        /**
         * The download is failed for some reason
         */
        FAILED,

        /**
         * The download was cancelled, already downloaded bytes are deleted
         */
        CANCELLED,

        /**
         * The download is finished, the result is ready
         */
        FINISHED
    }

    /**
     * Returns the {@link Status} of the current download process
     */
    public Status getStatus() {
        LOCK.lock();
        try {
            return status;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Wait for a result to be ready and return it
     *
     * @return a download result if it was finished
     *         {@code null} if the download was cancelled or failed
     * @throws InterruptedException if hte Thread was interrupted during wait
     */
    public byte[] getResult()
            throws InterruptedException {
        return getResult(0, null);
    }

    /**
     * Timed wait for the result to be ready
     * <p/>
     * See: {@link pchelolo.downloader.DownloadResponse#getResult()}
     *
     * @return a download result if it was finished
     *         all byte, downloaded before the failure if the download fails
     *         {@code null} if the download was cancelled or a timeout passed
     * @throws InterruptedException
     */
    public byte[] getResult(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        LOCK.lock();
        try {
            while (true) {
                switch (this.status) {
                    case FINISHED:
                    case FAILED:
                        return downloadResult.toByteArray();
                    case CANCELLED:
                        return null;
                    default:
                        if (timeout == 0 && timeUnit == null) {
                            STATE_CHANGED.await();
                        } else {
                            if (!STATE_CHANGED.await(timeout, timeUnit)) {
                                return null;
                            }
                        }
                }
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Requests a downloader to pause the current download.
     * <p/>
     * If the underlying protocol supports ranged downloads, the thread will be freed and a connection closed
     * Otherwise, the downloading thread will wait to continue the download without disconnecting
     *
     * @throws IllegalStateException if the download is not in {@link Status#NOT_STARTED} or {@link Status#IN_PROGRESS}
     */
    public void pause() {
        LOCK.lock();
        try {
            if (this.getStatus() != Status.IN_PROGRESS
                    && this.getStatus() != Status.NOT_STARTED) {
                throw new IllegalStateException("Only not started or progressing downloads can be paused");
            }
            setStatus(Status.PAUSED);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Requests a downloader to resume the current download process
     *
     * @throws IllegalStateException if the download process is not in {@link Status#PAUSED}
     */
    public void resume() {
        LOCK.lock();
        try {
            if (this.getStatus() != Status.PAUSED) {
                throw new IllegalStateException("Only paused download could be resumed");
            }
            if (isThreadReleased) {
                isThreadReleased = false;
                manager.resumeDownload(request, this);
            }
            // If we did not yet release a thread - short circuit and let it not release
            this.setStatus(DownloadResponse.Status.IN_PROGRESS);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Requests a downloader to cancel the current download process.
     * <p/>
     * It deletes the already downloaded bytes.
     *
     * @throws IllegalStateException if the download process is in {@link Status#FINISHED} or {@link Status#FAILED}
     */
    public void cancel() {
        LOCK.lock();
        try {
            if (this.getStatus() == Status.FAILED || this.getStatus() == Status.FINISHED) {
                throw new IllegalStateException("Could not cancel finished or failed download");
            }
            this.setStatus(Status.CANCELLED);
        } finally {
            LOCK.unlock();
        }
    }

}
