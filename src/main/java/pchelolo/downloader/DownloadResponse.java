package pchelolo.downloader;

import java.util.concurrent.TimeUnit;

/**
 * The class representing a DownloadResponse.
 * Used to get downloaded bytes and to control the download process.
 */
public interface DownloadResponse {

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
     * Returns the {@link DownloadResponse.Status} of the current download process
     */
    Status getStatus();

    /**
     * Wait for a result to be ready and return it
     *
     * @return a download result if it was finished
     *         {@code null} if the download was cancelled or failed
     * @throws InterruptedException if hte Thread was interrupted during wait
     */
    byte[] getResult()
            throws InterruptedException;

    /**
     * Timed wait for the result to be ready
     * <p/>
     * See: {@link DownloadResponse#getResult()}
     *
     * @return a download result if it was finished
     *         all byte, downloaded before the failure if the download fails
     *         {@code null} if the download was cancelled or a timeout passed
     * @throws InterruptedException
     */
    byte[] getResult(long timeout, TimeUnit timeUnit)
            throws InterruptedException;

    /**
     * Requests a downloader to pause the current download.
     * <p/>
     * If the underlying protocol supports ranged downloads, the thread will be freed and a connection closed
     * Otherwise, the downloading thread will wait to continue the download without disconnecting
     *
     * @throws IllegalStateException if the download is not in {@link DownloadResponse.Status#NOT_STARTED} or {@link DownloadResponse.Status#IN_PROGRESS}
     */
    void pause();

    /**
     * Requests a downloader to resume the current download process
     *
     * @throws IllegalStateException if the download process is not in {@link DownloadResponse.Status#PAUSED}
     */
    void resume();

    /**
     * Requests a downloader to cancel the current download process.
     * <p/>
     * It deletes the already downloaded bytes.
     *
     * @throws IllegalStateException if the download process is in {@link DownloadResponse.Status#FINISHED} or {@link DownloadResponse.Status#FAILED}
     */
    void cancel();
}
