package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.PokerVoteEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PokerVoteJpaRepository extends JpaRepository<PokerVoteEntity, UUID> {

    @Query("""
            select v from PokerVoteEntity v
            join fetch v.player
            join fetch v.task
            where v.task.id in :taskIds
            """)
    List<PokerVoteEntity> findWithPlayerByTaskIdIn(@Param("taskIds") Collection<UUID> taskIds);
}
