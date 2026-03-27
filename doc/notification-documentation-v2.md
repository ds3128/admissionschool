# Notification Service — Documentation Technique v2

**Projet :** AdmissionSchool  
**Service :** Notification Service  
**Port :** 8087  
**Version :** 2.0.0  
**Dernière mise à jour :** Mars 2026  
**Stack :** Spring Boot 4.0.3, Java 21, Spring Cloud 2025.1.0, PostgreSQL, Kafka, JavaMailSender, Thymeleaf (templates email), MapStruct 1.6.3, SpringDoc 2.8.6

**Changelog v2 :**
- Migration NestJS → Java / Spring Boot
- Cohérence totale avec les autres services du projet
- JavaMailSender + Thymeleaf au lieu de Nodemailer + Handlebars
- Même pattern sécurité : `OncePerRequestFilter` + headers `X-User-*`
- Même pattern Kafka : `StringSerializer/Deserializer` + `ObjectMapper` manuel

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture technique](#2-architecture-technique)
3. [Contexte d'intégration](#3-contexte-dintégration)
4. [Canaux de notification](#4-canaux-de-notification)
5. [Catalogue complet des notifications](#5-catalogue-complet-des-notifications)
6. [Modèle de domaine — Entités](#6-modèle-de-domaine--entités)
7. [Énumérations](#7-énumérations)
8. [Cas d'utilisation](#8-cas-dutilisation)
9. [Règles métier transversales](#9-règles-métier-transversales)
10. [Templates email — Thymeleaf](#10-templates-email--thymeleaf)
11. [Sécurité des routes](#11-sécurité-des-routes)
12. [Événements Kafka consommés](#12-événements-kafka-consommés)
13. [Dépendances cross-services](#13-dépendances-cross-services)
14. [Endpoints API](#14-endpoints-api)
15. [Configuration](#15-configuration)

---

## 1. Vue d'ensemble

Le Notification Service est le service transversal responsable de toutes les communications sortantes vers les utilisateurs. Il consomme les events Kafka publiés par les autres services (Admission, Payment, Course) et déclenche les notifications appropriées par email.

C'est un service **entièrement réactif** — il ne reçoit aucun appel HTTP des autres services. Tout passe par Kafka. Il expose uniquement une API REST pour la consultation de l'historique des notifications par les utilisateurs et l'administration.

### Responsabilités

- Consommer les 23 events Kafka de tous les services
- Envoyer des emails transactionnels via JavaMailSender + SMTP
- Maintenir un historique de toutes les notifications envoyées
- Gérer les préférences de notification par utilisateur
- Gérer les tentatives de renvoi en cas d'échec (retry — 3 max)
- Exposer l'historique des notifications via REST

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Génération de PDF / bulletins | Document Service |
| Logique métier de validation | Services concernés |
| Stockage des fichiers | Infrastructure |

---

## 2. Architecture technique

### Structure du projet

```
notification-service/
├── config/
│   ├── SwaggerConfig.java
│   ├── RestClientConfig.java       ← appels HTTP vers User Service
│   ├── JacksonConfig.java
│   ├── MailConfig.java
│   └── SchedulerConfig.java
├── controllers/
│   ├── NotificationController.java
│   └── PreferenceController.java
├── dtos/
│   ├── requests/
│   └── responses/
├── entities/
│   ├── Notification.java
│   └── NotificationPreference.java
├── enums/
│   ├── NotificationType.java
│   ├── NotificationChannel.java
│   └── NotificationStatus.java
├── events/                         ← payloads des events consommés
│   ├── admission/
│   ├── payment/
│   └── course/
├── exceptions/
│   └── GlobalExceptionHandler.java
├── kafka/
│   ├── KafkaConfig.java
│   ├── consumers/
│   │   ├── AdmissionConsumer.java
│   │   ├── PaymentConsumer.java
│   │   └── CourseConsumer.java
├── mail/
│   ├── MailService.java
│   └── UserResolverService.java    ← résolution userId → email
├── repositories/
│   ├── NotificationRepository.java
│   └── NotificationPreferenceRepository.java
├── security/
│   ├── NotificationSecurityFilter.java
│   └── SecurityConfig.java
└── services/
    ├── NotificationService.java
    ├── PreferenceService.java
    └── impl/
        ├── NotificationServiceImpl.java
        └── PreferenceServiceImpl.java
```

### Flux de traitement d'une notification

```
Kafka Event (String JSON)
  ↓ Consumer (Admission / Payment / Course)
  ↓ ObjectMapper.readValue() → Event POJO
  ↓ Extraction des données (email, firstName, etc.)
  ↓ Si userId uniquement → UserResolverService.resolve() HTTP
  ↓ Vérification préférences utilisateur
  ↓ Si notifications critiques → bypass préférences
  ↓ MailService.send() → JavaMailSender + template Thymeleaf
  ↓ Enregistrement Notification en base (historique)
  ↓ En cas d'échec → retryCount++, statut FAILED
  ↓ Scheduler @Scheduled(fixedDelay=300000) → retryFailed()
```

### Flux de sécurité (même pattern que les autres services)

```
Client
  ↓ JWT Bearer token
API Gateway :8888
  ↓ Valide JWT, extrait email/role/userId
  ↓ Injecte X-User-Email, X-User-Role, X-User-Id
Notification Service :8087
  ↓ NotificationSecurityFilter lit les headers
  ↓ Peuple SecurityContext avec ROLE_{role}
  ↓ Controllers lisent @RequestHeader("X-User-Id")
```

---

## 3. Contexte d'intégration

```
Admission Service ─── Kafka (10 topics) ───►
Payment Service ────── Kafka (8 topics)  ────► Notification Service :8087
Course Service ──────── Kafka (5 topics) ───►       ↓
                                              JavaMailSender
                                                    ↓
                                              Serveur SMTP
                                                    ↓
                                              Utilisateur (email)

Notification Service
  ↓ HTTP (RestClient)
  └── User Service :8082  (GET /users/{id} → résoudre email)
```

Le Notification Service est **purement consommateur** — il ne publie aucun event Kafka.

---

## 4. Canaux de notification

### Email (principal — v1)

Implémenté via **JavaMailSender** avec templates **Thymeleaf** (`.html`).

- Toutes les notifications critiques envoyées par email
- Templates HTML responsive avec variables dynamiques `${...}`
- Sujet et corps personnalisés par type de notification

### In-app (historique)

Toutes les notifications sont enregistrées en base (`Notification` entity) et accessibles via l'API REST. L'étudiant peut consulter son historique depuis l'interface Angular.

### SMS (optionnel — v2)

Prévu pour v2 via Twilio ou Orange SMS API.

---

## 5. Catalogue complet des notifications

### 5.1 Notifications Admission (10 events)

| Event Kafka | Destinataire | Sujet email |
|---|---|---|
| `application.submitted` | Candidat | Dossier reçu — confirmation de candidature |
| `application.admin.review` | Candidat | Mise à jour de votre dossier |
| `application.pending.commission` | Candidat | Dossier transmis à la commission |
| `interview.scheduled` | Candidat | Entretien planifié — informations importantes |
| `thesis.approval.requested` | Enseignant directeur | Demande d'encadrement de thèse |
| `application.awaiting.confirmation` | Candidat | Félicitations — Confirmez votre inscription |
| `application.accepted` | Étudiant | Bienvenue à l'université ! |
| `application.rejected` | Candidat | Résultat de votre candidature |
| `waitlist.promoted` | Candidat | Bonne nouvelle — Place disponible ! |
| `choice.auto.confirmed` | Étudiant | Confirmation automatique de votre inscription |

### 5.2 Notifications Payment (8 events)

| Event Kafka | Destinataire | Sujet email |
|---|---|---|
| `payment.completed` | Candidat / Étudiant | Paiement confirmé — Reçu de paiement |
| `payment.failed` | Candidat / Étudiant | Échec du paiement — Action requise |
| `payment.refunded` | Étudiant | Remboursement effectué |
| `invoice.generated` | Étudiant | Nouvelle facture disponible |
| `invoice.paid` | Étudiant | Facture réglée — Merci |
| `invoice.overdue` | Étudiant | ⚠️ Facture en retard — Régularisez votre situation |
| `student.payment.blocked` | Étudiant | 🔒 Accès restreint — Impayé critique |
| `scholarship.disbursed` | Étudiant | Versement de bourse effectué |

### 5.3 Notifications Course (5 events)

| Event Kafka | Destinataire | Sujet email |
|---|---|---|
| `student.enrolled` | Étudiant | Inscriptions aux cours confirmées |
| `attendance.threshold.exceeded` | Étudiant + Admin | ⚠️ Seuil d'absences dépassé |
| `grades.published` | Étudiant | Vos notes sont disponibles |
| `session.cancelled` | Étudiants du groupe | Séance annulée |
| `semester.validated` | Étudiant | Résultats du semestre disponibles |

**Total : 23 types de notifications**

---

## 6. Modèle de domaine — Entités

### `Notification`

Historique complet de toutes les notifications envoyées.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `userId` | String | Destinataire — référence Auth Service |
| `recipientEmail` | String | Email au moment de l'envoi |
| `type` | NotificationType | Type de notification |
| `channel` | NotificationChannel | EMAIL, SMS, IN_APP |
| `status` | NotificationStatus | PENDING, SENT, FAILED, RETRYING |
| `subject` | String | Sujet de l'email |
| `body` | TEXT | Corps HTML de la notification |
| `metadata` | String (JSON) | Données contextuelles sérialisées |
| `referenceId` | String | ID de l'entité source (applicationId, invoiceId...) |
| `referenceType` | String | Type de l'entité (APPLICATION, INVOICE...) |
| `retryCount` | int | Nombre de tentatives (max 3) |
| `sentAt` | LocalDateTime | Date d'envoi effectif |
| `readAt` | LocalDateTime | Date de lecture in-app |
| `errorMessage` | TEXT | Message d'erreur si échec |
| `createdAt` | LocalDateTime | Date de création |

**Index :** `userId`, `status`, `type`, `createdAt`

---

### `NotificationPreference`

Préférences de notification par utilisateur.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `userId` | String UNIQUE | Référence Auth Service |
| `emailEnabled` | boolean | Emails activés (défaut true) |
| `smsEnabled` | boolean | SMS activés (défaut false) |
| `admissionNotifications` | boolean | Notifications admission (défaut true) |
| `paymentNotifications` | boolean | Notifications paiement (défaut true) |
| `courseNotifications` | boolean | Notifications cours (défaut true) |
| `gradeNotifications` | boolean | Notifications notes (défaut true) |
| `attendanceNotifications` | boolean | Notifications présences (défaut true) |
| `updatedAt` | LocalDateTime | Dernière modification |

---

## 7. Énumérations

### `NotificationType`

```java
public enum NotificationType {
    // Admission
    APPLICATION_SUBMITTED,
    APPLICATION_ADMIN_REVIEW,
    APPLICATION_PENDING_COMMISSION,
    INTERVIEW_SCHEDULED,
    THESIS_APPROVAL_REQUESTED,
    APPLICATION_AWAITING_CONFIRMATION,
    APPLICATION_ACCEPTED,
    APPLICATION_REJECTED,
    WAITLIST_PROMOTED,
    CHOICE_AUTO_CONFIRMED,

    // Payment
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    INVOICE_GENERATED,
    INVOICE_PAID,
    INVOICE_OVERDUE,
    STUDENT_PAYMENT_BLOCKED,
    SCHOLARSHIP_DISBURSED,

    // Course
    STUDENT_ENROLLED,
    ATTENDANCE_THRESHOLD_EXCEEDED,
    GRADES_PUBLISHED,
    SESSION_CANCELLED,
    SEMESTER_VALIDATED
}
```

### `NotificationChannel`

```java
public enum NotificationChannel { EMAIL, SMS, IN_APP }
```

### `NotificationStatus`

```java
public enum NotificationStatus { PENDING, SENT, FAILED, RETRYING }
```

---

## 8. Cas d'utilisation

### UC-NOTIF-001 — Confirmer la soumission d'un dossier

**Trigger :** Kafka `application.submitted`  
**Destinataire :** Candidat (email inclus dans l'event)

1. Désérialise `ApplicationSubmittedEvent`
2. Vérifie préférences (`admissionNotifications = true`)
3. Envoie email via template `admission/application-submitted`
4. Enregistre `Notification` statut `SENT`

---

### UC-NOTIF-002 — Notifier l'acceptation et la demande de confirmation

**Trigger :** Kafka `application.awaiting.confirmation`

1. Reçoit la liste des choix acceptés + `expiresAt`
2. Construit l'email avec délai de confirmation calculé

---

### UC-NOTIF-003 — Accueillir le nouvel étudiant

**Trigger :** Kafka `application.accepted`  
**Notification non désactivable**

1. Email de bienvenue avec `studentNumber`, filière, prochaines étapes

---

### UC-NOTIF-004 — Notifier un directeur de thèse

**Trigger :** Kafka `thesis.approval.requested`  
**Destinataire :** Enseignant — résolution email via User Service

1. Reçoit `directorId` → appel HTTP `GET /users/{directorId}`
2. Envoie email avec résumé du projet de recherche

---

### UC-NOTIF-005 — Notifier un paiement confirmé

**Trigger :** Kafka `payment.completed`

1. Reçoit `userId` → résolution email si absent
2. Template différent selon `type` (FRAIS_DOSSIER, FRAIS_SCOLARITE, BOURSE)

---

### UC-NOTIF-006 — Alerter d'une facture en retard

**Trigger :** Kafka `invoice.overdue`  
**Notification non désactivable**

1. Reçoit `studentId` → résolution email via User Service
2. Email urgent avec montant restant, jours de retard, conséquences

---

### UC-NOTIF-007 — Notifier le blocage pour impayé

**Trigger :** Kafka `student.payment.blocked`  
**Notification non désactivable**

1. Email urgent indiquant le blocage et les démarches de régularisation

---

### UC-NOTIF-008 — Notifier la publication des notes

**Trigger :** Kafka `grades.published`  
**Destinataires multiples** : liste de `studentIds`

1. Pour chaque `studentId` → résolution email via User Service
2. Email individuel par étudiant (bulk avec `sendBulk()`)

---

### UC-NOTIF-009 — Notifier l'annulation d'une séance

**Trigger :** Kafka `session.cancelled`  
**Destinataires multiples** : `affectedStudentIds`

1. Email pour chaque étudiant du groupe affecté

---

### UC-NOTIF-010 — Notifier les résultats du semestre

**Trigger :** Kafka `semester.validated`  
**Destinataires multiples** : tous les étudiants dans `results`

1. Email personnalisé par étudiant
2. Template différent : ADMIS (félicitations) vs AJOURNE (rattrapage)

---

### UC-NOTIF-011 — Consulter son historique

**Acteur :** Utilisateur authentifié  
**Endpoint :** `GET /notifications/me`

1. Filtre par `X-User-Id` du header
2. Retourne liste paginée triée par date décroissante

---

### UC-NOTIF-012 — Gérer ses préférences

**Acteur :** Utilisateur authentifié  
**Endpoint :** `PUT /notifications/preferences`

1. Créées automatiquement avec valeurs par défaut si inexistantes
2. Notifications critiques toujours envoyées quoi qu'il arrive

---

### UC-NOTIF-013 — Retry automatique

**Acteur :** Scheduler (`@Scheduled`)

1. Toutes les 5 minutes — récupère les `FAILED` avec `retryCount < 3`
2. Retente l'envoi → `SENT` ou `FAILED` définitif après 3 tentatives

---

## 9. Règles métier transversales

### Résolution de l'email du destinataire

```
Cas 1 — Email inclus dans l'event (ex: application.submitted)
  → Utiliser directement

Cas 2 — Seulement userId/studentId (ex: invoice.overdue)
  → RestClient GET /users/{userId} → User Service :8082
  → Extraire email depuis la réponse

Cas 3 — Liste de studentIds (ex: grades.published, session.cancelled)
  → Appel HTTP individuel par userId
  → sendBulk() — traitement par lots de 10
```

### Notifications non désactivables (critiques)

Toujours envoyées même si `emailEnabled = false` :

```java
private static final Set<NotificationType> CRITICAL_TYPES = Set.of(
    NotificationType.INVOICE_OVERDUE,
    NotificationType.STUDENT_PAYMENT_BLOCKED,
    NotificationType.APPLICATION_ACCEPTED
);
```

### Politique de retry

```
Tentative 1 : immédiate (à la réception du Kafka event)
Tentative 2 : +5 minutes (scheduler)
Tentative 3 : +5 minutes (scheduler)
Après 3 échecs : FAILED définitif + log WARN admin
```

### Vérification des préférences

```java
if (!prefs.isEmailEnabled()) → skip
if (type est ADMISSION && !prefs.isAdmissionNotifications()) → skip
if (type est PAYMENT   && !prefs.isPaymentNotifications())   → skip
if (type est COURSE    && !prefs.isCourseNotifications())    → skip
if (type est GRADE     && !prefs.isGradeNotifications())     → skip
if (type est ATTENDANCE && !prefs.isAttendanceNotifications()) → skip
```

---

## 10. Templates email — Thymeleaf

Les templates sont des fichiers `.html` dans `src/main/resources/templates/mail/`.

### Structure des dossiers

```
resources/templates/mail/
├── layout.html                    ← template parent commun
├── admission/
│   ├── application-submitted.html
│   ├── application-admin-review.html
│   ├── application-pending-commission.html
│   ├── interview-scheduled.html
│   ├── thesis-approval-requested.html
│   ├── application-awaiting-confirmation.html
│   ├── application-accepted.html
│   ├── application-rejected.html
│   ├── waitlist-promoted.html
│   └── choice-auto-confirmed.html
├── payment/
│   ├── payment-completed.html
│   ├── payment-failed.html
│   ├── payment-refunded.html
│   ├── invoice-generated.html
│   ├── invoice-paid.html
│   ├── invoice-overdue.html
│   ├── student-payment-blocked.html
│   └── scholarship-disbursed.html
└── course/
    ├── student-enrolled.html
    ├── grades-published.html
    ├── attendance-threshold-exceeded.html
    ├── session-cancelled.html
    ├── semester-validated-admis.html
    └── semester-validated-ajourne.html
```

### Variables Thymeleaf par template

| Template | Variables principales |
|---|---|
| `application-submitted` | `firstName`, `applicationId`, `academicYear`, `submittedAt` |
| `interview-scheduled` | `firstName`, `scheduledAt`, `duration`, `location`, `type` |
| `application-accepted` | `firstName`, `studentNumber`, `filiereName`, `academicYear` |
| `payment-completed` | `firstName`, `paymentReference`, `amount`, `currency`, `type`, `paidAt` |
| `invoice-overdue` | `firstName`, `remainingAmount`, `dueDate`, `overdueDays` |
| `student-payment-blocked` | `firstName`, `amount`, `overdueDays` |
| `grades-published` | `firstName`, `evaluationTitle`, `matiereName` |
| `session-cancelled` | `firstName`, `matiereName`, `date`, `startTime`, `reason` |
| `semester-validated-admis` | `firstName`, `semesterLabel`, `average`, `mention`, `rank` |
| `semester-validated-ajourne` | `firstName`, `semesterLabel`, `average` |

---

## 11. Sécurité des routes

Même pattern que tous les autres services Java du projet.

| Route | Méthode | Rôles |
|---|---|---|
| `/notifications/me` | GET | Authentifié |
| `/notifications/me/unread` | GET | Authentifié |
| `/notifications/me/count` | GET | Authentifié |
| `/notifications/{id}/read` | PUT | Authentifié (propriétaire) |
| `/notifications/me/read-all` | PUT | Authentifié |
| `/notifications/preferences` | GET | Authentifié |
| `/notifications/preferences` | PUT | Authentifié |
| `/notifications/admin` | GET | ADMIN_SCHOLAR, SUPER_ADMIN |
| `/notifications/admin/stats` | GET | ADMIN_SCHOLAR, SUPER_ADMIN |
| `/notifications/admin/{id}/resend` | POST | ADMIN_SCHOLAR, SUPER_ADMIN |

---

## 12. Événements Kafka consommés

**23 topics** — 3 consumers, groupe `notification-service-group`

### AdmissionConsumer (10 topics)

| Topic | Event POJO | Action |
|---|---|---|
| `application.submitted` | `ApplicationSubmittedEvent` | Email confirmation candidat |
| `application.admin.review` | `ApplicationAdminReviewEvent` | Email mise à jour dossier |
| `application.pending.commission` | `ApplicationPendingCommissionEvent` | Email transmission commission |
| `interview.scheduled` | `InterviewScheduledEvent` | Email détails entretien |
| `thesis.approval.requested` | `ThesisApprovalRequestedEvent` | Email directeur pressenti |
| `application.awaiting.confirmation` | `ApplicationAwaitingConfirmationEvent` | Email choix acceptés + délai |
| `application.accepted` | `ApplicationAcceptedEvent` | Email bienvenue + matricule |
| `application.rejected` | `ApplicationRejectedEvent` | Email résultat négatif |
| `waitlist.promoted` | `WaitlistPromotedEvent` | Email place disponible |
| `choice.auto.confirmed` | `ChoiceAutoConfirmedEvent` | Email confirmation auto |

### PaymentConsumer (8 topics)

| Topic | Event POJO | Action |
|---|---|---|
| `payment.completed` | `PaymentCompletedEvent` | Email reçu de paiement |
| `payment.failed` | `PaymentFailedEvent` | Email échec paiement |
| `payment.refunded` | `PaymentRefundedEvent` | Email remboursement |
| `invoice.generated` | `InvoiceGeneratedEvent` | Email nouvelle facture |
| `invoice.paid` | `InvoicePaidEvent` | Email facture soldée |
| `invoice.overdue` | `InvoiceOverdueEvent` | Email facture en retard |
| `student.payment.blocked` | `StudentPaymentBlockedEvent` | Email blocage accès |
| `scholarship.disbursed` | `ScholarshipDisbursedEvent` | Email versement bourse |

### CourseConsumer (5 topics)

| Topic | Event POJO | Action |
|---|---|---|
| `student.enrolled` | `StudentEnrolledEvent` | Email inscriptions cours |
| `attendance.threshold.exceeded` | `AttendanceThresholdExceededEvent` | Email seuil absences |
| `grades.published` | `GradesPublishedEvent` | Email notes disponibles (bulk) |
| `session.cancelled` | `SessionCancelledEvent` | Email séance annulée (bulk) |
| `semester.validated` | `SemesterValidatedEvent` | Email résultats semestre (bulk) |

---

## 13. Dépendances cross-services

| Service | Type | Sens | Description |
|---|---|---|---|
| **Admission Service** | Kafka consumed | Admission → Notification | 10 events |
| **Payment Service** | Kafka consumed | Payment → Notification | 8 events |
| **Course Service** | Kafka consumed | Course → Notification | 5 events |
| **User Service** | HTTP sync | Notification → User | `GET /users/{id}` pour résoudre email quand absent de l'event |

---

## 14. Endpoints API

### Notifications utilisateur

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/notifications/me` | Historique paginé `?page=&size=&type=` | Authentifié |
| GET | `/notifications/me/unread` | Notifications non lues | Authentifié |
| GET | `/notifications/me/count` | Nombre non lues | Authentifié |
| PUT | `/notifications/{id}/read` | Marquer comme lu | Authentifié |
| PUT | `/notifications/me/read-all` | Tout marquer comme lu | Authentifié |

### Préférences

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/notifications/preferences` | Mes préférences | Authentifié |
| PUT | `/notifications/preferences` | Mettre à jour | Authentifié |

### Administration

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/notifications/admin` | Toutes `?userId=&status=&type=` | ADMIN_SCHOLAR |
| GET | `/notifications/admin/stats` | Stats d'envoi | ADMIN_SCHOLAR |
| POST | `/notifications/admin/{id}/resend` | Forcer le renvoi | ADMIN_SCHOLAR |

---

## 15. Configuration

### `pom.xml` — dépendances spécifiques

```xml
<!-- Email -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Templates Thymeleaf pour les emails -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Toutes les autres dépendances sont identiques aux autres services -->
<!-- Spring Boot Web, JPA, PostgreSQL, Kafka, Security, Validation -->
<!-- Lombok, MapStruct, SpringDoc, Jackson JSR310 -->
```

### `application.yml` (local)

```yaml
spring:
  application:
    name: notification-service
  config:
    import: optional:configserver:http://localhost:8761
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### `notification-service.yaml` (Config Server)

```yaml
server:
  port: 8087

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/notificationdb
    username: notification_service
    password: notification_service#123@
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:noreply@admissionschool.com}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
  thymeleaf:
    prefix: classpath:/templates/mail/
    suffix: .html
    mode: HTML
    encoding: UTF-8
    cache: false

eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false

user-service:
  base-url: http://localhost:8082
```

### Base de données

```sql
CREATE DATABASE notificationdb;
CREATE USER notification_service WITH PASSWORD 'notification_service#123@';
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO notification_service;
```

### Gateway — route à ajouter

```java
// ProxyController.java
ROUTES.put("/notifications/", "http://localhost:8087");
```
