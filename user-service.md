# User Service - Documentation Technique

**Projet :** AdmissionSchool  
**Service :** User Service  
**Port :** 8082  
**Version :** 1.0.0  
**Dernière mise à jour :** Mars 2026

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Modèle de domaine - Description des entités](#2-modèle-de-domaine--description-des-entités)
3. [Énumérations](#3-énumérations)
4. [Cas d'utilisation](#4-cas-dutilisation)
5. [Dépendances cross-services](#5-dépendances-cross-services)
6. [Résumé des endpoints API](#6-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le User Service est responsable de la gestion de toutes les données de profil utilisateur, des structures académiques (départements, filières, niveaux d'étude), ainsi que de la distinction entre les étudiants, les enseignants et le personnel administratif. Il constitue la référence centrale pour l'identité des utilisateurs au-delà de l'authentification.

L'authentification et la gestion des tokens sont exclusivement gérées par l'**Auth Service**. Le User Service reçoit les headers `X-User-Email` et `X-User-Role` injectés par l'**API Gateway** et ne décode jamais les tokens JWT directement.

### Responsabilités

- Gestion des profils personnels de tous les utilisateurs
- Gestion des dossiers académiques des étudiants
- Gestion des profils professionnels des enseignants
- Gestion des départements, filières et niveaux d'étude
- Fourniture des endpoints de recherche d'utilisateurs consommés par les autres microservices

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Inscriptions aux cours et notes | Course Service |
| Dossiers de candidature | Admission Service |
| Paie et contrats | RH Service |
| Génération de documents | Document Service |

---

## 2. Modèle de domaine - Description des entités

### 2.1 `UserProfile`

Représente les informations personnelles et de contact de tout utilisateur du système, quel que soit son rôle. Chaque utilisateur authentifié possède exactement un `UserProfile`.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Référence vers `Users.id` dans l'Auth Service |
| `firstName` | String | Prénom légal |
| `lastName` | String | Nom de famille légal |
| `phone` | String | Numéro de téléphone |
| `birthDate` | LocalDate | Date de naissance |
| `gender` | Gender | MALE, FEMALE |
| `avatarUrl` | String | URL de la photo de profil |
| `createdAt` | LocalDateTime | Date de création de l'enregistrement |
| `updatedAt` | LocalDateTime | Date de dernière modification |

**Règles métier :**
- Un `UserProfile` est automatiquement créé lorsque l'Auth Service publie un événement `UserRegistered`.
- `userId` est unique et immuable — il ne peut pas être réaffecté.
- `firstName` est obligatoire et `lastName` est optionnel.
- La suppression d'un profil entraîne la suppression en cascade de `Student` ou `Teacher` associé.

---

### 2.2 `Student`

Représente le dossier académique d'un utilisateur ayant le rôle étudiant. Un `Student` est toujours lié à un `UserProfile`.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `profileId` | UUID | Référence vers `UserProfile.id` |
| `studentNumber` | String | Numéro matricule unique (généré automatiquement) |
| `enrollmentYear` | int | Année d'inscription initiale |
| `currentLevel` | String | Niveau actuel (ex : L1, L2, M1) |
| `filiere` | Filiere | Filière d'appartenance |
| `status` | StudentStatus | Statut académique courant |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Le `studentNumber` est généré automatiquement à la création selon le format `STU-YYYY-XXXXX`.
- Un étudiant ne peut appartenir qu'à une seule filière à la fois.
- Le changement de filière crée un historique et met à jour `currentLevel`.
- Un étudiant avec le statut `EXPELLED` ne peut plus accéder aux services académiques.

---

### 2.3 `Teacher`

Représente le profil professionnel d'un utilisateur ayant le rôle enseignant.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `profileId` | UUID | Référence vers `UserProfile.id` |
| `employeeNumber` | String | Numéro d'employé unique |
| `speciality` | String | Domaine de spécialité |
| `grade` | AcademicGrade | Grade académique (ASSISTANT, MAITRE_CONF, PROFESSEUR) |
| `department` | Department | Département d'appartenance |
| `diploma` | String | Plus haut diplôme obtenu |
| `maxHoursPerWeek` | int | Volume horaire maximum par semaine |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Le `employeeNumber` est généré automatiquement au format `TCH-YYYY-XXXXX`.
- Un enseignant appartient à un seul département à la fois.
- `maxHoursPerWeek` est utilisé par le Course Service pour valider les affectations.
- Un enseignant peut également être chef de département (`Department.headTeacherId`).

---

### 2.4 `Department`

Représente un département universitaire regroupant des enseignants et des filières.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom complet du département |
| `code` | String | Code court unique (ex : INFO, MATH) |
| `description` | String | Description du département |
| `headTeacherId` | UUID | Référence vers l'enseignant responsable |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Le `code` est unique et immuable une fois défini.
- Un département peut exister sans chef désigné (`headTeacherId` nullable).
- La suppression d'un département est interdite s'il contient des filières actives.

---

### 2.5 `Filiere`

Représente une filière d'études (ex : Licence Informatique, Master Finance).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom complet de la filière |
| `code` | String | Code court unique (ex : LINF, MFIN) |
| `department` | Department | Département responsable |
| `durationYears` | int | Durée en années |
| `description` | String | Description de la filière |
| `status` | FiliereStatus | ACTIVE, INACTIVE, ARCHIVED |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Une filière ne peut être supprimée que si elle n'a aucun étudiant inscrit.
- Une filière `INACTIVE` n'accepte plus de nouvelles candidatures.
- Le `code` est unique à l'échelle de l'établissement.

---

### 2.6 `StudyLevel`

Représente un niveau d'étude dans une filière (ex : L1, L2, L3, M1, M2).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `label` | String | Libellé (ex : Licence 1, Master 2) |
| `code` | String | Code court (ex : L1, M2) |
| `order` | int | Ordre dans la filière (1 = première année) |
| `filiere` | Filiere | Filière d'appartenance |
| `promotionYear` | LocalDate | Année de la promotion courante |

**Règles métier :**
- L'`order` détermine la progression : un étudiant passe du niveau `order N` au niveau `order N+1`.
- Deux niveaux d'une même filière ne peuvent pas avoir le même `order`.

---

## 3. Énumérations

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
| `VACATAIRE` | Vacataire / Intervenant |

### `FiliereStatus`
| Valeur | Description |
|---|---|
| `ACTIVE` | Filière ouverte aux inscriptions |
| `INACTIVE` | Filière fermée aux nouvelles inscriptions |
| `ARCHIVED` | Filière archivée (historique uniquement) |

---

## 4. Cas d'utilisation

### UC-001 — Créer un profil utilisateur

**Acteur :** Système (Auth Service via événement Kafka)  
**Déclencheur :** Réception de l'événement `UserActivated` (après activation du compte)  
**Préconditions :** L'utilisateur a activé son compte dans l'Auth Service  

**Scénario principal :**
1. L'Auth Service publie l'événement `UserActivated` avec `userId`, `email` et `role`.
2. Le User Service consomme l'événement.
3. Le système crée un `UserProfile` avec `userId` et des champs vides.
4. Si le rôle est `STUDENT`, le système crée également un `Student` avec un `studentNumber` généré.
5. Si le rôle est `TEACHER`, le système crée un `Teacher` avec un `employeeNumber` généré.
6. Le profil est disponible pour mise à jour par l'utilisateur.

**Scénario alternatif :**
- Si un profil avec le même `userId` existe déjà → l'opération est ignorée (idempotence).

---

### UC-002 — Mettre à jour son profil

**Acteur :** Utilisateur connecté  
**Déclencheur :** Requête `PUT /users/me`  
**Préconditions :** L'utilisateur est authentifié (token valide)  

**Scénario principal :**
1. L'utilisateur envoie ses nouvelles informations (firstName, lastName, phone, birthDate, gender).
2. Le système récupère le profil via `X-User-Email` injecté par la Gateway.
3. Le système valide les champs (prénom et nom obligatoires, format téléphone).
4. Le système met à jour le `UserProfile` et enregistre `updatedAt`.
5. Le système retourne le profil mis à jour.

**Scénario alternatif :**
- Si les champs obligatoires sont vides → retourner `400 Bad Request`.

---

### UC-003 — Uploader une photo de profil

**Acteur :** Utilisateur connecté  
**Déclencheur :** Requête `POST /users/me/avatar`  
**Préconditions :** L'utilisateur est authentifié  

**Scénario principal :**
1. L'utilisateur envoie une image (JPEG ou PNG, max 2 Mo).
2. Le système valide le format et la taille.
3. Le système stocke l'image et génère une URL.
4. Le système met à jour `UserProfile.avatarUrl`.
5. Le système retourne la nouvelle URL.

**Scénario alternatif :**
- Fichier trop volumineux → `400 Bad Request` avec message explicite.
- Format non supporté → `415 Unsupported Media Type`.

---

### UC-004 — Consulter un profil par email

**Acteur :** Service interne (Course Service, Admission Service, etc.)  
**Déclencheur :** Requête `GET /users?email=...`  
**Préconditions :** Requête authentifiée avec token valide  

**Scénario principal :**
1. Le service appelant envoie l'email en paramètre.
2. Le User Service recherche le profil correspondant.
3. Le système retourne le `UserDtoResponse` avec les informations du profil.

**Scénario alternatif :**
- Email introuvable → `404 Not Found`.

---

### UC-005 — Inscrire un étudiant dans une filière

**Acteur :** Administrateur scolarité (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /users/students/{id}/filiere`  
**Préconditions :** L'étudiant existe et a le statut `ACTIVE`  

**Scénario principal :**
1. L'administrateur envoie l'identifiant de la nouvelle filière.
2. Le système vérifie que la filière est `ACTIVE`.
3. Le système vérifie que l'étudiant n'est pas déjà dans cette filière.
4. Le système met à jour `Student.filiere` et `Student.currentLevel` (premier niveau de la filière).
5. Le système retourne le dossier étudiant mis à jour.

**Scénario alternatif :**
- Filière `INACTIVE` ou `ARCHIVED` → `422 Unprocessable Entity`.
- Étudiant déjà dans cette filière → `409 Conflict`.

---

### UC-006 — Lister les étudiants d'une filière

**Acteur :** Enseignant, Administrateur  
**Déclencheur :** Requête `GET /users/students?filiereId=...`  
**Préconditions :** L'utilisateur a la permission `USER_READ`  

**Scénario principal :**
1. L'utilisateur envoie l'identifiant de la filière et des filtres optionnels (niveau, statut).
2. Le système retourne la liste paginée des étudiants correspondants.
3. Chaque entrée contient le profil, le numéro étudiant, le niveau et le statut.

---

### UC-007 — Créer un département

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /users/departments`  
**Préconditions :** L'utilisateur a le rôle `SUPER_ADMIN`  

**Scénario principal :**
1. L'administrateur envoie le nom, le code et la description du département.
2. Le système vérifie l'unicité du code.
3. Le système crée le département et retourne l'entité créée.

**Scénario alternatif :**
- Code déjà existant → `409 Conflict`.

---

### UC-008 — Créer une filière

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `POST /users/filieres`  
**Préconditions :** Le département cible existe  

**Scénario principal :**
1. L'administrateur envoie le nom, le code, le département, la durée et la description.
2. Le système vérifie l'unicité du code de filière.
3. Le système crée la filière avec le statut `ACTIVE`.
4. Le système crée automatiquement les `StudyLevel` correspondants (autant que `durationYears`).
5. Le système retourne la filière créée avec ses niveaux.

**Scénario alternatif :**
- Département introuvable → `404 Not Found`.
- Code filière déjà existant → `409 Conflict`.

---

### UC-009 — Désigner un chef de département

**Acteur :** Super Admin (`SUPER_ADMIN`)  
**Déclencheur :** Requête `PUT /users/departments/{id}/head`  
**Préconditions :** L'enseignant cible appartient au département  

**Scénario principal :**
1. L'administrateur envoie l'identifiant de l'enseignant.
2. Le système vérifie que l'enseignant appartient bien au département.
3. Le système met à jour `Department.headTeacherId`.
4. Le système retourne le département mis à jour.

**Scénario alternatif :**
- Enseignant n'appartenant pas au département → `422 Unprocessable Entity`.

---

### UC-010 — Rechercher des utilisateurs (global)

**Acteur :** Administrateur, Enseignant  
**Déclencheur :** Requête `GET /users/search?q=...`  
**Préconditions :** Permission `USER_READ`  

**Scénario principal :**
1. L'utilisateur envoie une chaîne de recherche (nom, prénom, email ou numéro matricule).
2. Le système effectue une recherche insensible à la casse sur ces champs.
3. Le système retourne une liste paginée de résultats.
4. Chaque résultat indique le type de profil (STUDENT, TEACHER, STAFF).

---

## 5. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **Auth Service** | Consommateur Kafka | Reçoit `UserActivated` pour créer le profil |
| **Course Service** | Fournisseur HTTP | Expose les endpoints de profil et de filière |
| **Admission Service** | Fournisseur HTTP | Expose les données étudiant pour les candidatures |
| **RH Service** | Fournisseur HTTP | Expose les données enseignant pour les contrats |
| **Document Service** | Fournisseur HTTP | Expose les données étudiant pour la génération de documents |

### Événements publiés (Kafka)

| Événement | Déclencheur | Consommateurs |
|---|---|---|
| `UserProfileCreated` | Création d'un profil | Notification Service |
| `StudentEnrolled` | Inscription dans une filière | Course Service, Notification Service |
| `TeacherAssigned` | Affectation à un département | Course Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `UserActivated` | Auth Service | Création automatique du profil |

---

## 6. Résumé des endpoints API

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/users/me` | Profil de l'utilisateur connecté | Tout utilisateur authentifié |
| `PUT` | `/users/me` | Mettre à jour son profil | Tout utilisateur authentifié |
| `POST` | `/users/me/avatar` | Uploader une photo de profil | Tout utilisateur authentifié |
| `GET` | `/users/{id}` | Profil par ID | `USER_READ` |
| `GET` | `/users?email=` | Profil par email | `USER_READ` |
| `GET` | `/users/search?q=` | Recherche globale | `USER_READ` |
| `GET` | `/users/students` | Liste des étudiants (filtrable) | `USER_READ` |
| `GET` | `/users/students/{id}` | Dossier étudiant | `USER_READ` |
| `PUT` | `/users/students/{id}/filiere` | Inscrire dans une filière | `ADMIN_SCHOLAR` |
| `PUT` | `/users/students/{id}/level` | Changer le niveau | `ADMIN_SCHOLAR` |
| `GET` | `/users/teachers` | Liste des enseignants | `USER_READ` |
| `GET` | `/users/teachers/{id}` | Profil enseignant | `USER_READ` |
| `GET` | `/users/departments` | Liste des départements | `USER_READ` |
| `POST` | `/users/departments` | Créer un département | `SUPER_ADMIN` |
| `PUT` | `/users/departments/{id}/head` | Désigner un chef | `SUPER_ADMIN` |
| `GET` | `/users/filieres` | Liste des filières | `USER_READ` |
| `POST` | `/users/filieres` | Créer une filière | `SUPER_ADMIN` |
| `PUT` | `/users/filieres/{id}` | Modifier une filière | `SUPER_ADMIN` |
| `GET` | `/users/filieres/{id}/levels` | Niveaux d'une filière | `USER_READ` |
