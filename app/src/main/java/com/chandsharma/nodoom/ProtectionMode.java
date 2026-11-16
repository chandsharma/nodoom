package com.chandsharma.nodoom;

enum ProtectionMode {
    ON,
    COUNTDOWN,
    UNLOCK_WINDOW,
    OFF;

    boolean isProtected() {
        return this != OFF;
    }
}

