# User Service - Documentation Technique

**Projet :** AdmissionSchool  
**Service :** User Service  
**Port :** 8082  
**Version :** 3.0.0  
**Dernière mise à jour :** Mars 2026  
**Changelog v3 :** Mise à jour du parcours candidat → étudiant avec `CandidateProfile` complet depuis l'Admission Service. Le `UserProfile` étudiant est créé directement avec toutes les données - sans ressaisie.

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Parcours d'onboarding](#2-parcours-donboarding)
3. [Modèle de domaine - Description des entités](#3-modèle-de-domaine--description-des-entités)
4. [Énumérations](#4-énumérations)
5. [Cas d'utilisation](#5-cas-dutilisation)
6. [Règles métier transversales](#6-règles-métier-transversales)
7. [Dépendances cross-services](#7-dépendances-cross-services)
8. [Résumé des endpoints API](#8-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le User Service est responsable de la gestion de toutes les données de profil utilisateur, des structures académiques (départements, filières, niveaux d'étude), et de la distinction entre les étudiants, les enseignants et le personnel administratif. Il constitue la référence centrale pour l'identité des utilisateurs au-delà de l'authentification.

Il intervient à trois moments clés du cycle de vie d'un utilisateur :

- **Après acceptation d'une candidature** - création automatique du profil étudiant sur événement Kafka
- **Après création manuelle par un responsable** - création du profil enseignant ou administratif
- **Durant la vie académique** - mise à jour des profils, gestion des niveaux, affectations

L'authentification et la gestion des tokens sont exclusivement gérées par l'**Auth Service**. Le User Service reçoit les headers `X-User-Email` et `X-User-Role` injectés par l'**API Gateway** et ne décode jamais les tokens JWT directement.

### Responsabilités

- Création automatique des profils étudiants après acceptation d'une candidature
- Création manuelle des profils enseignants et du personnel administratif
- Gestion des informations personnelles de tous les utilisateurs
- Gestion des structures académiques (départements, filières, niveaux d'étude)
- Fourniture des endpoints de recherche consommés par les autres microservices

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT / génération d'email institutionnel | Auth Service |
| Inscriptions aux cours, notes, emploi du temps | Course Service |
| Dossiers de candidature et décisions d'admission | Admission Service |
| Paie, contrats, congés | RH Service |
| Génération de documents (bulletins, diplômes) | Document Service |

---

## 2. Parcours d'onboarding

### 2.1 Parcours Candidat → Étudiant

Ce parcours est entièrement piloté par des événements Kafka. Le User Service n'est jamais appelé directement par l'Admission Service - il réagit à l'événement `ApplicationAccepted`.

```
[Admission Service]                    [User Service]

Publie ApplicationAccepted
  - userId (UUID candidat)
  - filiereId, studentNumber
  - personalEmail
  - institutionalEmail (généré par Auth Service)
  - CandidateProfile complet :
      firstName, lastName, birthDate, birthPlace,
      nationality, gender, phone, address, photoUrl,
      currentInstitution, currentDiploma, graduationYear
        │
        ▼
                              Consomme ApplicationAccepted
                                      │
                                      ▼
                              Crée UserProfile COMPLET
                                - userId
                                - firstName, lastName
                                - email = institutionalEmail
                                - phone, birthDate, gender
                                - personalEmail
                                - avatarUrl = photoUrl
                                (depuis CandidateProfile)
                                      │
                                      ▼
                              Crée Student
                                - profileId
                                - studentNumber
                                - filiereId
                                - currentLevel = premier niveau de la filière
                                - status = ACTIVE
                                - enrollmentYear = année courante
                                      │
                                      ▼
                              Publie StudentProfileCreated
                                      │
                              ┌───────┴────────┐
                              ▼                ▼
                    Course Service      Notification Service
                    (crée StudentGroup  (envoie email de
                     d'accueil)          bienvenue)
```

---

### 2.2 Parcours Enseignant

Ce parcours est initié manuellement par un responsable habilité. Le User Service coordonne avec l'Auth Service pour la création du compte.

```
[Responsable ADMIN_SCHOLAR ou SUPER_ADMIN]      [User Service]

POST /users/teachers
  - firstName, lastName
  - personalEmail
  - speciality, grade
  - departmentId
  - maxHoursPerWeek
        │
        ▼
                              Valide les données
                              Vérifie que le département existe
                                      │
                                      ▼
                              Génère employeeNumber
                              Format : TCH-YYYY-XXXXX
                                      │
                                      ▼
                              Publie TeacherCreationRequested
                              vers Auth Service
                                      │
                              [Auth Service répond]
                              - institutionalEmail généré
                                (prenom.nom@universite.edu)
                              - mot de passe temporaire généré
                              - mustChangePassword = true
                              - userId créé
                                      │
                                      ▼
                              Crée UserProfile (avec userId)
                              Crée Teacher (avec profileId)
                                      │
                                      ▼
                              Publie TeacherProfileCreated
                                      │
                              ┌───────┴────────┐
                              ▼                ▼
                    Course Service      Notification Service
                    (rend l'enseignant  (envoie credentials
                     disponible pour    sur email personnel)
                     affectation)
```

---

### 2.3 Parcours Personnel administratif

Identique au parcours enseignant, initié par `SUPER_ADMIN` ou `ADMIN_RH`.

```
POST /users/staff
  - firstName, lastName
  - personalEmail
  - position
  - departmentId
        │
        ▼
Génère staffNumber (STF-YYYY-XXXXX)
Publie StaffCreationRequested vers Auth Service
Crée UserProfile + Staff
Publie StaffProfileCreated → Notification Service
```

---

## 3. Modèle de domaine - Description des entités

### 3.1 `UserProfile`

Représente les informations personnelles et de contact de tout utilisateur du système, quel que soit son rôle. Chaque compte actif possède exactement un `UserProfile`.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Référence vers `Users.id` dans l'Auth Service |
| `firstName` | String | Prénom légal |
| `lastName` | String | Nom de famille légal |
| `phone` | String | Numéro de téléphone |
| `birthDate` | LocalDate | Date de naissance |
| `gender` | Gender | MALE, FEMALE, OTHER |
| `avatarUrl` | String | URL de la photo de profil |
| `personalEmail` | String | Email personnel (hors système) |
| `createdAt` | LocalDateTime | Date de création |
| `updatedAt` | LocalDateTime | Date de dernière modification |

**Règles métier :**
- Un `UserProfile` est créé automatiquement via événement Kafka pour les étudiants, et manuellement pour les enseignants et le personnel.
- `userId` est unique et immuable - il ne peut pas être réaffecté.
- `firstName` et `lastName` sont obligatoires.
- `personalEmail` est conservé pour les communications hors système (ex : envoi de credentials).
- La suppression d'un profil entraîne la suppression en cascade de `Student`, `Teacher` ou `Staff` associé.

---

### 3.2 `Student`

Représente le dossier académique d'un étudiant. Créé automatiquement lors de l'acceptation d'une candidature.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `profileId` | UUID | Référence vers `UserProfile.id` |
| `studentNumber` | String | Numéro matricule unique - format `STU-YYYY-XXXXX` |
| `enrollmentYear` | int | Année d'inscription initiale |
| `currentLevelId` | Long | Référence vers `StudyLevel.id` courant |
| `filiereId` | Long | Référence vers `Filiere.id` |
| `status` | StudentStatus | Statut académique courant |
| `admissionApplicationId` | UUID | Référence vers la candidature d'origine |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Le `studentNumber` est transmis depuis l'Admission Service via l'événement `ApplicationAccepted`.
- Un étudiant ne peut appartenir qu'à une seule filière à la fois.
- Le changement de filière ou de niveau est tracé dans `StudentAcademicHistory`.
- Un étudiant `EXPELLED` ne peut plus accéder aux services académiques.
- `admissionApplicationId` garantit la traçabilité de l'origine du dossier.

---

### 3.3 `StudentAcademicHistory`

Trace les changements de filière et de niveau d'un étudiant au fil du temps.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `studentId` | UUID | Référence vers `Student.id` |
| `filiereId` | Long | Filière concernée |
| `levelId` | Long | Niveau concerné |
| `academicYear` | String | Année académique (ex : 2025-2026) |
| `startDate` | LocalDate | Date de début dans ce niveau |
| `endDate` | LocalDate | Date de fin (null si niveau courant) |
| `reason` | String | Motif du changement (PROMOTION, REDOUBLEMENT, TRANSFERT) |

**Règles métier :**
- Un enregistrement est créé à chaque changement de niveau ou de filière.
- L'enregistrement courant a `endDate = null`.

---

### 3.4 `Teacher`

Représente le profil professionnel d'un enseignant. Créé manuellement par un responsable habilité.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `profileId` | UUID | Référence vers `UserProfile.id` |
| `employeeNumber` | String | Numéro d'employé - format `TCH-YYYY-XXXXX` |
| `speciality` | String | Domaine de spécialité principal |
| `grade` | AcademicGrade | Grade académique |
| `departmentId` | Long | Référence vers `Department.id` |
| `diploma` | String | Plus haut diplôme obtenu |
| `maxHoursPerWeek` | int | Volume horaire maximum par semaine |
| `isActive` | boolean | Actif ou inactif (départ, congé long) |
| `createdAt` | LocalDateTime | Date de création |
| `createdBy` | UUID | ID du responsable ayant créé le profil |

**Règles métier :**
- L'`employeeNumber` est généré automatiquement à la création.
- Un enseignant appartient à un seul département à la fois.
- `maxHoursPerWeek` est transmis au Course Service pour valider les affectations.
- Un enseignant `isActive = false` n'est plus affectable à des cours.
- Un enseignant peut être chef de département (`Department.headTeacherId`).

---

### 3.5 `Staff`

Représente le profil d'un membre du personnel administratif.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `profileId` | UUID | Référence vers `UserProfile.id` |
| `staffNumber` | String | Numéro de personnel - format `STF-YYYY-XXXXX` |
| `position` | String | Intitulé du poste |
| `departmentId` | Long | Département d'affectation |
| `isActive` | boolean | Actif ou inactif |
| `createdAt` | LocalDateTime | Date de création |
| `createdBy` | UUID | ID du responsable ayant créé le profil |

---

### 3.6 `Department`

Représente un département universitaire regroupant des enseignants et des filières.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom complet du département |
| `code` | String | Code court unique (ex : INFO, MATH, ECO) |
| `description` | String | Description du département |
| `headTeacherId` | UUID | Référence vers l'enseignant responsable (nullable) |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Le `code` est unique et immuable une fois défini.
- Un département peut exister sans chef désigné (`headTeacherId` nullable).
- La suppression d'un département est interdite s'il contient des filières actives ou des enseignants actifs.

---

### 3.7 `Filiere`

Représente une filière d'études (ex : Licence Informatique, Master Finance).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom complet de la filière |
| `code` | String | Code court unique (ex : LINF, MFIN) |
| `departmentId` | Long | Référence vers `Department.id` |
| `durationYears` | int | Durée totale en années |
| `description` | String | Description et objectifs |
| `status` | FiliereStatus | ACTIVE, INACTIVE, ARCHIVED |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Une filière ne peut être supprimée que si elle n'a aucun étudiant actif.
- Une filière `INACTIVE` n'accepte plus de nouvelles candidatures mais les étudiants déjà inscrits continuent leur parcours.
- Le `code` est unique à l'échelle de l'établissement.
- La création d'une filière génère automatiquement ses `StudyLevel`.

---

### 3.8 `StudyLevel`

Représente un niveau d'étude dans une filière (ex : L1, L2, L3, M1, M2).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `label` | String | Libellé complet (ex : Licence 1, Master 2) |
| `code` | String | Code court (ex : L1, M2) |
| `order` | int | Ordre dans la filière (1 = première année) |
| `filiereId` | Long | Référence vers `Filiere.id` |
| `academicYear` | String | Année académique courante de ce niveau |

**Règles métier :**
- L'`order` détermine la progression : un étudiant passe du niveau `N` au niveau `N+1`.
- Deux niveaux d'une même filière ne peuvent pas avoir le même `order`.
- Chaque `StudyLevel` est référencé par le Course Service pour y attacher des `TeachingUnit`.

---

## 4. Énumérations

### `Gender`
| Valeur | Description |
|---|---|
| `MALE` | Masculin |
| `FEMALE` | Féminin |
| `OTHER` | Autre / Non précisé |

### `StudentStatus`
| Valeur | Description |
|---|---|
| `ACTIVE` | Étudiant inscrit et actif |
| `SUSPENDED` | Suspension temporaire |
| `GRADUATED` | Diplômé |
| `TRANSFERRED` | Transféré vers un autre établissement |
| `EXPELLED` | Exclu définitivement |
| `ON_LEAVE` | En congé académique |

### `AcademicGrade`
| Valeur | Description |
|---|---|
| `ASSISTANT` | Assistant |
| `MAITRE_ASSISTANT` | Maître-assistant |
| `MAITRE_CONF` | Maître de conférences |
| `PROFESSEUR` | Professeur |
| `VACATAIRE` | Vacataire / Intervenant externe |

### `FiliereStatus`
| Valeur | Description |
|---|---|
| `ACTIVE` | Filière ouverte aux inscriptions |
| `INACTIVE` | Fermée aux nouvelles inscriptions |
| `ARCHIVED` | Archivée - consultation historique uniquement |

### `HistoryChangeReason`
| Valeur | Description |
|---|---|
| `PROMOTION` | Passage au niveau supérieur |
| `REDOUBLEMENT` | Maintien au même niveau |
| `TRANSFERT_FILIERE` | Changement de filière |
| `TRANSFERT_ETABLISSEMENT` | Transfert vers un autre établissement |

---

## 5. Cas d'utilisation

### UC-USR-001 - Créer un profil étudiant (automatique)

**Acteur :** Système (Admission Service via événement Kafka)  
**Déclencheur :** Réception de l'événement `ApplicationAccepted`  
**Préconditions :** La candidature a été acceptée par le conseil d'admission

**Scénario principal :**
1. L'Admission Service publie `ApplicationAccepted` avec le `CandidateProfile` complet (données personnelles et académiques remplies par le candidat durant son dossier).
2. Le User Service consomme l'événement.
3. Le système crée un `UserProfile` **directement rempli** à partir du `CandidateProfile` :
   - `firstName`, `lastName`, `phone`, `birthDate`, `gender` → depuis `CandidateProfile`
   - `avatarUrl` → depuis `CandidateProfile.photoUrl`
   - `personalEmail` → email personnel du candidat
   - `userId` → depuis l'Auth Service
4. Le système identifie le premier `StudyLevel` de la filière (`order = 1`).
5. Le système crée un `Student` avec `status = ACTIVE`, `currentLevelId`, `studentNumber` et `admissionApplicationId`.
6. Le système crée un enregistrement initial dans `StudentAcademicHistory`.
7. Le système publie l'événement `StudentProfileCreated`.

**Points clés :**
- Le `UserProfile` est **complet dès la création** - le nouvel étudiant n'a pas à ressaisir ses informations personnelles.
- Seule la photo de profil peut être mise à jour ultérieurement si le candidat souhaite en changer.
- `admissionApplicationId` assure la traçabilité entre le dossier d'admission et le profil étudiant.

**Scénario alternatif :**
- `userId` déjà existant → l'opération est ignorée (idempotence Kafka).
- Filière introuvable → l'événement est mis en file d'erreur pour traitement manuel.
- `CandidateProfile` incomplet → l'événement est rejeté avec log d'erreur, traitement manuel requis.

---

### UC-USR-002 - Créer un profil enseignant (manuel)

**Acteur :** Administrateur scolarité (`ADMIN_SCHOLAR`) ou Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /users/teachers`  
**Préconditions :** Le département cible existe et est actif

**Scénario principal :**
1. Le responsable envoie les informations de l'enseignant (firstName, lastName, personalEmail, speciality, grade, departmentId, maxHoursPerWeek).
2. Le système valide les données et vérifie l'existence du département.
3. Le système génère un `employeeNumber` au format `TCH-YYYY-XXXXX`.
4. Le système publie `TeacherCreationRequested` vers l'Auth Service avec les données personnelles.
5. L'Auth Service génère l'email institutionnel, le mot de passe temporaire et crée le compte avec `mustChangePassword = true`.
6. L'Auth Service retourne le `userId` et l'`institutionalEmail` générés.
7. Le système crée le `UserProfile` et le `Teacher`.
8. Le système publie `TeacherProfileCreated`.
9. Le Notification Service envoie les credentials (email institutionnel + mot de passe temporaire) sur l'email personnel.
10. Le système retourne le profil créé.

**Scénarios alternatifs :**
- Département introuvable → `404 Not Found`.
- Email personnel déjà utilisé dans le système → `409 Conflict`.

---

### UC-USR-003 - Créer un profil personnel administratif (manuel)

**Acteur :** Super Admin (`SUPER_ADMIN`) ou Admin RH (`ADMIN_RH`)  
**Déclencheur :** Requête `POST /users/staff`  
**Préconditions :** Le département cible existe

**Scénario principal :**  
Identique à UC-USR-002 avec génération d'un `staffNumber` au format `STF-YYYY-XXXXX`.

---

### UC-USR-004 - Mettre à jour son profil

**Acteur :** Tout utilisateur connecté  
**Déclencheur :** Requête `PUT /users/me`  
**Préconditions :** L'utilisateur est authentifié

**Scénario principal :**
1. L'utilisateur envoie ses nouvelles informations (firstName, lastName, phone, birthDate, gender).
2. Le système récupère le profil via le header `X-User-Email` injecté par la Gateway.
3. Le système valide les champs obligatoires.
4. Le système met à jour le `UserProfile` et enregistre `updatedAt`.
5. Le système retourne le profil mis à jour.

**Scénario alternatif :**
- Champs obligatoires manquants → `400 Bad Request`.

---

### UC-USR-005 - Promouvoir un étudiant au niveau supérieur

**Acteur :** Administrateur scolarité (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /users/students/{id}/promote`  
**Préconditions :** L'étudiant a le statut `ACTIVE` et n'est pas au dernier niveau de sa filière

**Scénario principal :**
1. L'administrateur déclenche la promotion de l'étudiant.
2. Le système récupère le `StudyLevel` courant et identifie le niveau suivant (`order + 1`).
3. Le système ferme l'enregistrement courant dans `StudentAcademicHistory` (`endDate = today`).
4. Le système crée un nouvel enregistrement dans `StudentAcademicHistory` avec `reason = PROMOTION`.
5. Le système met à jour `Student.currentLevelId`.
6. Le système publie l'événement `StudentPromoted`.

**Scénarios alternatifs :**
- Étudiant déjà au dernier niveau → `422 Unprocessable Entity`.
- Étudiant non actif → `422 Unprocessable Entity`.

---

### UC-USR-006 - Marquer un étudiant comme diplômé

**Acteur :** Administrateur scolarité (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /users/students/{id}/graduate`  
**Préconditions :** L'étudiant est au dernier niveau de sa filière et a validé tous ses crédits (vérifié via Course Service)

**Scénario principal :**
1. L'administrateur marque l'étudiant comme diplômé.
2. Le système vérifie auprès du Course Service que tous les crédits sont validés.
3. Le système passe `Student.status = GRADUATED`.
4. Le système ferme l'historique académique.
5. Le système publie l'événement `StudentGraduated`.
6. Le Document Service consomme l'événement et génère le diplôme.

---

### UC-USR-007 - Transférer un étudiant vers une autre filière

**Acteur :** Administrateur scolarité (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /users/students/{id}/transfer`  
**Préconditions :** L'étudiant est `ACTIVE`, la filière cible est `ACTIVE`

**Scénario principal :**
1. L'administrateur envoie `filiereId` et `levelId` cibles avec un motif.
2. Le système vérifie que la filière cible est `ACTIVE`.
3. Le système ferme l'enregistrement courant dans `StudentAcademicHistory`.
4. Le système crée un nouvel enregistrement avec `reason = TRANSFERT_FILIERE`.
5. Le système met à jour `Student.filiereId` et `Student.currentLevelId`.
6. Le système publie l'événement `StudentTransferred`.

---

### UC-USR-008 - Lister les étudiants d'une filière

**Acteur :** Enseignant, Administrateur  
**Déclencheur :** Requête `GET /users/students?filiereId=&levelId=&status=`  
**Préconditions :** Permission `USER_READ`

**Scénario principal :**
1. L'utilisateur envoie les filtres (filière, niveau, statut).
2. Le système retourne une liste paginée avec pour chaque étudiant : profil, matricule, niveau courant et statut.

---

### UC-USR-009 - Désigner un chef de département

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `PUT /users/departments/{id}/head`  
**Préconditions :** L'enseignant cible appartient au département et est actif

**Scénario principal :**
1. L'administrateur envoie l'`id` de l'enseignant.
2. Le système vérifie que l'enseignant appartient bien au département et est `isActive = true`.
3. Le système met à jour `Department.headTeacherId`.
4. Le système retourne le département mis à jour.

---

### UC-USR-010 - Créer une filière avec ses niveaux

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /users/filieres`  
**Préconditions :** Le département cible existe

**Scénario principal :**
1. L'administrateur envoie le nom, code, département, durée en années et description.
2. Le système vérifie l'unicité du code.
3. Le système crée la filière avec `status = ACTIVE`.
4. Le système génère automatiquement N `StudyLevel` (N = `durationYears`) avec les `order` de 1 à N.
5. Le système retourne la filière créée avec ses niveaux.

---

### UC-USR-011 - Désactiver un enseignant

**Acteur :** Super Admin (`SUPER_ADMIN`) ou Admin RH (`ADMIN_RH`)  
**Déclencheur :** Requête `PUT /users/teachers/{id}/deactivate`  
**Préconditions :** L'enseignant est actuellement actif

**Scénario principal :**
1. Le responsable déclenche la désactivation avec un motif.
2. Le système passe `Teacher.isActive = false`.
3. Le système publie `TeacherDeactivated`.
4. Le Course Service consomme l'événement et retire l'enseignant des affectations futures.

---

## 6. Règles métier transversales

### Génération des numéros d'identification

```
Étudiant  : STU-YYYY-XXXXX  (ex : STU-2025-00142)
Enseignant: TCH-YYYY-XXXXX  (ex : TCH-2025-00018)
Personnel : STF-YYYY-XXXXX  (ex : STF-2025-00005)

YYYY  = année courante
XXXXX = séquentiel sur 5 chiffres, remis à 0 chaque année
```

### Email institutionnel

La génération de l'email institutionnel est la responsabilité exclusive de l'**Auth Service**. Le User Service reçoit l'email déjà généré via l'événement ou la réponse synchrone.

```
Règles de génération (Auth Service) :
  prenom.nom@universite.edu
  Accents supprimés, minuscules, espaces → tirets
  Doublon → prenom.nom2@universite.edu
```

### Mot de passe temporaire (enseignant / personnel)

- Généré par l'**Auth Service** à la création du compte.
- Communiqué sur l'email personnel uniquement.
- Flag `mustChangePassword = true` - changement forcé à la première connexion.
- Expire après **72 heures** si non utilisé.

### Progression académique

La promotion d'un étudiant est déclenchée manuellement par l'administrateur. Le Course Service informe le User Service via l'événement `SemesterValidated` que les crédits sont acquis, mais la promotion effective reste une action humaine.

---

## 7. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **Auth Service** | Synchrone (HTTP/Event) | Création des comptes enseignant/personnel, réception des `userId` |
| **Admission Service** | Consommateur Kafka | Reçoit `ApplicationAccepted` pour créer le profil étudiant |
| **Course Service** | Fournisseur HTTP | Expose profils, filières et niveaux pour le Course Service |
| **Course Service** | Consommateur Kafka | Reçoit `SemesterValidated` pour suivre la progression |
| **RH Service** | Fournisseur HTTP | Expose les données enseignant/personnel pour les contrats |
| **Document Service** | Fournisseur HTTP + Consommateur Kafka | Expose les données pour la génération de documents |
| **Notification Service** | Consommateur Kafka | Consomme les événements pour notifier les utilisateurs |

### Événements publiés (Kafka)

| Événement | Déclencheur | Données | Consommateurs |
|---|---|---|---|
| `StudentProfileCreated` | Création profil étudiant | studentId, userId, filiereId, levelId, studentNumber | Course Service, Notification Service |
| `TeacherProfileCreated` | Création profil enseignant | teacherId, userId, departmentId, employeeNumber | Course Service, Notification Service |
| `StaffProfileCreated` | Création profil personnel | staffId, userId, departmentId, staffNumber | Notification Service |
| `StudentPromoted` | Promotion de niveau | studentId, fromLevelId, toLevelId, academicYear | Course Service, Notification Service |
| `StudentGraduated` | Diplômé | studentId, filiereId, academicYear | Document Service, Notification Service |
| `StudentTransferred` | Transfert de filière | studentId, fromFiliereId, toFiliereId, toLevelId | Course Service |
| `TeacherDeactivated` | Désactivation enseignant | teacherId | Course Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `UserActivated` | Auth Service | Création d'un `UserProfile` minimal (userId, email) pour le candidat - complété à l'acceptation via `ApplicationAccepted` |
| `ApplicationAccepted` | Admission Service | Création automatique du `UserProfile` complet (depuis `CandidateProfile`) + `Student` |
| `SemesterValidated` | Course Service | Mise à jour progression étudiant |

---

## 8. Résumé des endpoints API

### Profil personnel

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/me` | Profil de l'utilisateur connecté | Authentifié |
| `PUT` | `/users/me` | Mettre à jour son profil | Authentifié |
| `POST` | `/users/me/avatar` | Uploader une photo de profil | Authentifié |

### Étudiants

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/students` | Liste des étudiants (filtrable) | `USER_READ` |
| `GET` | `/users/students/{id}` | Dossier étudiant complet | `USER_READ` |
| `PUT` | `/users/students/{id}/promote` | Promouvoir au niveau supérieur | `ADMIN_SCHOLAR` |
| `PUT` | `/users/students/{id}/graduate` | Marquer comme diplômé | `ADMIN_SCHOLAR` |
| `PUT` | `/users/students/{id}/transfer` | Transférer vers une autre filière | `ADMIN_SCHOLAR` |
| `PUT` | `/users/students/{id}/status` | Changer le statut (suspend, expel...) | `ADMIN_SCHOLAR` |
| `GET` | `/users/students/{id}/history` | Historique académique | `USER_READ` |

### Enseignants

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `POST` | `/users/teachers` | Créer un profil enseignant | `ADMIN_SCHOLAR` |
| `GET` | `/users/teachers` | Liste des enseignants (filtrable) | `USER_READ` |
| `GET` | `/users/teachers/{id}` | Profil enseignant complet | `USER_READ` |
| `PUT` | `/users/teachers/{id}` | Mettre à jour un profil | `ADMIN_SCHOLAR` |
| `PUT` | `/users/teachers/{id}/deactivate` | Désactiver un enseignant | `SUPER_ADMIN` |

### Personnel administratif

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `POST` | `/users/staff` | Créer un profil personnel | `ADMIN_RH` |
| `GET` | `/users/staff` | Liste du personnel | `USER_READ` |
| `GET` | `/users/staff/{id}` | Profil complet | `USER_READ` |
| `PUT` | `/users/staff/{id}/deactivate` | Désactiver un membre | `SUPER_ADMIN` |

### Recherche

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/{id}` | Profil par ID | `USER_READ` |
| `GET` | `/users?email=` | Profil par email | `USER_READ` |
| `GET` | `/users/search?q=` | Recherche globale | `USER_READ` |

### Départements

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/departments` | Liste des départements | `USER_READ` |
| `POST` | `/users/departments` | Créer un département | `SUPER_ADMIN` |
| `PUT` | `/users/departments/{id}` | Modifier un département | `SUPER_ADMIN` |
| `PUT` | `/users/departments/{id}/head` | Désigner un chef | `SUPER_ADMIN` |

### Filières et niveaux

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/filieres` | Liste des filières | Public |
| `POST` | `/users/filieres` | Créer une filière | `SUPER_ADMIN` |
| `PUT` | `/users/filieres/{id}` | Modifier une filière | `SUPER_ADMIN` |
| `PUT` | `/users/filieres/{id}/status` | Changer le statut | `SUPER_ADMIN` |
| `GET` | `/users/filieres/{id}/levels` | Niveaux d'une filière | Public |
| `GET` | `/users/filieres/{id}/students` | Étudiants d'une filière | `USER_READ` |
