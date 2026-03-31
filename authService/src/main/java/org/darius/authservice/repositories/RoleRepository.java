package org.darius.authservice.repositories;

import org.darius.authservice.common.enums.RoleType;
import org.darius.authservice.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleType(RoleType roleType);
}
