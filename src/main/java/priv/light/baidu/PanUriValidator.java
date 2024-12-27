package priv.light.baidu;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Light
 * @date 2022/3/31 12:06
 */

@Slf4j
@Data
public class PanUriValidator {

    private final HttpGet panGet;
    private final CloseableHttpClient httpClient;

    public PanUriValidator(HttpGet panGet) {
        this.panGet = panGet;
        this.httpClient = HttpClients.createDefault();
    }

    public boolean notFoundOrNoShareCheck() {
        try {
            CloseableHttpResponse response = httpClient.execute(panGet);
            if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
                log.warn("链接不存在或已被删除.");
                return true;
            } else if (response.getCode() == HttpStatus.SC_FORBIDDEN) {
                log.warn("链接已被取消分享.");
                return true;
            }
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            log.error("验证链接时发生错误:", e);
        }
        return false;
    }

    public void dispose() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("关闭 HttpClient 时发生错误:", e);
        }
    }
}