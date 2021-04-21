package segeraroot.performancecounter.impl;

public class ArrayUtil {
    public static <T> void copyExcept(T[] from, T[] to, T object) {
        int found = 0;
        for (int i = 0; i < to.length; i++) {
            while (from[i + found] == object) {
                found++;
            }
            to[i] = from[i + found];
        }
    }
}
