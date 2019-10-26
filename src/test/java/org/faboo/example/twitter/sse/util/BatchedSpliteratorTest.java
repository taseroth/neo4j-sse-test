package org.faboo.example.twitter.sse.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchedSpliteratorTest {

    @Test
    void collectToStreamOfCollection100() {

        Stream<Integer> intStream = IntStream.range(1, 101).boxed();
        List<Collection<Integer>> coll = BatchedSpliterator.batched(10, intStream.iterator())
                .collect(Collectors.toList());

        assertThat(coll.size()).isEqualTo(10);

        int sum = coll.stream().flatMap(Collection::stream).mapToInt(Integer::intValue).sum();

        assertThat(sum).isEqualTo(IntStream.range(1, 101).sum());

        coll.forEach(c -> assertThat(c.size()).isEqualTo(10));
    }

    @Test
    void collectToStreamOfCollection99() {

        Stream<Integer> intStream = IntStream.range(1, 100).boxed();
        List<Collection<Integer>> coll = BatchedSpliterator.batched(10, intStream.iterator())
                .collect(Collectors.toList());

        assertThat(coll.size()).isEqualTo(10);

        int sum = coll.stream().flatMap(Collection::stream).mapToInt(Integer::intValue).sum();

        assertThat(sum).isEqualTo(IntStream.range(1, 100).sum());

        coll.stream().limit(9).forEach(c -> assertThat(c.size()).isEqualTo(10));
        assertThat(coll.get(9).size()).isEqualTo(9);
    }
}