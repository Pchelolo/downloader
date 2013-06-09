package pchelolo.downloader.request;

import java.net.MalformedURLException;
import java.net.URL;

public interface DownloadRequest {

    public static class Builder {

        private Builder() {
            //Avoid instantiation
        }

        public static DownloadRequest createHttpDownloadRequest(URL downloadURL) {
            return new HttpDownloadRequest(downloadURL);
        }

        public static DownloadRequest createHttpDownloadRequest(String downloadURL)
                throws MalformedURLException {
            return new HttpDownloadRequest(new URL(downloadURL));
        }
    }
}
