# Admission Service - User Stories complètes

**Projet :** AdmissionSchool  
**Service :** Admission Service  
**Version :** 1.0.0  
**Date :** Mars 2026

---

## Acteurs

| Acteur | Rôle système | Description |
|---|---|---|
| **Super Admin** | `SUPER_ADMIN` | Administrateur système - configure les campagnes et offres |
| **Scolarité** | `ADMIN_SCHOLAR` | Gestionnaire académique - traite les dossiers administrativement |
| **Candidat** | `CANDIDATE` | Utilisateur non encore inscrit - dépose sa candidature |
| **Étudiant** | `STUDENT` | Utilisateur inscrit - peut consulter ses anciennes candidatures |
| **Enseignant** | `TEACHER` | Membre de commission ou directeur de thèse |
| **Système** | `system` | Jobs schedulés - actions automatiques |

---

## EPIC 1 - Gestion des campagnes d'admission

---

### US-001 - Créer une campagne d'admission

**En tant que** Super Admin  
**Je veux** créer une campagne d'admission pour une année académique  
**Afin de** définir la fenêtre temporelle pendant laquelle les candidatures sont acceptées

**Critères d'acceptation :**
- [ ] Je peux saisir l'année académique au format `YYYY-YYYY` (ex : 2026-2027)
- [ ] Je peux définir une date d'ouverture et une date de clôture
- [ ] Je peux définir le montant des frais de dossier
- [ ] Je peux configurer le délai de confirmation en jours ouvrables (défaut : 5)
- [ ] Je peux configurer le nombre maximum de choix par candidature (défaut : 3)
- [ ] Le système rejette la création si une campagne existe déjà pour cette année académique (409 Conflict)
- [ ] Le système rejette la création si la date de clôture est antérieure à la date d'ouverture (422)
- [ ] La campagne est créée en statut `UPCOMING`

**Endpoint :** `POST /admissions/campaigns`

---

### US-002 - Consulter les campagnes disponibles

**En tant que** visiteur (non authentifié)  
**Je veux** consulter la liste des campagnes d'admission  
**Afin de** connaître les périodes de candidature

**Critères d'acceptation :**
- [ ] La liste est accessible sans token JWT
- [ ] Je vois toutes les campagnes (UPCOMING, OPEN, CLOSED)
- [ ] Je vois la campagne actuellement ouverte via `/admissions/campaigns/current`
- [ ] Si aucune campagne n'est ouverte, je reçois une erreur 404 explicite

**Endpoints :** `GET /admissions/campaigns`, `GET /admissions/campaigns/current`

---

### US-003 - Gérer le cycle de vie d'une campagne

**En tant que** Super Admin  
**Je veux** pouvoir changer manuellement le statut d'une campagne  
**Afin de** gérer des situations exceptionnelles (ouverture anticipée, clôture prématurée)

**Critères d'acceptation :**
- [ ] Je peux passer une campagne `UPCOMING` → `OPEN` manuellement
- [ ] Je peux passer une campagne `OPEN` → `CLOSED` manuellement
- [ ] Je peux archiver une campagne `CLOSED` → `ARCHIVED`
- [ ] Le système refuse tout changement depuis `ARCHIVED` (irréversible)

**Endpoint :** `PUT /admissions/campaigns/{id}/status`

---

### US-004 - Transitions automatiques de la campagne

**En tant que** Système  
**Je veux** que les transitions de statut soient automatiques selon les dates  
**Afin de** ne pas avoir à gérer manuellement l'ouverture et la clôture

**Critères d'acceptation :**
- [ ] La campagne passe automatiquement `UPCOMING → OPEN` quand `startDate <= today` (scheduler 00:05)
- [ ] La campagne passe automatiquement `OPEN → CLOSED` quand `endDate < today` (scheduler 00:05)
- [ ] Les dossiers soumis avant la clôture continuent leur traitement après `CLOSED`

---

### US-005 - Consulter les statistiques d'une campagne

**En tant que** Super Admin ou Scolarité  
**Je veux** voir les statistiques d'une campagne  
**Afin de** suivre l'avancement des candidatures

**Critères d'acceptation :**
- [ ] Je vois le nombre total de candidatures
- [ ] Je vois la répartition par statut (DRAFT, SUBMITTED, UNDER_REVIEW, ACCEPTED, REJECTED, etc.)
- [ ] Je vois le nombre de candidats en attente de confirmation

**Endpoint :** `GET /admissions/campaigns/{id}/stats`

---

## EPIC 2 - Gestion des offres de formation

---

### US-006 - Créer une offre de formation

**En tant que** Super Admin  
**Je veux** créer une offre de formation dans une campagne  
**Afin de** permettre aux candidats de postuler à une filière

**Critères d'acceptation :**
- [ ] Je peux associer l'offre à une filière existante dans le User Service (filiereId)
- [ ] Je peux définir le niveau (LICENCE, MASTER, DOCTORAT)
- [ ] Je peux définir une deadline propre à l'offre (dans la fenêtre de la campagne)
- [ ] Je peux définir le nombre de places disponibles
- [ ] Le système rejette si la deadline est hors de la fenêtre de la campagne (422)
- [ ] Le système crée automatiquement la `ReviewCommission` associée
- [ ] L'offre est créée en statut `OPEN`

**Endpoint :** `POST /admissions/offers`

---

### US-007 - Consulter les offres disponibles

**En tant que** visiteur (non authentifié)  
**Je veux** voir les offres de formation disponibles  
**Afin de** choisir les formations auxquelles je souhaite candidater

**Critères d'acceptation :**
- [ ] La liste est accessible sans token JWT
- [ ] Je peux filtrer par campagne et par niveau (LICENCE, MASTER, DOCTORAT)
- [ ] Je vois les places restantes pour chaque offre (`availablePlaces = maxCapacity - acceptedCount`)
- [ ] Je vois la deadline de chaque offre
- [ ] Je vois les documents requis pour chaque offre

**Endpoints :** `GET /admissions/offers?campaignId=&level=`, `GET /admissions/offers/{id}`

---

### US-008 - Gérer les transitions automatiques des offres

**En tant que** Système  
**Je veux** que les offres passent automatiquement à `CLOSED` ou `FULL`  
**Afin de** garantir que les candidats ne postulent pas à des offres expirées

**Critères d'acceptation :**
- [ ] Une offre passe à `CLOSED` quand sa deadline est dépassée (scheduler 00:10)
- [ ] Une offre passe à `FULL` quand `acceptedCount >= maxCapacity` (scheduler 00:10)
- [ ] Un candidat ne peut pas ajouter un choix vers une offre `CLOSED` ou `FULL`

---

### US-009 - Définir les documents requis pour une offre

**En tant que** Super Admin  
**Je veux** définir quels documents sont requis pour une offre  
**Afin de** guider les candidats dans la constitution de leur dossier

**Critères d'acceptation :**
- [ ] Je peux définir un ou plusieurs types de documents (DIPLOME_BAC, CV, LETTRE_MOTIVATION, etc.)
- [ ] Je peux marquer chaque document comme obligatoire ou optionnel
- [ ] Je peux définir la taille maximale par document (défaut 5 Mo)
- [ ] Je peux supprimer un document requis
- [ ] Les documents requis sont visibles publiquement

**Endpoints :** `POST /admissions/required-documents`, `DELETE /admissions/required-documents/{id}`

---

## EPIC 3 - Flux candidat

---

### US-010 - Créer sa candidature

**En tant que** Candidat  
**Je veux** créer une candidature pour la campagne en cours  
**Afin de** démarrer le processus d'inscription à l'université

**Critères d'acceptation :**
- [ ] Je ne peux créer une candidature que si une campagne est en statut `OPEN`
- [ ] Je ne peux avoir qu'une seule candidature par campagne (409 si doublon)
- [ ] Ma candidature est créée en statut `DRAFT`
- [ ] Un dossier vide et un profil candidat vide sont créés automatiquement

**Endpoint :** `POST /admissions/applications`

---

### US-011 - Remplir mon profil candidat

**En tant que** Candidat  
**Je veux** remplir les informations académiques de mon profil  
**Afin de** compléter mon dossier de candidature

**Critères d'acceptation :**
- [ ] Je peux saisir mon établissement d'origine, diplôme, mention et année d'obtention
- [ ] Pour une candidature Doctorat, je peux saisir mon projet de recherche et le nom du directeur pressenti
- [ ] Je peux saisir une lettre de motivation
- [ ] Le profil ne peut être modifié que si le dossier n'est pas verrouillé
- [ ] Le champ `isComplete` est recalculé automatiquement à chaque modification

**Endpoint :** `PUT /admissions/applications/{id}/profile`

---

### US-012 - Ajouter des choix de formation

**En tant que** Candidat  
**Je veux** sélectionner les formations auxquelles je souhaite candidater  
**Afin de** exprimer mes préférences par ordre de priorité

**Critères d'acceptation :**
- [ ] Je peux ajouter de 1 à 3 choix (limité par `maxChoicesPerApplication` de la campagne)
- [ ] Chaque choix a un ordre de priorité (1 = priorité haute)
- [ ] Je ne peux pas sélectionner deux fois la même offre (409 Conflict)
- [ ] Je ne peux pas sélectionner une offre dont la deadline est dépassée (422)
- [ ] Je ne peux pas sélectionner une offre `CLOSED` ou `FULL` (422)
- [ ] Chaque choix ajouté incrémente `AdmissionOffer.currentCount`
- [ ] Je ne peux modifier les choix que si ma candidature est en `DRAFT`

**Endpoint :** `POST /admissions/applications/{id}/choices`

---

### US-013 - Réordonner mes choix

**En tant que** Candidat  
**Je veux** modifier l'ordre de priorité de mes choix  
**Afin de** ajuster mes préférences avant la soumission

**Critères d'acceptation :**
- [ ] Je peux changer l'ordre de priorité de chaque choix
- [ ] La réorganisation n'est possible que si la candidature est en `DRAFT`

**Endpoint :** `PUT /admissions/applications/{id}/choices/reorder`

---

### US-014 - Retirer un choix

**En tant que** Candidat  
**Je veux** retirer un choix de ma candidature  
**Afin de** ajuster ma sélection avant la soumission

**Critères d'acceptation :**
- [ ] Je peux retirer un choix tant que ma candidature est en `DRAFT`
- [ ] Le retrait décrémente `AdmissionOffer.currentCount`

**Endpoint :** `DELETE /admissions/applications/{id}/choices/{choiceId}`

---

### US-015 - Uploader mes documents justificatifs

**En tant que** Candidat  
**Je veux** uploader les documents requis pour ma candidature  
**Afin de** constituer un dossier complet

**Critères d'acceptation :**
- [ ] Je peux uploader des fichiers au format PDF, JPEG ou PNG (422 sinon)
- [ ] La taille maximale par fichier est de 5 Mo (422 si dépassée)
- [ ] Le dossier doit être déverrouillé pour uploader
- [ ] Chaque upload recalcule automatiquement `isComplete` du dossier
- [ ] `isComplete = true` quand tous les documents obligatoires de toutes mes offres sont présents et non rejetés

**Endpoint :** `POST /admissions/applications/{id}/documents` (multipart)

---

### US-016 - Supprimer un document

**En tant que** Candidat  
**Je veux** supprimer un document uploadé par erreur  
**Afin de** le remplacer par le bon fichier

**Critères d'acceptation :**
- [ ] Je peux supprimer un document tant que le dossier n'est pas verrouillé
- [ ] Après suppression, `isComplete` est recalculé

**Endpoint :** `DELETE /admissions/applications/{id}/documents/{documentId}`

---

### US-017 - Payer les frais de dossier

**En tant que** Candidat  
**Je veux** payer les frais de dossier  
**Afin de** pouvoir soumettre ma candidature

**Critères d'acceptation :**
- [ ] Je ne peux initier le paiement que si j'ai au moins un choix actif
- [ ] Un `AdmissionPayment` en statut `PENDING` est créé avec le montant de la campagne
- [ ] Après confirmation du Paiement Service (Kafka `PaymentCompleted`), ma candidature passe en `PAID`
- [ ] Je ne peux pas initier un deuxième paiement si le premier est déjà `COMPLETED` (422)

**Endpoint :** `POST /admissions/applications/{id}/payment`

---

### US-018 - Soumettre ma candidature

**En tant que** Candidat  
**Je veux** soumettre officiellement ma candidature  
**Afin de** la faire examiner par la commission

**Critères d'acceptation :**
- [ ] Je ne peux soumettre que si le paiement est `COMPLETED` (422 sinon)
- [ ] Je ne peux soumettre que si `isComplete = true` (422 sinon)
- [ ] Le système re-vérifie les deadlines de tous mes choix actifs à la soumission
- [ ] Les choix dont la deadline est dépassée sont automatiquement retirés
- [ ] Si tous mes choix sont retirés → soumission bloquée (422)
- [ ] Mon dossier est verrouillé (`isLocked = true`)
- [ ] Mon profil candidat est gelé (`isFrozen = true`)
- [ ] Ma candidature passe en `SUBMITTED`
- [ ] L'événement `ApplicationSubmitted` est publié sur Kafka

**Endpoint :** `POST /admissions/applications/{id}/submit`

---

### US-019 - Suivre l'avancement de ma candidature

**En tant que** Candidat  
**Je veux** consulter l'état de ma candidature à tout moment  
**Afin de** savoir où en est mon dossier

**Critères d'acceptation :**
- [ ] Je vois le statut courant de ma candidature
- [ ] Je vois le statut de chacun de mes choix
- [ ] Je vois l'historique complet des changements de statut (avec dates et acteurs)
- [ ] Je vois l'état de mon dossier (complet, verrouillé)
- [ ] Je vois l'état de mon paiement

**Endpoint :** `GET /admissions/applications/{id}`

---

### US-020 - Confirmer mon choix de formation

**En tant que** Candidat  
**Je veux** confirmer la formation que je souhaite intégrer parmi celles qui m'ont accepté  
**Afin de** finaliser mon inscription

**Critères d'acceptation :**
- [ ] Je ne peux confirmer que si ma candidature est en `AWAITING_CONFIRMATION`
- [ ] Je ne peux confirmer que dans le délai imparti (avant `expiresAt`)
- [ ] Je dois choisir parmi les choix en statut `ACCEPTED`
- [ ] Le choix confirmé passe en `CONFIRMED`
- [ ] Les autres choix `ACCEPTED` passent en `WITHDRAWN` et les places sont libérées
- [ ] Ma candidature passe en `ACCEPTED`
- [ ] Un numéro matricule `STU-YYYY-NNNNN` est généré
- [ ] L'événement `ApplicationAccepted` est publié sur Kafka
- [ ] Le délai restant est visible en heures dans la réponse

**Endpoint :** `POST /admissions/applications/{id}/confirm`

---

### US-021 - Consulter les choix acceptés et le délai de confirmation

**En tant que** Candidat  
**Je veux** voir quels choix ont été acceptés et combien de temps il me reste pour confirmer  
**Afin de** prendre ma décision en connaissance de cause

**Critères d'acceptation :**
- [ ] Je vois la liste des choix `ACCEPTED` avec leur filière et niveau
- [ ] Je vois le délai restant en heures
- [ ] Je vois si la confirmation a déjà été effectuée

**Endpoint :** `GET /admissions/applications/{id}/confirmation`

---

### US-022 - Retirer ma candidature

**En tant que** Candidat  
**Je veux** retirer ma candidature  
**Afin de** annuler ma démarche si je change d'avis

**Critères d'acceptation :**
- [ ] Je ne peux retirer ma candidature que si elle est en `DRAFT`
- [ ] Le retrait libère les places des offres (`currentCount--`)
- [ ] Ma candidature passe en `WITHDRAWN`

**Endpoint :** `DELETE /admissions/applications/{id}`

---

## EPIC 4 - Administration des dossiers

---

### US-023 - Lister et filtrer les candidatures

**En tant que** Scolarité  
**Je veux** voir toutes les candidatures avec des filtres  
**Afin de** gérer efficacement les dossiers à traiter

**Critères d'acceptation :**
- [ ] Je peux filtrer par statut (`SUBMITTED`, `UNDER_ADMIN_REVIEW`, etc.)
- [ ] Je peux filtrer par campagne
- [ ] Les résultats sont paginés (défaut 20 par page)
- [ ] Les candidatures sont triées par date de soumission décroissante
- [ ] Je vois pour chaque dossier : nom, prénom, email, statut, date de soumission

**Endpoint :** `GET /admissions/admin/applications?status=&campaignId=&page=&size=`

---

### US-024 - Consulter un dossier complet

**En tant que** Scolarité  
**Je veux** consulter le dossier complet d'un candidat  
**Afin de** vérifier la conformité administrative

**Critères d'acceptation :**
- [ ] Je vois toutes les informations : profil candidat, choix, documents, paiement, historique
- [ ] Je vois les URLs des documents uploadés
- [ ] Je vois l'historique complet des changements de statut

**Endpoint :** `GET /admissions/admin/applications/{id}`

---

### US-025 - Valider administrativement un dossier

**En tant que** Scolarité  
**Je veux** valider ou refuser administrativement un dossier  
**Afin de** le transmettre à la commission ou demander des compléments

**Critères d'acceptation :**
- [ ] Le dossier doit être en `SUBMITTED` ou `ADDITIONAL_DOCS_REQUIRED`
- [ ] **Si approuvé :**
  - La candidature passe en `PENDING_COMMISSION`
  - Tous les choix actifs passent en `PENDING_COMMISSION`
  - L'événement `ApplicationPendingCommission` est publié par choix
- [ ] **Si refusé :**
  - La candidature passe en `ADDITIONAL_DOCS_REQUIRED`
  - Le dossier est déverrouillé pour que le candidat puisse compléter
  - L'événement `ApplicationAdminReview` est publié
- [ ] Je peux ajouter un commentaire dans les deux cas
- [ ] Le changement de statut est tracé dans l'historique avec mon userId

**Endpoint :** `PUT /admissions/admin/applications/{id}/admin-review`

---

### US-026 - Demander des documents supplémentaires

**En tant que** Scolarité  
**Je veux** demander des documents manquants à un candidat  
**Afin de** compléter son dossier avant transmission à la commission

**Critères d'acceptation :**
- [ ] Je dois fournir un motif explicite
- [ ] La candidature passe en `ADDITIONAL_DOCS_REQUIRED`
- [ ] Le dossier est déverrouillé avec le motif renseigné
- [ ] Le candidat peut uploader de nouveaux documents et resoumettre

**Endpoint :** `POST /admissions/admin/applications/{id}/request-docs`

---

### US-027 - Transmettre manuellement à la commission

**En tant que** Scolarité  
**Je veux** transmettre manuellement un dossier à la commission  
**Afin de** pallier une transmission automatique échouée

**Critères d'acceptation :**
- [ ] Le dossier doit être en `UNDER_ADMIN_REVIEW`
- [ ] Tous les choix actifs passent en `PENDING_COMMISSION`
- [ ] La candidature passe en `PENDING_COMMISSION`

**Endpoint :** `PUT /admissions/admin/applications/{id}/forward-commission`

---

## EPIC 5 - Commission pédagogique

---

### US-028 - Gérer les membres d'une commission

**En tant que** Super Admin  
**Je veux** ajouter ou retirer des membres d'une commission  
**Afin de** constituer le jury pour une offre de formation

**Critères d'acceptation :**
- [ ] Je peux ajouter un enseignant comme `MEMBER`, `PRESIDENT` ou `RAPPORTEUR`
- [ ] Il ne peut y avoir qu'un seul `PRESIDENT` par commission (422 si doublon)
- [ ] Le président est enregistré dans `ReviewCommission.presidentId`
- [ ] Je peux retirer un membre (le presidentId est mis à null si c'est le président)
- [ ] Un enseignant ne peut pas être ajouté deux fois (409 Conflict)

**Endpoints :** `POST /admissions/commissions/{id}/members`, `DELETE /admissions/commissions/{id}/members/{memberId}`

---

### US-029 - Consulter les dossiers à examiner

**En tant que** Enseignant (membre de commission)  
**Je veux** voir la liste des dossiers en attente d'examen  
**Afin de** consulter les candidatures à traiter

**Critères d'acceptation :**
- [ ] Je ne vois que les dossiers liés à ma commission
- [ ] Je dois être membre de la commission pour accéder (403 sinon)
- [ ] Je vois les choix en statut `PENDING_COMMISSION` ou `UNDER_COMMISSION_REVIEW`
- [ ] Les dossiers sont triés par date de soumission

**Endpoint :** `GET /admissions/commissions/{id}/choices`

---

### US-030 - Voter sur un choix de candidature

**En tant que** Enseignant (membre de commission)  
**Je veux** voter sur un choix de candidature  
**Afin de** participer à la délibération collégiale

**Critères d'acceptation :**
- [ ] Je dois être membre de la commission pour voter (403 sinon)
- [ ] Je peux voter `ACCEPT`, `REJECT` ou `ABSTAIN`
- [ ] Je ne peux voter qu'une seule fois par choix (409 si doublon)
- [ ] Le choix passe automatiquement en `UNDER_COMMISSION_REVIEW` au premier vote
- [ ] Je peux ajouter un commentaire optionnel

**Endpoint :** `POST /admissions/commissions/{id}/votes`

---

### US-031 - Consulter le résultat du vote

**En tant que** Enseignant (président de commission)  
**Je veux** voir le résultat agrégé des votes sur un choix  
**Afin de** prendre une décision éclairée

**Critères d'acceptation :**
- [ ] Je vois le nombre de votes ACCEPT, REJECT et ABSTAIN
- [ ] Je vois si le quorum est atteint
- [ ] Je vois la décision suggérée : `ACCEPTED`, `REJECTED` ou `TIE` (égalité)
- [ ] Si quorum non atteint → `QUORUM_NOT_REACHED`
- [ ] Le calcul exclut les abstentions (majorité des ACCEPT vs REJECT)

**Endpoint :** `GET /admissions/commissions/{id}/choices/{choiceId}/vote-result`

---

### US-032 - Valider la décision finale

**En tant que** Enseignant (président de commission)  
**Je veux** valider la décision finale sur un choix  
**Afin de** formaliser le résultat de la délibération

**Critères d'acceptation :**
- [ ] Seul le président peut valider (403 sinon)
- [ ] Les décisions possibles : `ACCEPTED`, `REJECTED`, `WAITLISTED`, `INTERVIEW_REQUIRED`
- [ ] **Si `WAITLISTED`** : une `WaitlistEntry` est créée avec le prochain rang disponible
- [ ] **Si `INTERVIEW_REQUIRED`** : le choix attend la planification d'un entretien
- [ ] **Si `ACCEPTED` ou `REJECTED`** : l'évaluation globale de la candidature est déclenchée
- [ ] La décision, la date et l'ID du président sont enregistrés sur le choix
- [ ] Je peux ajouter un motif de décision

**Endpoint :** `POST /admissions/commissions/{id}/choices/{choiceId}/validate`

---

## EPIC 6 - Entretiens

---

### US-033 - Planifier un entretien

**En tant que** Scolarité  
**Je veux** planifier un entretien pour un candidat dont la commission a requis un entretien  
**Afin de** organiser la phase d'entretien

**Critères d'acceptation :**
- [ ] Le choix doit être en `INTERVIEW_REQUIRED` (422 sinon)
- [ ] Je peux définir : date/heure, durée (min 15 min), lieu ou lien visio, type (PRESENTIEL/VISIO)
- [ ] Je peux désigner les enseignants présents (liste d'IDs)
- [ ] La date doit être dans le futur (422 sinon)
- [ ] Le choix passe en `INTERVIEW_SCHEDULED`
- [ ] L'événement `InterviewScheduled` est publié (notification au candidat)

**Endpoint :** `POST /admissions/applications/{id}/choices/{choiceId}/interview`

---

### US-034 - Clôturer un entretien

**En tant que** Enseignant (président)  
**Je veux** clôturer un entretien après sa réalisation  
**Afin de** déclencher la phase de vote post-entretien

**Critères d'acceptation :**
- [ ] L'entretien doit être en `SCHEDULED` (422 sinon)
- [ ] Je peux saisir des notes confidentielles (non visibles du candidat)
- [ ] L'entretien passe en `DONE`
- [ ] Le choix passe en `INTERVIEW_DONE`
- [ ] Un nouveau vote de commission est attendu

**Endpoint :** `PUT /admissions/interviews/{id}/complete`

---

### US-035 - Annuler un entretien

**En tant que** Scolarité  
**Je veux** annuler un entretien  
**Afin de** le replanifier si nécessaire

**Critères d'acceptation :**
- [ ] L'entretien doit être en `SCHEDULED` (422 sinon)
- [ ] L'annulation doit être effectuée au moins 48h avant l'entretien (422 sinon)
- [ ] L'entretien passe en `CANCELLED`
- [ ] Le choix repasse en `INTERVIEW_REQUIRED` pour permettre une replanification
- [ ] Je peux saisir un motif d'annulation

**Endpoint :** `PUT /admissions/interviews/{id}/cancel`

---

## EPIC 7 - Directeur de thèse (Doctorat)

---

### US-036 - Consulter les demandes d'accord en attente

**En tant que** Enseignant HDR (directeur pressenti)  
**Je veux** voir les demandes d'accord de direction de thèse en attente  
**Afin de** traiter les candidats qui me sollicitent

**Critères d'acceptation :**
- [ ] Je vois uniquement les demandes qui me sont adressées (filtré par `directorId = mon userId`)
- [ ] Je vois le projet de recherche du candidat
- [ ] Je vois la date d'expiration de la demande

**Endpoint :** `GET /admissions/thesis-approvals`

---

### US-037 - Répondre à une demande d'accord

**En tant que** Enseignant HDR  
**Je veux** répondre à une demande d'accord de direction de thèse  
**Afin de** accepter ou refuser d'encadrer ce candidat

**Critères d'acceptation :**
- [ ] La demande doit m'être adressée (403 sinon)
- [ ] La demande doit être en `PENDING` (422 sinon)
- [ ] **Si j'approuve** : le choix passe en `PENDING_COMMISSION` (transmis à la commission)
- [ ] **Si je refuse** : le choix passe en `REJECTED` avec le motif
- [ ] Je peux ajouter un commentaire
- [ ] La date de réponse est enregistrée

**Endpoint :** `PUT /admissions/thesis-approvals/{id}/respond`

---

### US-038 - Expiration automatique des demandes sans réponse

**En tant que** Système  
**Je veux** que les demandes sans réponse expirent après 15 jours  
**Afin de** ne pas bloquer indéfiniment les candidatures Doctorat

**Critères d'acceptation :**
- [ ] Un rappel est envoyé automatiquement à J+7 sans réponse
- [ ] La demande expire automatiquement après 15 jours (scheduler 06:00)
- [ ] À l'expiration : `ApprovalStatus → EXPIRED`, choix → `REJECTED`

---

## EPIC 8 - Liste d'attente

---

### US-039 - Consulter la liste d'attente d'une offre

**En tant que** Scolarité  
**Je veux** voir la liste d'attente d'une offre de formation  
**Afin de** suivre les candidats en attente d'une place

**Critères d'acceptation :**
- [ ] Je vois les candidats en attente triés par rang croissant
- [ ] Je vois le statut de chaque entrée (WAITING, PROMOTED, EXPIRED)

**Endpoint :** `GET /admissions/waitlist?offerId=`

---

### US-040 - Promotion automatique depuis la liste d'attente

**En tant que** Système  
**Je veux** que le premier candidat en liste d'attente soit automatiquement promu quand une place se libère  
**Afin de** gérer équitablement les places disponibles

**Critères d'acceptation :**
- [ ] Quand `acceptedCount` diminue (retrait ou expiration), le rang 1 est automatiquement promu
- [ ] La promotion passe `WaitlistEntry → PROMOTED` avec `expiresAt = now + 48h`
- [ ] L'événement `WaitlistPromoted` est publié (notification au candidat)
- [ ] Si le candidat promu n'a pas confirmé dans les 48h → `EXPIRED`, rang 2 promu

---

### US-041 - Expiration des promotions sans confirmation

**En tant que** Système  
**Je veux** que les promotions non confirmées expirent après 48h  
**Afin de** proposer la place au suivant

**Critères d'acceptation :**
- [ ] Un scheduler vérifie chaque heure les promotions expirées
- [ ] Toute promotion en `PROMOTED` avec `expiresAt < now` passe en `EXPIRED`
- [ ] Le prochain candidat en WAITING est automatiquement promu

---

## EPIC 9 - Confirmation et inscription

---

### US-042 - Évaluation globale automatique

**En tant que** Système  
**Je veux** qu'une évaluation globale soit déclenchée après chaque décision finale de commission  
**Afin de** gérer automatiquement la suite du processus

**Critères d'acceptation :**
- [ ] L'évaluation se déclenche quand tous les choix d'une candidature ont un statut final
- [ ] **Si au moins 1 choix `ACCEPTED`** :
  - Une `ConfirmationRequest` est créée
  - La candidature passe en `AWAITING_CONFIRMATION`
  - L'événement `ApplicationAwaitingConfirmation` est publié
- [ ] **Si tous les choix sont `REJECTED`** :
  - La candidature passe en `REJECTED`
  - L'événement `ApplicationRejected` est publié
- [ ] **Si certains choix sont `WAITLISTED`** : on attend la promotion

---

### US-043 - Confirmation automatique à expiration du délai

**En tant que** Système  
**Je veux** que la confirmation soit faite automatiquement si le candidat ne répond pas dans les délais  
**Afin de** garantir l'attribution des places

**Critères d'acceptation :**
- [ ] Un scheduler vérifie toutes les heures (à :30) les confirmations expirées
- [ ] Le choix `ACCEPTED` avec le `choiceOrder` le plus bas est confirmé automatiquement
- [ ] `ConfirmationRequest.autoConfirmed = true`
- [ ] L'événement `ApplicationAccepted` est publié avec `autoConfirmed = true`
- [ ] L'événement `ChoiceAutoConfirmed` est publié (notification au candidat)

---

### US-044 - Déclenchement de l'inscription après confirmation

**En tant que** Système  
**Je veux** que l'inscription de l'étudiant soit déclenchée après confirmation  
**Afin de** créer automatiquement son compte et son dossier étudiant

**Critères d'acceptation :**
- [ ] L'événement `ApplicationAccepted` est publié sur le topic `application.accepted`
- [ ] L'événement contient toutes les données nécessaires : userId, filiereId, profil candidat complet, matricule
- [ ] Le matricule est au format `STU-YYYY-NNNNN` (généré de façon atomique)
- [ ] L'Auth Service consomme l'événement pour créer le compte étudiant
- [ ] Le User Service consomme l'événement pour créer le profil étudiant complet

---

## EPIC 10 - Paiement

---

### US-045 - Traitement du paiement via Kafka

**En tant que** Système  
**Je veux** que l'Admission Service mette à jour le statut du paiement quand le Paiement Service confirme  
**Afin de** permettre la soumission du dossier

**Critères d'acceptation :**
- [ ] L'Admission Service consomme l'événement `PaymentCompleted` du topic `payment.completed`
- [ ] L'`AdmissionPayment` est retrouvé via `paymentReference` ou `applicationId`
- [ ] Le paiement passe en `COMPLETED`
- [ ] La candidature passe en `PAID`
- [ ] Si le paiement est introuvable, le message est acquitté pour ne pas bloquer la queue

---

## Matrice des user stories par acteur

| Epic | US | Super Admin | Scolarité | Candidat | Enseignant | Système |
|---|---|---|---|---|---|---|
| Campagnes | US-001 | ✅ | | | | |
| Campagnes | US-002 | | | ✅ Public | | |
| Campagnes | US-003 | ✅ | | | | |
| Campagnes | US-004 | | | | | ✅ |
| Campagnes | US-005 | ✅ | ✅ | | | |
| Offres | US-006 | ✅ | | | | |
| Offres | US-007 | | | ✅ Public | | |
| Offres | US-008 | | | | | ✅ |
| Offres | US-009 | ✅ | | | | |
| Candidat | US-010 | | | ✅ | | |
| Candidat | US-011 | | | ✅ | | |
| Candidat | US-012 | | | ✅ | | |
| Candidat | US-013 | | | ✅ | | |
| Candidat | US-014 | | | ✅ | | |
| Candidat | US-015 | | | ✅ | | |
| Candidat | US-016 | | | ✅ | | |
| Candidat | US-017 | | | ✅ | | |
| Candidat | US-018 | | | ✅ | | |
| Candidat | US-019 | | | ✅ | | |
| Candidat | US-020 | | | ✅ | | |
| Candidat | US-021 | | | ✅ | | |
| Candidat | US-022 | | | ✅ | | |
| Admin | US-023 | ✅ | ✅ | | | |
| Admin | US-024 | ✅ | ✅ | | | |
| Admin | US-025 | | ✅ | | | |
| Admin | US-026 | | ✅ | | | |
| Admin | US-027 | | ✅ | | | |
| Commission | US-028 | ✅ | | | | |
| Commission | US-029 | | | | ✅ | |
| Commission | US-030 | | | | ✅ | |
| Commission | US-031 | | | | ✅ | |
| Commission | US-032 | | | | ✅ (président) | |
| Entretiens | US-033 | | ✅ | | | |
| Entretiens | US-034 | | | | ✅ (président) | |
| Entretiens | US-035 | | ✅ | | | |
| Thèse | US-036 | | | | ✅ (HDR) | |
| Thèse | US-037 | | | | ✅ (HDR) | |
| Thèse | US-038 | | | | | ✅ |
| Waitlist | US-039 | ✅ | ✅ | | | |
| Waitlist | US-040 | | | | | ✅ |
| Waitlist | US-041 | | | | | ✅ |
| Confirmation | US-042 | | | | | ✅ |
| Confirmation | US-043 | | | | | ✅ |
| Confirmation | US-044 | | | | | ✅ |
| Paiement | US-045 | | | | | ✅ |

**Total : 45 user stories réparties sur 10 epics**
