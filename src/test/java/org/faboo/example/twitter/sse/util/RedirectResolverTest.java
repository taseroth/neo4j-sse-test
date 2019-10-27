package org.faboo.example.twitter.sse.util;

import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedirectResolverTest {

    private final ProtocolVersion version = new ProtocolVersion("http", 1, 1);

    @Test
    void notARealTest() {

        RedirectResolver resolver = new RedirectResolver(HttpClients.createDefault());
        RedirectResolver.ResolveResult resolveStatus = resolver.resolve("https://r.neo4j.com/2X0PniV");

        System.out.println(resolveStatus);
    }

    @Test
    void status200() throws IOException {

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        RedirectResolver resolver = new RedirectResolver(httpClient);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(version, 200, "Ok"));

        HostResolver hostResolver = new HostResolver();
        String url = "http://go.gl/fooba";
        RedirectResolver.ResolveResult result = resolver.resolve(url);

        assertThat(result.getUrl()).isEqualTo(url);
        assertThat(result.isResolved()).isEqualTo(true);
        assertThat(result.getHostName()).isEqualTo(hostResolver.getCleanedHost(url));
    }
}