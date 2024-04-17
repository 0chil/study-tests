package javastudy.memory_visibility;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class VolatileStudyTest {

    @Nested
    class 일반_변수의_변경은 {

        private boolean busy = true;
        private volatile Boolean whatWaiterSees = null;

        @Test
        void 다른_쓰레드에서_보이지_않을_수도_있다() throws InterruptedException {
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

    @Nested
    class Volatile_변수의_변경은 {


        private volatile boolean busy = true;
        private volatile Boolean whatWaiterSees = null;

        @Test
        void 다른_쓰레드에서_즉시_보인다() throws InterruptedException {
            Thread waiter = new Thread(() -> {
                whatWaiterSees = this.busy;
                while (this.busy) {
                }
                whatWaiterSees = this.busy;
            });
            waiter.start();
            Thread.sleep(500);

            busy = false;

            waiter.join(500);
            assertThat(whatWaiterSees).isFalse();
            assertThat(waiter.isAlive()).isFalse();
        }
    }

    private void sleepUnsafely(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }
}
