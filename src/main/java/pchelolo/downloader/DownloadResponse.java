package pchelolo.downloader;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DownloadResponse {

    private final Future<byte[]> future;

    DownloadResponse(Future<byte[]> future) {
        this.future = future;
    }

    public byte[] getResult()
            throws ExecutionException, InterruptedException {
        return future.get();
    }

    public byte[] getResult(long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, timeUnit);
    }
}
