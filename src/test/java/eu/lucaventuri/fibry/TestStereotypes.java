package eu.lucaventuri.fibry;

import eu.lucaventuri.common.SystemUtils;
import eu.lucaventuri.fibry.Actor;
import eu.lucaventuri.fibry.Stereotypes;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TestStereotypes {
    @Test
    public void testWorkers() throws ExecutionException, InterruptedException {
        final AtomicInteger val = new AtomicInteger();
        Supplier<Actor<Integer, Void, Void>> master = Stereotypes.auto().workersCreator(val::addAndGet);

        master.get().sendMessageReturn(1).get();
        master.get().sendMessageReturn(2).get();
        master.get().sendMessageReturn(3).get();

        assertEquals(6, val.get());
    }

    @Test
    public void testWorkersConsumer() throws ExecutionException, InterruptedException {
        final AtomicInteger val = new AtomicInteger();
        Consumer<Integer> master = Stereotypes.auto().workersAsConsumerCreator(n -> {
            System.out.println(n);

            val.addAndGet(n);
        });

        master.accept(1);
        master.accept(2);
        master.accept(3);

        while (val.get() < 6)
            SystemUtils.sleep(100);

        assertEquals(6, val.get());
    }

    @Test
    public void testWorkerWithReturn() throws ExecutionException, InterruptedException {
        final AtomicInteger val = new AtomicInteger();
        Supplier<Actor<Integer, Integer, Void>> master = Stereotypes.auto().workersWithReturnCreator(val::addAndGet);

        assertEquals(1, master.get().sendMessageReturn(1).get().intValue());
        assertEquals(3, master.get().sendMessageReturn(2).get().intValue());
        assertEquals(6, master.get().sendMessageReturn(3).get().intValue());

        assertEquals(6, val.get());
    }

    @Test
    public void testWorkerWithReturnFunction() throws ExecutionException, InterruptedException {
        final AtomicInteger val = new AtomicInteger();
        Function<Integer, CompletableFuture<Integer>> master = Stereotypes.auto().workersAsFunctionCreator(val::addAndGet);

        assertEquals(1, master.apply(1).get().intValue());
        assertEquals(3, master.apply(2).get().intValue());
        assertEquals(6, master.apply(3).get().intValue());

        assertEquals(6, val.get());
    }

    @Test
    public void testMapReducePool() {
        MapReducer<Integer, Integer> mr = Stereotypes.def().mapReduce(PoolParameters.fixedSize(4), (Integer n) -> n * n, Integer::sum, 0);

        mr.map(1,2,3,4,5);
        // 1+4+9+16+25 == 55
        assertEquals(Integer.valueOf(55), mr.get(true));
    }

    @Test
    public void testMapReducePool2() {
        MapReducer<Integer, Integer> mr = Stereotypes.def().mapReduce(PoolParameters.fixedSize(4), (Integer n) -> n * n, Integer::sum, 0);
        int num = 100_000;

        for(int i=0; i<num; i++) {
            mr.map(i%2);
        }

        System.out.println("Messages sent");

        System.out.println(mr.get(true));
        assertEquals(Integer.valueOf(num/2), mr.get(false));
    }

    @Test
    public void testMapReduceSpawner() {
        int res = Stereotypes.def().mapReduce((Integer n) -> n * n, Integer::sum, 0).map(1,2,3,4,5).get(true);

        // 1+4+9+16+25 == 55
        assertEquals(55, res);
    }

    @Test
    public void testMapReduceSpawner2() {
        MapReducer<Integer, Integer> mr = Stereotypes.def().mapReduce((Integer n) -> n * n, Integer::sum, 0);
        int num = 1000; // Too slow without fibers

        for(int i=0; i<num; i++) {
            mr.map(i%2);
        }

        System.out.println(mr.get(true));
        assertEquals(Integer.valueOf(num/2), mr.get(false));
    }
}
