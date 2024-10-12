package segeraroot.performancecounter.impl;

import lombok.Getter;
import segeraroot.performancecounter.Aggregator;


public class PCCounterSingleThreaded {
    private final Aggregator aggregator;
    @Getter
    private final Thread thread;

    public PCCounterSingleThreaded(Thread thread, Aggregator aggregator) {
        this.thread = thread;
        this.aggregator = aggregator;
    }

    public void add(int count) {
        aggregator.add(count);
    }

    public void dump(Aggregator aggregatorsCache) {
        aggregator.reset(aggregatorsCache);
    }
}
