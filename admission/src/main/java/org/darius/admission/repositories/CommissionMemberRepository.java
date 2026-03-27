package org.darius.admission.repositories;

import org.darius.admission.common.enums.MemberRole;
import org.darius.admission.entities.CommissionMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionMemberRepository extends JpaRepository<CommissionMember, Long> {

    List<CommissionMember> findByCommission_Id(Long commissionId);

    Optional<CommissionMember> findByCommission_IdAndTeacherId(
            Long commissionId, String teacherId
    );

    Optional<CommissionMember> findByCommission_IdAndRole(
            Long commissionId, MemberRole role
    );

    boolean existsByCommission_IdAndTeacherId(Long commissionId, String teacherId);

    int countByCommission_Id(Long commissionId);
}