package pchelolo.downloader.impl;

import pchelolo.downloader.DownloadManager;
import pchelolo.downloader.DownloadRequest;

/**
 * An extension of the download manager by the internal APIs
 */
public interface DownloadManagerImpl extends DownloadManager {

    /**
     * Resumes the download if it requires recreation of the downloading thread and connection
     */
    void resumeDownload(DownloadRequest request, DownloadResponseImpl response);
}
