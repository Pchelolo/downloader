package pchelolo.test;

import junit.framework.Assert;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pchelolo.downloader.DownloadManager;
import pchelolo.downloader.DownloadRequest;
import pchelolo.downloader.DownloadResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;


/**
 * Integration tests for the lib.
 * Some of the tests check multithreaded correctness, so might not be reliable.
 */
public class IntegrationTest {

    private static final String RESPONSE_CONTENT;
    private static final int RESPONSE_WAIT_TIME = 5;
    private static final int SERVER_PORT = 12345;
    private Server server;

    static {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            builder.append("Test!");
        }
        RESPONSE_CONTENT = builder.toString();
    }

    @Before
    public void startJetty() {
        server = new Server(SERVER_PORT);
        server.setHandler(new TestHandler());
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start jetty server for testing.");
            throw new RuntimeException(e);
        }
    }

    @After
    public void stopJetty() throws Exception {
        server.stop();
    }

    /*
        Checks if the simple download functionality is working
     */
    @Test
    public void simpleDownloadTest() throws Exception {
        DownloadManager manager = DownloadManager.Factory.createDefaultDownloadManager();
        DownloadRequest request = new DownloadRequest.Builder("http://localhost:" + SERVER_PORT).build();
        DownloadResponse result = manager.download(request);
        Assert.assertNotNull("DownloadResult is null", result);
        byte[] resultBytes = result.getResult(RESPONSE_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertNotNull("Downloaded byte[] is null", resultBytes);
        Assert.assertEquals("Download error:", RESPONSE_CONTENT, new String(resultBytes));
    }

    @Test
    public void pauseResumeDownloadTest() throws Exception {
        DownloadManager manager = DownloadManager.Factory.createDefaultDownloadManager();
        DownloadRequest request = new DownloadRequest.Builder("http://localhost:" + SERVER_PORT).build();
        DownloadResponse result = manager.download(request);
        Assert.assertNotNull("DownloadResult is null", result);

        int waitCount = 0;
        while (result.getStatus() == DownloadResponse.Status.NOT_STARTED) {
            Thread.sleep(10);
            waitCount++;
            if (waitCount > 20) {
                throw new RuntimeException("Failed. Test does no start");
            }
        }
        result.pause();
        Thread.sleep(100);
        result.resume();
        byte[] resultBytes = result.getResult(RESPONSE_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertNotNull("Downloaded byte[] is null", resultBytes);
        Assert.assertEquals("Download error:", RESPONSE_CONTENT, new String(resultBytes));
    }

    @Test
    public void pauseResumeWigglingTest() throws Exception {
        DownloadManager manager = DownloadManager.Factory.createDefaultDownloadManager();
        DownloadRequest request = new DownloadRequest.Builder("http://localhost:" + SERVER_PORT).build();
        DownloadResponse result = manager.download(request);
        Assert.assertNotNull("DownloadResult is null", result);

        while (result.getStatus() != DownloadResponse.Status.FINISHED) {
            try {
                result.pause();
            } catch (IllegalStateException e) {
                //IGNORE... already finished probably
            }
            try {
                result.resume();
            } catch (IllegalStateException e) {
                //IGNORE... already finished probably
            }
        }
        byte[] resultBytes = result.getResult(RESPONSE_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertNotNull("Downloaded byte[] is null", resultBytes);
        Assert.assertEquals("Download error:", RESPONSE_CONTENT, new String(resultBytes));
    }

    private static class TestHandler extends AbstractHandler {

        @Override
        public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            try {
                //Make a little delay before answering to let us test a pause/resume functionality
                byte[] bytesToServe = RESPONSE_CONTENT.getBytes("UTF-8");
                int start = 0;
                String rangeHeader = httpServletRequest.getHeader("Range");
                if (rangeHeader != null) {
                    start = Integer.parseInt(rangeHeader.substring(6, rangeHeader.length() - 1));
                }
                try (OutputStream outputStream = httpServletResponse.getOutputStream()) {
                    httpServletResponse.setContentType("text/html;charset=utf-8");
                    httpServletResponse.setHeader("Accept-Ranges", "bytes");
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                    while (start < bytesToServe.length) {
                        outputStream.write(bytesToServe, start, Math.min(bytesToServe.length - start, 50));
                        start += 50;
                        Thread.sleep(20);
                        outputStream.flush();
                    }
                }
                request.setHandled(true);
            } catch (InterruptedException e) {
                //IGNORE
            }
        }
    }

}
