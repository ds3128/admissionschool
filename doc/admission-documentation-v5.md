# Admission Service - Documentation Technique v5

**Projet :** AdmissionSchool  
**Service :** Admission Service  
**Port :** 8084  
**Version :** 5.0.0  
**Dernière mise à jour :** Mars 2026  
**Stack :** Spring Boot 4.0.3, Java 21, Spring Cloud 2025.1.0, PostgreSQL, Kafka, MapStruct 1.6.3, SpringDoc 2.8.6

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture technique](#2-architecture-technique)
3. [Cycle d'une campagne d'admission](#3-cycle-dune-campagne-dadmission)
4. [Parcours candidat pas à pas](#4-parcours-candidat-pas-à-pas)
5. [Flux par niveau d'études](#5-flux-par-niveau-détudes)
6. [Modèle de domaine - Entités](#6-modèle-de-domaine--entités)
7. [Énumérations](#7-énumérations)
8. [Cas d'utilisation](#8-cas-dutilisation)
9. [Règles métier transversales](#9-règles-métier-transversales)
10. [Sécurité des routes](#10-sécurité-des-routes)
11. [Événements Kafka](#11-événements-kafka)
12. [Dépendances cross-services](#12-dépendances-cross-services)
13. [Schedulers](#13-schedulers)
14. [Endpoints API complets](#14-endpoints-api-complets)
15. [Configuration](#15-configuration)

---

## 1. Vue d'ensemble

L'Admission Service gère l'intégralité du processus de candidature à l'université, depuis l'ouverture de la campagne jusqu'à la confirmation de l'inscription de l'étudiant. Il orchestre trois flux distincts selon le niveau d'études visé : Licence, Master et Doctorat.

### Responsabilités

- Gestion du cycle de vie des campagnes d'admission et des offres de formation
- Gestion des candidatures avec jusqu'à 3 choix ordonnés par priorité
- Validation administrative par la scolarité centrale
- Gestion des commissions pédagogiques et de leurs votes à la majorité
- Planification et suivi des entretiens (Master / Doctorat)
- Gestion de l'accord du directeur de thèse (Doctorat uniquement)
- Gestion de la liste d'attente active avec promotion automatique
- Gestion de la confirmation du candidat parmi ses choix acceptés
- Déclenchement de la création du compte étudiant après confirmation
- Génération du numéro matricule étudiant (STU-YYYY-NNNNN)

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Génération de l'email institutionnel | Auth Service |
| Création du compte étudiant | Auth Service |
| Création du profil étudiant complet | User Service |
| Paiement des frais de dossier | Paiement Service |
| Envoi des notifications | Notification Service |
| Génération des documents d'admission | Document Service |

---

## 2. Architecture technique

### Structure du projet

```
admission-service/
├── config/
│   ├── SwaggerConfig.java
│   └── SchedulerConfig.java        ← 5 jobs @Scheduled
├── controllers/
│   ├── CampaignController.java
│   ├── OfferController.java
│   ├── ApplicationController.java
│   ├── AdminApplicationController.java
│   ├── CommissionController.java
│   ├── InterviewController.java
│   ├── ThesisDirectorController.java
│   └── WaitlistController.java
├── dtos/
│   ├── requests/                   ← 18 Request DTOs
│   └── responses/                  ← 22 Response DTOs
├── entities/                       ← 16 entités JPA
├── enums/                          ← 17 énumérations
├── events/
│   ├── consumed/                   ← 1 event consommé
│   └── published/                  ← 10 events publiés
├── exceptions/
│   └── GlobalExceptionHandler.java ← ProblemDetail RFC 7807
├── kafka/
│   ├── KafkaConfig.java
│   ├── AdmissionEventProducer.java
│   └── AdmissionEventConsumer.java
├── mapper/
│   └── AdmissionMapper.java        ← MapStruct
├── repositories/                   ← 17 repositories JPA
├── security/
│   ├── AdmissionSecurityFilter.java ← lit X-User-Email/Role/Id
│   └── SecurityConfig.java
└── services/
    ├── impl/                       ← 10 implémentations
    └── [10 interfaces]
```

### Flux de sécurité

```
Client
  ↓ JWT Bearer token
API Gateway :8888
  ↓ Valide JWT, extrait email/role/userId
  ↓ Injecte X-User-Email, X-User-Role, X-User-Id
Admission Service :8084
  ↓ AdmissionSecurityFilter lit les headers
  ↓ Peuple SecurityContext avec ROLE_{role}
  ↓ Controllers lisent @RequestHeader("X-User-Id")
```

---

## 3. Cycle d'une campagne d'admission

```
UPCOMING ──(startDate atteinte)──► OPEN ──(endDate+1 atteinte)──► CLOSED ──► ARCHIVED
```

| Statut | Description |
|---|---|
| `UPCOMING` | Campagne créée, pas encore ouverte |
| `OPEN` | Candidatures acceptées |
| `CLOSED` | Clôturée - dossiers en traitement |
| `ARCHIVED` | Archivée - irréversible |

**Règles :**
- Transition `UPCOMING → OPEN` : automatique via scheduler quotidien (00:05) quand `startDate <= today`
- Transition `OPEN → CLOSED` : automatique via scheduler quotidien (00:05) quand `endDate < today`
- Transitions manuelles possibles via `PUT /admissions/campaigns/{id}/status`
- Une campagne `ARCHIVED` ne peut plus changer de statut
- Aucune candidature ne peut être créée hors d'une campagne `OPEN`

---

## 4. Parcours candidat pas à pas

### Étape 1 - Consulter les formations disponibles

```
GET /admissions/campaigns/current    → campagne OPEN
GET /admissions/offers?campaignId=N  → offres disponibles
GET /admissions/required-documents?offerId=N → documents requis
```
**Authentification :** Aucune - routes publiques.

---

### Étape 2 - Créer son compte et l'activer

```
POST /auth/register → crée Users (status=false)
GET  /auth/verify?token=... → active le compte
  ↓ Kafka UserActivated → User Service crée profil minimal
```

---

### Étape 3 - Créer la candidature

```
POST /admissions/applications { campaignId }
→ Application DRAFT
→ Dossier vide créé
→ CandidateProfile vide créé
```

---

### Étape 4 - Remplir le profil candidat

```
PUT /admissions/applications/{id}/profile
  { currentInstitution, currentDiploma, graduationYear, mention, ... }
→ CandidateProfile mis à jour
→ isComplete recalculé
```

---

### Étape 5 - Ajouter des choix (1 à 3)

```
POST /admissions/applications/{id}/choices
  { offerId, choiceOrder }
→ ApplicationChoice PENDING_ADMIN créé
→ AdmissionOffer.currentCount++
```

**Validations :**
- Offre `OPEN` + deadline non dépassée
- Pas de doublon (même offre)
- Max 3 choix actifs (configurable par campagne)

---

### Étape 6 - Uploader les documents

```
POST /admissions/applications/{id}/documents (multipart)
  file + documentType
→ DossierDocument PENDING créé
→ isComplete recalculé (vérifie RequiredDocuments de l'offre)
```

**Formats acceptés :** PDF, JPEG, PNG. **Taille max :** 5 Mo par fichier.

---

### Étape 7 - Payer les frais

```
POST /admissions/applications/{id}/payment
→ AdmissionPayment PENDING créé
  ↓ Paiement Service traite (async Kafka PaymentCompleted)
→ Application → PAID
```

---

### Étape 8 - Soumettre le dossier

```
POST /admissions/applications/{id}/submit
→ Vérifications : paiement COMPLETED + isComplete = true
→ Re-vérification deadlines (choix expirés retirés)
→ Dossier verrouillé (isLocked = true)
→ CandidateProfile gelé (isFrozen = true)
→ Application → SUBMITTED
→ Kafka ApplicationSubmitted publié
```

---

### Étape 9 - Suivi et confirmation

```
GET /admissions/applications/{id}    → statut + historique
GET /admissions/applications/{id}/confirmation → choix acceptés + délai

POST /admissions/applications/{id}/confirm { choiceId }
→ Choix CONFIRMED
→ Autres choix ACCEPTED → WITHDRAWN (places libérées)
→ Application → ACCEPTED
→ Matricule STU-YYYY-NNNNN généré
→ Kafka ApplicationAccepted publié
```

---

## 5. Flux par niveau d'études

### Licence (L1, L2, L3)

```
SUBMITTED → UNDER_ADMIN_REVIEW → PENDING_COMMISSION
  → UNDER_COMMISSION_REVIEW → Vote commission
  → ACCEPTED / REJECTED / WAITLISTED
  → (si ACCEPTED) AWAITING_CONFIRMATION → ACCEPTED (après confirmation)
```

Pas d'entretien pour la Licence.

---

### Master (M1, M2)

```
SUBMITTED → UNDER_ADMIN_REVIEW → PENDING_COMMISSION
  → UNDER_COMMISSION_REVIEW → Vote
  ┌─────────────────────────────────────────┐
  ▼                                         ▼
Décision sur dossier              INTERVIEW_REQUIRED
ACCEPTED/REJECTED/WAITLISTED           ↓
                               INTERVIEW_SCHEDULED
                                       ↓
                               INTERVIEW_DONE
                               Vote post-entretien
                                       ↓
                          ACCEPTED / REJECTED / WAITLISTED
```

---

### Doctorat

```
SUBMITTED → UNDER_ADMIN_REVIEW
  → PENDING_THESIS_DIRECTOR (demande au directeur, délai 15j)
    ├── Refus directeur → REJECTED
    └── Accord directeur → PENDING_COMMISSION
        → UNDER_COMMISSION_REVIEW → INTERVIEW_REQUIRED (quasi-systématique)
        → INTERVIEW_SCHEDULED → INTERVIEW_DONE
        → Vote final → ACCEPTED / REJECTED / WAITLISTED
```

---

### Confirmation commune (tous niveaux)

```
Au moins 1 ACCEPTED
  → ConfirmationRequest créée (délai = confirmationDeadlineDays jours ouvrables)
  → Application → AWAITING_CONFIRMATION
  → Notification candidat avec liste des choix acceptés

Candidat confirme (POST /confirm) :
  → Choix sélectionné → CONFIRMED
  → Autres ACCEPTED → WITHDRAWN
  → Places libérées sur offres retirées
  → Application → ACCEPTED
  → Matricule généré
  → ApplicationAccepted publié

Délai expiré (scheduler :30) :
  → Auto-confirmation du choix priorité 1
  → autoConfirmed = true dans ConfirmationRequest
  → Même flux que confirmation manuelle
```

---

## 6. Modèle de domaine - Entités

### `AdmissionCampaign`

Campagne d'admission annuelle. Contrainte d'unicité sur `academicYear`.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `academicYear` | String UNIQUE | Format `2025-2026` |
| `startDate` | LocalDate | Date d'ouverture |
| `endDate` | LocalDate | Date de clôture |
| `resultsDate` | LocalDate | Publication des résultats |
| `confirmationDeadlineDays` | int | Délai confirmation (défaut 5j) |
| `feeAmount` | BigDecimal | Frais de dossier |
| `status` | CampaignStatus | UPCOMING/OPEN/CLOSED/ARCHIVED |
| `maxChoicesPerApplication` | int | Max choix (défaut 3) |

**Relations :** `@OneToMany` → `AdmissionOffer`, `Application`

---

### `AdmissionOffer`

Offre de formation dans une campagne.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `campaign_id` | Long FK | Campagne parente |
| `filiereId` | Long | Référence User Service (dénormalisé) |
| `filiereName` | String | Nom filière (dénormalisé) |
| `level` | OfferLevel | LICENCE/MASTER/DOCTORAT |
| `deadline` | LocalDate | Date limite candidature |
| `maxCapacity` | int | Nombre de places |
| `currentCount` | int | Candidatures actives |
| `acceptedCount` | int | Inscriptions confirmées |
| `waitlistCount` | int | Candidats en liste d'attente |
| `status` | OfferStatus | OPEN/CLOSED/FULL |

**Relations :** `@ManyToOne` → `AdmissionCampaign`  
`@OneToMany` → `RequiredDocument`  
`@OneToOne` → `ReviewCommission`

---

### `Application`

Dossier de candidature d'un utilisateur pour une campagne. Contrainte d'unicité sur `(userId, campaign_id)`.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID PK | Clé primaire |
| `userId` | String | Référence Auth Service |
| `campaign_id` | Long FK | Campagne |
| `academicYear` | String | Année académique |
| `status` | ApplicationStatus | Statut courant |
| `submittedAt` | LocalDateTime | Date de soumission |
| `paidAt` | LocalDateTime | Date de paiement |
| `lastStatusChange` | LocalDateTime | Dernier changement |

**Relations :** `@ManyToOne` → `AdmissionCampaign`  
`@OneToMany` → `ApplicationChoice`, `ApplicationStatusHistory`  
`@OneToOne` → `Dossier`, `CandidateProfile`, `AdmissionPayment`, `ConfirmationRequest`

---

### `CandidateProfile`

Profil complet du candidat dans le dossier. Gelé après soumission (`isFrozen = true`).

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature parente |
| `firstName` / `lastName` | String | Nom/Prénom |
| `birthDate` / `birthPlace` | LocalDate/String | Naissance |
| `nationality` / `gender` | String | Nationalité/Genre |
| `phone` / `address` | String | Contact |
| `personalEmail` / `photoUrl` | String | Email/Photo |
| `currentInstitution` | String | Établissement d'origine |
| `currentDiploma` / `mention` | String | Diplôme/Mention |
| `graduationYear` | int | Année d'obtention |
| `researchProject` | Text | Projet de recherche (Doctorat) |
| `thesisDirectorName` | String | Directeur pressenti |
| `motivationLetter` | Text | Lettre de motivation |
| `isComplete` | boolean | Profil complet |
| `isFrozen` | boolean | Gelé après soumission |

---

### `ApplicationChoice`

Choix de formation dans une candidature. Contrainte d'unicité sur `(application_id, offer_id)`.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature |
| `offer_id` | Long FK | Offre de formation |
| `filiereId` / `filiereName` | Long/String | Dénormalisé |
| `level` | OfferLevel | Détermine le flux |
| `choiceOrder` | int | Priorité 1 (haute) à 3 |
| `status` | ChoiceStatus | Statut courant |
| `decidedAt` / `decidedBy` | DateTime/String | Décision |
| `decisionReason` | Text | Motif de refus |

**Relations :** `@OneToOne` → `WaitlistEntry`, `Interview`, `ThesisDirectorApproval`  
`@OneToMany` → `CommissionVote`

---

### `Dossier`

Conteneur des documents d'une candidature.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature |
| `isComplete` | boolean | Documents obligatoires présents |
| `isLocked` | boolean | Verrouillé après soumission |
| `lockedAt` | LocalDateTime | Date de verrouillage |
| `unlockReason` | Text | Motif de déverrouillage |

**Relations :** `@OneToMany` → `DossierDocument`

---

### `DossierDocument`

Document individuel uploadé dans un dossier.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `dossier_id` | Long FK | Dossier parent |
| `type` | DocumentType | Type de document |
| `fileName` / `fileUrl` | String | Nom et URL du fichier |
| `fileSize` / `mimeType` | Long/String | Taille et type MIME |
| `status` | DocumentStatus | PENDING/VALIDATED/REJECTED |
| `rejectionReason` | Text | Motif de rejet |
| `uploadedAt` / `validatedAt` | LocalDateTime | Dates |

---

### `RequiredDocument`

Documents obligatoires définis par offre de formation.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `offer_id` | Long FK | Offre parente |
| `documentType` | DocumentType | Type requis |
| `label` / `description` | String/Text | Libellé/Instructions |
| `isMandatory` | boolean | Obligatoire ou optionnel |
| `maxFileSizeMb` | int | Taille max en Mo |

---

### `ReviewCommission`

Commission pédagogique associée à une offre. Créée automatiquement à la création de l'offre.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `offer_id` | Long FK | Offre (relation `@OneToOne`) |
| `name` | String | Nom de la commission |
| `type` | CommissionType | LICENCE/MASTER/DOCTORAT |
| `presidentId` | String | ID de l'enseignant président |
| `quorum` | int | Votants minimum (défaut 3) |

---

### `CommissionMember`

Membre d'une commission. Contrainte d'unicité sur `(commission_id, teacher_id)`.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `commission_id` | Long FK | Commission |
| `teacherId` | String | Référence User Service |
| `role` | MemberRole | PRESIDENT/MEMBER/RAPPORTEUR |
| `joinedAt` | LocalDateTime | Date d'entrée |

---

### `CommissionVote`

Vote individuel d'un membre sur un choix. Contrainte d'unicité sur `(choice_id, member_id)`.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `choice_id` | Long FK | Choix examiné |
| `commission_id` | Long FK | Commission |
| `memberId` | String | ID du membre |
| `vote` | VoteType | ACCEPT/REJECT/ABSTAIN |
| `comment` | Text | Commentaire optionnel |
| `votedAt` | LocalDateTime | Date du vote |

---

### `Interview`

Entretien planifié pour un choix Master ou Doctorat.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature |
| `choice_id` | Long FK | Choix concerné (relation `@OneToOne`) |
| `scheduledAt` | LocalDateTime | Date et heure |
| `duration` | int | Durée en minutes (défaut 30) |
| `location` | String | Salle ou lien visio |
| `type` | InterviewType | PRESENTIEL/VISIO |
| `status` | InterviewStatus | SCHEDULED/DONE/CANCELLED/RESCHEDULED |
| `interviewers` | List\<String\> | IDs des enseignants |
| `notes` | Text | Notes post-entretien (confidentielles) |

---

### `ThesisDirectorApproval`

Demande d'accord du directeur de thèse (Doctorat uniquement). Délai de réponse : 15 jours.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `choice_id` | Long FK | Choix concerné (relation `@OneToOne`) |
| `directorId` | String | ID de l'enseignant HDR |
| `researchProject` | Text | Projet de recherche |
| `status` | ApprovalStatus | PENDING/APPROVED/REFUSED/EXPIRED |
| `comment` | Text | Commentaire du directeur |
| `requestedAt` | LocalDateTime | Date d'envoi |
| `respondedAt` | LocalDateTime | Date de réponse |
| `expiresAt` | LocalDateTime | Délai d'expiration |

---

### `WaitlistEntry`

Position d'un candidat en liste d'attente.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `choice_id` | Long FK | Choix concerné (relation `@OneToOne`) |
| `offerId` | Long | Offre (dénormalisé) |
| `rank` | int | Rang (1 = premier) |
| `status` | WaitlistStatus | WAITING/PROMOTED/CONFIRMED/EXPIRED/WITHDRAWN |
| `promotedAt` | LocalDateTime | Date de promotion |
| `expiresAt` | LocalDateTime | Délai 48h pour confirmer si promu |
| `notifiedAt` | LocalDateTime | Date de notification |

---

### `ConfirmationRequest`

Demande de confirmation envoyée au candidat quand au moins un choix est accepté.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature (relation `@OneToOne`) |
| `acceptedChoiceIds` | List\<Long\> | IDs des choix ACCEPTED |
| `expiresAt` | LocalDateTime | Délai de confirmation |
| `confirmedChoiceId` | Long | Choix confirmé |
| `status` | ConfirmationStatus | PENDING/CONFIRMED/EXPIRED |
| `autoConfirmed` | boolean | Confirmé automatiquement |
| `createdAt` / `confirmedAt` | LocalDateTime | Dates |

---

### `AdmissionPayment`

Paiement des frais de dossier.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID PK | Clé primaire |
| `application_id` | UUID FK | Candidature (relation `@OneToOne`) |
| `amount` / `currency` | BigDecimal/String | Montant et devise |
| `status` | PaymentStatus | PENDING/COMPLETED/FAILED/REFUNDED |
| `paymentReference` | String | Référence externe |
| `paidAt` / `createdAt` | LocalDateTime | Dates |

---

### `ApplicationStatusHistory`

Historique complet des changements de statut.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `application_id` | UUID FK | Candidature |
| `fromStatus` / `toStatus` | ApplicationStatus | Transition |
| `changedBy` | String | Acteur (userId ou "system") |
| `comment` | Text | Commentaire |
| `changedAt` | LocalDateTime | Date du changement |

---

## 7. Énumérations

### `ApplicationStatus` (14 valeurs)

| Valeur | Description |
|---|---|
| `DRAFT` | Brouillon en cours |
| `PAID` | Frais payés |
| `SUBMITTED` | Dossier soumis et verrouillé |
| `UNDER_ADMIN_REVIEW` | Vérification administrative |
| `ADDITIONAL_DOCS_REQUIRED` | Documents supplémentaires demandés |
| `PENDING_COMMISSION` | Transmis à la commission |
| `UNDER_COMMISSION_REVIEW` | Commission en cours d'examen |
| `PENDING_THESIS_DIRECTOR` | En attente accord directeur |
| `INTERVIEW_SCHEDULED` | Entretien planifié |
| `INTERVIEW_DONE` | Entretien réalisé |
| `AWAITING_CONFIRMATION` | Au moins 1 choix accepté |
| `ACCEPTED` | Confirmation effectuée |
| `REJECTED` | Tous les choix refusés |
| `WITHDRAWN` | Retirée par le candidat |

### `ChoiceStatus` (13 valeurs)

`PENDING_ADMIN` → `PENDING_COMMISSION` → `UNDER_COMMISSION_REVIEW` → `INTERVIEW_REQUIRED` → `INTERVIEW_SCHEDULED` → `INTERVIEW_DONE` → `ACCEPTED` / `REJECTED` / `WAITLISTED` → `PROMOTED_FROM_WAITLIST` → `CONFIRMED` / `WITHDRAWN`

### `CampaignStatus` : `UPCOMING`, `OPEN`, `CLOSED`, `ARCHIVED`
### `OfferStatus` : `OPEN`, `CLOSED`, `FULL`
### `OfferLevel` : `LICENCE`, `MASTER`, `DOCTORAT`
### `CommissionType` : `LICENCE`, `MASTER`, `DOCTORAT`
### `MemberRole` : `PRESIDENT`, `MEMBER`, `RAPPORTEUR`
### `VoteType` : `ACCEPT`, `REJECT`, `ABSTAIN`
### `InterviewType` : `PRESENTIEL`, `VISIO`
### `InterviewStatus` : `SCHEDULED`, `DONE`, `CANCELLED`, `RESCHEDULED`
### `ApprovalStatus` : `PENDING`, `APPROVED`, `REFUSED`, `EXPIRED`
### `WaitlistStatus` : `WAITING`, `PROMOTED`, `CONFIRMED`, `EXPIRED`, `WITHDRAWN`
### `ConfirmationStatus` : `PENDING`, `CONFIRMED`, `EXPIRED`
### `PaymentStatus` : `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`
### `DocumentStatus` : `PENDING`, `VALIDATED`, `REJECTED`
### `DocumentType` (14 valeurs) : `DIPLOME_BAC`, `RELEVE_NOTES_BAC`, `DIPLOME_LICENCE`, `RELEVE_NOTES_LICENCE`, `DIPLOME_MASTER`, `PROJET_RECHERCHE`, `LETTRE_MOTIVATION`, `CV`, `PIECE_IDENTITE`, `PHOTO_IDENTITE`, `CERTIFICAT_MEDICAL`, `LETTRE_RECOMMANDATION`, `PUBLICATIONS`, `AUTRE`

---

## 8. Cas d'utilisation

### UC-ADM-001 - Créer une campagne
**Acteur :** SUPER_ADMIN | **Endpoint :** `POST /admissions/campaigns`  
Vérifie l'unicité de l'année académique. Crée en statut `UPCOMING`. Le scheduler gère les transitions automatiques.

### UC-ADM-002 - Créer une offre de formation
**Acteur :** SUPER_ADMIN | **Endpoint :** `POST /admissions/offers`  
Vérifie que la deadline est dans la fenêtre de la campagne. Crée automatiquement la `ReviewCommission` associée.

### UC-ADM-003 - Créer une candidature
**Acteur :** CANDIDATE | **Endpoint :** `POST /admissions/applications`  
Vérifie campagne `OPEN`. Un candidat ne peut avoir qu'une candidature par campagne. Crée `Dossier` et `CandidateProfile` vides.

### UC-ADM-004 - Ajouter un choix
**Acteur :** CANDIDATE | **Endpoint :** `POST /admissions/applications/{id}/choices`  
Vérifie : offre `OPEN`, deadline non dépassée, pas de doublon, max choix non atteint. Incrémente `currentCount`.

### UC-ADM-005 - Soumettre la candidature
**Acteur :** CANDIDATE | **Endpoint :** `POST /admissions/applications/{id}/submit`  
Vérifie paiement `COMPLETED` et `isComplete = true`. Re-vérifie les deadlines. Verrouille le dossier. Gèle le profil. Publie `ApplicationSubmitted`.

### UC-ADM-006 - Validation administrative
**Acteur :** ADMIN_SCHOLAR | **Endpoint :** `PUT /admissions/admin/applications/{id}/admin-review`  
Si conforme → `PENDING_COMMISSION`, choix transmis à la commission.  
Si non conforme → `ADDITIONAL_DOCS_REQUIRED`, dossier déverrouillé.

### UC-ADM-007 - Vote de la commission
**Acteur :** TEACHER (membre) | **Endpoint :** `POST /admissions/commissions/{id}/votes`  
Vote `ACCEPT`, `REJECT` ou `ABSTAIN`. Un membre ne peut voter qu'une fois par choix.

### UC-ADM-008 - Validation de la décision (président)
**Acteur :** TEACHER (président) | **Endpoint :** `POST /admissions/commissions/{id}/choices/{id}/validate`  
Décision : `ACCEPTED`, `REJECTED`, `WAITLISTED` ou `INTERVIEW_REQUIRED`.  
Si `WAITLISTED` → crée `WaitlistEntry` avec rang calculé.  
Déclenche l'évaluation globale de la candidature.

### UC-ADM-009 - Planifier un entretien
**Acteur :** ADMIN_SCHOLAR | **Endpoint :** `POST /admissions/applications/{id}/choices/{id}/interview`  
Choix doit être en `INTERVIEW_REQUIRED`. Publie `InterviewScheduled`.

### UC-ADM-010 - Clôturer un entretien
**Acteur :** TEACHER (président) | **Endpoint :** `PUT /admissions/interviews/{id}/complete`  
Ajoute les notes confidentielles. Choix → `INTERVIEW_DONE`. Déclenche un nouveau vote.

### UC-ADM-011 - Accord directeur de thèse
**Acteur :** TEACHER (HDR) | **Endpoint :** `PUT /admissions/thesis-approvals/{id}/respond`  
Accord → choix → `PENDING_COMMISSION`. Refus → choix → `REJECTED`.

### UC-ADM-012 - Gestion liste d'attente
**Acteur :** Système | Déclenché quand une place se libère.  
Promu le rang 1 → `PROMOTED`, délai 48h. Sans réponse → `EXPIRED`, rang 2 promu.

### UC-ADM-013 - Confirmation du choix
**Acteur :** CANDIDATE | **Endpoint :** `POST /admissions/applications/{id}/confirm`  
Confirme un choix `ACCEPTED`. Retire les autres `ACCEPTED`. Publie `ApplicationAccepted` avec matricule.

### UC-ADM-014 - Confirmation automatique (expiration)
**Acteur :** Scheduler | Tous les jours à :30.  
Confirme automatiquement le choix `ACCEPTED` de priorité 1. `autoConfirmed = true`.

### UC-ADM-015 - Évaluation globale
**Acteur :** Système | Déclenché après chaque décision finale sur un choix.  
Si au moins 1 `ACCEPTED` → `ConfirmationRequest` + `AWAITING_CONFIRMATION`.  
Si tous `REJECTED` → `REJECTED` + `ApplicationRejected` publié.

---

## 9. Règles métier transversales

### Calcul du résultat du vote

```
Résultat = majorité des votes ACCEPT vs REJECT (abstentions exclues)
Égalité  = voix du président prépondérante (suggestedDecision = "TIE")
Quorum   = nombre minimum de votants requis (défaut 3)
Si quorum non atteint → suggestedDecision = "QUORUM_NOT_REACHED"
```

### Délais importants

| Délai | Valeur | Déclencheur |
|---|---|---|
| Confirmation candidat | 5 jours ouvrables | `confirmationDeadlineDays` de la campagne |
| Promotion liste d'attente | 48 heures | À partir de `promotedAt` |
| Accord directeur de thèse | 15 jours | Rappel automatique à J+7 |
| Annulation entretien | Min 48h avant | Avant `scheduledAt` |

### Gestion des places

```
Ajout choix      : AdmissionOffer.currentCount++
Retrait choix    : AdmissionOffer.currentCount--
Confirmation     : acceptedCount++
Waitlist         : waitlistCount++
acceptedCount >= maxCapacity → OfferStatus = FULL
```

### Génération du matricule

Format : `STU-{année}-{séquence sur 5 chiffres}`  
Exemple : `STU-2026-00042`  
Implémentation : `AtomicLong` - threadsafe, sans base de données.

---

## 10. Sécurité des routes

| Route | Méthode | Rôles |
|---|---|---|
| `/admissions/campaigns/**` | GET | Public |
| `/admissions/campaigns/**` | POST/PUT | SUPER_ADMIN |
| `/admissions/offers/**` | GET | Public |
| `/admissions/offers/**` | POST/PUT | SUPER_ADMIN |
| `/admissions/required-documents/**` | GET | Public |
| `/admissions/required-documents/**` | POST/DELETE | SUPER_ADMIN |
| `/admissions/applications/**` | ALL | CANDIDATE, STUDENT, ADMIN_SCHOLAR, SUPER_ADMIN |
| `/admissions/admin/**` | ALL | ADMIN_SCHOLAR, SUPER_ADMIN |
| `/admissions/commissions/**` | GET | SUPER_ADMIN, ADMIN_SCHOLAR, TEACHER |
| `/admissions/commissions/**` | POST | SUPER_ADMIN, TEACHER |
| `/admissions/commissions/**` | DELETE | SUPER_ADMIN |
| `/admissions/interviews/**` | ALL | ADMIN_SCHOLAR, TEACHER, SUPER_ADMIN |
| `/admissions/thesis-approvals/**` | ALL | TEACHER, SUPER_ADMIN |
| `/admissions/waitlist/**` | ALL | ADMIN_SCHOLAR, SUPER_ADMIN |

**Headers injectés par la Gateway :**
- `X-User-Email` - email de l'utilisateur connecté
- `X-User-Role` - rôle (SUPER_ADMIN, ADMIN_SCHOLAR, TEACHER, CANDIDATE, STUDENT...)
- `X-User-Id` - identifiant userId (claim custom dans le JWT)

---

## 11. Événements Kafka

### Consommés (1)

| Topic | Producteur | Action |
|---|---|---|
| `payment.completed` | Paiement Service | `AdmissionPayment → COMPLETED`, `Application → PAID` |

### Publiés (10)

| Topic | Déclencheur | Consommateurs |
|---|---|---|
| `application.submitted` | Soumission dossier | Notification Service |
| `application.admin.review` | Décision admin | Notification Service |
| `application.pending.commission` | Transmission commission | Notification Service |
| `interview.scheduled` | Entretien planifié | Notification Service |
| `thesis.approval.requested` | Demande directeur | Notification Service |
| `application.awaiting.confirmation` | Choix accepté | Notification Service |
| `application.accepted` | Confirmation définitive | Auth Service, User Service, Notification Service, Document Service |
| `application.rejected` | Tous choix refusés | Notification Service |
| `waitlist.promoted` | Place libérée | Notification Service |
| `choice.auto.confirmed` | Délai expiré | Notification Service |

### Structure de `ApplicationAcceptedEvent`

```java
// Champs publiés dans application.accepted
applicationId, userId, studentNumber, filiereId
personalEmail, firstName, lastName
birthDate, birthPlace, nationality, gender, phone, address, photoUrl
currentInstitution, currentDiploma, graduationYear
autoConfirmed, academicYear
```

---

## 12. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **Auth Service** | Consommateur Kafka | Consomme `application.accepted` → crée compte étudiant |
| **User Service** | Consommateur Kafka | Consomme `application.accepted` → crée profil étudiant |
| **Paiement Service** | Producteur Kafka | Publie `payment.completed` → Admission Service met à jour le paiement |
| **Notification Service** | Consommateur Kafka | Consomme tous les events de notification |
| **Document Service** | Consommateur Kafka | Consomme `application.accepted` → génère la lettre d'admission |

---

## 13. Schedulers

| Cron | Méthode | Description |
|---|---|---|
| `0 5 0 * * *` | `processCampaignTransitions()` | UPCOMING→OPEN et OPEN→CLOSED |
| `0 10 0 * * *` | `processOfferTransitions()` | Deadline→CLOSED et FULL |
| `0 0 * * * *` | `processExpiredPromotions()` | Expirations liste d'attente (48h) |
| `0 0 6 * * *` | `processExpiredApprovals()` + `sendReminders()` | Directeurs de thèse |
| `0 30 * * * *` | `processExpiredConfirmations()` | Confirmations automatiques |

---

## 14. Endpoints API complets

### Campagnes (public GET, SUPER_ADMIN POST/PUT)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/campaigns` | Lister toutes les campagnes |
| GET | `/admissions/campaigns/current` | Campagne OPEN |
| GET | `/admissions/campaigns/{id}` | Détail |
| POST | `/admissions/campaigns` | Créer |
| PUT | `/admissions/campaigns/{id}/status` | Changer statut |
| GET | `/admissions/campaigns/{id}/stats` | Statistiques |

### Offres (public GET, SUPER_ADMIN POST/PUT)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/offers?campaignId=&level=` | Lister |
| GET | `/admissions/offers/{id}` | Détail avec documents requis |
| POST | `/admissions/offers` | Créer |
| PUT | `/admissions/offers/{id}` | Modifier |
| GET | `/admissions/required-documents?offerId=` | Documents requis |
| POST | `/admissions/required-documents` | Définir un document requis |
| DELETE | `/admissions/required-documents/{id}` | Supprimer |

### Candidatures - flux candidat

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/applications` | Mes candidatures |
| GET | `/admissions/applications/{id}` | Détail |
| POST | `/admissions/applications` | Créer |
| DELETE | `/admissions/applications/{id}` | Retirer (DRAFT) |
| GET | `/admissions/applications/{id}/profile` | Profil candidat |
| PUT | `/admissions/applications/{id}/profile` | Remplir profil |
| POST | `/admissions/applications/{id}/choices` | Ajouter choix |
| DELETE | `/admissions/applications/{id}/choices/{id}` | Retirer choix |
| PUT | `/admissions/applications/{id}/choices/reorder` | Réordonner |
| POST | `/admissions/applications/{id}/documents` | Uploader document |
| DELETE | `/admissions/applications/{id}/documents/{id}` | Supprimer document |
| POST | `/admissions/applications/{id}/payment` | Initier paiement |
| POST | `/admissions/applications/{id}/submit` | Soumettre |
| GET | `/admissions/applications/{id}/confirmation` | Statut confirmation |
| POST | `/admissions/applications/{id}/confirm` | Confirmer choix |

### Administration

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/admin/applications` | Lister toutes |
| GET | `/admissions/admin/applications/{id}` | Dossier complet |
| PUT | `/admissions/admin/applications/{id}/admin-review` | Valider/refuser |
| POST | `/admissions/admin/applications/{id}/request-docs` | Demander documents |
| PUT | `/admissions/admin/applications/{id}/forward-commission` | Transmettre commission |

### Commissions

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/commissions` | Lister |
| GET | `/admissions/commissions/{id}` | Détail |
| POST | `/admissions/commissions/{id}/members` | Ajouter membre |
| DELETE | `/admissions/commissions/{id}/members/{id}` | Retirer membre |
| GET | `/admissions/commissions/{id}/choices` | Dossiers à examiner |
| POST | `/admissions/commissions/{id}/votes` | Voter |
| GET | `/admissions/commissions/{id}/choices/{id}/vote-result` | Résultat vote |
| POST | `/admissions/commissions/{id}/choices/{id}/validate` | Décision finale |

### Entretiens

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/admissions/applications/{id}/choices/{id}/interview` | Planifier |
| GET | `/admissions/interviews/{id}` | Détail |
| PUT | `/admissions/interviews/{id}/complete` | Clôturer |
| PUT | `/admissions/interviews/{id}/cancel` | Annuler |

### Directeurs de thèse

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/thesis-approvals` | Demandes en attente |
| PUT | `/admissions/thesis-approvals/{id}/respond` | Répondre |

### Liste d'attente

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/admissions/waitlist?offerId=` | Liste d'attente d'une offre |

---

## 15. Configuration

### `application.yml` (local)

```yaml
spring:
  application:
    name: admission-service
  config:
    import: optional:configserver:http://localhost:8761
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### `admission-service.yaml` (Config Server)

```yaml
server:
  port: 8084

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/admissiondb
    username: admission_service
    password: admission_service#123@
  jpa:
    hibernate:
      ddl-auto: update
  kafka:
    bootstrap-servers: localhost:9092
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

### Base de données

```sql
CREATE DATABASE admissiondb;
CREATE USER admission_service WITH PASSWORD 'admission_service#123@';
GRANT ALL PRIVILEGES ON DATABASE admissiondb TO admission_service;
```

### Swagger

```
Documentation locale  : http://localhost:8084/swagger-ui.html
Via Gateway          : http://localhost:8888/swagger-ui.html (sélectionner "Admission Service")
API docs             : http://localhost:8084/v3/api-docs
```
