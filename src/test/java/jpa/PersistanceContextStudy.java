package dev.vingle.study;

import dev.vingle.product.category.domain.Style;
import dev.vingle.support.general.ServiceTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("연구용")
@ServiceTest
public class PersistanceContextStudy {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void EntityManager의_contains는_영속성_컨텍스트에_포함되어있는지_여부이다() {
        // persist된 객체, merge, 조회를 통해 영속상태인 놈들이 contains=true, 즉 관리대상이다.
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        Style newEntity = new Style("캐주얼", "asdf", 0);
        assertThat(entityManager.contains(newEntity)).isFalse(); // new 상태는 영속성 컨텍스트에 의해 관리되는 상태가 아니다

        entityManager.persist(newEntity);
        assertThat(entityManager.contains(newEntity)).isTrue(); // 영속화된 entity는 관리되는 상태다.

        Style newEntityWithProperId = new Style("캐주얼", "asdf", 1);
        assertThat(entityManager.contains(newEntityWithProperId)).isFalse(); // Id가 있어도 new는 관리되는 상태가 아니다

        Style merged = entityManager.merge(newEntityWithProperId); // Id 1이 영속성 컨텍스트에 있기 때문에 select가 발생하지 않는다.
        assertThat(entityManager.contains(merged)).isTrue(); // 머지로 반환된 객체는 관리되는 상태다
        assertThat(entityManager.contains(newEntityWithProperId)).isFalse(); // 머지 대상 객체는 관리대상이 아니다
//        merged.setLocalizedName("asdf"); // 관리 대상이므로 변경사항이 감지된다.

        Style found = entityManager.find(Style.class, 1L);
        assertThat(entityManager.contains(found)).isTrue(); // 조회된 객체는 관리되는 상태다

        transaction.commit();
    }

    @Test
    void ID가_있는_상태로는_persist가_불가하다() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        Style newEntity = new Style("캐주얼", "asdf", 1);
        assertThatThrownBy(() -> entityManager.persist(newEntity));

        transaction.commit();
    }

    @Test
    void ID없이_merge해도_select가_발생하지_않는다() {
        // Id가 없는 경우 새로 저장하는 경우이기 때문에 select가 필요없다
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        Style newEntity = new Style("캐주얼", "asdf", 0);
        entityManager.merge(newEntity);

        transaction.commit();
    }

    @Test
    void ID가_있는_객체를_merge하면_select가_발생한다() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        entityManager.merge(new Style("캐주얼", "asdf", 1));

        transaction.commit();
    }

    @Test
    void 기존_데이터가_있는_경우_merge하면_select가_발생하지_않는다() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        entityManager.persist(new Style("캐주얼", "asdf", 0)); // Id 1이 영속성 컨텍스트에서 관리된다

        entityManager.merge(new Style("크크크", "asdf", 1)); // 1이 있으므로 select할 필요도 없다

        transaction.commit();
    }

    @Test
    void 기존_데이터가_있어도_영속성_컨텍스트가_다르면_select가_발생한다() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(new Style("캐주얼", "asdf", 0)); // Id 1이 영속성 컨텍스트에 있었다
        transaction.commit(); // 영속성 컨텍스트가 끝났다.

        EntityTransaction otherTransaction = entityManager.getTransaction();
        otherTransaction.begin();
        entityManager.merge(new Style("크크크", "asdf", 0)); // 새로운 영속성 컨텍스트. 여기서는 1의 존재 여부를 알 수 없다.
        otherTransaction.commit();
    }
}
