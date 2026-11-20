package com.example.maidbooking.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class MaidLocks {

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String maidId, Supplier<T> action) {
        Object lock = locks.computeIfAbsent(maidId, id -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }
}
