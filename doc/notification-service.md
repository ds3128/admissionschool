# Notification Service - Documentation Technique v1

**Projet :** AdmissionSchool  
**Service :** Notification Service  
**Port :** 8087  
**Version :** 1.0.0  
**Dernière mise à jour :** Mars 2026

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Modèle de domaine - Description des entités](#2-modèle-de-domaine--description-des-entités)
3. [Énumérations](#3-énumérations)
4. [Cas d'utilisation](#4-cas-dutilisation)
5. [Règles métier transversales](#5-règles-métier-transversales)
6. [Dépendances cross-services](#6-dépendances-cross-services)
7. [Résumé des endpoints API](#7-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le Notification Service est un service purement réactif - il ne produit aucun événement et ne contient aucune logique métier propre. Il consomme les événements publiés par tous les autres services et envoie les notifications aux utilisateurs via email, SMS ou push.

Il centralise la gestion des templates de notification, les préférences utilisateurs et l'historique des envois.

### Responsabilités

- Consommation de tous les événements Kafka du système
- Envoi d'emails (Spring Mail / SMTP)
- Envoi de SMS (intégration externe)
- Gestion des templates de notification
- Gestion des préférences de notification par utilisateur
- Historique et traçabilité des envois

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Production d'événements | Tous les autres services |
| Logique métier | Chaque service concerné |
| Génération de PDF | Document Service |

---

## 2. Modèle de domaine - Description des entités

### 2.1 `Notification`

Représente une notification envoyée ou en attente d'envoi.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Destinataire (référence Auth Service) |
| `channel` | Channel | EMAIL, SMS, PUSH |
| `subject` | String | Sujet (email uniquement) |
| `content` | String | Contenu du message |
| `templateId` | Long | Template utilisé |
| `eventType` | String | Type d'événement déclencheur |
| `status` | NotifStatus | PENDING, SENT, FAILED, SKIPPED |
| `attempts` | int | Nombre de tentatives d'envoi |
| `sentAt` | Instant | Date d'envoi effectif |
| `failureReason` | String | Motif d'échec si applicable |
| `createdAt` | Instant | Date de création |

**Règles métier :**
- Maximum 3 tentatives d'envoi avant passage en `FAILED`.
- Un retry automatique est effectué après 5 minutes si `FAILED`.
- `SKIPPED` si l'utilisateur a désactivé ce canal de notification.

---

### 2.2 `EmailTemplate`

Représente un template de notification réutilisable.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom unique du template |
| `eventType` | String | Événement déclencheur associé |
| `channel` | Channel | Canal concerné |
| `subject` | String | Sujet (avec variables `{{variable}}`) |
| `body` | String | Corps du message (HTML ou texte) |
| `variables` | List\<String\> | Variables attendues dans le template |
| `isActive` | boolean | Template actif ou désactivé |
| `createdAt` | LocalDateTime | Date de création |
| `updatedAt` | LocalDateTime | Dernière modification |

**Exemples de templates :**

```
Template : ACCOUNT_ACTIVATION
  Subject : "Activez votre compte AdmissionSchool"
  Variables : {{firstName}}, {{activationLink}}, {{expiresIn}}

Template : APPLICATION_ACCEPTED
  Subject : "Félicitations ! Votre candidature a été acceptée"
  Variables : {{firstName}}, {{filiereName}}, {{confirmationDeadline}}

Template : GRADE_PUBLISHED
  Subject : "Vos notes de {{matiereName}} sont disponibles"
  Variables : {{firstName}}, {{matiereName}}, {{average}}
```

---

### 2.3 `NotificationPreference`

Représente les préférences de notification d'un utilisateur.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `userId` | UUID | Utilisateur concerné |
| `channel` | Channel | Canal de communication |
| `eventType` | String | Type d'événement (ou `ALL` pour tous) |
| `enabled` | boolean | Notification activée ou non |

**Règles métier :**
- Par défaut, toutes les notifications sont activées pour tous les canaux.
- Les notifications critiques (credentials, sécurité) ne peuvent pas être désactivées.
- Un utilisateur peut désactiver les notifications SMS tout en gardant les emails.

---

### 2.4 `NotificationBatch`

Représente un envoi en masse à un groupe d'utilisateurs.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom de la campagne |
| `templateId` | Long | Template utilisé |
| `recipientIds` | List\<UUID\> | Destinataires |
| `status` | BatchStatus | PENDING, RUNNING, COMPLETED, FAILED |
| `sentCount` | int | Nombre d'envois réussis |
| `failedCount` | int | Nombre d'échecs |
| `createdAt` | LocalDateTime | Date de création |
| `completedAt` | LocalDateTime | Date de fin |

---

## 3. Énumérations

### `Channel`
| Valeur | Description |
|---|---|
| `EMAIL` | Notification par email |
| `SMS` | Notification par SMS |
| `PUSH` | Notification push (mobile) |

### `NotifStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente d'envoi |
| `SENT` | Envoyée avec succès |
| `FAILED` | Échec définitif (3 tentatives) |
| `SKIPPED` | Ignorée (préférences utilisateur) |

### `BatchStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente |
| `RUNNING` | En cours d'envoi |
| `COMPLETED` | Terminé |
| `FAILED` | Échoué |

---

## 4. Cas d'utilisation

### UC-NOT-001 - Envoyer une notification sur événement Kafka

**Acteur :** Système (consommateur Kafka)  
**Déclencheur :** Réception d'un événement Kafka

**Scénario principal :**
1. Le service reçoit un événement (ex : `ApplicationAccepted`).
2. Le système identifie le template associé à ce type d'événement.
3. Le système vérifie les préférences de l'utilisateur.
4. Le système remplace les variables dans le template avec les données de l'événement.
5. Le système crée une `Notification` en `PENDING`.
6. Le système envoie via le canal approprié.
7. Sur succès → statut `SENT`.
8. Sur échec → retry après 5 minutes (max 3 tentatives) → `FAILED`.

---

### UC-NOT-002 - Envoyer une notification en masse

**Acteur :** Admin (`SUPER_ADMIN` ou `ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `POST /notifications/batch`

**Scénario principal :**
1. L'admin définit le template, les destinataires et les variables.
2. Le système crée un `NotificationBatch`.
3. Le système envoie les notifications en arrière-plan (async).
4. Le statut du batch est mis à jour en temps réel.

---

### UC-NOT-003 - Gérer ses préférences de notification

**Acteur :** Utilisateur connecté  
**Déclencheur :** Requête `PUT /notifications/preferences`

**Scénario principal :**
1. L'utilisateur met à jour ses préférences (canal, type d'événement, activé/désactivé).
2. Le système met à jour `NotificationPreference`.
3. Les futures notifications respecteront ces préférences.

---

## 5. Règles métier transversales

### Mapping événements → templates

```
UserActivated                → ACCOUNT_ACTIVATION
ApplicationSubmitted         → APPLICATION_SUBMITTED_CONFIRMATION
ApplicationUnderReview       → APPLICATION_UNDER_REVIEW
ApplicationAwaitingConfirm.  → APPLICATION_AWAITING_CONFIRMATION
ApplicationAccepted          → APPLICATION_ACCEPTED (avec credentials)
ApplicationRejected          → APPLICATION_REJECTED
InterviewScheduled           → INTERVIEW_SCHEDULED
SessionCancelled             → SESSION_CANCELLED
GradesPublished              → GRADE_PUBLISHED
SemesterValidated            → SEMESTER_RESULTS
AttendanceThresholdExceeded  → ATTENDANCE_WARNING
InvoiceCreated               → INVOICE_CREATED
InvoiceOverdue               → INVOICE_OVERDUE
PaymentCompleted             → PAYMENT_CONFIRMATION
ScholarshipDisbursed         → SCHOLARSHIP_DISBURSEMENT
ContractCreated              → CONTRACT_CREATED
LeaveApproved                → LEAVE_APPROVED
LeaveRejected                → LEAVE_REJECTED
PayslipPaid                  → PAYSLIP_AVAILABLE
```

### Notifications critiques (non désactivables)

```
ACCOUNT_ACTIVATION
APPLICATION_ACCEPTED (avec credentials)
SECURITY_ALERT
PAYMENT_CONFIRMATION
CONTRACT_CREATED
```

### Retry policy

```
Tentative 1 : immédiat
Tentative 2 : + 5 minutes
Tentative 3 : + 15 minutes
Après 3 échecs → statut FAILED, alerte admin
```

---

## 6. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| Tous les services | Consommateur Kafka | Consomme tous les événements du système |

### Événements consommés (Kafka) - liste complète

| Événement | Producteur |
|---|---|
| `UserActivated` | Auth Service |
| `ApplicationSubmitted` | Admission Service |
| `ApplicationUnderAdminReview` | Admission Service |
| `ApplicationAdditionalDocsRequired` | Admission Service |
| `ApplicationAwaitingConfirmation` | Admission Service |
| `ApplicationAccepted` | Admission Service |
| `ApplicationRejected` | Admission Service |
| `WaitlistPromoted` | Admission Service |
| `ChoiceAutoConfirmed` | Admission Service |
| `InterviewScheduled` | Admission Service |
| `StudentProfileCreated` | User Service |
| `StudentPromoted` | User Service |
| `StudentGraduated` | User Service |
| `TeacherProfileCreated` | User Service |
| `SessionCancelled` | Course Service |
| `GradesPublished` | Course Service |
| `AttendanceThresholdExceeded` | Course Service |
| `SemesterValidated` | Course Service |
| `PaymentCompleted` | Paiement Service |
| `PaymentFailed` | Paiement Service |
| `InvoicePaid` | Paiement Service |
| `InvoiceOverdue` | Paiement Service |
| `StudentPaymentBlocked` | Paiement Service |
| `ScholarshipDisbursed` | Paiement Service |
| `ContractCreated` | RH Service |
| `LeaveApproved` | RH Service |
| `LeaveRejected` | RH Service |
| `PayslipPaid` | RH Service |

---

## 7. Résumé des endpoints API

### Notifications

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/notifications/me` | Mes notifications | Authentifié |
| `PUT` | `/notifications/{id}/read` | Marquer comme lu | Authentifié |
| `GET` | `/notifications/admin` | Toutes les notifications | `SUPER_ADMIN` |
| `POST` | `/notifications/batch` | Envoi en masse | `SUPER_ADMIN`, `ADMIN_SCHOLAR` |

### Préférences

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/notifications/preferences` | Mes préférences | Authentifié |
| `PUT` | `/notifications/preferences` | Mettre à jour | Authentifié |

### Templates

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/notifications/templates` | Lister les templates | `SUPER_ADMIN` |
| `POST` | `/notifications/templates` | Créer un template | `SUPER_ADMIN` |
| `PUT` | `/notifications/templates/{id}` | Modifier un template | `SUPER_ADMIN` |
