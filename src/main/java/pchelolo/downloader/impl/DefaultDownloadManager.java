package pchelolo.downloader.impl;

import pchelolo.downloader.DownloadRequest;
import pchelolo.downloader.DownloadResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of the download manager.
 * Uses a cachedThreadPool as an executor
 */
public class DefaultDownloadManager implements DownloadManagerImpl {

    private final ExecutorService controller = Executors.newCachedThreadPool();

    @Override
    public void resumeDownload(DownloadRequest request, DownloadResponseImpl response) {
        controller.execute(createDownloadTask(request, response));
    }

    private Runnable createDownloadTask(DownloadRequest request, DownloadResponseImpl response) {
        String protocol = request.getUrl().getProtocol().toLowerCase();
        switch (protocol) {
            case "http":
                return new HttpDownloadTask(request, response);
            default:
                throw new UnsupportedOperationException("Protocol " + protocol + " is not supported");
        }
    }

    // ------------- PUBLIC API ---------- //

    @Override
    public DownloadResponse download(DownloadRequest request) {
        DownloadResponseImpl response = new DownloadResponseImpl(request, this);
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
