package dev.vingle.study;

import dev.vingle.product.category.domain.Categories;
import dev.vingle.product.itself.domain.Product;
import dev.vingle.product.size.domain.StandardSize;
import dev.vingle.support.general.ServiceTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
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
public class TestFixtureSavingFrequencyStudy {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private EntityManager globalEm;

    @BeforeEach
    void setUp() {
        this.globalEm = entityManagerFactory.createEntityManager();
    }

    @Test
    void 픽스쳐들을_한방에_커밋한다() {
        // 로컬이므로 오버헤드가 거의 없을 것이라고 생각한다
        Duration 한방 = commit(10000);
        System.out.println("한방 = " + 한방.toMillis());
    }

    @Test
    void 픽스쳐_클래스_단위로_찔끔찔끔_커밋한다() {
        Duration 찔끔찔끔 = Duration.ZERO;
        for (int i = 0; i < 500; i++) {
            찔끔찔끔 = 찔끔찔끔.plus(commit(20));
        }
        System.out.println("찔끔찔끔 = " + 찔끔찔끔.toMillis());
    }

    @Test
    void 메서드간_em_매번_생성() {
        Duration duration = Duration.ZERO;
        for (int i = 0; i < 500; i++) {
            duration = duration.plus(commit(20));
        }
        System.out.println("duration = " + duration.toMillis());
    }

    @Test
    void 메서드간_em_공유() {
        Duration duration = Duration.ZERO;
        for (int i = 0; i < 500; i++) {
            duration = duration.plus(commitWithGlobalEM(20));
        }
        System.out.println("duration = " + duration.toMillis());
    }

    private Duration commit(int entityCounts) {
        LocalDateTime start = LocalDateTime.now();
        var em = entityManagerFactory.createEntityManager();
        var transaction = em.getTransaction();
        transaction.begin();
        em.merge(캐주얼());
        em.merge(판매자1());
        for (int i = 0; i < entityCounts; i++) {
            em.merge(new Product(
                    null,
                    SALE,
                    List.of(캐주얼()),
                    new Categories(바지(), null),
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

    private Duration commitWithGlobalEM(int entityCounts) {
        LocalDateTime start = LocalDateTime.now();
        var transaction = globalEm.getTransaction();
        transaction.begin();
        globalEm.merge(캐주얼());
        globalEm.merge(판매자1());
        for (int i = 0; i < entityCounts; i++) {
            globalEm.merge(new Product(
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
