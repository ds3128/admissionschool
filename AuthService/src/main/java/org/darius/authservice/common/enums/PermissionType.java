package org.darius.authservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionType {
    // Utilisateurs
    USER_CREATE("user:create"),
    USER_READ("user:read"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete"),

    // Avis/évaluations
    AVIS_CREATE("avis:create"),
    AVIS_READ("avis:read"),
    AVIS_UPDATE("avis:update"),
    AVIS_DELETE("avis:delete"),
    AVIS_MODERATE("avis:moderate"),

    // Cours
    COURSE_CREATE("cours:create"),
    COURSE_READ("cours:read"),
    COURSE_UPDATE("cours:update"),
    COURSE_DELETE("cours:delete"),
    COURSE_ENROLL("cours:enroll"),
    COURSE_GRADE("cours:grade"), // Noter les étudiants

    // Notes
    NOTE_READ("note:read"),
    NOTE_CREATE("note:create"),
    NOTE_UPDATE("note:update"),
    NOTE_DELETE("note:delete"),

    // Admissions
    ADMISSION_READ("admission:read"),
    ADMISSION_CREATE("admission:create"),
    ADMISSION_UPDATE("admission:update"),
    ADMISSION_DELETE("admission:delete"),
    ADMISSION_DECIDE("admission:decide"), // Accepter/refuser

    // RH (Ressources Humaines)
    RH_READ("rh:read"),
    RH_CREATE("rh:create"),
    RH_UPDATE("rh:update"),
    RH_DELETE("rh:delete"),
    RH_CONTRACT("rh:contrat"),
    RH_SALAIRE("rh:salaire"),

    // Finances
    FINANCE_READ("finance:read"),
    FINANCE_CREATE("finance:create"),
    FINANCE_UPDATE("finance:update"),
    FINANCE_DELETE("finance:delete"),

    // Administration système
    ADMIN_USER_MANAGE("admin:user:manage"),
    ADMIN_ROLE_MANAGE("admin:role:manage"),
    ADMIN_PERMISSION_MANAGE("admin:permission:manage"),
    ADMIN_LOGS_READ("admin:logs:read"),
    ADMIN_CONFIG_MANAGE("admin:config:manage"),

    // Dashboard et reporting
    DASHBOARD_VIEW("dashboard:view"),
    REPORTS_CREATE("reports:create"),
    REPORTS_READ("reports:read"),

    // Spécifiques université
    EMPLOI_DU_TEMPS_VIEW("emploi:view"),
    EMPLOI_DU_TEMPS_MANAGE("emploi:manage"),
    BIBLIOTHEQUE_VIEW("bibliotheque:view"),
    BIBLIOTHEQUE_MANAGE("bibliotheque:manage");

    private final String permission;
}