package priv.light.baidu;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author Light
 * @date 2022/3/30 21:37
 */

@Slf4j
@Data
public class RetryStrategyExecutor implements HttpClientResponseHandler<Object> {

    private final HttpUtil httpUtil;
    private final RetryStrategy retryStrategy;

    public RetryStrategyExecutor(HttpUtil httpUtil, RetryStrategy retryStrategy) {
        this.httpUtil = httpUtil;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public Object handleResponse(ClassicHttpResponse response) throws IOException {
        int retryCount = 0;
        while (retryCount < 5) {
            try {
                return httpUtil.entity2String(response);
            } catch (IOException e) {
                if (!retryStrategy.retryRequest(response, retryCount, null)) {
                    throw e;
                }
                retryCount++;
                log.warn("请求失败，正在进行第 {} 次重试...", retryCount);
            }
        }
        throw new IOException("请求失败，超过重试次数");
    }
}