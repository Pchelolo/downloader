package pchelolo.downloader;

import pchelolo.downloader.request.DownloadRequest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {

    private final ExecutorService controller = Executors.newCachedThreadPool();

    public DownloadResponse download(DownloadRequest request) {
        return new DownloadResponse(controller.submit(new DownloadTask(request)));
    }

    private class DownloadTask implements Callable<byte[]> {

        private final DownloadRequest request;

        private DownloadTask(DownloadRequest request) {
            this.request = request;
        }

        @Override
        public byte[] call() throws Exception {
            return new byte[1];
        }
    }
}
