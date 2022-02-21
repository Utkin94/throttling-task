package ru.task.throttler.impl;


import ru.task.throttler.PriceProcessor;
import ru.task.throttler.ShutdownHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicStampedReference;

import static ru.task.throttler.ThrottlerUtils.DISTRIBUTORS_COUNT;
import static ru.task.throttler.ThrottlerUtils.createAllCcyPairsMap;


public class PriceThrottler implements PriceProcessor, ShutdownHook {

    /**
     * Map with last versions of ccyPairs.
     * It can be non-synchronized because of the fact we know by advance all possible ccyPairs before application startup.
     */
    private final Map<String, AtomicStampedReference<Double>> actualCcyRates;

    /**
     * List of async PriceDistributors, those are used to make calls of onPrice faster.
     * We can create more distributors to be able to parallelize distribution process.
     */
    private final List<PriceDistributor> priceDistributors;

    public PriceThrottler() {
        this(
                createAllCcyPairsMap(pair -> new AtomicStampedReference<>(null, 0)),
                initPriceDistributors()
        );
    }

    public PriceThrottler(Map<String, AtomicStampedReference<Double>> actualCcyRates, List<PriceDistributor> priceDistributors) {
        this.actualCcyRates = actualCcyRates;
        this.priceDistributors = priceDistributors;
    }

    /**
     * Method should not be called concurrently with same ccyPair, otherwise it will be impossible to determine the
     * up-to-date version of ccyPair's rate. Based on this assumption it's safe to use AtomicStampedReference here without
     * synchronization.
     *
     * @param ccyPair - EURUSD, EURRUB, USDJPY - up to 200 different currency pairs
     * @param rate    - any double rate like 1.12, 200.23 etc
     */
    @Override
    public void onPrice(String ccyPair, double rate) {
        var actualRateRef = actualCcyRates.get(ccyPair);
        actualRateRef.set(rate, actualRateRef.getStamp() + 1);

        var distributorIndex = ccyPair.hashCode() % DISTRIBUTORS_COUNT;
        priceDistributors.get(distributorIndex).distributeRateUpdate(ccyPair, actualRateRef);
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        for (var priceDistributor : priceDistributors) {
            priceDistributor.addSubscriber(priceProcessor);
        }
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        for (var priceDistributor : priceDistributors) {
            priceDistributor.removeSubscriber(priceProcessor);
        }
    }

    @Override
    public void shutdown() {
        priceDistributors.forEach(PriceDistributor::shutdown);
    }

    private static ArrayList<PriceDistributor> initPriceDistributors() {
        var priceDistributors = new ArrayList<PriceDistributor>();
        for (int i = 0; i < DISTRIBUTORS_COUNT; i++) {
            priceDistributors.add(new PriceDistributor());
        }

        return priceDistributors;
    }
}
