package pchelolo.downloader;

import sun.awt.Mutex;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadResponse {
    public static enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        PAUSED,
        FAILED,
        CANCELLED,
        FINISHED
    }

    private final Object stateLock = new Object();

    private final DownloadManager manager;
    private final DownloadRequest request;

    private ByteArrayOutputStream downloadResult;
    private Status status;

    DownloadResponse(DownloadRequest request, DownloadManager manager) {
        this.status = Status.NOT_STARTED;
        this.request = request;
        this.manager = manager;
    }

    public Status getStatus() {
        return status;
    }

    void setStatus(Status status) {
        synchronized (stateLock) {
            this.status = status;
            stateLock.notifyAll();
        }
    }

    public byte[] getResult(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        synchronized (stateLock) {
            while (true) {
                switch (this.status) {
                    case FINISHED:
                        return downloadResult.toByteArray();
                    case FAILED:
                    case CANCELLED:
                        return null;
                    default:
                        timeUnit.timedWait(stateLock, timeout);
                }
            }
        }
    }

    /*
        Requests an executor to pause the current download.
     */
    public void pause() {
        if (this.getStatus() != Status.IN_PROGRESS
                || this.getStatus() != Status.NOT_STARTED) {
            throw new IllegalStateException("Only not started or progressing downloads can be paused");
        }
        this.setStatus(Status.PAUSED);
    }

    public void resume() {
        if (this.getStatus() != Status.PAUSED &&
                this.getStatus() != Status.CANCELLED) {
            throw new IllegalStateException("Only paused or cancelled download could be resumed");
        }

        this.manager.resumeDownload(request, this);
    }

    public void cancel() {
        this.setStatus(Status.CANCELLED);
    }

    ByteArrayOutputStream getStream() {
        return downloadResult;
    }

    void setStream(ByteArrayOutputStream stream) {
        this.downloadResult = stream;
    }
}
