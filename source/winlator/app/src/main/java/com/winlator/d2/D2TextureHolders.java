package com.winlator.d2;

import java.util.ArrayList;

public final class D2TextureHolders<H> {
    private final ArrayList<H> holders = new ArrayList<>(2);

    public synchronized void hold(H holder) {
        if (holder != null && !holders.contains(holder)) holders.add(holder);
    }

    public synchronized boolean letGo(H holder) {
        holders.remove(holder);
        return holders.isEmpty();
    }

    public synchronized H any() {
        return holders.isEmpty() ? null : holders.get(0);
    }

    public synchronized int count() {
        return holders.size();
    }
}
