package jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;

import static jpa.Fixture.JAVAJIGI;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(ReplaceUnderscores.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class TransactionStudyTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    // DB 트랜잭션에 영향을 주는가?, 구구가 그렇다 하였음
    // 참고: https://www.marcobehler.com/guides/spring-transaction-management-transactional-in-depth
    @Test
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    void Transactional_어노테이션의_Isolation은_DB에_전파되는_속성이다() {
        // connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        // 위 구문이 수행된다.
        Session session = entityManager.unwrap(Session.class);
        Integer connectionIsolationLevel = session.doReturningWork(Connection::getTransactionIsolation);

        assertThat(connectionIsolationLevel).isEqualTo(Connection.TRANSACTION_READ_UNCOMMITTED);
        // 눈여겨봐야할 점은 어플리케이션 레벨의 트랜잭션이 아니라, DB 레벨의 트랜잭션에 적용된다는 점이다.
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void READ_UNCOMMITTED는_DB_트랜잭션에_전파되므로_내용을_다른_트랜잭션에서_읽을_수_있다() throws InterruptedException {
        User user = Fixture.JAVAJIGI();
        userRepository.save(user);

        CountDownLatch updateLatch = new CountDownLatch(1);
        CountDownLatch readLatch = new CountDownLatch(1);

        Thread updateThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            transactionTemplate.execute(status -> {
                User found = entityManager.find(User.class, user.getId());
                found.setName("땡칠");
                entityManager.flush();
                updateLatch.countDown();
                try {
                    readLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("읽기 쓰레드를 기다리는 중 오류");
                }
                status.setRollbackOnly();
                return null;
            });
        });

        Thread readThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            transactionTemplate.execute(status -> {
                try {
                    updateLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("쓰기 쓰레드를 기다리는 중 오류");
                }
                User found = entityManager.find(User.class, user.getId());
                assertThat(found.getName()).isEqualTo("땡칠");
                readLatch.countDown();
                return null;
            });
        });

        updateThread.start();
        readThread.start();

        updateThread.join();
        readThread.join();
        userRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void READ_COMMITTED는_DB_트랜잭션에_전파되므로_내용을_다른_트랜잭션에서_읽을_수_없다() throws InterruptedException {
        User user = Fixture.JAVAJIGI();
        userRepository.save(user);

        CountDownLatch updateLatch = new CountDownLatch(1);
        CountDownLatch readLatch = new CountDownLatch(1);

        Thread updateThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            transactionTemplate.execute(status -> {
                User found = entityManager.find(User.class, user.getId());
                found.setName("땡칠");
                entityManager.flush();
                updateLatch.countDown();
                try {
                    readLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("읽기 쓰레드를 기다리는 중 오류");
                }
                status.setRollbackOnly();
                return null;
            });
        });

        Thread readThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            transactionTemplate.execute(status -> {
                try {
                    updateLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("쓰기 쓰레드를 기다리는 중 오류");
                }
                User found = entityManager.find(User.class, user.getId());
                assertThat(found.getName()).isNotEqualTo("땡칠");
                readLatch.countDown();
                return null;
            });
        });

        updateThread.start();
        readThread.start();

        updateThread.join();
        readThread.join();
        userRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void 격리_레벨은_읽는_쪽에서만_상관있다() throws InterruptedException {
        User user = Fixture.JAVAJIGI();
        userRepository.save(user);

        CountDownLatch updateLatch = new CountDownLatch(1);
        CountDownLatch readLatch = new CountDownLatch(1);

        Thread updateThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
            transactionTemplate.execute(status -> {
                User found = entityManager.find(User.class, user.getId());
                found.setName("땡칠");
                entityManager.flush();
                updateLatch.countDown();
                try {
                    readLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("읽기 쓰레드를 기다리는 중 오류");
                }
                status.setRollbackOnly();
                return null;
            });
        });

        Thread readThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            transactionTemplate.execute(status -> {
                try {
                    updateLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("쓰기 쓰레드를 기다리는 중 오류");
                }
                User found = entityManager.find(User.class, user.getId());
                assertThat(found.getName()).isEqualTo("땡칠");
                readLatch.countDown();
                return null;
            });
        });

        updateThread.start();
        readThread.start();

        updateThread.join();
        readThread.join();
        userRepository.deleteAll();
    }

    @Test
    void 스프링_TX는_어느시점에_시작되는가() {
        // 트랜잭션 시작 부분에서 setAutoCommit(false), IsolationLevel 등 각종 설정을 한다.
        // doBegin() 함수
        assertThat(TestTransaction.isActive()).isTrue();

        User user = Fixture.JAVAJIGI();
        userRepository.save(user);

        TestTransaction.flagForRollback(); // 수동으로 트랜잭션을 종료한다
        TestTransaction.end();

        assertThat(TestTransaction.isActive()).isFalse();
    }

    @Test
    void 트랜잭션이_다르면_같은_데이터의_인스턴스는_다르다() throws InterruptedException {
        User user = JAVAJIGI();
        userRepository.saveAndFlush(user);

        Thread readThread = new Thread(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(new JpaTransactionManager(entityManager.getEntityManagerFactory()));
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            transactionTemplate.execute(status -> {
                User found = entityManager.find(User.class, user.getId());
                assertThat(found.getEmail()).isNotEqualTo("A");
                assertThat(found).isNotSameAs(user);
                return null;
            });
        });

        readThread.start();
        readThread.join();
    }
}
