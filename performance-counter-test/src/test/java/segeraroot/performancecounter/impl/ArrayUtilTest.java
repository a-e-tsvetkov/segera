package segeraroot.performancecounter.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ArrayUtilTest {

    @Test
    void copyExcept() {
        String[] from = {"a", "b", "c"};
        String[] to = new String[from.length - 1];
        ArrayUtil.copyExcept(from, to, from[1]);
        assertSame(from[0], to[0]);
        assertSame(from[2], to[1]);
    }
}