package org.faboo.example.twitter.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RedirectResolver {

    private final Logger log = LoggerFactory.getLogger(RedirectResolver.class);

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

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return ResolveResult.resolved(urlString);
            }

            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {

                return ResolveResult.moved(conn.getHeaderField("Location"));
            }
            throw new ResolveException(new ResolveError(status, conn.getResponseMessage()));
        } catch (IOException e) {
            throw new ResolveException(new ResolveError(-1, e.getMessage()));
        }
    }

    public static class ResolveException extends Exception {

        private final ResolveError error;

        ResolveException(ResolveError error) {
            this.error = error;
        }

        public ResolveError getError() {
            return error;
        }
    }

    public static class ResolveError {
        private final int status;
        private final String message;

        public ResolveError(int status, String httpMessage) {
            this.status = status;
            this.message = httpMessage;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ResolveResult {

        private final boolean resolved;
        private final String url;
        private final ResolveError error;

        static ResolveResult resolved(String finalUrl) {
            return new ResolveResult(true, finalUrl, null);
        }

        static ResolveResult moved(String nextUrl) {
            return new ResolveResult(false, nextUrl, null);
        }

        static ResolveResult error(String url, ResolveError error) {
            return new ResolveResult(false, url, error);
        }

        private ResolveResult(boolean resolved, String url, ResolveError error) {
            this.resolved = resolved;
            this.url = url;
            this.error = error;
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

        @Override
        public String toString() {
            return "ResolveStatus{" +
                    "resolved=" + resolved +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

}
