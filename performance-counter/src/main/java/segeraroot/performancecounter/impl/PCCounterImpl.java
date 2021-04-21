package segeraroot.performancecounter.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.performancecounter.Aggregator;
import segeraroot.performancecounter.PCCounter;
import segeraroot.performancecounter.ValueConsumer;
import segeraroot.performancecounter.impl.aggregators.SummAggregator;

import java.util.function.BiConsumer;

@Slf4j
public class PCCounterImpl implements PCCounter {
    private final ThreadLocal<PCCounterSingleThreaded> byThread = new ThreadLocal<>();
    private final CopyOnWriteList<PCCounterSingleThreaded> all = new CopyOnWriteList<>(PCCounterSingleThreaded[]::new);
    private final Object dumpLock = new Object();
    private final String name;
    private final Aggregator aggregator;
    private boolean sealed = false;

    public PCCounterImpl(String name) {
        this.name = name;
        aggregator = new SummAggregator();
    }

    @Override
    public void add(int count) {
        PCCounterSingleThreaded counter = byThread.get();
        if (counter == null) {
            Thread currentThread = Thread.currentThread();
            log.trace("add: attach to thread: {}", currentThread);

            synchronized (dumpLock) {
                sealed = true;
                counter = new PCCounterSingleThreaded(currentThread, new SummAggregator());
            }
            byThread.set(counter);
            addCounter(counter);
        }
        counter.add(count);
    }

    private void addCounter(PCCounterSingleThreaded counter) {
        all.add(counter);
    }

    private void removeCounter(PCCounterSingleThreaded counter) {
        all.remove(counter);
    }

    public void dump(BiConsumer<String, Aggregator> consumer) {
        synchronized (dumpLock) {
            if (!sealed) {
                return;
            }

            aggregator.reset(ValueConsumer.NULL);

            PCCounterSingleThreaded[] current = all.get();
            //Do not use enhanced for because it creates instance of iterator
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < current.length; i++) {
                PCCounterSingleThreaded counter = current[i];
                Thread thread = counter.getThread();
                if (!thread.isAlive()) {
                    unregister(counter, thread);
                }

                counter.dump(aggregator);
            }
            consumer.accept(name, aggregator);
        }
    }

    private void unregister(PCCounterSingleThreaded counter, Thread thread) {
        log.trace("unregister: detach from thread: {}", thread);
        removeCounter(counter);
    }
}
