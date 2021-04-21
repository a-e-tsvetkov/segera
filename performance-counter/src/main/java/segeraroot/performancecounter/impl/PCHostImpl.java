package segeraroot.performancecounter.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.performancecounter.Aggregator;
import segeraroot.performancecounter.PCHost;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class PCHostImpl implements PCHost {
    private final Map<String, PCCounterImpl> byName = new HashMap<>();
    private final CopyOnWriteList<PCCounterImpl> all = new CopyOnWriteList<>(PCCounterImpl[]::new);
    private final Object lock = new Object();

    public PCHostImpl() {
    }

    @Override
    public PCCounterImpl counter(String counterName) {
        synchronized (lock) {
            PCCounterImpl counter = byName.get(counterName);
            if (counter == null) {
                log.trace("registerAggregator: create PCCounterImpl for: {}", counterName);
                counter = new PCCounterImpl(counterName);
                all.add(counter);
                byName.put(counterName, counter);
            }
            return counter;
        }
    }

    public void dump(BiConsumer<String, Aggregator> consumer) {
        PCCounterImpl[] counters = all.get();
        //Do not use enhanced for because it creates instance of iterator
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < counters.length; i++) {
            counters[i].dump(consumer);
        }
    }
}
