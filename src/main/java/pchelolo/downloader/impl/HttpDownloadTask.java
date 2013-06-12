package pchelolo.downloader.impl;

import pchelolo.downloader.DownloadRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

public class HttpDownloadTask extends DownloadTask {

    public HttpDownloadTask(DownloadRequest request, DownloadResponseImpl response) {
        super(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URLConnection prepareConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection)request.getUrl().openConnection();
        conn.setConnectTimeout(request.getConnectionWaitTime());
        conn.setInstanceFollowRedirects(request.isFollowsRedirects());
        return conn;
    }

    /**
     *  {@inheritDoc}
     *
     *  http-specific: disconnects from the server
     */
    @Override
    protected void finalizeConnection(URLConnection conn) {
        if (conn != null) {
            ((HttpURLConnection)conn).disconnect();
        }
    }

    /**
     *  {@inheritDoc}
     *
     *  http-specific: uses an Accept-Range header
     */
    @Override
    protected boolean supportsRangedDownload(URLConnection conn) {
        return "bytes".equals(conn.getHeaderField("Accept-Ranges"));
    }

    /**
     *  {@inheritDoc}
     *
     *  http-specific: uses a Range header
     */
    @Override
    protected void setContinueDownloadFrom(int byteNumber, URLConnection conn) {
        conn.setRequestProperty("Range", "bytes=" + byteNumber + "-");
    }


}