package com.winlator.d2;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;












final class D2TextCache<C, V> {
    static final int SLOTS = 4;

    private final UnaryOperator<C> copy;
    @SuppressWarnings("unchecked")
    private final WeakReference<Object>[] owners = new WeakReference[SLOTS];
    private final String[] languages = new String[SLOTS];
    private final Object[] configs = new Object[SLOTS];
    private final Object[] values = new Object[SLOTS];
    private int next, builds;

    D2TextCache(UnaryOperator<C> copy) {
        this.copy = copy;
    }


    @SuppressWarnings("unchecked")
    synchronized V get(Object owner, String language, C config, Supplier<V> build) {
        for (int i = 0; i < SLOTS; i++) {
            WeakReference<Object> o = owners[i];
            if (o != null && o.get() == owner && language.equals(languages[i]) && config.equals(configs[i])) return (V) values[i];
        }
        V v = build.get();
        builds++;
        int i = free(owner);
        owners[i] = new WeakReference<>(owner);
        languages[i] = language;
        configs[i] = copy.apply(config);
        values[i] = v;
        return v;
    }


    private int free(Object owner) {
        for (int i = 0; i < SLOTS; i++) if (owners[i] != null && owners[i].get() == owner) return i;
        for (int i = 0; i < SLOTS; i++) if (owners[i] == null || owners[i].get() == null) return i;
        int i = next;
        next = (next + 1) % SLOTS;
        return i;
    }


    synchronized int builds() {
        return builds;
    }
}
