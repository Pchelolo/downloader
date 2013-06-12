package pchelolo.downloader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents an executor used to run downloads.
 */
public class DownloadManager implements AutoCloseable {

    private final ExecutorService controller = Executors.newCachedThreadPool();

    void resumeDownload(DownloadRequest request, DownloadResponse response) {
        controller.execute(createDownloadTask(request, response));
    }

    private Runnable createDownloadTask(DownloadRequest request, DownloadResponse response) {
        String protocol = request.getUrl().getProtocol().toLowerCase();
        switch (protocol) {
            case "http":
                return new HttpDownloadTask(request, response);
            default:
                throw new UnsupportedOperationException("Protocol " + protocol + " is not supported");
        }
    }

    // ------------- PUBLIC API ---------- //

    /**
     * Start a download process for the specified request.
     * @param request a request for the download
     * @throws UnsupportedOperationException if the protocol is not supported
     * @return an instance of the {@link DownloadResponse} which represents the ongoing download process
     */
    public DownloadResponse download(DownloadRequest request) {
        DownloadResponse response = new DownloadResponse(request, this);
        resumeDownload(request, response);
        return response;
    }

    /**
     * Shuts down a thread pool used to run download tasks
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        controller.shutdown();
    }
}
