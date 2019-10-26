package org.faboo.example.twitter.sse;

import org.faboo.example.twitter.sse.util.BatchedSpliterator;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;

public class LinkResolver {

    private final static Logger log = LoggerFactory.getLogger(LinkResolver.class);

    private final static Collection<Node> poisonPill = Collections.emptyList();

    @Context
    public GraphDatabaseService db;

    @Procedure(
            name = "twitter.resolveLinks",
            mode = Mode.WRITE)
    public void resolveLinks() {

        try {
            int nbThreads = Runtime.getRuntime().availableProcessors() * 2;
            final BlockingQueue<Collection<Node>> queue = new ArrayBlockingQueue<>(nbThreads * 10);
            final ExecutorService executorService = Executors.newFixedThreadPool(nbThreads);

            Collection<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < nbThreads; i++) {
                futures.add(executorService.submit(new ResolverRunner(queue)));
            }

            BatchedSpliterator.batched(10,
                    db.findNodes(Label.label("Link")).stream()
                            .filter(node -> node.hasProperty("url"))
                            .filter(node -> !node.getRelationships(RelationshipType.withName("LINKS_TO")).iterator().hasNext())
//                            .map(node -> (String) node.getProperty("url"))
                            .iterator())
                    .forEach(nodes -> {
                        try {
                            log.debug("adding {} nodes to processing queue", nodes.size());
                            queue.put(nodes);
                        } catch (InterruptedException e) {
                            log.error("error adding nodes to work queue", e);
                        }
                    });


            terminateAndWait(executorService, futures, queue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void terminateAndWait(ExecutorService executorService, Collection<Future<?>> futures,
                                  BlockingQueue<Collection<Node>> queue) throws InterruptedException {
        for (int i = 0; i < futures.size(); i++) {
            queue.put(poisonPill);
        }

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("error stopping runnables", e);
            }
        });
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    private class ResolverRunner implements Runnable {

        final BlockingQueue<Collection<Node>> queue;

        private ResolverRunner(BlockingQueue<Collection<Node>> queue) {
            this.queue = queue;
            log.debug("created ResolverRunner");
        }

        @Override
        public void run() {

            log.debug("running ResolverRunner");
            RedirectResolver resolver = new RedirectResolver();

            Collection<Node> work;
            try {
                do {
                    work = queue.take();
                    if (work == poisonPill) {
                        break;
                    }
                    log.debug("processing {} Link nodes", work.size());

                    try ( Transaction tx = db.beginTx()) {
                        Collection<Pair<Node, RedirectResolver.ResolveResult>> results = resolveUrls(resolver, work);

                        results.forEach(pair -> {
                            if (pair.other().isError()) {
                                pair.first().setProperty("errorCode", pair.other().getError().getStatus());
                                pair.first().setProperty("errorMessage", pair.other().getError().getMessage());
                            } else {
                                Node urlNode = findOrCreateUrlNode(pair.other().getUrl());
                                pair.first().createRelationshipTo(urlNode, RelationshipType.withName("LINKS_TO"));
                            }

                        });
                        tx.success();
                    } catch (MultipleFoundException e) {
                        log.error("node for url already exists", e);
                    }
                } while (true);
            } catch (InterruptedException e) {
                log.error("interrupted while waiting for work", e);
            }
            log.debug("ResolverRunner stopped");
        }

        private Node findOrCreateUrlNode(String url) {
            Node node = db.findNode(Label.label("Url"), "url", url);
            if (null == node) {
                log.debug("creating Url node for {}", url);
                node = db.createNode(Label.label("Url"));
                node.setProperty("url", url);
            }
            return node;
        }

        private Collection<Pair<Node, RedirectResolver.ResolveResult>> resolveUrls(
                RedirectResolver resolver, Collection<Node> work) {

            Collection<Pair<Node, RedirectResolver.ResolveResult>> results = new ArrayList<>();
            work.forEach(w -> {
                RedirectResolver.ResolveResult result = resolver.resolve((String) w.getProperty("url"));
                results.add(Pair.of(w, result));
            });
            return results;
        }

    }


}