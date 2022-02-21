package ru.task.throttler.impl.publisher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SetLinkedBlockingQueue<T> extends LinkedBlockingQueue<T> {

    private Set<T> alreadyAssignedTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public synchronized boolean add(T t) {
        if (alreadyAssignedTasks.contains(t)) {
            return false;
        } else {
            alreadyAssignedTasks.add(t);
            return super.add(t);
        }
    }

    @Override
    public T take() throws InterruptedException {
        T t = super.take();
        alreadyAssignedTasks.remove(t);
        return t;
    }
}
