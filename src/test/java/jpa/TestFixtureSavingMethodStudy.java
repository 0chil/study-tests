package dev.vingle.study;

import dev.vingle.product.category.domain.Categories;
import dev.vingle.product.category.domain.Style;
import dev.vingle.product.itself.domain.Product;
import dev.vingle.product.size.domain.StandardSize;
import dev.vingle.seller.domain.Seller;
import dev.vingle.support.general.ServiceTest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static dev.vingle.fixture.SellerFixture.판매자1;
import static dev.vingle.fixture.StyleFixture.캐주얼;
import static dev.vingle.product.itself.domain.ProductStatus.SALE;

@Disabled("연구용")
@ServiceTest
public class TestFixtureSavingMethodStudy {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void persist로_저장() {
        Duration duration = Duration.ZERO;
        for (int i = 0; i < 500; i++) {
            duration = duration.plus(commitWithPersist(20));
        }
        System.out.println("duration = " + duration.toMillis());
    }

    @Test
    void merge로_저장() {
        Duration duration = Duration.ZERO;
        for (int i = 0; i < 500; i++) {
            duration = duration.plus(commitWithMerge(20));
        }
        System.out.println("duration = " + duration.toMillis());
    }

    @Test
    void JPA_Auditing_때문에_update쿼리가_추가발생한다() {
        commitWithMerge(1);
    }

    private Duration commitWithPersist(int entityCounts) {
        LocalDateTime start = LocalDateTime.now();
        var em = entityManagerFactory.createEntityManager();
        var transaction = em.getTransaction();
        transaction.begin();
        em.persist(new Style("캐주얼", "asdf", 0));
        em.persist(new Seller(
                "빈집",
                "홍길동",
                "서울시 강남구 역삼동 111-1",
                "123-12-12345",
                "02-1234-1234",
                "https://vinzip.co.kr/"
        ));
        for (int i = 0; i < entityCounts; i++) {
            em.persist(new Product(
                    null,
                    SALE,
                    List.of(캐주얼()),
                    new Categories(LegacyPrimaryCategory.PANTS, null),
                    "아디다스 트레이닝 바지",
                    29_000L,
                    판매자1(),
                    "아디다스",
                    StandardSize.FREE,
                    95,
                    "허리 32 총기장 75",
                    "https://vinzip.kr/3"
            ));
        }
        transaction.commit();
        LocalDateTime end = LocalDateTime.now();
        return Duration.between(start, end);
    }

    private Duration commitWithMerge(int entityCounts) {
        LocalDateTime start = LocalDateTime.now();
        var em = entityManagerFactory.createEntityManager();
        var transaction = em.getTransaction();
        transaction.begin();
        em.merge(new Style("캐주얼", "asdf", 0));
        em.merge(new Seller(
                "빈집",
                "홍길동",
                "서울시 강남구 역삼동 111-1",
                "123-12-12345",
                "02-1234-1234",
                "https://vinzip.co.kr/"
        ));
        for (int i = 0; i < entityCounts; i++) {
            em.merge(new Product(
                    null,
                    SALE,
                    List.of(캐주얼()),
                    new Categories(LegacyPrimaryCategory.PANTS, null),
                    "아디다스 트레이닝 바지",
                    29_000L,
                    판매자1(),
                    "아디다스",
                    StandardSize.FREE,
                    95,
                    "허리 32 총기장 75",
                    "https://vinzip.kr/3"
            ));
        }
        transaction.commit();
        LocalDateTime end = LocalDateTime.now();
        return Duration.between(start, end);
    }
}
