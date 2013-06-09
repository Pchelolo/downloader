package pchelolo.downloader;

import junit.framework.Assert;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pchelolo.downloader.request.DownloadRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IntegrationTest {

    private static final String RESPONSE_CONTENT = "<h1>Hello World</h1>";
    private static final int RESPONSE_WAIT_TIME = 5;
    private Server server;

    @Before
    public void startJetty() {
        server = new Server(8080);
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

    @Test
    public void simpleDownloadTest() throws Exception {
        DownloadManager manager = new DownloadManager();
        DownloadRequest request = DownloadRequest.Builder.createHttpDownloadRequest("http://127.0.0.1:8080");
        DownloadResponse result = manager.download(request);
        Assert.assertNotNull("DownloadResult is null", result);
        byte[] resultBytes;
        try {
            resultBytes = result.getResult(RESPONSE_WAIT_TIME, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Waiting for a task to finished failed.");
        }
        Assert.assertNotNull("Downloaded byte[] is null", resultBytes);
        String resultStr = new String(resultBytes);
        Assert.assertEquals("Download error:", RESPONSE_CONTENT, resultStr);
    }

    private static class TestHandler extends AbstractHandler {

        @Override
        public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            httpServletResponse.setContentType("text/html;charset=utf-8");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            request.setHandled(true);
            httpServletResponse.getWriter().println(RESPONSE_CONTENT);
        }
    }

}
