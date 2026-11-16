package com.chandsharma.nodoom;

import java.util.List;

/** Single registration point for every app supported by No Doom. */
final class ProtectedAppRegistry {
    private final List<ProtectedAppModule> modules = List.of(
            new InstagramProtectionModule());

    ProtectedAppModule find(String packageName) {
        if (packageName == null) {
            return null;
        }
        for (ProtectedAppModule module : modules) {
            if (module.packageName().equals(packageName)) {
                return module;
            }
        }
        return null;
    }
}
