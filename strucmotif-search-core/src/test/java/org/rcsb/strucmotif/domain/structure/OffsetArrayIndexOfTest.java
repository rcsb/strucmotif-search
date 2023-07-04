package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OffsetArrayIndexOfTest {
    private static final int[] TEST_DATA = new int[] { 0, 25, 50, 60, 70, 80, 90, 100, 200, 250, 400 };

    @Test
    void whenElementAtStart_thenFirstIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 0);
        assertEquals(0, i);
    }

    @Test
    void whenElementPastStart_thenFirstIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 1);
        assertEquals(0, i);
    }

    @Test
    void whenElementBeforeOffset_thenFirstIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 24);
        assertEquals(0, i);
    }

    @Test
    void whenElementAtOffset_thenSecondIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 25);
        assertEquals(1, i);
    }

    @Test
    void whenElementBeforePenultimateOffset_thenAntepenultimateIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 249);
        assertEquals(8, i);
    }

    @Test
    void whenElementAtPenultimateOffset_thenPenultimateIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 250);
        assertEquals(9, i);
    }

    @Test
    void whenElementBeforeLastOffset_thenPenultimateIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 399);
        assertEquals(9, i);
    }

    @Test
    void whenElementAtLastOffset_thenLastIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 400);
        assertEquals(10, i);
    }

    @Test
    void whenElementAfterLastOffset_thenLastIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, 401);
        assertEquals(10, i);
    }

    @Test
    void whenNegativeInput_thenMinus1Returned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, -1);
        assertEquals(-1, i);
    }

    @Test
    void whenAboveHighestValue_thenLastIndexReturned() {
        int i = DefaultStructure.offsetArrayIndexOf(TEST_DATA, Integer.MAX_VALUE);
        assertEquals(TEST_DATA.length - 1, i);
    }
}
