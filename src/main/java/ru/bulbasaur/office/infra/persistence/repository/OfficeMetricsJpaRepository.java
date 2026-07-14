package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.OfficeMetricsEntity;

import java.time.Instant;
import java.util.List;

public interface OfficeMetricsJpaRepository extends JpaRepository<OfficeMetricsEntity, Instant> {

    List<OfficeMetricsEntity> findAllByBucketStartGreaterThanEqualOrderByBucketStartAsc(Instant from);

    @Modifying
    @Query("delete from OfficeMetricsEntity e where e.bucketStart < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
