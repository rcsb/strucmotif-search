package org.rcsb.strucmotif.update;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Partitions a collection into (roughly) equal-sized chunks. Will retain the order (if any) of the original collection
 * by converting all elements into a list of lists.
 *
 * code from: https://e.printstacktrace.blog/divide-a-list-to-lists-of-n-size-in-Java-8/
 * @param <T> type of the original collection
 */
public class Partition<T> extends AbstractList<List<T>> {
    private final List<T> list;
    private final int chunkSize;

    public Partition(Collection<T> list, int chunkSize) {
        this.list = new ArrayList<>(list);
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
