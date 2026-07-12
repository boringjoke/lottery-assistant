package com.hotchpotch.lottery.crawler.client;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.CrawlerProperties;
import com.hotchpotch.lottery.crawler.record.CrawlerDrawResponse;
import com.hotchpotch.lottery.crawler.record.CrawlerHealthResponse;
import com.hotchpotch.lottery.crawler.record.CrawlerHistoryPageResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 中国体彩网 crawler HTTP 客户端。
 */
@Component
public class SportteryCrawlerClient {

    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final RestClient restClient;

    public SportteryCrawlerClient(
            @Qualifier("crawlerRestClientBuilder") RestClient.Builder restClientBuilder,
            CrawlerProperties crawlerProperties) {
        this.restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(crawlerProperties.baseUrl()))
                .build();
    }

    /**
     * 调用 crawler 健康检查接口。
     */
    public boolean isHealthy() {
        try {
            CrawlerHealthResponse response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(CrawlerHealthResponse.class);

            return response != null && "ok".equals(response.status());
        } catch (RestClientException ex) {
            return false;
        }
    }

    /**
     * 抓取最新一期大乐透开奖。
     */
    public CrawlerDrawResponse fetchLatestDraw() {
        try {
            return restClient.get()
                    .uri("/api/crawler/dlt/latest")
                    .retrieve()
                    .body(CrawlerDrawResponse.class);
        } catch (RestClientResponseException ex) {
            throw upstreamException(ex);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler 服务调用失败: " + ex.getMessage());
        }
    }

    /**
     * 抓取一页大乐透历史开奖。
     */
    public CrawlerHistoryPageResponse fetchHistoryPage(int pageNo, int pageSize) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/crawler/dlt/history-page")
                            .queryParam("pageNo", pageNo)
                            .queryParam("pageSize", pageSize)
                            .build())
                    .retrieve()
                    .body(CrawlerHistoryPageResponse.class);
        } catch (RestClientResponseException ex) {
            throw upstreamException(ex);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler 服务调用失败: " + ex.getMessage());
        }
    }

    private BusinessException upstreamException(RestClientResponseException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String message = extractErrorMessage(ex.getResponseBodyAsString());

        return new BusinessException(
                ErrorCode.UPSTREAM_SERVICE_ERROR,
                "crawler 服务异常(" + statusCode.value() + "): " + message);
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return ErrorCode.UPSTREAM_SERVICE_ERROR.defaultMessage();
        }

        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return responseBody;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:8001";
        }

        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

}
