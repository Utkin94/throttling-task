package ru.task.throttler.test.fake;


import ru.task.throttler.PriceProcessor;


public abstract class FakePriceProcessor implements PriceProcessor {

    public FakePriceProcessor() {
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
    }
}
