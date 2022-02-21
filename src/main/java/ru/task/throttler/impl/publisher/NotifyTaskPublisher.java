package ru.task.throttler.impl.publisher;

import ru.task.throttler.ShutdownHook;
import ru.task.throttler.impl.ProcessorNotifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

public class NotifyTaskPublisher implements ShutdownHook {

    private final ProcessorNotifier processorNotifier;

    private final ExecutorService executor;

    /**
     * Create executor with {@link SetLinkedBlockingQueue} to throttle frequently changed ccyPairs.
     */
    public NotifyTaskPublisher(ProcessorNotifier processorNotifier) {
        this.processorNotifier = processorNotifier;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SetLinkedBlockingQueue<>());
    }

    /**
     * Creates NotifyTask with current processorNotifier and submit it to executor.
     */
    public void publishNotifyTask(String ccyPair, AtomicStampedReference<Double> actualVersion) {
        executor.submit(new NotifyTask(ccyPair, actualVersion, processorNotifier));
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
