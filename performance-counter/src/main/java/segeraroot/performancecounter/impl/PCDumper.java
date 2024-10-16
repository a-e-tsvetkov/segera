package segeraroot.performancecounter.impl;

import lombok.RequiredArgsConstructor;
import segeraroot.performancecounter.PCSink;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class PCDumper {
    private final PCHostImpl host;
    private final PCSink sink;
    private static final long PERIOD_SECONDS = 10;

    public void start() {
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
        threadPool.scheduleAtFixedRate(this::run, 0, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void run() {
        host.dump(sink::dump);
    }
}
