package org.rcsb.strucmotif.math;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Partitions a collection into (roughly) equal-sized chunks. Will shuffle the original collection.
 *
 * code from: https://e.printstacktrace.blog/divide-a-list-to-lists-of-n-size-in-Java-8/
 * @param <T> type of the original collection
 */
public class Partition<T> extends AbstractList<List<T>> {
    private final List<T> list;
    private final int chunkSize;

    /**
     * Split a collection into partitions up to a defined size.
     * @param list collection to partition
     * @param chunkSize how many elements in one batch
     */
    public Partition(Collection<T> list, int chunkSize) {
        this.list = new ArrayList<>(list);
        // shuffle to prevent troublemakers such as ribosome and virus capsids occurring in the same chunk
        Collections.shuffle(this.list);
        this.chunkSize = chunkSize;
    }

    @Override
    public List<T> get(int index) {
        int start = index * chunkSize;
        int end = Math.min(start + chunkSize, list.size());

        if (start > end) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of the list range <0," + (size() - 1) + ">");
        }

        return new ArrayList<>(list.subList(start, end));
    }

    @Override
    public int size() {
        return (int) Math.ceil((double) list.size() / (double) chunkSize);
    }
}
