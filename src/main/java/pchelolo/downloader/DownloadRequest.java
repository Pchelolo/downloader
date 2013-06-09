package pchelolo.downloader;

import java.net.MalformedURLException;
import java.net.URL;

public class DownloadRequest {

    private final URL url;
    private final int connectionWaitTime;
    private final boolean followsRedirects;

    private DownloadRequest(URL url,
                            int connectionWaitTime,
                            boolean followsRedirects) {
        this.url = url;
        this.connectionWaitTime = connectionWaitTime;
        this.followsRedirects = followsRedirects;
    }

    public URL getUrl() {
        return url;
    }

    public int getConnectionWaitTime() {
        return connectionWaitTime;
    }

    public boolean isFollowsRedirects() {
        return followsRedirects;
    }

    public static class Builder {

        private URL url;
        private int connectionWaitTime = 5000;
        private boolean followsRedirects = false;

        public Builder(URL url) {
            this.url = url;
        }

        public Builder(String url) throws MalformedURLException {
            this.url = new URL(url);
        }

        public Builder setConnectionWaitTime(int time) {
            this.connectionWaitTime = time;
            return this;
        }

        public Builder setURL(URL url) {
            this.url = url;
            return this;
        }

        public Builder setURL(String url) throws MalformedURLException {
            this.url = new URL(url);
            return this;
        }

        public Builder followsRedirects(boolean followsRedirects) {
            this.followsRedirects = followsRedirects;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(url, connectionWaitTime, followsRedirects);
        }
    }
}
