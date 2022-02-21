package ru.task.throttler.impl;


import ru.task.throttler.PriceProcessor;
import ru.task.throttler.ShutdownHook;
import ru.task.throttler.impl.publisher.NotifyTaskPublisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicStampedReference;

import static ru.task.throttler.ThrottlerUtils.NOTIFIERS_PER_PROCESSOR_COUNT;


public class PriceDistributor implements ShutdownHook {

    private final ConcurrentHashMap<PriceProcessor, List<NotifyTaskPublisher>> processorNotifyTaskPublishers;

    private final ExecutorService executor;

    public PriceDistributor() {
        this.processorNotifyTaskPublishers = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        processorNotifyTaskPublishers.values().stream()
                .flatMap(Collection::stream)
                .forEach(NotifyTaskPublisher::shutdown);
    }

    public void distributeRateUpdate(String ccyPair, AtomicStampedReference<Double> actualRateRef) {
        executor.submit(() -> syncDistributeRateUpdate(ccyPair, actualRateRef));
    }

    /**
     * Distributes ccyPairs' updates over all ProcessesNotifiers base on ccyPair hashCode to decrease contentions
     * between different ccyPair updates.
     */
    private void syncDistributeRateUpdate(String ccyPair, AtomicStampedReference<Double> actualVersion) {
        for (var notifyTaskPublishers : processorNotifyTaskPublishers.values()) {
            var notifiedIndex = identifyNotifierIndex(ccyPair);
            var notifyTaskPublisher = notifyTaskPublishers.get(notifiedIndex);

            notifyTaskPublisher.publishNotifyTask(ccyPair, actualVersion);
        }
    }

    public void addSubscriber(PriceProcessor priceProcessor) {
        var taskPublishers = new ArrayList<NotifyTaskPublisher>();
        for (int i = 0; i < NOTIFIERS_PER_PROCESSOR_COUNT; i++) {
            taskPublishers.add(new NotifyTaskPublisher(new ProcessorNotifier(priceProcessor)));
        }

        processorNotifyTaskPublishers.put(priceProcessor, taskPublishers);
    }

    public void removeSubscriber(PriceProcessor priceProcessor) {
        var removedNotifyTaskPublishers = processorNotifyTaskPublishers.remove(priceProcessor);

        for (var removedNotifierExecutor : removedNotifyTaskPublishers) {
            removedNotifierExecutor.shutdown();
        }
    }

    private int identifyNotifierIndex(String ccyPair) {
        return ccyPair.hashCode() % NOTIFIERS_PER_PROCESSOR_COUNT;
    }
}
