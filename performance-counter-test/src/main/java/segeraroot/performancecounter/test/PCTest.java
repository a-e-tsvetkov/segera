package segeraroot.performancecounter.test;

import lombok.extern.slf4j.Slf4j;
import segeraroot.performancecounter.PCSink;
import segeraroot.performancecounter.impl.PCCounterImpl;
import segeraroot.performancecounter.impl.PCDumper;
import segeraroot.performancecounter.impl.PCHostImpl;
import segeraroot.performancecounter.impl.PCSinkSOut;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PCTest {
    public static void main(String[] args) {
        String counterName = "test";

        var host = new PCHostImpl();

        var counter = host.counter(counterName);
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        for (int i = 0; i < 50; i++) {
            service.scheduleAtFixedRate(addCommand(counter), 0, 1, TimeUnit.SECONDS);
        }
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                Runnable command = addCommand(counter);
                command.run();
                sleep(10_000);
                command.run();
            }).start();
        }

        PCSink sink = new PCSinkSOut();
        PCDumper dumper = new PCDumper(host, sink);
        dumper.start();
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static Runnable addCommand(PCCounterImpl counter) {
        byte[] v = {0};
        return () -> {
            counter.add(v[0] & 0xFF);
            v[0]++;
        };
    }
}

