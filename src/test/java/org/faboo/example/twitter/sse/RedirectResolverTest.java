package org.faboo.example.twitter.sse;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.stream.IntStream;

class RedirectResolverTest {

    private final static Logger log = LoggerFactory.getLogger(RedirectResolverTest.class);

    @Test
    void name() {

        RedirectResolver resolver = new RedirectResolver();
        RedirectResolver.ResolveResult resolveStatus = resolver.resolve("https://r.neo4j.com/2X0PniV");

        System.out.println(resolveStatus);
    }

    @Test
    void multithread() throws InterruptedException {

        int nbThreads = 10;

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(20);

        ExecutorService executorService = Executors.newFixedThreadPool(nbThreads);

        Collection<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < nbThreads; i++) {
            futures.add(executorService.submit(new Foo(queue, "runner-" + i)));
        }

        IntStream.range(1, 101).boxed().forEach(i -> {
            try {
                log.info("adding to queue: {}", i);
                queue.put(i);
            } catch (InterruptedException e) {
                log.error("error add ing to work queue", e);
            }
        });
        log.info("poison runners");
        // kill them with poison
        queue.drainTo(new ArrayList<>()); // empty queue
        for (int i = 0; i < nbThreads; i++) {
            queue.put(-1);
        }
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("error waiting runners to finish");
            }
        });
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    private static class Foo implements Runnable {

        private final BlockingQueue<Integer> queue;
        private final String name;

        Foo(BlockingQueue<Integer> queue, String name) {
            this.queue = queue;
            this.name = name;
            log.info("created {}", name);
        }

        @Override
        public void run() {
            int number = -1;
            do {
                try {
                    number = queue.take();
                    log.info("\t{} got {}", name, number);
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (number != -1);
            log.error("terminating " + name);
        }
    }
}