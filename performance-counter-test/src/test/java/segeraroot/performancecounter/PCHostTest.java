package segeraroot.performancecounter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import segeraroot.performancecounter.impl.PCHostImpl;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        host.dump((name, aggregator) -> map.put(name, aggregator.getSum()));

        Assertions.assertEquals(6L, map.get(counterName + ":SUMM"));
        Assertions.assertEquals(3L, map.get(counterName + ":COUNT"));
    }

    @RepeatedTest(1)
    void multiThreadCounter() throws InterruptedException {
        String counterName = "test";
        long addCount = 10000000;
        int threadNumber = 16;

        var host = new PCHostImpl();
        var counter = host.counter(counterName);
        ExecutorService service = Executors.newFixedThreadPool(threadNumber);
        CyclicBarrier barrier = new CyclicBarrier(threadNumber);
        Runnable runnable = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
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

        var map = new HashMap<String, Long>();
        host.dump((name, aggregator) -> map.put(name, aggregator.getSum()));

        Assertions.assertEquals(addCount * threadNumber, map.get(counterName + ":COUNT"));
        Assertions.assertEquals((addCount * (addCount - 1) / 2) * threadNumber, map.get(counterName + ":SUMM"));
    }

}