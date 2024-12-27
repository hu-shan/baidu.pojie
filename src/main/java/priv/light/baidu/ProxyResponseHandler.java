package priv.light.baidu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.hc.client5.http.classic.methods.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ProxyResponseHandler implements HttpClientResponseHandler<HttpHost> {

    private static final Logger logger = LogManager.getLogger(ProxyResponseHandler.class);
    private static final String SUCCESS = "success";
    private static final String IP_KEY = "IP";
    private static final String PORT_KEY = "Port";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CloseableHttpClient proxyClient;
    private StringBuilder addWhiteList;
    private String clearWhiteList;
    private String ip;

    @Override
    public HttpHost handleResponse(CloseableHttpResponse response) throws IOException, HttpException {
        if (Objects.isNull(this.proxyClient) || Objects.isNull(this.addWhiteList) || Objects.isNull(this.clearWhiteList) || Objects.isNull(this.ip)) {
            throw new HttpException("HttpProxyClient or WhiteList or Ip is null.");
        }

        String bodyText = entity2String(response);
        try {
            JsonNode ipAndPort = OBJECT_MAPPER.readTree(bodyText);
            JsonNode data = ipAndPort.path("data").path(0);
            String ip = data.path(IP_KEY).asText();
            int port = data.path(PORT_KEY).asInt();
            if (ip.isEmpty()) {
                boolean whiteListCleared = false;
                boolean addedToWhiteList = false;
                while (!whiteListCleared) {
                    try (CloseableHttpResponse ipResponse = proxyClient.execute(HttpClients.createDefault().newHttpGet(this.ip))) {
                        if (ipResponse.getCode()!= 200) {
                            return null;
                        }
                        String localIp = entity2String(ipResponse);
                        try (CloseableHttpResponse clearResponse = proxyClient.execute(HttpClients.createDefault().newHttpGet(this.clearWhiteList))) {
                            bodyText = entity2String(clearResponse);
                            logger.info("Clearing white list. Response: {}", bodyText);
                            whiteListCleared = SUCCESS.equals(bodyText);
                        } catch (IOException e) {
                            logger.error("Error clearing white list", e);
                            break;
                        }
                        if (whiteListCleared) {
                            try (CloseableHttpResponse addResponse = proxyClient.execute(HttpClients.createDefault().newHttpGet(this.addWhiteList.toString()))) {
                                bodyText = entity2String(addResponse);
                                logger.info("Adding to white list. Response: {}", bodyText);
                                addedToWhiteList = SUCCESS.equals(bodyText);
                            } catch (IOException e) {
                                logger.error("Error adding to white list", e);
                                break;
                            }
                        }
                        if (!addedToWhiteList) {
                            return null;
                        }
                    } catch (IOException e) {
                        logger.error("Error executing IP request", e);
                        return null;
                    }
                }
                return new HttpHost(ip, port);
            }
        } catch (IOException e) {
            logger.error("An error occurred while handling the response", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse response body", e);
            throw new IOException("Failed to parse response body", e);
        }
        return null;
    }


    private String entity2String(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String bodyText = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        EntityUtils.consume(entity);
        return bodyText;
    }
}