package com.qq.tarkovhealthfxlab.client;

public enum HealthFxSource {
    MOCK,
    LAB_SERVER,
    TARKOV_LIVE;

    public HealthFxSource next() {
        HealthFxSource[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
