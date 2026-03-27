package org.darius.admission.repositories;

import org.darius.admission.common.enums.VoteType;
import org.darius.admission.entities.CommissionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommissionVoteRepository extends JpaRepository<CommissionVote, Long> {

    List<CommissionVote> findByChoice_Id(Long choiceId);

    List<CommissionVote> findByChoice_IdAndCommission_Id(Long choiceId, Long commissionId);

    boolean existsByChoice_IdAndMemberId(Long choiceId, String memberId);

    // Compte des votes par type pour un choix
    long countByChoice_IdAndVote(Long choiceId, VoteType vote);

    // Tous les membres ont-ils voté ?
    @Query("""
        SELECT COUNT(m) = COUNT(v)
        FROM CommissionMember m
        LEFT JOIN CommissionVote v
          ON v.memberId = m.teacherId AND v.choice.id = :choiceId
        WHERE m.commission.id = :commissionId
        """)
    boolean allMembersVoted(
            @Param("choiceId")      Long choiceId,
            @Param("commissionId")  Long commissionId
    );

    // Nombre total de votes (hors abstentions) pour le calcul de majorité
    @Query("""
        SELECT COUNT(v) FROM CommissionVote v
        WHERE v.choice.id = :choiceId
          AND v.vote != 'ABSTAIN'
        """)
    long countNonAbstainVotes(@Param("choiceId") Long choiceId);
}