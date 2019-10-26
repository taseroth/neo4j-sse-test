package org.faboo.example.twitter.sse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LinkResolverTest {

    private static final Config driverConfig = Config.build().withoutEncryption().toConfig();
    private ServerControls embeddedDatabaseServer;

    @BeforeAll
    void initializeNeo4j() {

        this.embeddedDatabaseServer = TestServerBuilders
                .newInProcessBuilder()
                .withProcedure(LinkResolver.class)
                .withFixture(
                                "CREATE (:Link {url: 'https://r.neo4j.com/2X0PniV'}) " +
                                "CREATE (:Link {url: 'url2'}) " +
                                "CREATE (:Link {url: 'url3'}) " +
                                "CREATE (:Link {url: 'url4'})<-[:CONTAINS]-(:Post {foo:'bar'}) " +
                                "CREATE (:Link {wrongAttr: 'shall not be processed'}) " +
                                "CREATE (n:Link {url: 'urlx'})-[:LINKS_TO]->(:Url {something:'whatnot'})")
                .newServer();
    }

    @Test
    void shallPrintSomeUrls() {
        try(Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(),driverConfig);
            Session session = driver.session() ) {
            session.run("call twitter.resolveLinks()");

            StatementResult result = session.run("match (u:Url) return u");
            List<Record> list = result.list();
            System.out.println(list);
        }
    }
}