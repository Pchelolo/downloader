package pchelolo.downloader;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The immutable class representing a download request and options.
 *
 * The only required parameter is a URL to download, all the rest are optional
 * Instances are built using a mutable stateful builder, which lets to cache the
 * specified options to build multiple similar requests.
 *
 * Example the DownloadRequest construction: </br>
 * <pre>
 * {@code
 *  DownloadRequest.Builder builder
 *          = new DownloadRequest.Builder(""http://test2.test.com"")
 *                               .setConnectionTime(1000);
 *  DownloadRequest request1 = builder.build();
 *  DownloadRequest request2 = builder.setURL("http://test2.test.com").build();
 * }
 *
 * Some of the options could be silently ignored if they are not supported by the
 * underlying protocol
 * </pre>
 *
 */
public class DownloadRequest {

    private static final int     DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final boolean DEFAULT_FOLLOWS_REDIRECT   = false;

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

    /**
     * @return URL to download
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Returns the timeout to wait for the connection.
     * The default value is {@value #DEFAULT_CONNECTION_TIMEOUT}
     * @return connection timeout
     */
    public int getConnectionWaitTime() {
        return connectionWaitTime;
    }

    /**
     * Specifies if the download process follows redirects
     * The default value is {@value #DEFAULT_FOLLOWS_REDIRECT}
     *
     * The option is silently ignored if the underlying protocol
     * does not support redirects
     *
     *
     * @return if download process follows redirects
     */
    public boolean isFollowsRedirects() {
        return followsRedirects;
    }

    @Override
    public String toString() {
        return url.toString() + "[waitTime: " + connectionWaitTime + " redirects: " + followsRedirects + "]";
    }

    public static class Builder {

        private URL url;
        private int connectionWaitTime   = DEFAULT_CONNECTION_TIMEOUT;
        private boolean followsRedirects = DEFAULT_FOLLOWS_REDIRECT;

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

        public Builder setFollowsRedirects(boolean followsRedirects) {
            this.followsRedirects = followsRedirects;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(url, connectionWaitTime, followsRedirects);
        }
    }
}
