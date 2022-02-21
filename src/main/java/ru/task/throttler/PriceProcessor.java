package ru.task.throttler;

public interface PriceProcessor {
    /**
     * Call from an upstream
     * <p>
     * You can assume that only correct data comes in here - no need for extra validation
     *
     * @param ccyPair - EURUSD, EURRUB, USDJPY - up to 200 different currency pairs
     * @param rate    - any double rate like 1.12, 200.23 etc
     */
    void onPrice(String ccyPair, double rate);

    /**
     * Subscribe for updates
     * <p>
     * Called rarely during operation of PriceProcessor
     *
     * @param priceProcessor - can be up to 200 subscribers
     */
    void subscribe(PriceProcessor priceProcessor);

    /**
     * Unsubscribe from updates
     * <p>
     * Called rarely during operation of PriceProcessor
     *
     * @param priceProcessor
     */
    void unsubscribe(PriceProcessor priceProcessor);
}

