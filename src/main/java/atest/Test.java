package atest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;

public class Test {
    static FooCompletion<String> Listener = new FooCompletion<String>() {
        @Override
        public void success(String result) {
            System.out.println(Thread.currentThread() + " success: " + result);
        }
        @Override
        public void failure(FooException exception) {
            System.out.println(Thread.currentThread() + " failure: " + exception);
        }
    };

    public static void main(String[] args) throws SuspendExecution, Exception {
        System.setProperty("co.paralleluniverse.fibers.verifyInstrumentation", "true");
        test7_100FibersWithFiberSleep();
    }

    public static void test7_100FibersWithFiberSleep() throws InterruptedException {
        System.setProperty("co.paralleluniverse.fibers.detectRunawayFibers", "false");
        //Then fiber callback
        System.out.println("Started fiber callback on main thread");
        for (int i = 0; i < 10; i++) {
            final FooClient2 foo = new FooClient2(Listener);
            final Fiber<Void> f = new Fiber<Void>() {
                private static final long serialVersionUID = -6534731847688655231L;

                @Override
                protected Void run() throws SuspendExecution, InterruptedException {
                    try {
                        (foo).op("FooClient2");
                    } catch (FooException e) {
                        Listener.failure(e);
                    }
                    return null;
                }
            };
            f.start();
        }
        System.out.println("Finished fiber callback main thread, pausing 5 seconds");
        Thread.sleep(50000L);
        System.out.println("Exiting test on main thread");
    }

    public static void test6_100AsyncToSync() throws InterruptedException {
        System.setProperty("co.paralleluniverse.fibers.detectRunawayFibers", "false");
        //First normal callback
        System.out.println("Started normal callback on main thread");
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < 100; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    FooClient1 inst = new FooClient1(Listener);
                    try {
                        inst.op("hello");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (FooException e) {
                        inst.c.failure(e);
                    } catch (SuspendExecution e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }
        pool.shutdown();
        System.out.println("Finished normal callback main thread, pausing 50 seconds");
        Thread.sleep(50000L);
        
        //Then fiber callback
        System.out.println("Started fiber callback on main thread");
        for (int i = 0; i < 100; i++) {
            new Fiber<Void>() {
                private static final long serialVersionUID = -6534731847688655231L;

                @Override
                protected Void run() throws SuspendExecution, InterruptedException {
                    try {
                        (new FooClient1(Listener)).op("FooClient1");
                    } catch (FooException e) {
                        Listener.failure(e);
                    }
                    return null;
                }
            }.start();
        }
        System.out.println("Finished fiber callback main thread, pausing 5 seconds");
        Thread.sleep(50000L);
        System.out.println("Exiting test on main thread");
    }

    public static void test5_AsyncToSync() throws InterruptedException {
        //First normal callback
        System.out.println("Started normal callback on main thread");
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                FooClient1 inst = new FooClient1(Listener);
                try {
                    inst.op("hello");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (FooException e) {
                    inst.c.failure(e);
                } catch (SuspendExecution e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        pool.shutdown();
        System.out.println("Finished normal callback main thread, pausing 5 seconds");
        Thread.sleep(5000L);
        
        //Then fiber callback
        System.out.println("Started fiber callback on main thread");
        new Fiber<Void>() {
            private static final long serialVersionUID = -6534731847688655231L;

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                try {
                    (new FooClient1(Listener)).op("FooClient1");
                } catch (FooException e) {
                    Listener.failure(e);
                }
                return null;
            }
        }.start();
        System.out.println("Finished fiber callback main thread, pausing 5 seconds");
        Thread.sleep(50000L);
        System.out.println("Exiting test on main thread");
    }

    static class FooException extends Exception {
        private static final long serialVersionUID = 1321620529753968499L;
    }
    interface FooClient {
        String op(String arg) throws FooException, InterruptedException, SuspendExecution;
    }
    interface FooCompletion<T> {
        void success(T result);
        void failure(FooException exception);
    }
    interface AsyncFooClient {
          Future<String> asyncOp(String arg, FooCompletion<String> callback);
    }

    static class FooClient2 extends FooClient1 {

        public FooClient2(FooCompletion<String> c) {
            super(c);
        }

        protected void pause(long l) throws InterruptedException, SuspendExecution {
            Fiber.sleep(l);
        }
    }

    static class FooClient1 implements FooClient {
        static AtomicInteger Counter = new AtomicInteger();
        private final FooCompletion<String> c;
        private final int count;

        public FooClient1(FooCompletion<String> c) {
            this.c = c;
            this.count = Counter.incrementAndGet();
        }

        @Override
        public String op(String arg) throws FooException, InterruptedException, SuspendExecution {
            long start = System.currentTimeMillis();
            System.out.println(this.count+" Started op, will take 2 seconds to complete");
            pause(2000L);
            long end = System.currentTimeMillis();
            System.out.println(this.count+" Completed op "+arg + " in (ms) " +(end-start));
            this.c.success(this.count+" done "+arg);
            return null;
        }

        protected void pause(long l) throws InterruptedException, SuspendExecution {
            Thread.sleep(l);
        }
        
    }

    public static void test4_2FibersWithReturns() throws ExecutionException,
            InterruptedException {
        Fiber<Boolean> f1 = new Fiber<Boolean>() {
            private static final long serialVersionUID = -2177109702031132993L;

            @Override
            protected Boolean run() throws SuspendExecution,
                    InterruptedException {
                System.out.println("Hi");
                return true;
            }
        };
        Fiber<Boolean> f2 = new Fiber<Boolean>() {
            private static final long serialVersionUID = 1415576028721937840L;

            @Override
            protected Boolean run() throws SuspendExecution,
                    InterruptedException {
                System.out.println("Lo");
                return false;
            }
        };
        f1.start();
        f2.start();
        System.out.println(f1.get());
        System.out.println(f2.get());
        f1.join();
        f2.join();
    }

    public static void test3_AFiberWithSuspendableCallable() {
        SuspendableCallable<Object> s = new SuspendableCallable<Object>() {
            private static final long serialVersionUID = -2775995918124813040L;

            @Override
            public Object run() throws SuspendExecution, InterruptedException {
                System.out.println("Hi");
                return null;
            }
        };
        new Fiber<Object>(s).start();
    }

    public static void test2_AFiberWithSuspendableRunnable() {
        SuspendableRunnable s = new SuspendableRunnable() {
            private static final long serialVersionUID = 3105618376533859114L;

            @Override
            public void run() throws SuspendExecution, InterruptedException {
                System.out.println("Hi");
            }
        };
        new Fiber<Void>(s).start();
    }

    public static void test1_AFiber() {
        new Fiber<Void>() {
            private static final long serialVersionUID = 2235156549710445873L;

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                System.out.println("Hi");
                return null;
            }
        }.start();
    }

}