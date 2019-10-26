package org.faboo.example.twitter.sse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Spliterator that allows to turn a Stream of T into a Stream of Collection<T>.
 * @param <T> type of the Stream to work on
 */
public class BatchedSpliterator<T> implements Spliterator<Collection<T>> {

    public static <T> Stream<Collection<T>> batched(int batchSize, Iterator<T> iterator) {
        return StreamSupport.stream(new BatchedSpliterator<>(batchSize, iterator), false);
    }

    private int bufferSize;
    private final Collection<T> buffer;
    private Iterator<T> sourceIterator;

    private BatchedSpliterator(int bufferSize, Iterator<T> sourceIterator) {
        this.bufferSize = bufferSize;
        buffer = new ArrayList<>(bufferSize);
        this.sourceIterator = sourceIterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Collection<T>> action) {

        while (sourceIterator.hasNext()) {
            buffer.add(sourceIterator.next());
            if (buffer.size() == bufferSize) {
                break;
            }
        }
        action.accept(new ArrayList<>(buffer));
        buffer.clear();
        return sourceIterator.hasNext();
    }

    @Override
    public Spliterator<Collection<T>> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return NONNULL | IMMUTABLE;
    }
}
