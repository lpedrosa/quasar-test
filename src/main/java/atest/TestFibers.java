package atest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import atest.client.RandomClient;
import atest.client.RandomClientCallback;
import atest.client.RandomClientException;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;import co.paralleluniverse.strands.Strand;


public final class TestFibers {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("co.paralleluniverse.fibers.verifyInstrumentation", "true");
        
        System.out.println("::: SAME THREAD VERSION");
        testRandomClientCallback();
        
        System.out.println("::: THREAD POOL VERSION");
        testRandomClientThreaded();

        System.out.println("::: FIBER VERSION");
        testRandomClientFiber();
    }
    
    private static void testRandomClientCallback() throws InterruptedException {
        RandomClient client = new RandomClient(3000);
        
        System.out.println("Calling RandomClient with provided callback");

        try {
            client.fetchRandom(CALLBACK);
        } catch (SuspendExecution ex) {
            throw new AssertionError("Shouldn't happen!");
        }
        
        System.out.println("Finished calling client...");
    }
    
    private static void testRandomClientThreaded() throws InterruptedException {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final RandomClient client = new RandomClient(3000);
        
        System.out.println("Calling RandomClient with provided callback");
        pool.submit(() -> {
            try { 
                client.fetchRandom(CALLBACK);
            } catch (InterruptedException ex) { 
                Strand.currentStrand().interrupt();
            } catch (SuspendExecution ex) {
                throw new AssertionError("Shouldn't happen!");
            }
        });
        System.out.println("Finished calling client...");

        pool.shutdown();
        pool.awaitTermination(5000, TimeUnit.MILLISECONDS);
    }
    
    private static void testRandomClientFiber() {
        
        final RandomClient client = new RandomClient(3000);
        
        System.out.println("Calling RandomClient with provided callback");
        
        new Fiber<Void>(() -> {
            try { 
                client.fetchRandom(CALLBACK);
            } catch (InterruptedException ex) { 
                Strand.currentStrand().interrupt(); 
            }
        }).start();

        System.out.println("Finished calling client...");
    }
    
    private static RandomClientCallback CALLBACK = new RandomClientCallback() {
            @Override
            public void onSuccess(int random) {
                System.out.println("Got a random! " + random);
            }
            
            @Override
            public void onError(RandomClientException exception) {
                System.out.println("Got error! " + exception.getMessage());
            }
    };

}
