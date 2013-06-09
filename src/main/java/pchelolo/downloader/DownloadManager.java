package pchelolo.downloader;

import pchelolo.downloader.HttpDownloadTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadManager {
    private final ExecutorService controller = Executors.newCachedThreadPool();

    public DownloadResponse download(DownloadRequest request) {
        DownloadResponse response = new DownloadResponse(request, this);
        resumeDownload(request, response);
        return response;
    }

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
}
