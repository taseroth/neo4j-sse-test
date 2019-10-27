package org.faboo.example.twitter.sse.util;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

class HostResolverTest {

    @Test
    void mustRemoveWWW() throws MalformedURLException {
        HostResolver resolver = new HostResolver();

        String cleanedHost = resolver.getCleanedHost("http://www.google.com/faa/boo");

        assertThat(cleanedHost).isEqualTo("google.com");
    }

    @Test
    void mustRemoveMobileShort() throws MalformedURLException {

        HostResolver resolver = new HostResolver();

        String cleanedHost = resolver.getCleanedHost("http://m.google.com/faa/boo");

        assertThat(cleanedHost).isEqualTo("google.com");
    }

    @Test
    void mustRemoveMobileLong() throws MalformedURLException {

        HostResolver resolver = new HostResolver();

        String cleanedHost = resolver.getCleanedHost("http://mobile.google.com/faa/boo");

        assertThat(cleanedHost).isEqualTo("google.com");
    }

    @Test
    void mustRemoveWWWAndMobile() throws MalformedURLException {
        HostResolver resolver = new HostResolver();

        String cleanedHost = resolver.getCleanedHost("http://mobile.www.google.com/faa/boo");

        assertThat(cleanedHost).isEqualTo("google.com");
    }
}