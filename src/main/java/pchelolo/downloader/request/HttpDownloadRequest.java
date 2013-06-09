package pchelolo.downloader.request;

import java.net.URL;

class HttpDownloadRequest implements DownloadRequest {

    private final URL downloadURL;

    public HttpDownloadRequest(URL downloadURL) {
        this.downloadURL = downloadURL;
    }
}
