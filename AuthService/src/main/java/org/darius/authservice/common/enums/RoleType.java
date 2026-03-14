package org.darius.authservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

import static org.darius.authservice.common.enums.PermissionType.*;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    CANDIDATE(
            Set.of(
                    ADMISSION_CREATE,
                    AVIS_CREATE,
                    AVIS_READ,
                    USER_READ,
                    USER_UPDATE
            )
    ),

    // Étudiant
    STUDENT(
            Set.of(
                    COURSE_READ,
                    COURSE_ENROLL,
                    NOTE_READ,
                    EMPLOI_DU_TEMPS_VIEW,
                    AVIS_CREATE,
                    AVIS_READ,
                    BIBLIOTHEQUE_VIEW,
                    USER_READ,
                    USER_UPDATE,
                    DASHBOARD_VIEW
            )
    ),

    // Enseignant
    TEACHER(
            Set.of(
                    COURSE_READ,
                    COURSE_CREATE,
                    COURSE_UPDATE,
                    NOTE_READ,
                    NOTE_CREATE,
                    NOTE_UPDATE,
                    AVIS_MODERATE,
                    EMPLOI_DU_TEMPS_VIEW,
                    EMPLOI_DU_TEMPS_MANAGE,
                    BIBLIOTHEQUE_VIEW,
                    USER_READ,
                    DASHBOARD_VIEW
            )
    ),

    // Chef de département
    CHEF_DEPARTMENT(
            Set.of(
                    COURSE_READ,
                    COURSE_CREATE,
                    COURSE_UPDATE,
                    COURSE_DELETE,
                    NOTE_READ,
                    NOTE_CREATE,
                    NOTE_UPDATE,
                    AVIS_MODERATE,
                    EMPLOI_DU_TEMPS_MANAGE,
                    USER_READ,
                    ADMISSION_READ,
                    ADMISSION_DECIDE,
                    REPORTS_READ,
                    DASHBOARD_VIEW
            )
    ),

    // Personnel administratif (générique)
    PERSONNEL(
            Set.of(
                    USER_READ,
                    ADMISSION_READ,
                    ADMISSION_UPDATE,
                    COURSE_READ,
                    BIBLIOTHEQUE_MANAGE,
                    EMPLOI_DU_TEMPS_MANAGE,
                    DASHBOARD_VIEW
            )
    ),

    // Administrateur scolarité
    ADMIN_SCHOLAR(
            Set.of(
                    USER_READ,
                    USER_CREATE,
                    USER_UPDATE,
                    ADMISSION_READ,
                    ADMISSION_UPDATE,
                    ADMISSION_DECIDE,
                    COURSE_READ,
                    COURSE_CREATE,
                    COURSE_UPDATE,
                    NOTE_READ,
                    NOTE_CREATE,
                    NOTE_UPDATE,
                    NOTE_DELETE,
                    EMPLOI_DU_TEMPS_MANAGE,
                    REPORTS_READ,
                    DASHBOARD_VIEW
            )

    ),

    // Ressources Humaines
    ADMIN_RH(
            Set.of(
                    USER_READ,
                    USER_CREATE,
                    USER_UPDATE,
                    USER_DELETE,
                    RH_READ,
                    RH_CREATE,
                    RH_UPDATE,
                    RH_DELETE,
                    RH_CONTRACT,
                    RH_SALAIRE,
                    REPORTS_READ,
                    DASHBOARD_VIEW
            )
    ),

    // Finances
    ADMIN_FINANCE(
            Set.of(
                    FINANCE_READ,
                    FINANCE_CREATE,
                    FINANCE_UPDATE,
                    FINANCE_DELETE,
                    REPORTS_READ,
                    DASHBOARD_VIEW
            )
    ),

    // Super Administrateur (tous les droits)
    SUPER_ADMIN(
            Set.of()
    );

    private final Set<PermissionType> permissions;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<PermissionType> allPermissions = new HashSet<>();

        // SUPER_ADMIN hérite de TOUT
        if (this == SUPER_ADMIN) {
            for (RoleType role : values()) {
                if (role != SUPER_ADMIN) {
                    allPermissions.addAll(role.getPermissions());
                }
            }
        } else {
            allPermissions.addAll(this.permissions);
        }

        List<SimpleGrantedAuthority> authorities = allPermissions.stream()
                .map(perm -> new SimpleGrantedAuthority(perm.name()))
                .collect(Collectors.toList());

        // Toujours ajouter le ROLE_XX
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return authorities;
    }

//        private final Set<PermissionType> permission;
//
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        List<SimpleGrantedAuthority> grantedAuthorities = this.getPermission().stream().map(
//                permission -> new SimpleGrantedAuthority(permission.name())
//        ).collect(Collectors.toList());
//
//        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_"+this.name()));
//        return grantedAuthorities;
//    }
}
