package dev.openfeature.contrib.providers.flagd.resolver.process.storage.connector.sync;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Fetches content from a given HTTP endpoint using caching headers to optimize network usage.
 * If cached ETag or Last-Modified values are available, they are included in the request headers
 * to potentially receive a 304 Not Modified response, reducing data transfer.
 * Updates the cached ETag and Last-Modified values upon receiving a 200 OK response.
 * It does not store the cached response, assuming not needed after first successful fetching.
 *
 * @param httpClient the HTTP client used to send the request
 * @param httpRequestBuilder the builder for constructing the HTTP request
 * @return the HTTP response received from the server
 */
@Slf4j
public class HttpCacheFetcher {
    private static String cachedETag = null;
    private static String cachedLastModified = null;

    @SneakyThrows
    public HttpResponse<String> fetchContent(HttpClient httpClient, HttpRequest.Builder httpRequestBuilder) {
        if (cachedETag != null) {
            httpRequestBuilder.header("If-None-Match", cachedETag);
        }
        if (cachedLastModified != null) {
            httpRequestBuilder.header("If-Modified-Since", cachedLastModified);
        }

        HttpRequest request = httpRequestBuilder.build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() == 200) {
            cachedETag = httpResponse.headers().firstValue("ETag").orElse(null);
            cachedLastModified = httpResponse.headers().firstValue("Last-Modified").orElse(null);
            log.debug("fetched new content");
        } else if (httpResponse.statusCode() == 304) {
            log.debug("got 304 Not Modified");
        }
        return httpResponse;
    }
}
