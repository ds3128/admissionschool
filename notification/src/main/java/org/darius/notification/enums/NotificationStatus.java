package org.darius.notification.enums;

public enum NotificationStatus {
    PENDING,   // En attente d'envoi
    SENT,      // Envoyé avec succès
    FAILED,    // Échec — retryCount < 3
    RETRYING   // En cours de renvoi
}
