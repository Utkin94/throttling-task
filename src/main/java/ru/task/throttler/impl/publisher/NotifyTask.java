package ru.task.throttler.impl.publisher;


import ru.task.throttler.impl.ProcessorNotifier;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicStampedReference;

public class NotifyTask implements Runnable {

    private final String ccyPair;
    private final AtomicStampedReference<Double> actualVersion;
    private final ProcessorNotifier processorNotifier;

    public NotifyTask(String ccyPair, AtomicStampedReference<Double> actualVersion, ProcessorNotifier processorNotifier) {
        this.ccyPair = ccyPair;
        this.actualVersion = actualVersion;
        this.processorNotifier = processorNotifier;
    }

    @Override
    public void run() {
        processorNotifier.notifyRateUpdate(ccyPair, actualVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotifyTask that = (NotifyTask) o;
        return ccyPair.equals(that.ccyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ccyPair);
    }
}
