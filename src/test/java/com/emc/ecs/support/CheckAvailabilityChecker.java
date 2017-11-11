package com.emc.ecs.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

public class CheckAvailabilityChecker extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(CheckAvailabilityChecker.class);

    private URIResourceStore store;
    private String key;
    private boolean stop;
    private boolean available = true;

    public CheckAvailabilityChecker(URIResourceStore store, String key) {
        this.store = store;
        this.key = key;
    }

    @Override
    public void run() {
        super.run();

        while (!stop) {
            try {
                Resource r = store.getResource("test-object");
                if (!r.exists()) {
                    available = false;
                }
            } catch (Exception e) {
                available = false;
            }

            logger.info("available: " + available);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSignal() {
        stop = true;
    }

    public boolean isAvailable() {
        return available;
    }
}