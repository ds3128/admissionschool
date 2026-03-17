# Admission Service - Documentation Technique v4

**Projet :** AdmissionSchool  
**Service :** Admission Service  
**Port :** 8084  
**Version :** 4.0.0  
**Dernière mise à jour :** Mars 2026  
**Changelog v4 :**
- Ajout de l'entité `CandidateProfile` - le dossier d'admission porte le profil complet du candidat
- Ajout du parcours candidat pas à pas avec les services impliqués à chaque étape
- Mise à jour de l'événement `ApplicationAccepted` avec les données du profil
- Clarification des routes publiques (sans JWT) vs routes authentifiées
- Architecture Option A : front-end appelle chaque service directement via la Gateway

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Cycle d'une campagne d'admission](#2-cycle-dune-campagne-dadmission)
3. [Parcours candidat pas à pas - services impliqués](#3-parcours-candidat-pas-à-pas--services-impliqués)
4. [Flux par niveau d'études](#4-flux-par-niveau-détudes)
5. [Modèle de domaine - Description des entités](#5-modèle-de-domaine--description-des-entités)
6. [Énumérations](#6-énumérations)
7. [Cas d'utilisation](#7-cas-dutilisation)
8. [Règles métier transversales](#8-règles-métier-transversales)
9. [Dépendances cross-services](#9-dépendances-cross-services)
10. [Résumé des endpoints API](#10-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

L'Admission Service gère l'intégralité du processus de candidature à l'université, depuis l'ouverture de la campagne jusqu'à la confirmation de l'inscription de l'étudiant. Il orchestre trois flux distincts selon le niveau d'études visé : Licence, Master et Doctorat.

### Spécificités v3

- Trois flux distincts adaptés au niveau d'études : **Licence** (commission seule), **Master** (commission + entretien possible), **Doctorat** (accord directeur de thèse + commission + entretien).
- La commission pédagogique **vote à la majorité** - chaque membre vote individuellement, la décision est validée par le président.
- La **liste d'attente est active** - chaque candidat a un rang, les places libérées sont proposées dans l'ordre.
- La campagne d'admission s'ouvre sur une **période définie** (ex : janvier → avril) - aucune soumission n'est acceptée hors de cette fenêtre.
- Chaque offre de formation possède sa propre **deadline** dans la fenêtre de la campagne.

### Responsabilités

- Gestion du cycle de vie de la campagne et des offres de formation
- Gestion des candidatures avec jusqu'à 3 choix ordonnés
- Validation administrative par la scolarité centrale
- Gestion des commissions pédagogiques et de leurs votes
- Planification et suivi des entretiens (Master / Doctorat)
- Gestion de l'accord du directeur de thèse (Doctorat)
- Gestion de la liste d'attente active avec promotion automatique
- Gestion de la confirmation du candidat parmi ses choix acceptés
- Déclenchement de la création du compte étudiant après confirmation

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Génération de l'email institutionnel | Auth Service |
| Création du compte étudiant | Auth Service |
| Création du profil étudiant | User Service |
| Paiement des frais de dossier | Paiement Service |
| Envoi des notifications | Notification Service |
| Génération des documents d'admission | Document Service |

---

## 2. Cycle d'une campagne d'admission

Une campagne d'admission est une période définie pendant laquelle les candidats peuvent soumettre leurs dossiers. Une fois la campagne clôturée, aucune nouvelle soumission n'est acceptée. Les dossiers déjà soumis continuent leur traitement.

```
Janvier              Avril              Mai - Juin           Juillet
    │                   │                    │                  │
    ▼                   ▼                    ▼                  ▼
 OPEN ──────────────► CLOSED ──────────► Délibérations ──► Résultats
 Candidats soumettent  Plus de           Commissions        Confirmations
 leurs dossiers        soumissions       examinent          des candidats
 (max 3 choix)         acceptées         les dossiers       acceptés
                       Dossiers en cours
                       continuent
```

### Règles du cycle

- La campagne passe automatiquement à `OPEN` à la date `startDate`.
- La campagne passe automatiquement à `CLOSED` à la date `endDate + 1 jour`.
- Chaque offre de formation peut avoir une `deadline` propre dans la fenêtre de la campagne.
- Une offre peut fermer avant la fin de la campagne (ex : Master ferme le 15 mars, Licence le 1er avril).
- Aucune candidature ne peut être créée en dehors d'une campagne `OPEN`.
- Aucun dossier ne peut être soumis après la `deadline` de toutes ses offres sélectionnées.

---


## 3. Parcours candidat pas à pas - services impliqués

Cette section décrit les interactions exactes entre le front-end, la Gateway et chaque microservice à chaque étape du parcours candidat. L'architecture retenue est l'**Option A** : le front-end appelle chaque service directement via la Gateway, sans couche d'agrégation intermédiaire (pas de BFF).

---

### Étape 1 - Consulter les formations disponibles (sans compte)

```
Navigateur
    ↓ GET /admissions/offers?campaignId=current
API Gateway (route publique - pas de filtre JWT)
    ↓
Admission Service
    → Retourne la liste des AdmissionOffer :
      filière, niveau, deadline, places restantes, statut
```

**Service impliqué :** Admission Service uniquement.  
**Authentification :** Aucune - route publique.

---

### Étape 2 - Créer son compte

```
Candidat saisit : email personnel + password
    ↓ POST /auth/register
API Gateway → Auth Service
    → Crée Users (status = false, role = CANDIDATE)
    → Génère token JWT de vérification (24h)
    → Envoie email d'activation
         ↓
Candidat clique le lien reçu par email
    ↓ GET /auth/verify?token=...
Auth Service
    → Valide le token
    → status = true
    → Publie UserActivated (Kafka)
         ↓
User Service consomme UserActivated
    → Crée UserProfile vide (userId, email)
```

**Services impliqués :** Auth Service, User Service (via Kafka).  
**Authentification :** Aucune pour register et verify.

---

### Étape 3 - Remplir son profil personnel

```
Candidat saisit : prénom, nom, date de naissance, nationalité,
                  téléphone, adresse, photo d'identité
    ↓ PUT /users/me
API Gateway (JWT requis) → User Service
    → Met à jour UserProfile
    → Retourne profil mis à jour
```

**Service impliqué :** User Service.  
**Authentification :** JWT valide requis.  
**Note :** Cette étape est distincte du dossier d'admission. Le profil User Service est le profil de référence de l'utilisateur dans tout le système.

---

### Étape 4 - Créer le dossier de candidature

```
Candidat clique "Déposer ma candidature"
    ↓ POST /admissions/applications
API Gateway (JWT requis) → Admission Service
    → Vérifie campagne OPEN
    → Crée Application (DRAFT)
    → Crée Dossier vide
    → Crée CandidateProfile vide lié au dossier
    → Retourne application + liste RequiredDocuments par filière
```

**Service impliqué :** Admission Service.  
**Authentification :** JWT requis (`X-User-Email` injecté par Gateway).

---

### Étape 5 - Remplir le profil candidat dans le dossier

```
Candidat complète les informations académiques :
  établissement d'origine, diplôme obtenu, mention,
  année d'obtention, projet de recherche (Doctorat)
    ↓ PUT /admissions/applications/{id}/profile
API Gateway (JWT requis) → Admission Service
    → Met à jour CandidateProfile
    → Recalcule isComplete sur le Dossier
```

**Service impliqué :** Admission Service.  
**Note :** Les informations personnelles de base (prénom, nom, etc.) sont récupérées depuis le User Service au moment de la soumission pour construire le profil complet du dossier. Le candidat ne les ressaisit pas.

---

### Étape 6 - Ajouter ses choix de formation

```
Candidat sélectionne 1, 2 ou 3 formations dans la liste
    ↓ POST /admissions/applications/{id}/choices (x1 à x3)
API Gateway → Admission Service
    → Vérifie deadline + disponibilité de l'offre
    → Vérifie absence de doublon
    → Crée ApplicationChoice (PENDING_ADMIN)
    → Incrémente AdmissionOffer.currentCount
```

**Service impliqué :** Admission Service.

---

### Étape 7 - Uploader les documents justificatifs

```
Candidat uploade : diplôme, relevés de notes,
                   lettre de motivation, CV, etc.
    ↓ POST /admissions/applications/{id}/documents
API Gateway → Admission Service
    → Valide format (PDF, JPEG, PNG) et taille (max 5 Mo)
    → Stocke le fichier
    → Crée DossierDocument (PENDING)
    → Recalcule isComplete
```

**Service impliqué :** Admission Service.

---

### Étape 8 - Payer les frais de dossier

```
Candidat clique "Payer les frais"
    ↓ POST /admissions/applications/{id}/payment
API Gateway → Admission Service
    → Crée AdmissionPayment (PENDING)
    → Appelle Paiement Service (HTTP synchrone)
          ↓
    Paiement Service traite la transaction
    Publie PaymentCompleted (Kafka)
          ↓
    Admission Service consomme PaymentCompleted
    → AdmissionPayment → COMPLETED
    → Application → PAID
    → Notification Service notifie le candidat
```

**Services impliqués :** Admission Service, Paiement Service (sync), Notification Service (async).

---

### Étape 9 - Soumettre le dossier

```
Candidat clique "Soumettre mon dossier"
    ↓ POST /admissions/applications/{id}/submit
API Gateway → Admission Service
    → Vérifie paiement COMPLETED
    → Vérifie isComplete = true
    → Re-vérifie deadlines de tous les choix
    → Récupère les données UserProfile depuis User Service
      (pour compléter CandidateProfile)
    → Application → SUBMITTED
    → Dossier verrouillé (isLocked = true)
    → Publie ApplicationSubmitted (Kafka)
          ↓
    Notification Service → email de confirmation au candidat
```

**Services impliqués :** Admission Service, User Service (sync pour compléter le profil), Notification Service (async).

---

### Étape 10 - Suivi du dossier (espace candidat)

```
Candidat consulte son tableau de bord
    ↓ GET /admissions/applications/{id}
API Gateway → Admission Service
    → Retourne :
      - statut courant + date
      - choix avec statuts individuels
      - documents avec statuts
      - statut paiement
      - délai de confirmation si AWAITING_CONFIRMATION
      - historique des changements de statut
      - notes du conseil (si communication autorisée)
```

**Service impliqué :** Admission Service uniquement.

---

### Étape 11 - Confirmer son choix de formation (après acceptation)

```
Candidat reçoit notification "Vous avez été accepté(e)"
    ↓ GET /admissions/applications/{id}/confirmation
API Gateway → Admission Service
    → Retourne la liste des choix ACCEPTED + délai restant

Candidat choisit la formation à confirmer
    ↓ POST /admissions/applications/{id}/confirm
API Gateway → Admission Service
    → Choix → CONFIRMED
    → Autres choix ACCEPTED → WITHDRAWN
    → Places libérées sur les offres retirées
    → Application → ACCEPTED
    → Publie ApplicationAccepted (Kafka)
          ↓
    Auth Service    → Génère email institutionnel
                    → Crée compte avec mustChangePassword = true
    User Service    → Crée profil étudiant complet
                      (depuis CandidateProfile + UserProfile)
    Notification    → Envoie credentials sur email personnel
    Document Service→ Génère lettre d'admission
```

**Services impliqués :** Admission Service, Auth Service, User Service, Notification Service, Document Service (tous via Kafka).

---

### Vue d'ensemble des services par étape

| Étape | Service principal | Services secondaires | Auth |
|---|---|---|---|
| 1. Voir les formations | Admission Service | - | ❌ Public |
| 2. Créer le compte | Auth Service | User Service (Kafka) | ❌ Public |
| 3. Remplir profil personnel | User Service | - | ✅ JWT |
| 4. Créer dossier | Admission Service | - | ✅ JWT |
| 5. Remplir profil candidat | Admission Service | - | ✅ JWT |
| 6. Ajouter choix | Admission Service | - | ✅ JWT |
| 7. Uploader documents | Admission Service | - | ✅ JWT |
| 8. Payer les frais | Admission Service | Paiement Service | ✅ JWT |
| 9. Soumettre | Admission Service | User Service, Notification | ✅ JWT |
| 10. Suivre le dossier | Admission Service | - | ✅ JWT |
| 11. Confirmer le choix | Admission Service | Auth, User, Notif, Doc | ✅ JWT |

---

## 4. Flux par niveau d'études

### 3.1 Licence (L1, L2, L3)

Flux simplifié : validation administrative puis commission pédagogique, vote à la majorité, pas d'entretien.

```
Candidat soumet le dossier (SUBMITTED)
        ↓
Scolarité centrale (ADMIN_SCHOLAR)
  Vérifie : conformité administrative, éligibilité,
             documents complets, niveau requis
  Statut : UNDER_ADMIN_REVIEW
        ↓
  ┌─────┴──────────────────────┐
  ▼                            ▼
Non conforme               Conforme
  ↓                            ↓
ADDITIONAL_DOCS          PENDING_COMMISSION
_REQUIRED                (attribué à la commission
Dossier déverrouillé      pédagogique de la filière)
Candidat resubmet                ↓
                        UNDER_COMMISSION_REVIEW
                        Membres consultent le dossier
                        Vote individuel à la majorité
                        Président valide la décision
                                 ↓
               ┌─────────────────┼──────────────────┐
               ▼                 ▼                   ▼
           ACCEPTED          REJECTED            WAITLISTED
               ↓                 ↓               (rang assigné)
    ConfirmationRequest      Notification            ↓
    créée (5 jours)          candidat        Place libérée ?
    Candidat confirme                        Promotion rang 1
    son choix                                Délai 48h
```

---

### 3.2 Master (M1, M2)

Flux intermédiaire : validation administrative, commission pédagogique avec vote, entretien possible avant décision finale.

```
Candidat soumet le dossier (SUBMITTED)
        ↓
Scolarité centrale
  Statut : UNDER_ADMIN_REVIEW
        ↓
PENDING_COMMISSION
        ↓
UNDER_COMMISSION_REVIEW
Commission examine le dossier
Vote à la majorité sur l'orientation
        ↓
  ┌─────┴──────────────────────────────┐
  ▼                                    ▼
Décision sur dossier                Entretien requis
ACCEPTED / REJECTED / WAITLISTED    INTERVIEW_REQUIRED
                                          ↓
                                    Planification entretien
                                    INTERVIEW_SCHEDULED
                                    Notification candidat
                                    (date, heure, lieu/visio)
                                          ↓
                                    Entretien réalisé
                                    INTERVIEW_DONE
                                    Commission délibère
                                    Vote à la majorité
                                          ↓
                                   ACCEPTED / REJECTED
                                   WAITLISTED
                                          ↓
                                ConfirmationRequest si ACCEPTED
```

---

### 3.3 Doctorat

Flux complet : accord préalable du directeur de thèse, puis commission doctorale, entretien quasi-systématique.

```
Candidat soumet le dossier (SUBMITTED)
  (inclut : projet de recherche, directeur pressenti)
        ↓
École doctorale / Scolarité centrale
  Statut : UNDER_ADMIN_REVIEW
        ↓
PENDING_THESIS_DIRECTOR
Demande envoyée au directeur de thèse pressenti
        ↓
  ┌─────┴──────────────────────────────┐
  ▼                                    ▼
Directeur REFUSE               Directeur APPROUVE
  ↓                                    ↓
REJECTED                      PENDING_COMMISSION
Motif notifié                         ↓
                              UNDER_COMMISSION_REVIEW
                              Commission examine :
                              projet recherche, CV,
                              publications, motivations
                              Vote à la majorité
                                       ↓
                              INTERVIEW_REQUIRED
                              (quasi-systématique)
                                       ↓
                              INTERVIEW_SCHEDULED
                                       ↓
                              INTERVIEW_DONE
                              Délibération finale
                              Vote à la majorité
                                       ↓
                         ACCEPTED / REJECTED / WAITLISTED
```

---

### 3.4 Flux de confirmation commun (tous niveaux)

Une fois au moins un choix accepté, le flux de confirmation est identique quel que soit le niveau :

```
Au moins 1 choix ACCEPTED (tous niveaux)
        ↓
ConfirmationRequest créée
expiresAt = now + 5 jours ouvrables
Statut application : AWAITING_CONFIRMATION
Notification candidat avec liste des choix acceptés
        ↓
  ┌─────┴──────────────────────────────┐
  ▼                                    ▼
Candidat confirme                 Délai expiré
dans les 5 jours                  (job schedulé)
  ↓                                    ↓
Choix → CONFIRMED               Choix priorité 1
Autres → WITHDRAWN              → CONFIRMED auto
Places libérées                 Autres → WITHDRAWN
  ↓                                    ↓
ApplicationAccepted publié      ApplicationAccepted publié
Auth Service : crée compte      autoConfirmed = true
User Service : crée profil
Notification : envoi credentials
```

---

## 5. Modèle de domaine - Description des entités

### 4.1 `AdmissionCampaign`

Représente une campagne d'admission pour une année académique donnée. Définit la fenêtre temporelle pendant laquelle les candidatures sont acceptées.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `academicYear` | String | Année académique (ex : 2025-2026) |
| `startDate` | LocalDate | Date d'ouverture des candidatures |
| `endDate` | LocalDate | Date de clôture des candidatures |
| `resultsDate` | LocalDate | Date prévue de publication des résultats |
| `confirmationDeadlineDays` | int | Délai de confirmation en jours ouvrables (défaut : 5) |
| `feeAmount` | BigDecimal | Montant des frais de dossier |
| `status` | CampaignStatus | UPCOMING, OPEN, CLOSED, ARCHIVED |
| `maxChoicesPerApplication` | int | Nombre maximum de choix (défaut : 3) |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Unicité de l'`academicYear` - une seule campagne par année.
- Transition automatique `UPCOMING` → `OPEN` à la date `startDate`.
- Transition automatique `OPEN` → `CLOSED` à la date `endDate + 1 jour`.
- Les dossiers soumis avant la clôture continuent leur traitement après `CLOSED`.

---

### 4.2 `AdmissionOffer`

Représente une offre de formation spécifique rattachée à une campagne, avec sa propre deadline et capacité.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `campaignId` | Long | Référence vers `AdmissionCampaign.id` |
| `filiereId` | Long | Référence vers la filière dans le User Service |
| `filiereName` | String | Nom de la filière (dénormalisé) |
| `level` | OfferLevel | LICENCE, MASTER, DOCTORAT |
| `deadline` | LocalDate | Date limite de candidature pour cette offre |
| `maxCapacity` | int | Nombre de places disponibles |
| `currentCount` | int | Nombre de candidatures actives |
| `acceptedCount` | int | Nombre d'inscriptions confirmées |
| `waitlistCount` | int | Nombre de candidats en liste d'attente |
| `status` | OfferStatus | OPEN, CLOSED, FULL |

**Règles métier :**
- `status` passe à `CLOSED` automatiquement quand `deadline < today`.
- `status` passe à `FULL` quand `acceptedCount >= maxCapacity`.
- Un candidat ne peut sélectionner une offre que si `status = OPEN`.
- `currentCount` est incrémenté à l'ajout d'un choix, décrémenté si `WITHDRAWN`.
- `acceptedCount` est incrémenté uniquement à la confirmation définitive.

---

### 4.3 `Application`

Représente le dossier de candidature global d'un utilisateur pour une campagne.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Référence vers `Users.id` dans l'Auth Service |
| `campaignId` | Long | Référence vers `AdmissionCampaign.id` |
| `status` | ApplicationStatus | Statut courant |
| `academicYear` | String | Année académique visée |
| `submittedAt` | LocalDateTime | Date de soumission officielle |
| `paidAt` | LocalDateTime | Date de confirmation du paiement |
| `lastStatusChange` | LocalDateTime | Date du dernier changement de statut |
| `createdAt` | LocalDateTime | Date de création |
| `updatedAt` | LocalDateTime | Date de dernière modification |

**Règles métier :**
- Un candidat ne peut avoir qu'une seule application par campagne.
- Une application `DRAFT` peut être supprimée par le candidat.
- Une application `SUBMITTED` ou au-delà est immuable côté candidat.

---

### 4.4 `ApplicationChoice`

Représente un choix de formation individuel dans une candidature. Chaque choix suit son propre flux selon le niveau d'études.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `offerId` | Long | Référence vers `AdmissionOffer.id` |
| `filiereId` | Long | Dénormalisé |
| `filiereName` | String | Dénormalisé pour historique |
| `level` | OfferLevel | LICENCE, MASTER, DOCTORAT - détermine le flux |
| `choiceOrder` | int | Priorité - 1 (haute), 2, 3 |
| `status` | ChoiceStatus | Statut courant du choix |
| `decidedAt` | LocalDateTime | Date de décision finale |
| `decidedBy` | UUID | ID de l'admin ou président de commission |
| `decisionReason` | String | Motif de refus si applicable |

**Règles métier :**
- Deux choix d'une même application ne peuvent pas pointer vers la même offre.
- Le `choiceOrder` peut être modifié tant que l'application est en `DRAFT`.
- Le `level` de l'offre détermine automatiquement le flux appliqué.

---

### 4.5 `Dossier`

Représente l'ensemble des documents associés à une candidature.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `documents` | List\<DossierDocument\> | Documents soumis |
| `isComplete` | boolean | Tous les documents obligatoires présents |
| `isLocked` | boolean | Verrouillé après soumission |
| `lockedAt` | LocalDateTime | Date de verrouillage |
| `unlockReason` | String | Motif de déverrouillage |

**Règles métier :**
- Verrouillage automatique à la soumission.
- Déverrouillage uniquement si statut `ADDITIONAL_DOCS_REQUIRED`.
- `isComplete` recalculé à chaque modification de document.

---

### 4.6 `DossierDocument`

Représente un document individuel dans un dossier.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `dossierId` | Long | Référence vers `Dossier.id` |
| `type` | DocumentType | Type de document |
| `fileName` | String | Nom original du fichier |
| `fileUrl` | String | URL de stockage |
| `fileSize` | Long | Taille en octets |
| `mimeType` | String | Type MIME |
| `status` | DocumentStatus | PENDING, VALIDATED, REJECTED |
| `rejectionReason` | String | Motif de rejet |
| `uploadedAt` | LocalDateTime | Date d'upload |
| `validatedAt` | LocalDateTime | Date de validation |

---

### 4.7 `RequiredDocument`

Définit les documents obligatoires pour une offre de formation.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `offerId` | Long | Référence vers `AdmissionOffer.id` |
| `documentType` | DocumentType | Type requis |
| `label` | String | Libellé affiché au candidat |
| `description` | String | Instructions détaillées |
| `isMandatory` | boolean | Obligatoire ou optionnel |
| `maxFileSizeMb` | int | Taille maximale en Mo |

---

### 4.8 `ReviewCommission`

Représente la commission pédagogique chargée d'examiner les dossiers pour une offre de formation.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom de la commission |
| `offerId` | Long | Référence vers `AdmissionOffer.id` |
| `type` | CommissionType | LICENCE, MASTER, DOCTORAT |
| `presidentId` | UUID | Enseignant président de la commission |
| `quorum` | int | Nombre minimum de votants requis |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Une commission est créée par offre de formation.
- Le quorum doit être atteint pour qu'un vote soit valide.
- Le président valide la décision finale après délibération.

---

### 4.9 `CommissionMember`

Représente un membre d'une commission pédagogique.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `commissionId` | Long | Référence vers `ReviewCommission.id` |
| `teacherId` | UUID | Référence vers l'enseignant dans le User Service |
| `role` | MemberRole | PRESIDENT, MEMBER, RAPPORTEUR |
| `joinedAt` | LocalDateTime | Date d'entrée dans la commission |

---

### 4.10 `CommissionVote`

Représente le vote individuel d'un membre de la commission sur un choix de formation.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `choiceId` | Long | Référence vers `ApplicationChoice.id` |
| `commissionId` | Long | Référence vers `ReviewCommission.id` |
| `memberId` | UUID | ID du membre votant |
| `vote` | VoteType | ACCEPT, REJECT, ABSTAIN |
| `comment` | String | Commentaire optionnel |
| `votedAt` | LocalDateTime | Date du vote |

**Règles métier :**
- Un membre ne peut voter qu'une seule fois par choix.
- Un membre absent peut s'abstenir (`ABSTAIN`).
- Le résultat est calculé à la majorité des votes ACCEPT/REJECT (hors abstentions).
- Si égalité : la voix du président est prépondérante.
- Le vote n'est valide que si le quorum est atteint.

---

### 4.11 `Interview`

Représente un entretien planifié pour un choix de formation (Master / Doctorat).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `choiceId` | Long | Référence vers `ApplicationChoice.id` |
| `scheduledAt` | LocalDateTime | Date et heure planifiées |
| `duration` | int | Durée en minutes |
| `location` | String | Salle ou lien visioconférence |
| `type` | InterviewType | PRESENTIEL, VISIO |
| `status` | InterviewStatus | SCHEDULED, DONE, CANCELLED, RESCHEDULED |
| `interviewers` | List\<UUID\> | IDs des enseignants présents |
| `notes` | String | Notes post-entretien (visibles uniquement de la commission) |
| `createdAt` | LocalDateTime | Date de planification |

**Règles métier :**
- Un entretien ne peut être planifié que si le statut du choix est `INTERVIEW_REQUIRED`.
- La notification au candidat est envoyée dès la planification.
- Annulation possible avec un délai minimum de 48h avant l'entretien.
- Les notes post-entretien sont confidentielles - non visibles par le candidat.

---

### 4.12 `ThesisDirectorApproval`

Représente la demande d'accord du directeur de thèse pour une candidature Doctorat.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `choiceId` | Long | Référence vers `ApplicationChoice.id` |
| `directorId` | UUID | ID de l'enseignant HDR pressenti |
| `researchProject` | String | Titre et résumé du projet de recherche |
| `status` | ApprovalStatus | PENDING, APPROVED, REFUSED |
| `comment` | String | Commentaire du directeur |
| `requestedAt` | LocalDateTime | Date d'envoi de la demande |
| `respondedAt` | LocalDateTime | Date de réponse |
| `expiresAt` | LocalDateTime | Délai de réponse (15 jours par défaut) |

**Règles métier :**
- Sans accord du directeur, le dossier Doctorat ne peut pas avancer vers la commission.
- Si le directeur ne répond pas dans le délai → rappel automatique à J+7, puis rejet automatique à expiration.
- Un directeur de thèse doit avoir le grade HDR (Habilitation à Diriger des Recherches).
- Un directeur ne peut pas approuver plus de N thèses simultanément (configurable).

---

### 4.13 `WaitlistEntry`

Représente la position d'un candidat sur la liste d'attente d'une offre.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `choiceId` | Long | Référence vers `ApplicationChoice.id` |
| `offerId` | Long | Référence vers `AdmissionOffer.id` |
| `rank` | int | Rang sur la liste (1 = premier) |
| `status` | WaitlistStatus | WAITING, PROMOTED, CONFIRMED, EXPIRED, WITHDRAWN |
| `promotedAt` | LocalDateTime | Date de promotion (place disponible) |
| `expiresAt` | LocalDateTime | Délai pour confirmer si promu (48h) |
| `notifiedAt` | LocalDateTime | Date de notification de promotion |

**Règles métier :**
- Le rang est attribué dans l'ordre chronologique des décisions `WAITLISTED`.
- Quand une place se libère, le candidat en rang 1 est automatiquement promu.
- Le candidat promu a 48h pour confirmer sa place.
- Sans réponse dans le délai → statut `EXPIRED`, rang 2 promu automatiquement.
- Si le candidat refuse → statut `WITHDRAWN`, rang 2 promu automatiquement.

---

### 4.14 `ConfirmationRequest`

Représente la demande de confirmation envoyée au candidat quand au moins un choix est accepté.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `acceptedChoiceIds` | List\<Long\> | IDs des choix `ACCEPTED` |
| `expiresAt` | LocalDateTime | Date limite de confirmation |
| `confirmedChoiceId` | Long | ID du choix confirmé (null avant confirmation) |
| `status` | ConfirmationStatus | PENDING, CONFIRMED, EXPIRED |
| `autoConfirmed` | boolean | `true` si confirmé automatiquement |
| `createdAt` | LocalDateTime | Date de création |
| `confirmedAt` | LocalDateTime | Date de confirmation effective |

---

### 4.15 `Decision`

Représente la décision globale sur l'application une fois tous les choix traités.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `result` | DecisionType | AWAITING_CONFIRMATION, REJECTED |
| `decidedAt` | LocalDateTime | Date de la décision globale |
| `notifiedAt` | LocalDateTime | Date de notification du candidat |

---

### 4.16 `AdmissionPayment`

Représente le paiement des frais de dossier.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `amount` | BigDecimal | Montant payé |
| `currency` | String | Devise (EUR, XAF, etc.) |
| `status` | PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED |
| `paymentReference` | String | Référence externe |
| `paidAt` | LocalDateTime | Date de confirmation |

---

### 4.17 `ApplicationStatusHistory`

Trace l'historique complet des changements de statut.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `applicationId` | UUID | Référence vers `Application.id` |
| `fromStatus` | ApplicationStatus | Statut précédent |
| `toStatus` | ApplicationStatus | Nouveau statut |
| `changedBy` | UUID | ID de l'acteur (admin, système, candidat) |
| `changedAt` | LocalDateTime | Date du changement |
| `comment` | String | Commentaire optionnel |

---

## 6. Énumérations

### `ApplicationStatus`
| Valeur | Description |
|---|---|
| `DRAFT` | Brouillon en cours de remplissage |
| `PAID` | Frais de dossier payés |
| `SUBMITTED` | Dossier soumis et verrouillé |
| `UNDER_ADMIN_REVIEW` | Vérification administrative en cours |
| `ADDITIONAL_DOCS_REQUIRED` | Documents supplémentaires demandés |
| `PENDING_COMMISSION` | Transmis à la commission, en attente |
| `UNDER_COMMISSION_REVIEW` | Commission en cours d'examen |
| `PENDING_THESIS_DIRECTOR` | En attente accord directeur (Doctorat) |
| `INTERVIEW_SCHEDULED` | Entretien planifié |
| `INTERVIEW_DONE` | Entretien réalisé, délibération finale |
| `AWAITING_CONFIRMATION` | Au moins 1 choix accepté, candidat doit confirmer |
| `ACCEPTED` | Confirmation effectuée, inscription déclenchée |
| `REJECTED` | Tous les choix refusés |
| `WITHDRAWN` | Retirée par le candidat (DRAFT uniquement) |

### `ChoiceStatus`
| Valeur | Description |
|---|---|
| `PENDING_ADMIN` | En attente de validation administrative |
| `PENDING_COMMISSION` | Transmis à la commission |
| `UNDER_COMMISSION_REVIEW` | Commission délibère |
| `PENDING_THESIS_DIRECTOR` | En attente accord directeur (Doctorat) |
| `INTERVIEW_REQUIRED` | Entretien requis avant décision |
| `INTERVIEW_SCHEDULED` | Entretien planifié |
| `INTERVIEW_DONE` | Entretien réalisé, vote final en attente |
| `WAITLISTED` | Sur liste d'attente avec rang |
| `PROMOTED_FROM_WAITLIST` | Promu depuis liste d'attente - délai 48h |
| `ACCEPTED` | Accepté |
| `REJECTED` | Refusé |
| `CONFIRMED` | Confirmé par le candidat - inscription déclenchée |
| `WITHDRAWN` | Retiré automatiquement |

### `OfferLevel`
| Valeur | Description |
|---|---|
| `LICENCE` | Niveau Licence (L1, L2, L3) |
| `MASTER` | Niveau Master (M1, M2) |
| `DOCTORAT` | Niveau Doctorat |

### `CommissionType`
| Valeur | Description |
|---|---|
| `LICENCE` | Vote sur dossier, pas d'entretien |
| `MASTER` | Vote sur dossier + entretien possible |
| `DOCTORAT` | Accord directeur + vote + entretien |

### `MemberRole`
| Valeur | Description |
|---|---|
| `PRESIDENT` | Voix prépondérante en cas d'égalité |
| `MEMBER` | Membre votant |
| `RAPPORTEUR` | Rapporteur du dossier (présente le dossier à la commission) |

### `VoteType`
| Valeur | Description |
|---|---|
| `ACCEPT` | Vote favorable |
| `REJECT` | Vote défavorable |
| `ABSTAIN` | Abstention |

### `InterviewType`
| Valeur | Description |
|---|---|
| `PRESENTIEL` | En personne |
| `VISIO` | Visioconférence |

### `InterviewStatus`
| Valeur | Description |
|---|---|
| `SCHEDULED` | Planifié |
| `DONE` | Réalisé |
| `CANCELLED` | Annulé |
| `RESCHEDULED` | Reporté |

### `ApprovalStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente de réponse du directeur |
| `APPROVED` | Accord donné |
| `REFUSED` | Refus du directeur |
| `EXPIRED` | Délai expiré sans réponse |

### `WaitlistStatus`
| Valeur | Description |
|---|---|
| `WAITING` | En attente d'une place |
| `PROMOTED` | Place disponible, délai 48h pour confirmer |
| `CONFIRMED` | Place confirmée |
| `EXPIRED` | Délai expiré après promotion |
| `WITHDRAWN` | Retiré de la liste |

### `ConfirmationStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente de la confirmation du candidat |
| `CONFIRMED` | Confirmé manuellement |
| `EXPIRED` | Délai expiré, confirmation automatique effectuée |

### `DocumentType`
| Valeur | Description |
|---|---|
| `DIPLOME_BAC` | Diplôme du baccalauréat |
| `RELEVE_NOTES_BAC` | Relevé de notes du baccalauréat |
| `DIPLOME_LICENCE` | Diplôme de licence |
| `RELEVE_NOTES_LICENCE` | Relevé de notes de licence |
| `DIPLOME_MASTER` | Diplôme de master |
| `PROJET_RECHERCHE` | Projet de recherche (Doctorat) |
| `LETTRE_MOTIVATION` | Lettre de motivation |
| `CV` | Curriculum vitae |
| `PIECE_IDENTITE` | Pièce d'identité |
| `PHOTO_IDENTITE` | Photo d'identité |
| `CERTIFICAT_MEDICAL` | Certificat médical |
| `LETTRE_RECOMMANDATION` | Lettre de recommandation |
| `PUBLICATIONS` | Liste de publications (Doctorat) |
| `AUTRE` | Autre document |

---

## 7. Cas d'utilisation

### UC-ADM-001 - Créer une campagne d'admission

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /admissions/campaigns`  
**Préconditions :** Aucune campagne `OPEN` pour la même année académique

**Scénario principal :**
1. L'administrateur définit l'année académique, les dates d'ouverture et de clôture, le montant des frais et le délai de confirmation.
2. Le système vérifie l'unicité de l'année académique.
3. Le système crée la campagne avec le statut `UPCOMING`.
4. Un job schedulé passe la campagne à `OPEN` à la date `startDate`.
5. Un job schedulé passe la campagne à `CLOSED` à la date `endDate + 1 jour`.

---

### UC-ADM-002 - Créer une offre de formation

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /admissions/offers`  
**Préconditions :** La campagne cible est `UPCOMING` ou `OPEN`

**Scénario principal :**
1. L'administrateur envoie `campaignId`, `filiereId`, `level`, `deadline`, `maxCapacity`.
2. Le système vérifie que la filière existe dans le User Service.
3. Le système vérifie que la `deadline` est dans la fenêtre de la campagne.
4. Le système crée l'offre avec `status = OPEN` et les compteurs à zéro.
5. Le système crée automatiquement la `ReviewCommission` associée.
6. Le système retourne l'offre créée.

---

### UC-ADM-003 - Créer une candidature (brouillon)

**Acteur :** Candidat (`CANDIDATE`)  
**Déclencheur :** Requête `POST /admissions/applications`  
**Préconditions :**
- Une campagne est en statut `OPEN`
- Le candidat n'a pas déjà une candidature pour cette campagne

**Scénario principal :**
1. Le système vérifie qu'une campagne `OPEN` existe.
2. Le système crée une `Application` en statut `DRAFT`.
3. Le système crée un `Dossier` vide.
4. Le système retourne la candidature et la liste des offres disponibles.

---

### UC-ADM-004 - Ajouter un choix de formation

**Acteur :** Candidat  
**Déclencheur :** Requête `POST /admissions/applications/{id}/choices`  
**Préconditions :** Application en `DRAFT`, moins de 3 choix actifs

**Scénario principal :**
1. Le candidat envoie l'`offerId` et l'ordre de priorité.
2. Le système vérifie que l'offre est `OPEN` et que sa deadline n'est pas dépassée.
3. Le système vérifie l'absence de doublon.
4. Le système vérifie que le nombre de choix actifs est inférieur à 3.
5. Le système crée l'`ApplicationChoice` en statut `PENDING_ADMIN`.
6. Le système incrémente `AdmissionOffer.currentCount`.

**Scénarios alternatifs :**
- Offre `CLOSED` ou `FULL` → `422 Unprocessable Entity`.
- Deadline dépassée → `422 Unprocessable Entity`.
- Doublon → `409 Conflict`.
- Maximum atteint → `422 Unprocessable Entity`.

---

### UC-ADM-005 - Payer les frais et soumettre

**Acteur :** Candidat  
**Déclencheur :** Requête `POST /admissions/applications/{id}/payment` puis `POST /admissions/applications/{id}/submit`

**Scénario principal (paiement) :**
1. Le candidat initie le paiement (au moins 1 choix requis).
2. Le système crée un `AdmissionPayment` en `PENDING`.
3. À la confirmation du Paiement Service → statut `COMPLETED`, application → `PAID`.

**Scénario principal (soumission) :**
1. Le candidat soumet son dossier.
2. Le système vérifie le paiement `COMPLETED` et `isComplete = true`.
3. Le système re-vérifie les deadlines de tous les choix.
4. Choix expirés → retirés automatiquement + notification.
5. Si aucun choix restant → soumission bloquée.
6. Le système récupère les données `UserProfile` depuis le User Service (prénom, nom, photo) et les intègre dans le `CandidateProfile`.
7. Application → `SUBMITTED`, dossier verrouillé, `CandidateProfile` figé, événement `ApplicationSubmitted` publié.

---

### UC-ADM-006 - Validation administrative

**Acteur :** Scolarité centrale (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /admissions/admin/applications/{id}/admin-review`  
**Préconditions :** Application en `SUBMITTED`

**Scénario principal :**
1. L'administrateur ouvre le dossier.
2. Application → `UNDER_ADMIN_REVIEW`.
3. L'administrateur vérifie la conformité administrative (documents, éligibilité).
4. Si conforme → application → `PENDING_COMMISSION`, choix transmis à la commission.
5. Si non conforme → application → `ADDITIONAL_DOCS_REQUIRED`, dossier déverrouillé.

---

### UC-ADM-007 - Vote de la commission sur un choix

**Acteur :** Membre de la commission (`TEACHER` avec rôle commission)  
**Déclencheur :** Requête `POST /admissions/commissions/{commissionId}/votes`  
**Préconditions :** Le choix est en `UNDER_COMMISSION_REVIEW`

**Scénario principal :**
1. Le membre envoie son vote (`ACCEPT`, `REJECT`, `ABSTAIN`) et un commentaire optionnel.
2. Le système enregistre le `CommissionVote`.
3. Le système vérifie si tous les membres ont voté.
4. Si quorum atteint et tous ont voté → calcul du résultat à la majorité.
5. En cas d'égalité → voix du président prépondérante.
6. Si résultat → président valide (UC-ADM-008).

---

### UC-ADM-008 - Validation de la décision par le président

**Acteur :** Président de commission  
**Déclencheur :** Requête `POST /admissions/commissions/{commissionId}/choices/{choiceId}/validate`

**Scénario principal :**
1. Le président consulte le résultat du vote.
2. Le président valide ou ajuste la décision (ACCEPTED / REJECTED / WAITLISTED / INTERVIEW_REQUIRED).
3. Le système met à jour le `ChoiceStatus`.
4. Si `INTERVIEW_REQUIRED` → choix attend la planification d'un entretien.
5. Si `WAITLISTED` → crée une `WaitlistEntry` avec rang calculé.
6. Si tous les choix ont une décision finale → déclenche UC-ADM-014 (évaluation globale).

---

### UC-ADM-009 - Planifier un entretien

**Acteur :** Commission / Scolarité (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `POST /admissions/applications/{id}/choices/{choiceId}/interview`  
**Préconditions :** Choix en `INTERVIEW_REQUIRED`

**Scénario principal :**
1. L'administrateur envoie la date, durée, lieu/lien, type et liste des interviewers.
2. Le système crée l'`Interview` en statut `SCHEDULED`.
3. Le choix passe à `INTERVIEW_SCHEDULED`.
4. Le système notifie le candidat (date, heure, lieu/lien visio).

---

### UC-ADM-010 - Enregistrer la décision post-entretien

**Acteur :** Président de commission  
**Déclencheur :** Requête `PUT /admissions/interviews/{id}/complete`  
**Préconditions :** Entretien en statut `SCHEDULED`

**Scénario principal :**
1. Le président marque l'entretien comme réalisé et ajoute les notes (confidentielles).
2. Interview → `DONE`, choix → `INTERVIEW_DONE`.
3. Un nouveau vote de commission est déclenché (post-entretien).
4. Après vote → président valide la décision finale.
5. Choix → `ACCEPTED` / `REJECTED` / `WAITLISTED`.

---

### UC-ADM-011 - Demande d'accord au directeur de thèse (Doctorat)

**Acteur :** Système (automatique après validation admin)  
**Déclencheur :** Choix de niveau `DOCTORAT` passe à `PENDING_COMMISSION`

**Scénario principal :**
1. Le système identifie le directeur de thèse pressenti depuis les données du candidat.
2. Le système crée une `ThesisDirectorApproval` en statut `PENDING`.
3. Le système notifie le directeur avec le projet de recherche et les informations du candidat.
4. Le directeur dispose de 15 jours pour répondre.
5. Rappel automatique à J+7 si pas de réponse.

**Scénarios :**
- Directeur approuve → choix passe à `PENDING_COMMISSION`, commission doctorale saisie.
- Directeur refuse → choix → `REJECTED`, motif notifié au candidat.
- Délai expiré → `ThesisDirectorApproval.status = EXPIRED` → choix → `REJECTED`.

---

### UC-ADM-012 - Gestion de la liste d'attente

**Acteur :** Système (automatique)  
**Déclencheur :** Place libérée dans une offre (confirmation retirée, expiration)

**Scénario principal :**
1. Le système détecte une libération de place dans une offre.
2. Le système identifie la `WaitlistEntry` avec le rang le plus bas (`rank = 1`).
3. Le système passe son statut à `PROMOTED`.
4. Le système calcule `expiresAt = now + 48h`.
5. Le système notifie le candidat : "Une place s'est libérée - vous avez 48h pour confirmer."
6. Si le candidat confirme → `WaitlistEntry.status = CONFIRMED`, inscription déclenchée.
7. Si délai expiré ou refus → `WaitlistEntry.status = EXPIRED` ou `WITHDRAWN`, rang 2 promu.

---

### UC-ADM-013 - Confirmer un choix de formation

**Acteur :** Candidat  
**Déclencheur :** Requête `POST /admissions/applications/{id}/confirm`  
**Préconditions :** Application en `AWAITING_CONFIRMATION`, délai non expiré

**Scénario principal :**
1. Le candidat envoie l'ID du choix confirmé.
2. Le système vérifie que le choix est `ACCEPTED` et appartient à cette application.
3. Le choix → `CONFIRMED`, les autres choix `ACCEPTED` → `WITHDRAWN`.
4. Les places des offres retirées sont libérées (`currentCount--`).
5. La place confirmée est comptabilisée (`acceptedCount++`).
6. Application → `ACCEPTED`.
7. Événement `ApplicationAccepted` publié.

---

### UC-ADM-014 - Expiration du délai de confirmation

**Acteur :** Système (job schedulé quotidien)  
**Déclencheur :** `ConfirmationRequest.expiresAt < now()` et `status = PENDING`

**Scénario principal :**
1. Le job détecte les demandes expirées.
2. Le système confirme automatiquement le choix `ACCEPTED` avec le `choiceOrder` le plus bas.
3. `ConfirmationRequest.autoConfirmed = true`, `status = EXPIRED`.
4. Même traitement que UC-ADM-013.
5. Notification candidat : confirmation automatique effectuée.

---

### UC-ADM-015 - Évaluation globale après décision sur tous les choix

**Acteur :** Système (automatique)  
**Déclencheur :** Tous les choix d'une application ont une décision finale

**Scénario principal :**
1. Le système vérifie qu'il n'existe plus de choix en statut intermédiaire.
2. Si au moins un choix est `ACCEPTED` → `ConfirmationRequest` créée, application → `AWAITING_CONFIRMATION`.
3. Si tous les choix sont `REJECTED` → `Decision` créée avec `result = REJECTED`, application → `REJECTED`, notification candidat.
4. Si certains choix sont `WAITLISTED` → traitement liste d'attente selon UC-ADM-012.

---

## 8. Règles métier transversales

### Calcul du résultat du vote

```
Résultat = majorité des votes ACCEPT vs REJECT (abstentions exclues)
Égalité  = voix du président prépondérante
Quorum   = nombre minimum de votants défini par la commission
Si quorum non atteint → vote invalide, nouveau vote requis
```

### Délais importants

```
Confirmation candidat    : 5 jours ouvrables après AWAITING_CONFIRMATION
Promotion liste d'attente: 48 heures pour confirmer
Accord directeur thèse   : 15 jours (rappel à J+7, expiration → rejet)
Annulation entretien     : minimum 48h avant l'entretien
```

### Deadline des offres

```
Ajout d'un choix :
  deadline >= today ET status = OPEN → OK
  deadline < today → 422 "Date limite dépassée pour cette formation"

Soumission :
  Re-vérification de toutes les deadlines
  Choix expiré → retiré + notification candidat
  Tous expirés → soumission bloquée
```

### Gestion des places

```
Ajout choix      : currentCount++
Retrait choix    : currentCount--
Confirmation     : acceptedCount++, currentCount--
Waitlist         : waitlistCount++
currentCount >= maxCapacity → OfferStatus = FULL
```

### Génération de l'email institutionnel

Déléguée à l'**Auth Service** via `ApplicationAccepted` :

```
prenom.nom@universite.edu
Doublon → prenom.nom2@universite.edu
Normalisation : minuscules, sans accents, espaces → tirets
```

---

## 9. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **Auth Service** | Consommateur Kafka | Consomme `ApplicationAccepted` pour créer le compte étudiant |
| **User Service** | HTTP synchrone | Vérifie l'existence des filières et des enseignants |
| **User Service** | Consommateur Kafka | Consomme `ApplicationAccepted` pour créer le profil étudiant |
| **Paiement Service** | HTTP synchrone | Traitement du paiement des frais de dossier |
| **Notification Service** | Consommateur Kafka | Notifie à chaque changement de statut |
| **Document Service** | HTTP synchrone | Génération du bulletin d'admission après confirmation |

### Événements publiés (Kafka)

| Événement | Déclencheur | Consommateurs |
|---|---|---|
| `ApplicationSubmitted` | Soumission | Notification Service |
| `ApplicationUnderAdminReview` | Prise en charge admin | Notification Service |
| `ApplicationAdditionalDocsRequired` | Docs manquants | Notification Service |
| `ApplicationPendingCommission` | Transmis à commission | Notification Service |
| `InterviewScheduled` | Entretien planifié | Notification Service |
| `ThesisDirectorApprovalRequested` | Demande directeur | Notification Service |
| `ApplicationAwaitingConfirmation` | Choix acceptés | Notification Service |
| `ApplicationAccepted` | Confirmation définitive | Auth Service, User Service, Notification Service, Document Service |
| `ApplicationRejected` | Tous refusés | Notification Service |
| `WaitlistPromoted` | Place libérée | Notification Service |
| `ChoiceAutoConfirmed` | Expiration délai | Notification Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `PaymentCompleted` | Paiement Service | Application → `PAID` |

---

## 10. Résumé des endpoints API

### Candidat

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `POST` | `/admissions/applications` | Créer une candidature | `CANDIDATE` |
| `GET` | `/admissions/applications` | Lister ses candidatures | `CANDIDATE` |
| `GET` | `/admissions/applications/{id}` | Consulter sa candidature | `CANDIDATE` |
| `DELETE` | `/admissions/applications/{id}` | Retirer (DRAFT uniquement) | `CANDIDATE` |
| `POST` | `/admissions/applications/{id}/choices` | Ajouter un choix | `CANDIDATE` |
| `DELETE` | `/admissions/applications/{id}/choices/{cid}` | Retirer un choix | `CANDIDATE` |
| `PUT` | `/admissions/applications/{id}/choices/reorder` | Réordonner les choix | `CANDIDATE` |
| `PUT` | `/admissions/applications/{id}/profile` | Remplir le profil candidat | `CANDIDATE` |
| `GET` | `/admissions/applications/{id}/profile` | Consulter son profil candidat | `CANDIDATE` |
| `POST` | `/admissions/applications/{id}/documents` | Uploader un document | `CANDIDATE` |
| `DELETE` | `/admissions/applications/{id}/documents/{did}` | Supprimer un document | `CANDIDATE` |
| `POST` | `/admissions/applications/{id}/payment` | Initier le paiement | `CANDIDATE` |
| `POST` | `/admissions/applications/{id}/submit` | Soumettre la candidature | `CANDIDATE` |
| `GET` | `/admissions/applications/{id}/confirmation` | Statut de confirmation | `CANDIDATE` |
| `POST` | `/admissions/applications/{id}/confirm` | Confirmer un choix | `CANDIDATE` |

### Administration

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/admissions/admin/applications` | Lister toutes les candidatures | `ADMIN_SCHOLAR` |
| `GET` | `/admissions/admin/applications/{id}` | Dossier complet | `ADMIN_SCHOLAR` |
| `PUT` | `/admissions/admin/applications/{id}/admin-review` | Prise en charge admin | `ADMIN_SCHOLAR` |
| `POST` | `/admissions/admin/applications/{id}/request-docs` | Demander des documents | `ADMIN_SCHOLAR` |
| `PUT` | `/admissions/admin/applications/{id}/forward-commission` | Transmettre à la commission | `ADMIN_SCHOLAR` |
| `GET` | `/admissions/admin/stats` | Statistiques campagne | `ADMIN_SCHOLAR` |

### Commission

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/admissions/commissions/{id}/choices` | Dossiers à examiner | `TEACHER` (membre) |
| `POST` | `/admissions/commissions/{id}/votes` | Voter sur un choix | `TEACHER` (membre) |
| `POST` | `/admissions/commissions/{id}/choices/{cid}/validate` | Valider la décision | `TEACHER` (président) |
| `POST` | `/admissions/applications/{id}/choices/{cid}/interview` | Planifier un entretien | `ADMIN_SCHOLAR` |
| `PUT` | `/admissions/interviews/{id}/complete` | Clôturer un entretien | `TEACHER` (président) |
| `PUT` | `/admissions/interviews/{id}/cancel` | Annuler un entretien | `ADMIN_SCHOLAR` |

### Directeur de thèse

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/admissions/thesis-approvals` | Demandes en attente | `TEACHER` (HDR) |
| `PUT` | `/admissions/thesis-approvals/{id}/respond` | Répondre à une demande | `TEACHER` (HDR) |

### Offres et campagnes

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/admissions/campaigns` | Lister les campagnes | Public |
| `GET` | `/admissions/campaigns/current` | Campagne en cours | Public |
| `POST` | `/admissions/campaigns` | Créer une campagne | `SUPER_ADMIN` |
| `PUT` | `/admissions/campaigns/{id}/status` | Changer le statut | `SUPER_ADMIN` |
| `GET` | `/admissions/offers` | Lister les offres | Public |
| `POST` | `/admissions/offers` | Créer une offre | `SUPER_ADMIN` |
| `PUT` | `/admissions/offers/{id}` | Modifier une offre | `SUPER_ADMIN` |
| `GET` | `/admissions/required-documents?offerId=` | Documents requis | Public |
| `POST` | `/admissions/required-documents` | Définir les documents requis | `SUPER_ADMIN` |

### Commissions

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/admissions/commissions` | Lister les commissions | `ADMIN_SCHOLAR` |
| `POST` | `/admissions/commissions` | Créer une commission | `SUPER_ADMIN` |
| `POST` | `/admissions/commissions/{id}/members` | Ajouter un membre | `SUPER_ADMIN` |
| `DELETE` | `/admissions/commissions/{id}/members/{mid}` | Retirer un membre | `SUPER_ADMIN` |
