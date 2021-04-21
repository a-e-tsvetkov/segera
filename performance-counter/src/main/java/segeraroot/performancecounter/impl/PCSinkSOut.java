package segeraroot.performancecounter.impl;

import segeraroot.performancecounter.Aggregator;
import segeraroot.performancecounter.PCSink;

public class PCSinkSOut implements PCSink {
    @Override
    public void dump(String name, Aggregator aggregator) {

        long[] longs = aggregator.reset((sum, count) -> new long[]{sum, count});
        System.out.print(name);
        System.out.print(": ");
        System.out.print(longs[0]);
        System.out.print(" ");
        System.out.println(longs[1]);
    }
}
