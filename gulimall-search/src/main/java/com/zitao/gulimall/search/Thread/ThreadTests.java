package com.zitao.gulimall.search.Thread;

import java.util.Random;
import java.util.concurrent.*;

public class ThreadTests {

    /**
     * 新建线程池
     * 1. Executors
     */
    private static ExecutorService service = Executors.newFixedThreadPool(5);

    /**
     * 新建线程池
     * 2. ThreadPoolExecutor
     * <p>
     * 工作顺序:
     * 1. 线程池创建，准备好corePoolSize数量的核心线程，准备接受任务
     * 2. 当核心线程都被占用后，新任务进入组赛队列中，空闲的core线程会去阻塞队列里获取任务执行
     * 3. 阻塞队列满后，就开新线程执行，但最大不能超过maximumPoolSize数量
     * 4. max值满后就用RejectedExecutionHandler拒绝任务
     * 5. max都执行完成，有很多空闲线程就会在指定的keepAliveTime后释放，但core线程永不释放
     */
    public void ThreadPoolExecutorTest() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10000), // 阻塞队列
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
        );
    }

    /**
     * run: 不接收结果
     * accept: 接受上一步的返回结果 自身不返回新的结果
     * apply: 接受上一步的返回结果 自身返回新的结果
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        runAsync();
        CompletableFuture<Long> future = supplyAsync();
        whenComplete();
        thenApply();
        handle();
        thenAccept();
        thenRun();
        thenCombine();
        thenAcceptBoth();
        applyToEither();
        acceptEither();
        System.out.println("主线程执行完成");
    }


    /**
     * CompletableFuture.runAsync() 无返回值
     *
     * @return
     * @throws Exception
     */
    public static CompletableFuture<Void> runAsync() throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {

            }
            System.out.println("run end ..." + Thread.currentThread().getId());
        }, service);
        return future;
    }

    /**
     * CompletableFuture.supplyAsync() 有返回值
     *
     * @return
     * @throws Exception
     */
    public static CompletableFuture<Long> supplyAsync() throws Exception {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {

            }
            System.out.println("run end ..." + Thread.currentThread().getId());
            return 1L;
        });
        System.out.println(future.get());
        return future;
    }

    /**
     * whenCompleteAsync 接收异步任务的结果和异常
     * exceptionally 出现异常后的处理
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void whenComplete() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("run start ...");
                TimeUnit.SECONDS.sleep(1);
                Long i = 1000L / 0;
            } catch (InterruptedException e) {

            }
            return 1L;
        }, service).whenCompleteAsync((res, exception) -> {
            System.out.println("结果完成" + res); // null
            System.out.println("出现异常:" + exception); // java.lang.ArithmeticException: / by zero
        }).exceptionally(t -> {
            // 处理异常信息: 感知异常并且返回默认值
            System.out.println(t.getMessage()); // java.lang.ArithmeticException: / by zero
            return 10L;
        });
        System.out.println("run end ..." + future.get()); // 10
    }

    private static void thenApply() throws Exception {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("run start ...");
                TimeUnit.SECONDS.sleep(1);
                Long i = 1000L / 3;
            } catch (InterruptedException e) {
            }
            return 1L;
        }, service).thenApply(res -> res * 2);
        System.out.println(future.get());
    }

    /**
     * handle 方法执行完成后的处理
     *
     * @throws Exception
     */
    private static void handle() throws Exception {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("run start ...");
            Long i = 1000L / 0;
            return i;
        }, service).handle((res, thr) -> {
            System.out.println(thr.getMessage()); // java.lang.ArithmeticException: / by zero
            if (res != null) {
                return res * 2;
            }
            return 0l;
        });
        System.out.println(future.get()); // 0
    }

    /**
     * thenAccept  线程串行化方法 感知上一步的执行结果 但无返回值 共用同一个线程
     * thenAcceptAsync 指定了线程池 则开启一个新的线程
     *
     * @throws Exception
     */
    private static void thenAccept() throws Exception {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("run start ...");
                TimeUnit.SECONDS.sleep(1);
                Long i = 1000L / 3;
                return 1L;
            } catch (InterruptedException e) {
                return 2L;
            }
        }, service).thenAcceptAsync(res -> {
            System.out.println(res);
        }, service);
    }

    /**
     * thenRun 线程串行化方法 不需要上一步的执行结果
     *
     * @throws Exception
     */
    private static void thenRun() throws Exception {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("run start ...");
                TimeUnit.SECONDS.sleep(1);
                Long i = 1000L / 0;
                return 1L;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return -1L;
            }
        }, service).thenRun(() -> {
            System.out.println("thenRun方法执行了，，");
        });
        System.out.println(future.get());
    }

    /**
     * thenCombine 合并多个任务 既能拿到之前任务的结果 并且再返回新的数据
     *
     * @throws Exception
     */
    private static void thenCombine() throws Exception {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "hello1");
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "hello2");
        CompletableFuture<String> result = future1.thenCombine(future2, (t, u) -> t + " " + u);
        System.out.println(result.get());
    }

    /**
     * thenAcceptBoth 两个一步任务都完成后开启新的任务 并且接收f1 f2的结果
     *
     * @throws Exception
     */
    private static void thenAcceptBoth() throws Exception {
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            System.out.println("f1=" + t);
            return t;
        }, service);

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            System.out.println("f2=" + t);
            return t;
        }, service);

        f1.thenAcceptBoth(f2, (future1, future2) -> {
            System.out.println("任务三开始...之前的结果: " + future1 + "->" + future2);
        });
    }

    /**
     * applyToEither 两个任务只要有一个完成就开始执行任务3
     *
     * @throws Exception
     */
    private static void applyToEither() throws Exception {
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            try {
                TimeUnit.SECONDS.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("f1=" + t);
            return t;
        }, service);
        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            try {
                TimeUnit.SECONDS.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("f2=" + t);
            return t;
        }, service);

        CompletableFuture<Integer> result = f1.applyToEither(f2, res -> {
            System.out.println("applyEither:" + res);
            return res * 2;
        });

    }

    /**
     * acceptEither 两个任务只要有一个完成就开始执行任务3
     *
     * @throws Exception
     */
    private static void acceptEither() throws Exception {
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            try {
                TimeUnit.SECONDS.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("f1=" + t);
            return t;
        }, service);
        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> {
            int t = new Random().nextInt(3);
            try {
                TimeUnit.SECONDS.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("f2=" + t);
            return t;
        }, service);

        CompletableFuture<Void> result = f1.acceptEither(f2, res -> {
            System.out.println("acceptEither:" + res);
        });

    }
}
