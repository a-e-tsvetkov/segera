package segeraroot.performancecounter.impl.aggregators;

import segeraroot.performancecounter.Aggregator;
import segeraroot.performancecounter.ValueConsumer;

import java.util.concurrent.atomic.AtomicLong;

public class SummAggregator implements Aggregator {
    private final AtomicLong count = new AtomicLong(0L);
    private final AtomicLong sum = new AtomicLong(0L);

    @Override
    public void add(int value) {
        sum.addAndGet(value);
        count.incrementAndGet();
    }

    @Override
    public long getSum() {
        return sum.get();
    }

    @Override
    public long getCount() {
        return count.get();
    }

    private long resetCount() {
        long tmp;
        do {
            tmp = count.get();
        } while (!count.compareAndSet(tmp, 0));
        return tmp;
    }

    private long resetSum() {
        long tmp;
        do {
            tmp = sum.get();
        } while (!sum.compareAndSet(tmp, 0));
        return tmp;
    }

    @Override
    public <T> T reset(ValueConsumer<T> consumer) {
        return consumer.consume(resetSum(), resetCount());
    }

    @Override
    public Void consume(long sum, long count) {
        this.sum.addAndGet(sum);
        this.count.addAndGet(count);
        return null;
    }
}
