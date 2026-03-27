package org.darius.notification.services;

import org.darius.notification.dtos.requests.UpdatePreferenceRequest;
import org.darius.notification.dtos.responses.PreferenceResponse;
import org.darius.notification.entities.NotificationPreference;

public interface PreferenceService {

    PreferenceResponse getPreferences(String userId);

    PreferenceResponse updatePreferences(String userId, UpdatePreferenceRequest request);

    /**
     * Retourne l'entité préférence — crée avec valeurs par défaut si inexistante.
     */
    NotificationPreference findOrCreate(String userId);
}
