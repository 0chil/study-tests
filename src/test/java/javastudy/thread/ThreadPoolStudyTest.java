package javastudy.thread;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ThreadPoolStudyTest {

    private AtomicInteger threadCounter;
    private Set<Long> threadIds;

    @Test
    void Executor_생성시에_쓰레드가_생성되지_않는다() throws InterruptedException {
        threadCounter = new AtomicInteger(0);
        threadIds = new CopyOnWriteArraySet<>();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                0, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        assertThat(pool.getPoolSize()).isEqualTo(0);
    }

    @Test
    void 작업_실행_시에_워커가_생성된다() throws InterruptedException {
        threadCounter = new AtomicInteger(0);
        threadIds = new CopyOnWriteArraySet<>();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                0, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        pool.execute(() -> {
        });

        assertThat(pool.getPoolSize()).isEqualTo(1);
    }

    @Test
    void 코어_풀이_2_맥스_풀이_10_큐_크기_미지정시_왠만하면_쓰레드가_늘어나지_않는다() throws InterruptedException {
        threadCounter = new AtomicInteger(0);
        threadIds = new CopyOnWriteArraySet<>();
        int queueSize = Integer.MAX_VALUE; // 미지정 시 Integer.MAX_VALUE가 된다. (왠만하면 큐를 넘지 않는다)
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize));

        for (int i = 0; i < 100; i++) {
            pool.submit(this::countFunction);
        }

        watch(pool);
        assertThat(threadIds).hasSize(2);
    }

    @Test
    void 큐가_꽉_찬_이후부터_쓰레드가_늘어난다() throws InterruptedException {
        threadCounter = new AtomicInteger(0);
        threadIds = new CopyOnWriteArraySet<>();
        int queueSize = 10;
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize)); // 삽입한 작업만으로 큐가 꽉 차도록 10으로 한다

        for (int i = 0; i < 2; i++) {
            pool.submit(this::countFunctionWithDelay);
            // 쓰레드를 채운다
        }
        for (int i = 0; i < queueSize; i++) {
            pool.submit(this::countFunctionWithDelay);
            // 큐를 채운다
        }
        for (int i = 0; i < 3; i++) {
            pool.submit(this::countFunctionWithDelay);
            // 작업을 추가로 보낸다, 보낸 수 만큼 쓰레드가 추가로 만들어진다.
        }
        watch(pool);
        pool.awaitTermination(5L, TimeUnit.SECONDS);

        assertThat(threadIds).hasSize(5);
    }

    @Test
    void 싱글_쓰레디드_쓰레드풀은_작업을_순차적으로_실행한다() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            System.out.println("started");
            sleepUnsafely(500);
            System.out.println("ended");
        });
        executorService.submit(() -> {
            System.out.println("started");
            sleepUnsafely(500);
            System.out.println("ended");
        });
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void 쓰레드풀은_같은_task를_여러번_주어도_괜찮다() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Runnable task = () -> {
            System.out.println(Thread.currentThread().getId() + "started");
            sleepUnsafely(500);
            System.out.println(Thread.currentThread().getId() + "ended");
        };
        executorService.submit(task);
        executorService.submit(task);
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void watch(ThreadPoolExecutor pool) throws InterruptedException {
        Thread watcher = new Thread(() -> {
            while (!pool.getQueue().isEmpty()) {
                System.out.println("[watcher] queueSize: " + pool.getQueue().size());
                System.out.println("[watcher] poolSize == threadSize == workerSize: " + pool.getPoolSize());
                sleepUnsafely(1000);
            }
        });
        watcher.start();
        watcher.join();
    }

    private void countFunction() {
        threadIds.add(Thread.currentThread().getId());
        int count = threadCounter.incrementAndGet();
        System.out.print("[Thread-" + Thread.currentThread().getId() + "] ");
        System.out.println("start " + count);
        System.out.print("[Thread-" + Thread.currentThread().getId() + "] ");
        System.out.println("end " + count);
    }

    private void countFunctionWithDelay() {
        threadIds.add(Thread.currentThread().getId());
        int count = threadCounter.incrementAndGet();
        System.out.print("[Thread-" + Thread.currentThread().getId() + "] ");
        System.out.println("start " + count);
        sleepUnsafely(1000);
        System.out.print("[Thread-" + Thread.currentThread().getId() + "] ");
        System.out.println("end " + count);
    }

    private void sleepUnsafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
