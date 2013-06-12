package pchelolo.downloader.impl;

import pchelolo.downloader.DownloadRequest;
import pchelolo.downloader.DownloadResponse;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadResponseImpl implements DownloadResponse {

    private final Lock LOCK = new ReentrantLock();
    private final Condition STATE_CHANGED = LOCK.newCondition();

    private final DownloadManagerImpl manager;
    private final DownloadRequest request;


    private ByteArrayOutputStream downloadResult;
    private Status status = Status.NOT_STARTED;
    private boolean isThreadReleased = false;

    DownloadResponseImpl(DownloadRequest request, DownloadManagerImpl manager) {
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
            if (this.getStatus() == DownloadResponseImpl.Status.PAUSED) {
                if (supportsRangedDownload) {
                    // Can free the thread and close a connection,
                    // the download will proceed using a ranged download
                    isThreadReleased = true;
                    return true;
                } else {
                    while (this.getStatus() == DownloadResponseImpl.Status.PAUSED) {
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

    @Override
    public Status getStatus() {
        LOCK.lock();
        try {
            return status;
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public byte[] getResult()
            throws InterruptedException {
        return getResult(0, null);
    }

    @Override
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

    @Override
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

    @Override
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
            this.setStatus(DownloadResponseImpl.Status.IN_PROGRESS);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
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
