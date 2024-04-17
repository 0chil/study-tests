package javastudy.memory_visibility;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MemoryVisibilityStudyTest {

    @Nested
    class 코드_재배치로_ {

        private boolean ready = false;
        private int number = 0;
        private int number2 = 0;

        @RepeatedTest(300)
        void 메모리_가시성_문제가_발생할_수_있다() throws InterruptedException {
            Thread writer = new Thread(() -> {
                number = 1;
                number2 = 2;
                ready = true; // 이 세 개의 액션에서 재배치가 발생한 경우
            });
            Thread reader = new Thread(() -> {
                while (true) {
                    if (ready) {
                        if (number == 0 || number2 == 0) {
                            System.err.println("메모리 가시성 문제 발견"); // 이 구문이 실행될 수 있다
                        }
                    }
                }
            });
            writer.start();
            reader.start();

            writer.join(500);
            reader.join(500);
        }
    }

    @Nested
    class CPU_CACHE_불일치로_ {

        private boolean busy = true;
        private volatile Boolean whatWaiterSees = null;

        @Test
        void 메모리_가시성_문제가_발생할_수_있다() throws InterruptedException {
            Thread waiter = new Thread(() -> {
                whatWaiterSees = this.busy; // 처음 관측한 상태를 기록한다
                while (this.busy) ; // busy == false가 될 때까지 busy waiting(쓰레드를 계속 점유) 한다.
                whatWaiterSees = this.busy; // busy == false가 되었을 때 관측한 상태를 기록한다
            });
            waiter.start();
            Thread.sleep(500); // waiter 쓰레드가 온전히 실행될 때까지 충분한 시간을 준다

            busy = false;

            waiter.join(500); // 위 변경사항이 전파되고 쓰레드가 종료될 시간을 500ms 정도 준다.

            assertThat(whatWaiterSees).isTrue(); // 그러나 waiter는 변경사항을 읽지 못했으며
            assertThat(waiter.isAlive()).isTrue(); // 쓰레드도 종료되지 못했다.
        }
    }
}
