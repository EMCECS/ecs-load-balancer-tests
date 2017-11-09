package com.emc.ecs.support;

import org.springframework.core.io.Resource;

public class CheckAvailabiltiyThread extends Thread {

    private URIResourceStore store;
    private String key;
    private boolean stop;
    private boolean available = true;

    public CheckAvailabiltiyThread(URIResourceStore store, String key) {
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

            System.out.println("available: " + available);

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
