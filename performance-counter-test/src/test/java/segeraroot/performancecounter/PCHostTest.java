package segeraroot.performancecounter;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import segeraroot.performancecounter.impl.PCHostImpl;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PCHostTest {

    @Test
    void createCounter() {
        String counterName = "test";
        var host = new PCHostImpl();
        var counter = host.counter(counterName);

        counter.add(1);
        counter.add(2);
        counter.add(3);

        var map = new HashMap<String, Long>();
        host.dump(updateMap(map));

        assertThat(map)
                .contains(
                        entry(counterName + ":SUM", 6L),
                        entry(counterName + ":COUNT", 3L)
                ).size().isEqualTo(2);
    }

    @SneakyThrows
    @RepeatedTest(1)
    void multiThreadCounter() {
        String counterName = "test";
        long addCount = 10000000;
        int threadNumber = 16;

        var host = new PCHostImpl();
        var counter = host.counter(counterName);
        try (var service = Executors.newFixedThreadPool(threadNumber)) {
            CyclicBarrier barrier = new CyclicBarrier(threadNumber);
            Runnable runnable = () -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < addCount; i++) {
                    counter.add(i);
                }
            };
            for (int i = 0; i < threadNumber; i++) {
                service.submit(runnable);
            }

            service.shutdown();
            Assertions.assertTrue(service.awaitTermination(30, TimeUnit.SECONDS));
        }

        var map = new HashMap<String, Long>();
        host.dump(updateMap(map));
        long expectedCount = addCount * threadNumber;
        long expectedSumm = (addCount * (addCount - 1) / 2) * threadNumber;
        assertThat(map)
                .contains(
                        entry(counterName + ":SUM", expectedSumm),
                        entry(counterName + ":COUNT", expectedCount)
                );
    }

    private static BiConsumer<String, Aggregator> updateMap(HashMap<String, Long> map) {
        return (name, aggregator) -> {
            map.put(name + ":COUNT", aggregator.getCount());
            map.put(name + ":SUM", aggregator.getSum());
        };
    }
}
