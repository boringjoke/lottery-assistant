package com.hotchpotch.lottery.crawler.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadGateway;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.CrawlerProperties;
import com.hotchpotch.lottery.crawler.record.CrawlerDrawResponse;
import com.hotchpotch.lottery.crawler.record.CrawlerHistoryPageResponse;
import java.math.BigDecimal;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SportteryCrawlerClientTest {

    @Test
    void healthReturnsTrueWhenCrawlerRespondsOk() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SportteryCrawlerClient client = new SportteryCrawlerClient(builder, crawlerProperties("http://crawler.test"));

        server.expect(once(), requestTo(URI.create("http://crawler.test/health")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(client.isHealthy()).isTrue();
        server.verify();
    }

    @Test
    void fetchLatestDrawParsesCrawlerResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SportteryCrawlerClient client = new SportteryCrawlerClient(builder, crawlerProperties("http://crawler.test"));

        server.expect(once(), requestTo(URI.create("http://crawler.test/api/crawler/dlt/latest")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "draw": {
                            "lotteryType": "DLT",
                            "issueNo": "26076",
                            "drawDate": "2026-07-11",
                            "frontNumbers": [1, 2, 3, 4, 5],
                            "backNumbers": [6, 7],
                            "poolBalance": "1000000.00",
                            "salesAmount": "500000.00",
                            "source": "https://www.sporttery.cn/",
                            "pdfUrl": null,
                            "prizeTiers": [
                              {
                                "name": "一等奖",
                                "stakeCount": 2,
                                "stakeAmount": "10000.00",
                                "totalPrizeAmount": "20000.00",
                                "sort": 1,
                                "group": "1"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        CrawlerDrawResponse response = client.fetchLatestDraw();

        assertThat(response.draw().lotteryType()).isEqualTo("DLT");
        assertThat(response.draw().issueNo()).isEqualTo("26076");
        assertThat(response.draw().frontNumbers()).containsExactly(1, 2, 3, 4, 5);
        assertThat(response.draw().poolBalance()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.draw().prizeTiers()).hasSize(1);
        assertThat(response.draw().prizeTiers().get(0).stakeAmount()).isEqualByComparingTo("10000.00");
        server.verify();
    }

    @Test
    void fetchHistoryPageSendsPageQueryAndParsesDraws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SportteryCrawlerClient client = new SportteryCrawlerClient(builder, crawlerProperties("http://crawler.test"));

        server.expect(once(), requestTo(URI.create("http://crawler.test/api/crawler/dlt/history-page?pageNo=2&pageSize=10")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "pageNo": 2,
                          "pageSize": 10,
                          "pages": 20,
                          "total": 200,
                          "draws": [
                            {
                              "lotteryType": "DLT",
                              "issueNo": "26075",
                              "drawDate": "2026-07-09",
                              "frontNumbers": [1, 2, 3, 4, 5],
                              "backNumbers": [6, 7],
                              "poolBalance": null,
                              "salesAmount": null,
                              "source": "https://www.sporttery.cn/",
                              "pdfUrl": null,
                              "prizeTiers": []
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        CrawlerHistoryPageResponse response = client.fetchHistoryPage(2, 10);

        assertThat(response.pageNo()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.draws()).hasSize(1);
        assertThat(response.draws().get(0).issueNo()).isEqualTo("26075");
        server.verify();
    }

    @Test
    void fetchLatestDrawConvertsUpstreamErrorToBusinessException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SportteryCrawlerClient client = new SportteryCrawlerClient(builder, crawlerProperties("http://crawler.test"));

        server.expect(once(), requestTo(URI.create("http://crawler.test/api/crawler/dlt/latest")))
                .andRespond(withBadGateway().body("""
                        {
                          "code": "UPSTREAM_FETCH_FAILED",
                          "message": "timeout"
                        }
                        """).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::fetchLatestDraw)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.errorCode()).isEqualTo(ErrorCode.UPSTREAM_SERVICE_ERROR);
                    assertThat(ex.getMessage()).contains("timeout");
                });
        server.verify();
    }

    private CrawlerProperties crawlerProperties(String baseUrl) {
        CrawlerProperties properties = new CrawlerProperties();
        properties.setBaseUrl(baseUrl);
        properties.setConnectTimeoutMillis(1000);
        properties.setReadTimeoutMillis(2000);
        return properties;
    }
}
