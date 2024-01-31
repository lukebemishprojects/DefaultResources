package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;

public interface OutdatedResourcesListener {
    void resourcesOutdated();

    static void register(String modId, OutdatedResourcesListener listener) {
        if (DefaultResources.isTargetOutdated(modId)) {
            listener.resourcesOutdated();
        } else {
            DefaultResources.addListener(modId, listener);
        }
    }
}
