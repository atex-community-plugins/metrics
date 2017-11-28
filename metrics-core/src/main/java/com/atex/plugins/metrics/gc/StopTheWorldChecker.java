package com.atex.plugins.metrics.gc;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StopTheWorldChecker
 *
 * @author mnova
 */
public class StopTheWorldChecker extends Thread {

    private static final Logger LOGGER = Logger.getLogger(StopTheWorldChecker.class.getName());

    private long min = -1;
    private long max = -1;
    private long lastLongPause = -1;
    private long oomCount = 0;
    private boolean oomSignal;

    private boolean runThread = true;

    private final long pause;
    private final long minLevel;

    public StopTheWorldChecker(final long pause,
                               final long minLevel) {
        this.pause = pause;
        this.minLevel = minLevel;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public long getOomCount() {
        return oomCount;
    }

    public boolean isOomSignal() {
        return oomSignal;
    }

    public synchronized void stopRunning() {
        LOGGER.info("stopRunning");
        runThread = false;
    }

    @Override
    public void run() {
        while (runThread) {
            final long startTime = System.currentTimeMillis();
            try {
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
                break;
            }
            final long endTime = System.currentTimeMillis();

            final long elapsed = endTime - startTime;
            if (min < 0 || elapsed < min) {
                min = elapsed;
            }
            if (max < 0 || elapsed > max) {
                max = elapsed;
            }
            if (elapsed > (pause + 10L)) {
                LOGGER.fine("gc took " + elapsed + "ms");
                if (lastLongPause > 0) {
                    if ((startTime - lastLongPause) <= minLevel) {
                        if (!oomSignal) {
                            oomCount += 1;
                            LOGGER.warning("oom! " + oomCount);
                        }
                    } else {
                        if (oomCount > 0) {
                            oomCount -= 1;
                        }
                        if (oomSignal && oomCount == 0) {
                            oomSignal = false;
                            LOGGER.warning("resetting oom signal");
                        }
                    }
                    if (oomCount >= 3 && !oomSignal) {
                        LOGGER.warning("signal oom");
                        oomSignal = true;
                    }
                }
                lastLongPause = startTime;
            } else {
                if (oomCount > 0) {
                    oomCount -= 1;
                }
                if (oomSignal && oomCount == 0) {
                    oomSignal = false;
                    LOGGER.warning("resetting oom signal");
                }
            }

            LOGGER.fine("GC MIN: " + min);
            LOGGER.fine("GC MAX: " + max);
            LOGGER.fine("OOMCount: " + oomCount);
            LOGGER.fine("OOMSignal: " + oomSignal);
        }

        LOGGER.info("GC MIN: " + min);
        LOGGER.info("GC MAX: " + max);
        LOGGER.info("OOMCount: " + oomCount);
        LOGGER.info("OOMSignal: " + oomSignal);
    }

}
