package org.faboo.example.twitter.sse.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * Resolver that follows http redirects to the end and report the final url.
 */
public class RedirectResolver {

    private final static Logger log = LoggerFactory.getLogger(RedirectResolver.class);

    private final CloseableHttpClient httpClient;
    private final RequestConfig requestConfig;
    private final static HostResolver hostResolver = new HostResolver();

    public RedirectResolver(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setRedirectsEnabled(false)
                .build();
    }

    public ResolveResult resolve(String url) {

        log.debug("resolving {}", url);

        try {
            int hopsRemaining = 10;
            String nextUrl = url;
            do {
                ResolveResult resolveStatus = resolveOnce(nextUrl);
                if (resolveStatus.isResolved()) {

                    return resolveStatus;
                }
                hopsRemaining--;
                nextUrl = resolveStatus.url;
            } while (hopsRemaining > 0);
        } catch (ResolveException e) {
            return ResolveResult.error(url, e.getError());
        }
        return ResolveResult.error(url, new ResolveError(-2, "to many redirects"));
    }


    private ResolveResult resolveOnce(String urlString) throws ResolveException {

        HttpHead head = new HttpHead(urlString);
        head.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(head)) {

            int status = response.getStatusLine().getStatusCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return ResolveResult.resolved(urlString);
            }

            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {

                return ResolveResult.moved(response.getLastHeader("Location").getValue());
            }
            throw new ResolveException(new ResolveError(status, response.getStatusLine().getReasonPhrase()));
        } catch (IOException e) {
            log.error("error resolving " + urlString, e);
            throw new ResolveException(new ResolveError(-1, findRootError(e)));
        }
    }

    private String findRootError(Exception e) {

        String message;
        Throwable next = e;
        do {
            message = next.getMessage();
            next = next.getCause();
        } while (message == null);

        return message;
    }

    public static class ResolveException extends Exception {

        private final ResolveError error;

        ResolveException(ResolveError error) {
            this.error = error;
        }

        ResolveError getError() {
            return error;
        }
    }

    public static class ResolveError {
        private final int status;
        private final String message;

        ResolveError(int status, String httpMessage) {
            this.status = status;
            this.message = httpMessage;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ResolveError{" +
                    "status=" + status +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    public static class ResolveResult {

        private final boolean resolved;
        private final String url;
        private final ResolveError error;
        private final String hostName;

        static ResolveResult resolved(String finalUrl) {
            try {
                return new ResolveResult(true, finalUrl, hostResolver.getCleanedHost(finalUrl), null);
            } catch (MalformedURLException e) {
                return new ResolveResult(false, finalUrl, null, new ResolveError(-3, e.getMessage()));
            }
        }

        static ResolveResult moved(String nextUrl) {
            return new ResolveResult(false, nextUrl, null, null);
        }

        static ResolveResult error(String url, ResolveError error) {
            return new ResolveResult(false, url, null, error);
        }

        private ResolveResult(boolean resolved, String url, String hostName, ResolveError error) {
            this.resolved = resolved;
            this.url = url;
            this.error = error;
            this.hostName = hostName;
        }

        public boolean isResolved() {
            return resolved;
        }

        public boolean isError() {
            return null != error;
        }

        public String getUrl() {
            return url;
        }

        public ResolveError getError() {
            return error;
        }

        public String getHostName() {
            return hostName;
        }

        @Override
        public String toString() {
            return "ResolveStatus{" +
                    "resolved=" + resolved +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

}
