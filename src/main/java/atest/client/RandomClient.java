package atest.client;

import java.util.Random;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;

public class RandomClient {
    
    private final long timeToWait;
    private final Random random;
    
    public RandomClient(long timeToWait) {
        this.timeToWait = timeToWait;
        this.random = new Random();
    }

    public void fetchRandom(RandomClientCallback callback) throws InterruptedException, SuspendExecution {
        System.out.println("Fetching a random number. Working for " + timeToWait + "ms");
        Strand.sleep(timeToWait);

        final int result = this.random.nextInt(100);
        
        if (result < 5) {
            callback.onError(new RandomClientException("Result was less than 5: " + result));
        }
        
        callback.onSuccess(result);
    }
    
}
