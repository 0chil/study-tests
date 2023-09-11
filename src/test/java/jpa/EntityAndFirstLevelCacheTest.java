package jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.time.LocalDateTime;
import java.util.Optional;

import static jpa.Fixture.JAVAJIGI;
import static jpa.Fixture.땡칠;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(ReplaceUnderscores.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class EntityAndFirstLevelCacheTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Test
    void 유저가_저장된_후_ID가_초기화_된다() {
        User user = JAVAJIGI();

        User save = userRepository.save(user);

        assertThat(save.getId()).isNotNull();
    }

    @Test
    void 저장_전_후_엔티티_래퍼런스는_동일하다() {
        User user = JAVAJIGI();

        User save = userRepository.save(user);

        assertThat(user).isEqualTo(save);
    }

    @Test
    void 유저의_변경사항이_자동으로_반영된다() {
        User user = JAVAJIGI();
        userRepository.save(user);

        user.setUserId("땡칠");
        userRepository.flush();

        final String userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM member where id = ?",
                String.class,
                user.getId()
        );
        assertThat(userId).isEqualTo("땡칠");
    }

    @Test
    void 유저의_PK값이_아닌_컬럼으로_엔티티를_조회해도_같은_인스턴스를_반환한다() {
        // Hibernate의 1차 캐시는 Entity의 Id 컬럼을 기준으로 인스턴스를 캐싱한다.
        // 별도의 컬럼으로 엔티티를 조회해도 같은 인스턴스가 반환될까?
        User user = 땡칠();
        userRepository.save(user);

        final Optional<User> found = userRepository.findByUserId("0chil");
        assertThat(found).get().isSameAs(user);

        // findByUserId 수행 시 SELECT 쿼리가 수행된다. [사실]
        // 인스턴스를 1차 캐시에서 가져올텐데 왜 SELECT 쿼리가 수행되는가?
        // -> 유저 아이디 0chil로는 1차 캐시가 hit 되지 않는다.
        // -> 따라서 0chil row를 조회하고 나서야 1차 캐시에 hit 된다는 사실을 알 수 있다.
        // -> Hibernate는 여기서 객체를 새로 생성하지 않고, 기존 객체를 반환한다.
        // 따라서 한 트랜잭션 안에서 객체의 동일성이 보장된다.
        // 이는 의도된 기능으로, 메모리 상의 객체와 영속화된 객체 사이의 간극을 줄여준다.
        // ORM의 목적 중 하나라고 할 수 있다.
    }

    @Test
    void 캐시가_hit되면_쿼리를_날리지_않는다() {
        User user = JAVAJIGI();
        System.out.println("userRepository.findAll() = " + userRepository.findAll());
        userRepository.save(user);

        assertThat(entityManager.unwrap(Session.class).contains(user)).isTrue();
    }

    @Test
    void 유저를_생성해도_생성시간은_채워지지않는다() {
        final User user = JAVAJIGI();

        assertThat(user.getTimeLog().getCreatedAt()).isNull();
    }

    @Test
    void 유저가_저장되면_생성_시간이_기록된다() {
        final User user = JAVAJIGI();

        userRepository.save(user);

        assertThat(user.getTimeLog().getCreatedAt()).isNotNull();
    }

    @Test
    void CreatedDate는_영속화시점에_채워진다(@Autowired UserRepository userRepository) {
        final User user = JAVAJIGI();

        userRepository.save(user);

        assertThat(user.getTimeLog().getCreatedAt()).isNotNull();
    }

    @Test
    void 유저의_정보가_변경되면_변경_시간이_최신화_된다() {
        final User user = JAVAJIGI();
        userRepository.save(user);
        final LocalDateTime originalLocalDateTime = LocalDateTime.from(user.getTimeLog().getUpdatedAt());

        user.setUserId("화이트캣");

        final User updatedUser = userRepository.findByUserId("화이트캣").get();
        assertThat(updatedUser.getTimeLog().getUpdatedAt()).isAfter(originalLocalDateTime);
    }
}
