package ru.task.throttler.impl;


import ru.task.throttler.PriceProcessor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import static ru.task.throttler.ThrottlerUtils.createAllCcyPairsMap;


/**
 * Object of ProcessorNotifier makes the best effort to notify PriceProcessor with most recent version of data.
 * It notifies processor only if new version present or else it CPU intensively skip the task and process next task.
 * There can be few ProcessorNotifier with same PriceProcessor in order to introduce the possibility of parallelism,
 * in this case each ProcessorNotifier should be serving only a group of all possible ccyPairs id.
 * It's mandatory because if there had been two ProcessorNotifier, with same processor PriceProcessor instance,
 * those would be calling onPrice method with same ccyPair at the same time it would have been impossible for the actual
 * PriceProcessor to identify which of the two version is "up-to-date version".
 */
public class ProcessorNotifier {

    private final Map<String, AtomicInteger> ccyVersionNotifiedMap;
    private final PriceProcessor processor;

    public ProcessorNotifier(PriceProcessor processor) {
        this.processor = processor;
        this.ccyVersionNotifiedMap = createCcyVersionNotifiedMap();
    }

    /**
     * Method notify (call onPrice method of current processor) serially according to ccyPairs.
     *
     * @param ccyPair          - ccyPair.
     * @param actualCcyRateRef - most recent version of ccyPair's rate with stamp value (version).
     */
    public void notifyRateUpdate(String ccyPair, AtomicStampedReference<Double> actualCcyRateRef) {
        var lastNotifiedVersionRef = ccyVersionNotifiedMap.get(ccyPair);

        var currentVersionHolder = new int[1];
        var currentRate = actualCcyRateRef.get(currentVersionHolder);

        var lastNotifiedVersion = lastNotifiedVersionRef.get();
        var currentVersion = currentVersionHolder[0];
        if (lastNotifiedVersion < currentVersion) {
            lastNotifiedVersionRef.set(currentVersion);
            processor.onPrice(ccyPair, currentRate);
        }
    }

    private Map<String, AtomicInteger> createCcyVersionNotifiedMap() {
        return createAllCcyPairsMap(pair -> new AtomicInteger(0));
    }
}
