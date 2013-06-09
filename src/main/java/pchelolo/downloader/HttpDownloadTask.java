package pchelolo.downloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HttpDownloadTask implements Runnable {

    private static final int TMP_BUF_SIZE = 512;
    private static final int INITIAL_OUTPUT_SIZE = 16384;

    private final DownloadRequest request;
    private final DownloadResponse response;

    public HttpDownloadTask(DownloadRequest request, DownloadResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void run() {
        HttpURLConnection conn = null;
        try {
            try {
                conn = (HttpURLConnection) request.getUrl().openConnection();
                conn.setConnectTimeout(request.getConnectionWaitTime());
                conn.setInstanceFollowRedirects(request.isFollowsRedirects());
            } catch (IOException e) {
                response.setStatus(DownloadResponse.Status.FAILED);
                return;
            }

            ByteArrayOutputStream output = response.getStream();
            if (output == null) {
                // Starting a new download task, create a stream
                int contentLength = conn.getContentLength();
                //If the contentLength is not set - take some initial size
                if (contentLength == -1) {
                    contentLength = INITIAL_OUTPUT_SIZE;
                }
                output = new ByteArrayOutputStream(contentLength);
                response.setStream(output);
            } else {
                // Resuming a download, do not need already downloaded bytes
                conn.setRequestProperty("Range", "bytes=" + output.size() + "-");
            }

            response.setStatus(DownloadResponse.Status.IN_PROGRESS);

            try (InputStream inputStream = conn.getInputStream()) {

                byte[] tmpBuf = new byte[TMP_BUF_SIZE];
                while (!Thread.currentThread().isInterrupted()) {
                    int len = inputStream.read(tmpBuf);
                    if (len == -1) {
                        response.setStatus(DownloadResponse.Status.FINISHED);
                        break;
                    }
                    output.write(tmpBuf, 0, len);

                    if (response.getStatus() == DownloadResponse.Status.PAUSED) {
                        return;
                    }

                    if (response.getStatus() == DownloadResponse.Status.CANCELLED) {
                        //Clean up already downloaded memory
                        response.setStream(null);
                        return;
                    }
                }

            } catch (IOException e) {
                response.setStatus(DownloadResponse.Status.FAILED);
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}