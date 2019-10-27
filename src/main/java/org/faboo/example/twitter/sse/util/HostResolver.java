package org.faboo.example.twitter.sse.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

class HostResolver {

    private static final List<String> prefixesToDelete = Arrays.asList("www.", "mobile.", "m.");

    String getCleanedHost(String urlString) throws MalformedURLException {

        URL url = new URL(urlString);

        String host = url.getHost();

        for (String pre : prefixesToDelete) {
            host = host.replaceFirst(pre, "");
        }
        return host;
    }

}
