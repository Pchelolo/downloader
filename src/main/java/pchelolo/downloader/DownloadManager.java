package pchelolo.downloader;

import pchelolo.downloader.impl.DefaultDownloadManager;

/**
 * Represents an executor used to run downloads.
 *
 * Instances could be created using a {@link Factory}
 */
public interface DownloadManager extends AutoCloseable {
    /**
     * Start a download process for the specified request.
     * @param request a request for the download
     * @throws UnsupportedOperationException if the protocol is not supported
     * @return an instance of the {@link DownloadResponse} which represents the ongoing download process
     */
    DownloadResponse download(DownloadRequest request);

    public static class Factory {
        /**
         * Creates a default download manager
         */
        public static DownloadManager createDefaultDownloadManager() {
            return new DefaultDownloadManager();
        }
    }
}
