package pchelolo.downloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * A runnable representing a download loop
 * Immutable.
 */
abstract class DownloadTask implements Runnable {

    private static final int TMP_BUF_SIZE = 512;
    private static final int INITIAL_OUTPUT_SIZE = 16384;

    final DownloadRequest request;
    private final DownloadResponse response;

    DownloadTask(DownloadRequest request, DownloadResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * The main loop of the download process
     */
    @Override
    public void run() {
        URLConnection conn = null;
        try {
            conn = prepareConnection();
            prepareStream(conn);

            response.setStatus(DownloadResponse.Status.IN_PROGRESS);

            try (InputStream inputStream = conn.getInputStream()) {
                byte[] tmpBuf = new byte[TMP_BUF_SIZE];

                while (!Thread.currentThread().isInterrupted()) {
                    int len = inputStream.read(tmpBuf);
                    if (len == -1) {
                        response.setStatus(DownloadResponse.Status.FINISHED);
                        break;
                    }
                    response.getStream().write(tmpBuf, 0, len);

                    if (response.checkPaused(supportsRangedDownload(conn))) {
                        // Should release the current thread and connection on pause
                        return;
                    }

                    if (response.getStatus() == DownloadResponse.Status.CANCELLED) {
                        //Clean up already downloaded memory
                        response.setStream(null);
                        return;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            response.setStatus(DownloadResponse.Status.FAILED);
        } finally {
            finalizeConnection(conn);
        }
    }

    /**
     * Prepares an output stream to start or resume a download and sets in to the
     * {@link DownloadResponse}.
     * Checks and saves if the server and a protocol supports resuming downloads.
     *
     * @throws RuntimeException if the {@link DownloadResponse} already has some output,
     *                          but the server does not support resuming connections
     */
    private void prepareStream(URLConnection conn) {
        if (response.getStream() == null) {
            // Starting a new download task
            // Initialize the stream
            int contentLength = conn.getContentLength();
            //If the contentLength is not set - take some initial size
            if (contentLength == -1) {
                contentLength = INITIAL_OUTPUT_SIZE;
            }
            response.setStream(new ByteArrayOutputStream(contentLength));
        } else {
            // Set to continue the download process from the specific point.
            // Should only get here if it is supported.
            setContinueDownloadFrom(response.getStream().size(), conn);
        }
    }

    // --------------- Abstract protocol-specific methods ------------------- //

    /**
     * Prepares a connection using properties from the DownloadRequest
     *
     * @return an instance of a URLConnection
     * @throws IOException if failed to connect
     */
    protected abstract URLConnection prepareConnection() throws IOException;

    /**
     * Closes the connection
     */
    protected abstract void finalizeConnection(URLConnection conn);

    /**
     * Checks if the server and a protocol supports resuming the download from a specific place
     *
     */
    protected abstract boolean supportsRangedDownload(URLConnection conn);

    /**
     * Sets to resume the download from the specific place
     *
     * @param byteNumber where to start download form. Inclusively
     */
    protected abstract void setContinueDownloadFrom(int byteNumber, URLConnection conn);
}
