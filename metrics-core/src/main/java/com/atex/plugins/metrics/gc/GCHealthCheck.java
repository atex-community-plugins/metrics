package com.atex.plugins.metrics.gc;

import com.codahale.metrics.health.HealthCheck;

/**
 * GCHealthCheck
 *
 * @author mnova
 */
public class GCHealthCheck extends HealthCheck {

    private final StopTheWorldChecker checker;

    public GCHealthCheck(final StopTheWorldChecker checker) {
        this.checker = checker;
    }

    @Override
    protected Result check() throws Exception {
        if (checker.isAlive()) {
            if (checker.isOomSignal()) {
                return Result.unhealthy("oom signalled");
            } else {
                return Result.healthy();
            }
        } else {
            return Result.unhealthy("check thread [" + checker.getId() + "] not alive");
        }
    }

}
