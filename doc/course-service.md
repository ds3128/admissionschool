# Course Service - Documentation Technique v1

**Projet :** AdmissionSchool  
**Service :** Course Service  
**Port :** 8083  
**Version :** 1.0.0  
**Dernière mise à jour :** Mars 2026

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Parcours étudiant](#2-parcours-étudiant)
3. [Parcours enseignant](#3-parcours-enseignant)
4. [Modèle de domaine - Description des entités](#4-modèle-de-domaine--description-des-entités)
5. [Énumérations](#5-énumérations)
6. [Cas d'utilisation](#6-cas-dutilisation)
7. [Règles métier transversales](#7-règles-métier-transversales)
8. [Dépendances cross-services](#8-dépendances-cross-services)
9. [Résumé des endpoints API](#9-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le Course Service gère l'ensemble de la vie académique d'un étudiant une fois inscrit : les unités d'enseignement, les matières, les inscriptions, l'emploi du temps, les séances, les présences, les évaluations, les notes et la progression semestrielle. Il est également le point central de coordination pour les enseignants concernant leurs affectations et leur charge horaire.

C'est le service le plus riche fonctionnellement du système. Il produit les données qui alimenteront le pipeline de détection d'anomalies (ML) : taux de présence, évolution des notes, progression par cohorte, statistiques par matière.

### Responsabilités

- Gestion de la structure académique (TeachingUnit, Matiere, Semester)
- Gestion des groupes d'étudiants (StudentGroup)
- Gestion des affectations enseignants (TeacherAssignment)
- Gestion de l'emploi du temps (PlannedSlot, WeeklySchedule)
- Gestion des séances réelles et des présences (Session, Attendance)
- Gestion des inscriptions aux matières (Enrollment)
- Gestion des évaluations et des notes (Evaluation, Grade)
- Calcul de la progression semestrielle (StudentProgress)
- Production des données statistiques (ClassStats, TeacherLoad, AttendanceStats)

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Profils personnels, filières, niveaux | User Service |
| Dossiers de candidature | Admission Service |
| Génération des bulletins et relevés de notes | Document Service |
| Envoi des notifications | Notification Service |
| Paie des enseignants | RH Service |

---

## 2. Parcours étudiant

```
[Étudiant inscrit]                    [Course Service]

Reçoit StudentProfileCreated
(User Service via Kafka)
        │
        ▼
Système crée StudentGroup
d'accueil (groupe promo)
        │
        ▼
Admin inscrit l'étudiant           Enrollment créé par matière
dans ses matières                  (N enrollments par semestre)
        │
        ▼
Étudiant consulte                  WeeklySchedule calculé
son emploi du temps                depuis PlannedSlots
        │
        ▼
Étudiant assiste aux               Session enregistrée
séances de cours                   Attendance marquée
        │
        ▼
Évaluations planifiées             Evaluation créée
(TP, TD, Devoir, Partiel...)       par l'enseignant
        │
        ▼
Notes saisies                      Grade créé par étudiant
par l'enseignant                   et par évaluation
        │
        ▼
Fin du semestre                    StudentProgress calculé :
                                   - moyenne semestrielle
                                   - crédits obtenus
                                   - rang dans la promo
                                   - statut : ADMIS / AJOURNÉ
        │
        ▼
Résultats validés                  Événement SemesterValidated publié
par l'administration               → User Service : mise à jour niveau
                                   → Document Service : génération bulletin
```

---

## 3. Parcours enseignant

```
[Enseignant]                          [Course Service]

Reçoit TeacherProfileCreated
(User Service via Kafka)
        │
        ▼
Admin affecte l'enseignant         TeacherAssignment créé
à une ou plusieurs matières        (role : CM, TD ou TP)
(par semestre)
        │
        ▼
Admin planifie les créneaux        PlannedSlot créé par matière
dans l'emploi du temps             (récurrent ou ponctuel)
Room assignée, groupe assigné      Validation : pas de conflit
                                   (salle, enseignant, groupe)
        │
        ▼
Enseignant consulte                TeacherLoad calculé :
sa charge horaire                  - heures CM, TD, TP
                                   - par semestre
        │
        ▼
Enseignant consulte                WeeklySchedule calculé
son emploi du temps                depuis ses PlannedSlots
        │
        ▼
Enseignant marque                  Session enregistrée
les présences                      Attendance mise à jour
        │
        ▼
Enseignant crée                    Evaluation créée
une évaluation                     avec coefficient et type
        │
        ▼
Enseignant saisit                  Grade créé par étudiant
les notes                          ClassStats recalculées
```

---

## 4. Modèle de domaine - Description des entités

### 4.1 `Semester`

Représente un semestre académique. Toutes les activités pédagogiques (inscriptions, séances, évaluations) sont rattachées à un semestre.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `label` | String | Libellé (ex : Semestre 1 - 2025/2026) |
| `academicYear` | String | Année académique (ex : 2025-2026) |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin |
| `isCurrent` | boolean | Semestre en cours |
| `status` | SemesterStatus | UPCOMING, ACTIVE, CLOSED, VALIDATED |

**Règles métier :**
- Un seul semestre peut avoir `isCurrent = true` à la fois.
- Un semestre `VALIDATED` est immuable - aucune note ne peut être modifiée.
- La validation d'un semestre publie l'événement `SemesterValidated`.

---

### 4.2 `TeachingUnit`

Représente une Unité d'Enseignement (UE) regroupant plusieurs matières. Une UE est associée à un niveau d'études d'une filière.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `code` | String | Code unique (ex : UE-INFO-L1-01) |
| `name` | String | Intitulé de l'UE |
| `credits` | int | Nombre de crédits ECTS |
| `studyLevelId` | Long | Référence vers `StudyLevel.id` (User Service) |
| `semester` | int | Numéro du semestre dans l'année (1 ou 2) |
| `coefficient` | double | Coefficient de l'UE dans la moyenne annuelle |

**Règles métier :**
- Le code est unique à l'échelle du système.
- Une UE ne peut être supprimée que si elle n'a aucune matière active.
- `studyLevelId` référence le User Service - pas de `@ManyToOne` JPA.

---

### 4.3 `Matiere`

Représente une matière enseignée dans le cadre d'une UE. C'est l'entité centrale autour de laquelle s'organisent les séances, évaluations et inscriptions.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `code` | String | Code unique (ex : MAT-ALGO-L1) |
| `name` | String | Intitulé de la matière |
| `teachingUnit` | TeachingUnit | UE d'appartenance |
| `departmentId` | Long | Référence vers `Department.id` (User Service) |
| `totalHours` | int | Volume horaire total |
| `hoursCM` | int | Heures de cours magistraux |
| `hoursTD` | int | Heures de travaux dirigés |
| `hoursTP` | int | Heures de travaux pratiques |

**Règles métier :**
- `totalHours = hoursCM + hoursTD + hoursTP`.
- Une matière ne peut être supprimée si des `Enrollment` actifs y sont rattachés.
- `departmentId` est une référence cross-service vers le User Service.

---

### 4.4 `TeacherAssignment`

Représente l'affectation d'un enseignant à une matière pour un semestre donné, avec un rôle spécifique (CM, TD ou TP).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `teacherId` | UUID | Référence vers `Teacher.id` (User Service) |
| `matiere` | Matiere | Matière concernée |
| `role` | TeachingRole | CM, TD, TP |
| `semester` | Semester | Semestre de l'affectation |
| `assignedHours` | int | Heures allouées pour ce rôle |

**Règles métier :**
- Un enseignant ne peut pas être affecté deux fois à la même matière avec le même rôle pour le même semestre.
- `assignedHours` ne peut pas dépasser `Teacher.maxHoursPerWeek × nombre de semaines` du semestre.
- La somme des `assignedHours` de tous les `TeacherAssignment` d'un enseignant pour un semestre est utilisée par `TeacherLoad`.

---

### 4.5 `StudentGroup`

Représente un groupe d'étudiants (promotion, groupe TD ou groupe TP) pour un semestre donné.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom du groupe (ex : G1, TP-3, Promo L1-INFO) |
| `type` | GroupType | PROMO, TD, TP |
| `levelId` | Long | Référence vers `StudyLevel.id` (User Service) |
| `filiereId` | Long | Référence vers `Filiere.id` (User Service) |
| `semester` | Semester | Semestre d'appartenance |
| `maxSize` | int | Capacité maximale du groupe |
| `studentIds` | List\<UUID\> | IDs des étudiants membres (références cross-service) |

**Règles métier :**
- `studentIds` contient des références vers `Student.id` dans le User Service - pas de relation JPA.
- Un étudiant peut appartenir à un groupe `PROMO`, un groupe `TD` et un groupe `TP` simultanément.
- `maxSize` est vérifié avant l'ajout d'un étudiant dans le groupe.
- Un groupe `PROMO` est créé automatiquement à la réception de `StudentProfileCreated`.

---

### 4.6 `Room`

Représente une salle de cours avec ses caractéristiques physiques.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom de la salle (ex : Amphi A, Salle 204) |
| `building` | String | Bâtiment |
| `capacity` | int | Capacité en nombre de personnes |
| `type` | RoomType | AMPHI, TD, TP, LABO, VISIO |
| `equipment` | List\<String\> | Équipements disponibles (projecteur, PC, tableau...) |
| `isAvailable` | boolean | Disponible pour planification |

**Règles métier :**
- La capacité est vérifiée lors de la création d'un `PlannedSlot` (`StudentGroup.maxSize <= Room.capacity`).
- Une salle `isAvailable = false` ne peut pas être assignée à un nouveau `PlannedSlot`.

---

### 4.7 `PlannedSlot`

Représente un créneau planifié dans l'emploi du temps. C'est la source de vérité de ce qui est prévu. Les `Session` réelles en sont issues.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `matiere` | Matiere | Matière enseignée |
| `teacherId` | UUID | Enseignant assigné (référence User Service) |
| `room` | Room | Salle assignée |
| `group` | StudentGroup | Groupe d'étudiants concerné |
| `semester` | Semester | Semestre du créneau |
| `dayOfWeek` | DayOfWeek | Jour de la semaine |
| `startTime` | LocalTime | Heure de début |
| `endTime` | LocalTime | Heure de fin |
| `type` | SessionType | CM, TD, TP |
| `recurrent` | boolean | Récurrent chaque semaine ou ponctuel |

**Règles métier :**
- Trois conflits sont détectés à la création :
  - Même salle + même jour + même créneau horaire.
  - Même enseignant + même jour + même créneau horaire.
  - Même groupe + même jour + même créneau horaire.
- Un `PlannedSlot` récurrent génère une `Session` par semaine du semestre.
- Un `PlannedSlot` ponctuel génère une seule `Session`.

---

### 4.8 `Session`

Représente une séance de cours réellement réalisée (ou à venir). Elle est issue d'un `PlannedSlot` mais peut s'en écarter (salle changée, enseignant remplaçant).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `plannedSlotId` | Long | Référence vers `PlannedSlot.id` d'origine |
| `teacherId` | UUID | Enseignant effectif (peut différer du slot) |
| `matiere` | Matiere | Matière enseignée |
| `room` | Room | Salle effective (peut différer du slot) |
| `date` | LocalDate | Date effective de la séance |
| `startTime` | LocalTime | Heure de début |
| `duration` | int | Durée en minutes |
| `type` | SessionType | CM, TD, TP |
| `status` | SessionStatus | SCHEDULED, DONE, CANCELLED, RESCHEDULED |

**Règles métier :**
- Une `Session` annulée (`CANCELLED`) est conservée pour l'historique.
- La présence ne peut être marquée que pour une `Session` en statut `DONE`.
- Si l'enseignant change sur une session, l'original reste tracé dans `PlannedSlot`.

---

### 4.9 `Attendance`

Représente la présence ou l'absence d'un étudiant à une séance.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `session` | Session | Séance concernée |
| `studentId` | UUID | Référence vers `Student.id` (User Service) |
| `present` | boolean | Présent ou absent |
| `justification` | String | Motif d'absence justifiée |

**Règles métier :**
- Une `Attendance` est créée pour chaque étudiant du groupe lors de la clôture d'une `Session`.
- Une absence peut être justifiée a posteriori si la `Session` est `DONE`.
- Le taux de présence minimum est configurable par matière (ex : 80%).
- Un dépassement du seuil d'absences peut bloquer l'accès aux examens.

---

### 4.10 `Enrollment`

Représente l'inscription formelle d'un étudiant à une matière pour un semestre. Un étudiant est inscrit à toutes les matières de son niveau, avec un groupe assigné.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `studentId` | UUID | Référence vers `Student.id` (User Service) |
| `matiere` | Matiere | Matière concernée |
| `group` | StudentGroup | Groupe d'appartenance |
| `semester` | Semester | Semestre d'inscription |
| `enrolledAt` | LocalDate | Date d'inscription |
| `status` | EnrollStatus | ACTIVE, WITHDRAWN, BLOCKED |

**Règles métier :**
- Un étudiant ne peut être inscrit qu'une seule fois à une matière par semestre.
- Le statut `BLOCKED` est appliqué si le taux de présence est insuffisant.
- Les `Enrollment` sont créés automatiquement à la réception de `StudentProfileCreated` pour toutes les matières du niveau courant.

---

### 4.11 `Evaluation`

Représente une évaluation planifiée dans le cadre d'une matière (TP noté, devoir, partiel, examen final, projet, oral).

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `title` | String | Intitulé de l'évaluation |
| `type` | EvalType | TP, TD, DEVOIR, PARTIEL, EXAMEN_FINAL, PROJET, ORAL |
| `matiere` | Matiere | Matière concernée |
| `semester` | Semester | Semestre de l'évaluation |
| `date` | LocalDate | Date prévue |
| `coefficient` | double | Coefficient dans la note finale de la matière |
| `maxScore` | double | Note maximale (ex : 20.0) |

**Règles métier :**
- La somme des coefficients de toutes les `Evaluation` d'une matière doit être égale à 1.0.
- Une `Evaluation` ne peut être supprimée si des `Grade` y sont associés.
- Le `coefficient` est utilisé dans le calcul de la note finale de la matière.

---

### 4.12 `Grade`

Représente la note d'un étudiant à une évaluation spécifique.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `studentId` | UUID | Référence vers `Student.id` (User Service) |
| `evaluation` | Evaluation | Évaluation concernée |
| `matiere` | Matiere | Matière (dénormalisé pour requêtes) |
| `score` | double | Note obtenue |
| `comment` | String | Commentaire de l'enseignant |
| `gradedAt` | LocalDateTime | Date de saisie |

**Règles métier :**
- `score` doit être compris entre 0 et `Evaluation.maxScore`.
- Un `Grade` ne peut être modifié que si le semestre est `ACTIVE`.
- Un `Grade` dans un semestre `VALIDATED` est immuable.
- `getScorePercent()` retourne `(score / maxScore) × 100`.

---

### 4.13 `StudentProgress`

Représente le bilan semestriel d'un étudiant - calculé à la fin du semestre.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `studentId` | UUID | Référence vers `Student.id` (User Service) |
| `semester` | Semester | Semestre concerné |
| `semesterAverage` | double | Moyenne générale du semestre |
| `ueAverage` | double | Moyenne pondérée des UEs |
| `creditsObtained` | int | Crédits ECTS validés |
| `status` | ProgressStatus | ADMIS, AJOURNE, EXCLUS |
| `rank` | int | Rang dans le groupe promotion |

**Règles métier :**
- `semesterAverage` = moyenne pondérée de toutes les matières du semestre.
- `creditsObtained` = somme des crédits des UEs validées (moyenne UE >= 10/20).
- `status = ADMIS` si `semesterAverage >= 10.0`.
- `status = AJOURNE` si `semesterAverage < 10.0` mais possibilité de rattrapage.
- `isValidated()` retourne `true` si `semesterAverage >= 10.0`.
- `getHonors()` retourne la mention : TB (>= 16), B (>= 14), AB (>= 12), P (>= 10).
- `rank` est calculé en comparant avec tous les `StudentProgress` du même groupe promo.

---

## 4.14 DTOs (objets calculés - non persistés)

### `WeeklySchedule`

Vue calculée de l'emploi du temps pour une semaine donnée.

| Champ | Type | Description |
|---|---|---|
| `semester` | Semester | Semestre concerné |
| `weekNumber` | int | Numéro de la semaine |
| `startDate` | LocalDate | Lundi de la semaine |
| `endDate` | LocalDate | Dimanche de la semaine |
| `slots` | List\<PlannedSlot\> | Créneaux planifiés de la semaine |
| `sessions` | List\<Session\> | Séances réelles de la semaine |

Calculé à la volée depuis les `PlannedSlot` et `Session` filtrés par semaine. Consultable par étudiant, enseignant ou groupe.

---

### `Transcript`

Relevé de notes complet d'un étudiant pour un semestre ou une année académique.

| Champ | Type | Description |
|---|---|---|
| `studentId` | UUID | Étudiant concerné |
| `academicYear` | String | Année académique |
| `semester` | Semester | Semestre (ou null pour vue annuelle) |
| `ues` | List\<UEResult\> | Résultats par UE et par matière |
| `generalAverage` | double | Moyenne générale |
| `totalCredits` | int | Total crédits obtenus |
| `mention` | Mention | TB, B, AB, P, INSUFFISANT |

Calculé depuis les `Grade` et `StudentProgress`. Transmis au Document Service pour génération du bulletin.

---

### `ClassStats`

Statistiques par évaluation - alimentera le pipeline ML.

| Champ | Type | Description |
|---|---|---|
| `evaluationId` | Long | Évaluation concernée |
| `matiereId` | Long | Matière concernée |
| `average` | double | Moyenne de la classe |
| `min` | double | Note minimale |
| `max` | double | Note maximale |
| `passRate` | double | Taux de réussite (score >= 50%) |
| `standardDeviation` | double | Écart-type |
| `totalStudents` | int | Nombre d'étudiants évalués |

---

### `TeacherLoad`

Charge horaire d'un enseignant par semestre.

| Champ | Type | Description |
|---|---|---|
| `teacherId` | UUID | Enseignant concerné |
| `semester` | Semester | Semestre concerné |
| `totalHours` | int | Total heures effectuées |
| `cmHours` | int | Heures CM |
| `tdHours` | int | Heures TD |
| `tpHours` | int | Heures TP |
| `courses` | List\<Matiere\> | Matières enseignées |

---

### `AttendanceStats`

Statistiques de présence d'un étudiant par matière - alimentera le pipeline ML.

| Champ | Type | Description |
|---|---|---|
| `studentId` | UUID | Étudiant concerné |
| `matiereId` | Long | Matière concernée |
| `semester` | Semester | Semestre concerné |
| `totalSessions` | int | Nombre total de séances |
| `presentCount` | int | Nombre de présences |
| `absenceCount` | int | Nombre d'absences |
| `justifiedCount` | int | Absences justifiées |
| `attendanceRate` | double | Taux de présence en % |

---

## 5. Énumérations

### `SemesterStatus`
| Valeur | Description |
|---|---|
| `UPCOMING` | Semestre à venir |
| `ACTIVE` | En cours - saisie des notes possible |
| `CLOSED` | Semestre terminé - en attente de validation |
| `VALIDATED` | Validé - immuable |

### `SessionType`
| Valeur | Description |
|---|---|
| `CM` | Cours magistral |
| `TD` | Travaux dirigés |
| `TP` | Travaux pratiques |

### `SessionStatus`
| Valeur | Description |
|---|---|
| `SCHEDULED` | Planifiée |
| `DONE` | Réalisée |
| `CANCELLED` | Annulée |
| `RESCHEDULED` | Reportée |

### `EvalType`
| Valeur | Description |
|---|---|
| `TP` | TP noté |
| `TD` | TD noté |
| `DEVOIR` | Devoir sur table |
| `PARTIEL` | Partiel de mi-semestre |
| `EXAMEN_FINAL` | Examen de fin de semestre |
| `PROJET` | Projet individuel ou groupe |
| `ORAL` | Épreuve orale |

### `ProgressStatus`
| Valeur | Description |
|---|---|
| `ADMIS` | Semestre validé |
| `AJOURNE` | Non validé - rattrapage possible |
| `EXCLUS` | Non validé - exclusion académique |

### `EnrollStatus`
| Valeur | Description |
|---|---|
| `ACTIVE` | Inscription active |
| `WITHDRAWN` | Désinscrit |
| `BLOCKED` | Bloqué (absences excessives) |

### `RoomType`
| Valeur | Description |
|---|---|
| `AMPHI` | Amphithéâtre |
| `TD` | Salle de TD |
| `TP` | Salle de TP |
| `LABO` | Laboratoire |
| `VISIO` | Salle de visioconférence |

### `GroupType`
| Valeur | Description |
|---|---|
| `PROMO` | Groupe promotion (tous les étudiants d'un niveau) |
| `TD` | Groupe de travaux dirigés |
| `TP` | Groupe de travaux pratiques |

### `TeachingRole`
| Valeur | Description |
|---|---|
| `CM` | Titulaire des cours magistraux |
| `TD` | Chargé des travaux dirigés |
| `TP` | Chargé des travaux pratiques |

### `Mention`
| Valeur | Seuil | Description |
|---|---|---|
| `TRES_BIEN` | >= 16/20 | Très bien |
| `BIEN` | >= 14/20 | Bien |
| `ASSEZ_BIEN` | >= 12/20 | Assez bien |
| `PASSABLE` | >= 10/20 | Passable |
| `INSUFFISANT` | < 10/20 | Non validé |

---

## 6. Cas d'utilisation

### UC-CRS-001 - Initialiser les groupes d'un étudiant

**Acteur :** Système (User Service via Kafka)  
**Déclencheur :** Réception de l'événement `StudentProfileCreated`  
**Préconditions :** Un semestre `ACTIVE` existe pour la filière et le niveau de l'étudiant

**Scénario principal :**
1. Le système reçoit `StudentProfileCreated` avec `studentId`, `filiereId`, `levelId`.
2. Le système identifie le semestre `ACTIVE` correspondant.
3. Le système identifie le groupe `PROMO` du niveau et de la filière.
4. Si aucun groupe `PROMO` n'existe → le système en crée un.
5. Le système ajoute `studentId` dans `StudentGroup.studentIds`.
6. Le système crée les `Enrollment` pour toutes les matières du niveau courant.
7. Le système publie `StudentEnrolled`.

---

### UC-CRS-002 - Affecter un enseignant à une matière

**Acteur :** Administrateur (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `POST /courses/assignments`  
**Préconditions :** La matière existe, l'enseignant est actif, le semestre est `UPCOMING` ou `ACTIVE`

**Scénario principal :**
1. L'administrateur envoie `teacherId`, `matiereId`, `role`, `semesterId`, `assignedHours`.
2. Le système vérifie l'absence de doublon (même enseignant, même matière, même rôle, même semestre).
3. Le système vérifie que les heures assignées ne dépassent pas `maxHoursPerWeek` de l'enseignant.
4. Le système crée le `TeacherAssignment`.
5. Le système notifie l'enseignant.

**Scénarios alternatifs :**
- Doublon → `409 Conflict`.
- Dépassement horaire → `422 Unprocessable Entity` avec détail de la charge actuelle.

---

### UC-CRS-003 - Planifier un créneau dans l'emploi du temps

**Acteur :** Administrateur (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `POST /courses/slots`  
**Préconditions :** La matière, la salle et le groupe existent, le semestre est actif

**Scénario principal :**
1. L'administrateur envoie `matiereId`, `teacherId`, `roomId`, `groupId`, `semesterId`, `dayOfWeek`, `startTime`, `endTime`, `type`, `recurrent`.
2. Le système vérifie la disponibilité de la salle sur ce créneau.
3. Le système vérifie la disponibilité de l'enseignant sur ce créneau.
4. Le système vérifie la disponibilité du groupe sur ce créneau.
5. Le système vérifie que `Room.capacity >= StudentGroup.maxSize`.
6. Le système crée le `PlannedSlot`.
7. Si `recurrent = true` → le système génère toutes les `Session` du semestre.
8. Si `recurrent = false` → le système génère une seule `Session`.

**Scénarios alternatifs :**
- Conflit de salle → `409 Conflict` avec détail du conflit.
- Conflit enseignant → `409 Conflict`.
- Conflit groupe → `409 Conflict`.
- Salle trop petite → `422 Unprocessable Entity`.

---

### UC-CRS-004 - Marquer les présences d'une séance

**Acteur :** Enseignant (`TEACHER`)  
**Déclencheur :** Requête `PUT /courses/sessions/{id}/attendance`  
**Préconditions :** La session est en statut `SCHEDULED` ou `DONE`

**Scénario principal :**
1. L'enseignant envoie la liste des présences (studentId + present).
2. Le système vérifie que l'enseignant est bien affecté à cette matière.
3. Le système crée ou met à jour les `Attendance` pour chaque étudiant du groupe.
4. La session passe en statut `DONE`.
5. Le système recalcule `AttendanceStats` pour chaque étudiant concerné.
6. Si un étudiant dépasse le seuil d'absences → son `Enrollment` passe à `BLOCKED`, notification envoyée.

---

### UC-CRS-005 - Créer une évaluation

**Acteur :** Enseignant (`TEACHER`)  
**Déclencheur :** Requête `POST /courses/evaluations`  
**Préconditions :** L'enseignant est affecté à cette matière pour ce semestre

**Scénario principal :**
1. L'enseignant envoie `title`, `type`, `matiereId`, `semesterId`, `date`, `coefficient`, `maxScore`.
2. Le système vérifie que l'enseignant est bien affecté à la matière.
3. Le système vérifie que la somme des coefficients de toutes les évaluations de la matière reste ≤ 1.0 après ajout.
4. Le système crée l'`Evaluation`.
5. Le système notifie les étudiants inscrits à la matière.

**Scénarios alternatifs :**
- Coefficient dépassant 1.0 au total → `422 Unprocessable Entity` avec le solde disponible.
- Enseignant non affecté à la matière → `403 Forbidden`.

---

### UC-CRS-006 - Saisir les notes d'une évaluation

**Acteur :** Enseignant (`TEACHER`)  
**Déclencheur :** Requête `POST /courses/evaluations/{id}/grades`  
**Préconditions :** Le semestre est `ACTIVE`, l'évaluation existe

**Scénario principal :**
1. L'enseignant envoie la liste des notes (studentId + score + comment optionnel).
2. Le système valide chaque score (0 ≤ score ≤ maxScore).
3. Le système crée les `Grade` pour chaque étudiant.
4. Le système recalcule les `ClassStats` de l'évaluation.
5. Le système met à jour la moyenne provisoire de chaque étudiant.

**Scénarios alternatifs :**
- Score hors plage → `400 Bad Request` avec liste des erreurs.
- Semestre non `ACTIVE` → `422 Unprocessable Entity`.

---

### UC-CRS-007 - Calculer la progression semestrielle

**Acteur :** Système (automatique) ou Administrateur  
**Déclencheur :** Fin du semestre ou requête `POST /courses/semesters/{id}/compute-progress`  
**Préconditions :** Le semestre est `CLOSED` - toutes les notes sont saisies

**Scénario principal :**
1. Le système récupère tous les `Grade` du semestre pour chaque étudiant.
2. Pour chaque matière → calcul de la note finale pondérée par les coefficients des évaluations.
3. Pour chaque UE → calcul de la moyenne pondérée des matières.
4. Calcul de la moyenne générale du semestre.
5. Calcul des crédits obtenus (UE validées avec moyenne >= 10).
6. Détermination du statut : `ADMIS`, `AJOURNE` ou `EXCLUS`.
7. Calcul du rang dans la promotion.
8. Création ou mise à jour du `StudentProgress`.
9. Génération du `Transcript` (DTO) pour chaque étudiant.

---

### UC-CRS-008 - Valider un semestre

**Acteur :** Administrateur (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `PUT /courses/semesters/{id}/validate`  
**Préconditions :** Semestre `CLOSED`, `StudentProgress` calculés pour tous les étudiants

**Scénario principal :**
1. L'administrateur déclenche la validation.
2. Le système vérifie qu'aucune note n'est manquante.
3. Le système passe le semestre en `VALIDATED`.
4. Le système publie `SemesterValidated` avec les résultats de tous les étudiants.
5. Le User Service consomme → promotion des étudiants `ADMIS` au niveau suivant.
6. Le Document Service consomme → génération des bulletins.
7. Le Notification Service consomme → notification des résultats aux étudiants.

---

### UC-CRS-009 - Consulter l'emploi du temps

**Acteur :** Étudiant, Enseignant, Administrateur  
**Déclencheur :** Requête `GET /courses/schedule?week=&userId=`

**Scénario principal :**
1. L'utilisateur envoie la semaine souhaitée (numéro ou date).
2. Le système filtre les `PlannedSlot` et `Session` de la semaine.
3. Le système construit le `WeeklySchedule` (DTO).
4. Selon l'acteur : vue étudiant (son groupe), vue enseignant (ses matières), vue admin (toutes).

---

### UC-CRS-010 - Consulter la charge horaire d'un enseignant

**Acteur :** Enseignant, Administrateur (`ADMIN_SCHOLAR`)  
**Déclencheur :** Requête `GET /courses/teachers/{id}/load?semesterId=`

**Scénario principal :**
1. Le système récupère tous les `TeacherAssignment` de l'enseignant pour le semestre.
2. Le système agrège les heures par type (CM, TD, TP).
3. Le système retourne le `TeacherLoad` (DTO) avec le détail par matière.

---

### UC-CRS-011 - Annuler ou reporter une séance

**Acteur :** Enseignant ou Administrateur  
**Déclencheur :** Requête `PUT /courses/sessions/{id}/cancel` ou `/reschedule`

**Scénario principal :**
1. L'acteur envoie le motif et, si report, la nouvelle date/heure/salle.
2. La `Session` passe en `CANCELLED` ou `RESCHEDULED`.
3. Une nouvelle `Session` est créée si report.
4. Le système notifie les étudiants du groupe.

---

### UC-CRS-012 - Générer les statistiques d'une évaluation

**Acteur :** Enseignant, Administrateur  
**Déclencheur :** Requête `GET /courses/evaluations/{id}/stats`

**Scénario principal :**
1. Le système récupère tous les `Grade` de l'évaluation.
2. Le système calcule `average`, `min`, `max`, `passRate`, `standardDeviation`.
3. Le système retourne le `ClassStats` (DTO).

---

## 7. Règles métier transversales

### Calcul de la note finale d'une matière

```
Note matière = Σ (Grade.score / Evaluation.maxScore × 20) × Evaluation.coefficient

Exemple :
  TP (coeff 0.3) : 14/20
  Devoir (coeff 0.3) : 12/20
  Partiel (coeff 0.4) : 16/20
  Note finale = 14×0.3 + 12×0.3 + 16×0.4 = 4.2 + 3.6 + 6.4 = 14.2/20
```

### Validation d'une UE

```
Moyenne UE = Σ (Note matière × coefficient matière dans UE) / Σ coefficients
UE validée si Moyenne UE >= 10/20
Crédits obtenus = Σ crédits des UEs validées
```

### Calcul de la moyenne générale

```
Moyenne générale = Σ (Note matière × coefficient UE × coefficient matière) / Σ poids
```

### Détermination du statut

```
ADMIS    → moyenne >= 10/20
AJOURNE  → moyenne < 10/20 (rattrapage possible selon règlement)
EXCLUS   → nombre de AJOURNE consécutifs dépasse le seuil défini
```

### Détection des conflits de planning

```
Conflit salle    : PlannedSlot A et B ont même roomId + dayOfWeek + chevauchement horaire
Conflit enseignant : PlannedSlot A et B ont même teacherId + dayOfWeek + chevauchement horaire
Conflit groupe   : PlannedSlot A et B ont même groupId + dayOfWeek + chevauchement horaire

Chevauchement : startTime_A < endTime_B ET startTime_B < endTime_A
```

### Seuil d'absences

```
Taux présence = (presentCount / totalSessions) × 100
Si taux < seuilAbsence (configurable, défaut 80%) :
  Enrollment.status = BLOCKED
  Étudiant ne peut pas accéder aux examens
  Notification envoyée à l'étudiant et à l'administrateur
```

---

## 8. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **User Service** | Consommateur Kafka | Reçoit `StudentProfileCreated` pour créer les groupes et enrollments |
| **User Service** | Consommateur Kafka | Reçoit `TeacherProfileCreated` pour rendre l'enseignant disponible |
| **User Service** | Consommateur Kafka | Reçoit `TeacherDeactivated` pour retirer l'enseignant des affectations |
| **User Service** | HTTP synchrone | Vérifie `maxHoursPerWeek` lors d'une affectation |
| **Admission Service** | Aucune interaction directe | - |
| **Document Service** | Consommateur Kafka | Consomme `SemesterValidated` pour générer les bulletins |
| **Notification Service** | Consommateur Kafka | Consomme les événements pour notifier étudiants et enseignants |

### Événements publiés (Kafka)

| Événement | Déclencheur | Données | Consommateurs |
|---|---|---|---|
| `StudentEnrolled` | Inscription aux matières | studentId, matiereIds, semesterId | Notification Service |
| `AttendanceThresholdExceeded` | Seuil absences dépassé | studentId, matiereId, attendanceRate | Notification Service |
| `GradesPublished` | Notes publiées | evaluationId, matiereId | Notification Service |
| `SessionCancelled` | Séance annulée | sessionId, groupId, date | Notification Service |
| `SemesterValidated` | Validation semestre | semesterId, results: List\<StudentResult\> | User Service, Document Service, Notification Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `StudentProfileCreated` | User Service | Création groupes + enrollments |
| `StudentPromoted` | User Service | Mise à jour des groupes et enrollments au nouveau niveau |
| `StudentTransferred` | User Service | Mise à jour filière et niveau dans les groupes |
| `TeacherProfileCreated` | User Service | Rend l'enseignant disponible pour les affectations |
| `TeacherDeactivated` | User Service | Retire l'enseignant des affectations futures |

---

## 9. Résumé des endpoints API

### Structure académique

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/teaching-units` | Lister les UEs (filtrable par niveau) | `USER_READ` |
| `POST` | `/courses/teaching-units` | Créer une UE | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/teaching-units/{id}` | Modifier une UE | `ADMIN_SCHOLAR` |
| `GET` | `/courses/matieres` | Lister les matières | `USER_READ` |
| `POST` | `/courses/matieres` | Créer une matière | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/matieres/{id}` | Modifier une matière | `ADMIN_SCHOLAR` |

### Semestres

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/semesters` | Lister les semestres | `USER_READ` |
| `POST` | `/courses/semesters` | Créer un semestre | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/semesters/{id}/close` | Clôturer un semestre | `ADMIN_SCHOLAR` |
| `POST` | `/courses/semesters/{id}/compute-progress` | Calculer les progressions | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/semesters/{id}/validate` | Valider un semestre | `SUPER_ADMIN` |

### Groupes

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/groups` | Lister les groupes | `USER_READ` |
| `POST` | `/courses/groups` | Créer un groupe | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/groups/{id}/students` | Gérer les étudiants du groupe | `ADMIN_SCHOLAR` |

### Affectations enseignants

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/assignments?teacherId=&semesterId=` | Affectations d'un enseignant | `USER_READ` |
| `POST` | `/courses/assignments` | Créer une affectation | `ADMIN_SCHOLAR` |
| `DELETE` | `/courses/assignments/{id}` | Supprimer une affectation | `ADMIN_SCHOLAR` |
| `GET` | `/courses/teachers/{id}/load?semesterId=` | Charge horaire | `USER_READ` |

### Emploi du temps

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/slots?semesterId=&groupId=` | Créneaux planifiés | `USER_READ` |
| `POST` | `/courses/slots` | Créer un créneau | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/slots/{id}` | Modifier un créneau | `ADMIN_SCHOLAR` |
| `DELETE` | `/courses/slots/{id}` | Supprimer un créneau | `ADMIN_SCHOLAR` |
| `GET` | `/courses/schedule?week=&userId=` | Emploi du temps hebdomadaire | Authentifié |

### Salles

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/rooms` | Lister les salles | `USER_READ` |
| `POST` | `/courses/rooms` | Créer une salle | `ADMIN_SCHOLAR` |
| `PUT` | `/courses/rooms/{id}` | Modifier une salle | `ADMIN_SCHOLAR` |
| `GET` | `/courses/rooms/available?day=&start=&end=` | Salles disponibles sur un créneau | `ADMIN_SCHOLAR` |

### Séances et présences

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/sessions?matiereId=&semesterId=` | Liste des séances | `USER_READ` |
| `PUT` | `/courses/sessions/{id}/attendance` | Marquer les présences | `TEACHER` |
| `PUT` | `/courses/sessions/{id}/cancel` | Annuler une séance | `TEACHER`, `ADMIN_SCHOLAR` |
| `PUT` | `/courses/sessions/{id}/reschedule` | Reporter une séance | `ADMIN_SCHOLAR` |
| `GET` | `/courses/students/{id}/attendance?semesterId=` | Présences d'un étudiant | `USER_READ` |

### Inscriptions

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/enrollments?studentId=&semesterId=` | Inscriptions d'un étudiant | `USER_READ` |
| `PUT` | `/courses/enrollments/{id}/status` | Changer le statut | `ADMIN_SCHOLAR` |

### Évaluations et notes

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/evaluations?matiereId=&semesterId=` | Évaluations d'une matière | `USER_READ` |
| `POST` | `/courses/evaluations` | Créer une évaluation | `TEACHER` |
| `PUT` | `/courses/evaluations/{id}` | Modifier une évaluation | `TEACHER` |
| `DELETE` | `/courses/evaluations/{id}` | Supprimer une évaluation | `TEACHER` |
| `POST` | `/courses/evaluations/{id}/grades` | Saisir les notes | `TEACHER` |
| `PUT` | `/courses/evaluations/{id}/grades/{gradeId}` | Modifier une note | `TEACHER` |
| `GET` | `/courses/evaluations/{id}/stats` | Statistiques d'une évaluation | `USER_READ` |
| `GET` | `/courses/students/{id}/grades?semesterId=` | Notes d'un étudiant | `USER_READ` |

### Progression

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/courses/students/{id}/progress?semesterId=` | Bilan semestriel | `USER_READ` |
| `GET` | `/courses/students/{id}/transcript?year=` | Relevé de notes | `USER_READ` |
| `GET` | `/courses/students/{id}/attendance-stats?semesterId=` | Stats de présence | `USER_READ` |
