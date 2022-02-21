package ru.task.throttler.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.task.throttler.impl.PriceThrottler;
import ru.task.throttler.test.fake.FakePriceProcessor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ThrottlerE2ETests {

    private PriceThrottler throttler;

    @Before
    public void before() {
        throttler = new PriceThrottler();
    }

    @After
    public void after() {
        throttler.shutdown();
    }

    @Test
    public void processCcyPairRate_subscribersShouldReceiveProcessedRate() throws InterruptedException {
        var latch1 = new CountDownLatch(1);
        var latch2 = new CountDownLatch(1);

        var fakePriceProcessor1 = new FakePriceProcessor() {
            @Override
            public void onPrice(String ccyPair, double rate) {
                latch1.countDown();
            }
        };
        var fakePriceProcessor2 = new FakePriceProcessor() {
            @Override
            public void onPrice(String ccyPair, double rate) {
                latch2.countDown();
            }
        };

        throttler.subscribe(fakePriceProcessor1);
        throttler.subscribe(fakePriceProcessor2);

        var ccyPair = "EURUSD";
        var expectedRate = Double.valueOf(1);

        throttler.onPrice(ccyPair, expectedRate);

        assertTrue(latch1.await(2L, TimeUnit.SECONDS));
        assertTrue(latch2.await(2L, TimeUnit.SECONDS));
    }

    @Test
    public void intensivelyUpdateSameCcyPairRateFewTimes_receiverShouldReceiveFewerCalls() throws InterruptedException {
        var expectedRate = 10_000;
        var receiverCallsCount = new AtomicInteger(0);
        var latch = new CountDownLatch(1);

        var fakePriceProcessor = new FakePriceProcessor() {
            @Override
            public void onPrice(String ccyPair, double rate) {
                receiverCallsCount.incrementAndGet();
                if (rate == expectedRate) {
                    latch.countDown();
                }
            }
        };

        throttler.subscribe(fakePriceProcessor);

        var ccyPair = "EURUSD";
        var updateCount = new AtomicInteger(0);

        var thread = new Thread(() -> {
            for (int i = 0; i < expectedRate; i++) {
                throttler.onPrice(ccyPair, updateCount.incrementAndGet());
            }
        });
        thread.start();
        thread.join();

        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(expectedRate, updateCount.get());
        assertTrue(receiverCallsCount.get() < updateCount.get());
    }


    @Test
    public void slowSubscriberShouldNotAffectFastSubscriberTest() throws InterruptedException {
        var slowProcessedCount = new AtomicInteger(0);

        var slowProcessor = new FakePriceProcessor() {
            @Override
            public void onPrice(String ccyPair, double rate) {
                try {
                    Thread.sleep(100000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                slowProcessedCount.incrementAndGet();
            }
        };

        int expectedRateUpdateCount = 1000;

        var latch = new CountDownLatch(1);

        var fastProcessor = new FakePriceProcessor() {
            @Override
            public void onPrice(String ccyPair, double rate) {
                if (rate == expectedRateUpdateCount) {
                    latch.countDown();
                }
            }
        };

        throttler.subscribe(slowProcessor);
        throttler.subscribe(fastProcessor);

        var ccyPair = "EURUSD";
        var thread = new Thread(() -> {
            for (int i = 0; i <= expectedRateUpdateCount; i++) {
                throttler.onPrice(ccyPair, i);
            }
        });
        thread.start();
        thread.join();

        assertTrue(latch.await(5L, TimeUnit.SECONDS));
    }

}
