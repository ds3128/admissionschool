# Course Service - Documentation Technique v3

**Projet :** AdmissionSchool  
**Service :** Course Service  
**Port :** 8083  
**Version :** 3.0.0  
**Dernière mise à jour :** Mars 2026  
**Stack :** Spring Boot 4.0.3, Java 21, Spring Cloud 2025.1.0, PostgreSQL, Kafka, MapStruct 1.6.3, SpringDoc 2.8.6

**Changelog v3 :**
- Ajout de l'entité `CourseResource` - supports de cours (PDF, slides, vidéos, liens)
- Ajout de l'entité `EvaluationAttachment` - pièces jointes liées aux évaluations (énoncés, fichiers TP)
- Ajout de l'énumération `ResourceType`
- Vue étudiant enrichie : liste de cours, progression par cours, supports accessibles
- Vue enseignant enrichie : gestion complète des supports et pièces jointes
- Nouveaux endpoints `/courses/resources/**` et `/courses/evaluations/{id}/attachments`

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture technique](#2-architecture-technique)
3. [Contexte d'intégration](#3-contexte-dintégration)
4. [Parcours étudiant](#4-parcours-étudiant)
5. [Parcours enseignant](#5-parcours-enseignant)
6. [Flux de validation semestrielle](#6-flux-de-validation-semestrielle)
7. [Modèle de domaine - Entités](#7-modèle-de-domaine--entités)
8. [DTOs calculés - non persistés](#8-dtos-calculés--non-persistés)
9. [Énumérations](#9-énumérations)
10. [Cas d'utilisation](#10-cas-dutilisation)
11. [Règles métier transversales](#11-règles-métier-transversales)
12. [Données pour le pipeline ML](#12-données-pour-le-pipeline-ml)
13. [Sécurité des routes](#13-sécurité-des-routes)
14. [Événements Kafka](#14-événements-kafka)
15. [Dépendances cross-services](#15-dépendances-cross-services)
16. [Schedulers](#16-schedulers)
17. [Endpoints API complets](#17-endpoints-api-complets)
18. [Configuration](#18-configuration)

---

## 1. Vue d'ensemble

Le Course Service gère l'ensemble de la vie académique d'un étudiant une fois inscrit : unités d'enseignement, matières, inscriptions, emploi du temps, séances, présences, évaluations, notes, progression semestrielle et **ressources pédagogiques**. C'est également le point central de coordination pour les enseignants concernant leurs affectations, leur charge horaire et les contenus qu'ils partagent avec leurs étudiants.

C'est le service le plus riche fonctionnellement du système. Il produit les données brutes qui alimenteront le **pipeline de détection d'anomalies ML** : taux de présence par étudiant, évolution des notes, progression par cohorte, statistiques par matière - données essentielles pour l'article de recherche.

### Responsabilités

- Gestion de la structure académique (TeachingUnit, Matiere, Semester)
- Gestion des groupes d'étudiants (StudentGroup : PROMO, TD, TP)
- Gestion des affectations enseignants (TeacherAssignment)
- Gestion de l'emploi du temps (PlannedSlot) avec détection de conflits
- Gestion des séances réelles et des présences (Session, Attendance)
- Gestion des inscriptions aux matières (Enrollment)
- Gestion des évaluations et des notes (Evaluation, Grade)
- **Gestion des supports de cours (CourseResource)** ← nouveau v3
- **Gestion des pièces jointes aux évaluations (EvaluationAttachment)** ← nouveau v3
- Calcul de la progression semestrielle (StudentProgress)
- Vue étudiant enrichie : progression par cours, supports accessibles
- Production des données statistiques pour le pipeline ML

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Authentification / JWT | Auth Service |
| Profils personnels, filières, niveaux | User Service |
| Dossiers de candidature | Admission Service |
| Frais de scolarité, bourses | Payment Service |
| Génération des bulletins PDF | Document Service |
| Envoi des notifications | Notification Service |
| Paie des enseignants | RH Service |
| Stockage physique des fichiers | Infrastructure (S3/MinIO) |

---

## 2. Architecture technique

### Structure du projet

```
course-service/
├── config/
│   ├── SwaggerConfig.java
│   ├── RestClientConfig.java
│   ├── JacksonConfig.java
│   └── SchedulerConfig.java
├── controllers/
│   ├── SemesterController.java
│   ├── TeachingUnitController.java
│   ├── MatiereController.java
│   ├── RoomController.java
│   ├── StudentGroupController.java
│   ├── TeacherAssignmentController.java
│   ├── PlannedSlotController.java
│   ├── SessionController.java
│   ├── EnrollmentController.java
│   ├── EvaluationController.java
│   ├── GradeController.java
│   ├── CourseResourceController.java      ← nouveau v3
│   ├── StudentProgressController.java
│   └── StatsController.java
├── entities/                              ← 15 entités JPA
├── enums/                                 ← 12 énumérations
├── events/
│   ├── consumed/                          ← 5 events consommés
│   └── published/                         ← 5 events publiés
├── exceptions/
│   └── GlobalExceptionHandler.java
├── kafka/
│   ├── KafkaConfig.java
│   ├── CourseEventProducer.java
│   └── CourseEventConsumer.java
├── mapper/
│   └── CourseMapper.java
├── repositories/                          ← 15 repositories JPA
├── security/
│   ├── CourseSecurityFilter.java
│   └── SecurityConfig.java
└── services/
    ├── impl/
    └── [15 interfaces]
```

### Flux de sécurité

```
Client
  ↓ JWT Bearer token
API Gateway :8888
  ↓ Valide JWT, extrait email/role/userId
  ↓ Injecte X-User-Email, X-User-Role, X-User-Id
Course Service :8083
  ↓ CourseSecurityFilter lit les headers
  ↓ Peuple SecurityContext avec ROLE_{role}
  ↓ Controllers lisent @RequestHeader("X-User-Id")
```

---

## 3. Contexte d'intégration

### Position dans l'architecture globale

```
User Service
  ↓ Kafka StudentProfileCreated
Course Service
  → Crée groupes + enrollments automatiquement
  → Étudiant visible dans l'emploi du temps

Course Service
  ↓ Kafka SemesterValidated (event critique)
  ├── User Service → promouvoir les étudiants ADMIS
  ├── Payment Service → renouvellement bourses mérite
  ├── Document Service → génération bulletins
  └── Notification Service → résultats aux étudiants

Payment Service
  ↓ Kafka StudentPaymentBlocked
Course Service
  → Enrollment.status = BLOCKED

Course Service
  ↓ HTTP synchrone (RestClient)
  └── User Service → vérifier maxHoursPerWeek enseignant
```

---

## 4. Parcours étudiant

```
[User Service publie StudentProfileCreated]
        ↓ Kafka
Course Service : groupes + enrollments créés automatiquement
        ↓
[Étudiant consulte sa liste de cours]
GET /courses/enrollments?studentId=
→ Toutes ses matières du semestre avec :
  - note provisoire actuelle
  - taux de présence actuel
  - nombre de supports disponibles
  - évaluations à venir
        ↓
[Étudiant consulte les supports d'une matière]
GET /courses/matieres/{id}/resources
→ Liste des CourseResource publiés (PDF, slides, vidéos, liens)
→ Téléchargement ou accès au lien
        ↓
[Étudiant consulte une évaluation]
GET /courses/evaluations/{id}
→ Détails + pièces jointes (EvaluationAttachment) :
  énoncé du devoir, fichier TP, barème
        ↓
[Étudiant assiste aux séances]
Session → DONE → Attendance marquée
→ AttendanceStats recalculées
        ↓
[Notes publiées par l'enseignant]
GET /courses/students/{id}/grades
→ Notes par évaluation, moyenne provisoire par matière
        ↓
[Fin du semestre]
GET /courses/students/{id}/progress
→ StudentProgress : moyenne, crédits, mention, rang
        ↓
[Semestre validé]
SemesterValidated → User Service : promotion au niveau suivant
```

---

## 5. Parcours enseignant

```
[User Service publie TeacherProfileCreated]
        ↓ Kafka
Course Service : enseignant disponible pour affectations
        ↓
[Admin affecte l'enseignant]
TeacherAssignment créé (CM, TD ou TP)
        ↓
[Enseignant consulte ses cours assignés]
GET /courses/assignments?teacherId=
→ Liste de ses matières avec charge horaire
        ↓
[Enseignant ajoute des supports de cours]
POST /courses/matieres/{id}/resources (multipart)
→ CourseResource créé en DRAFT
→ Enseignant modifie, corrige, puis publie
PUT /courses/resources/{id}/publish
→ CourseResource visible par les étudiants inscrits
        ↓
[Enseignant crée une évaluation]
POST /courses/evaluations
→ Evaluation créée
        ↓
[Enseignant ajoute l'énoncé du devoir]
POST /courses/evaluations/{id}/attachments (multipart)
→ EvaluationAttachment créé
→ Visible immédiatement par les étudiants inscrits
        ↓
[Enseignant marque les présences]
PUT /courses/sessions/{id}/attendance
→ Session → DONE → Attendance créée par étudiant
        ↓
[Enseignant saisit les notes]
POST /courses/evaluations/{id}/grades
→ Grade créé par étudiant → ClassStats recalculées
        ↓
[Enseignant publie les notes]
PUT /courses/evaluations/{id}/publish
→ isPublished = true → grades.published publié
```

---

## 6. Flux de validation semestrielle

```
Admin
  ↓ PUT /courses/semesters/{id}/close
Semestre → CLOSED
  ↓
Admin
  ↓ POST /courses/semesters/{id}/compute-progress
Pour chaque étudiant :
  1. Note finale par matière = Σ (score/maxScore × 20) × coeff évaluation
  2. Moyenne UE = Σ (note matière × coeff) / Σ coefficients
  3. Crédits = Σ crédits UEs validées (moyenne >= 10)
  4. Moyenne générale pondérée
  5. Statut : ADMIS / AJOURNE / EXCLUS
  6. Mention : TB / B / AB / P / INSUFFISANT
  7. Rang dans la promotion
StudentProgress créé pour chaque étudiant
  ↓
Admin
  ↓ PUT /courses/semesters/{id}/validate
Semestre → VALIDATED (immuable)
  ↓ Kafka SemesterValidated
  ├── User Service → promotion étudiants ADMIS
  ├── Payment Service → renouvellement bourses mérite
  ├── Document Service → génération bulletins
  └── Notification Service → résultats aux étudiants
```

---

## 7. Modèle de domaine - Entités

### `Semester`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `label` | String | Libellé (ex : Semestre 1 - 2025/2026) |
| `academicYear` | String | Année académique |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin |
| `isCurrent` | boolean | Semestre en cours |
| `isLastOfYear` | boolean | Dernier semestre - déclenche renouvellement bourses |
| `status` | SemesterStatus | UPCOMING, ACTIVE, CLOSED, VALIDATED |

---

### `TeachingUnit` (UE)

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `code` | String UNIQUE | Code (ex : UE-INFO-L1-01) |
| `name` | String | Intitulé |
| `credits` | int | Crédits ECTS |
| `studyLevelId` | Long | Référence User Service |
| `semesterNumber` | int | Numéro semestre (1 ou 2) |
| `coefficient` | double | Coefficient dans la moyenne annuelle |

---

### `Matiere`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `code` | String UNIQUE | Code (ex : MAT-ALGO-L1) |
| `name` | String | Intitulé |
| `teachingUnit` | TeachingUnit FK | UE d'appartenance |
| `departmentId` | Long | Référence User Service |
| `coefficient` | double | Coefficient dans l'UE |
| `totalHours` | int | Volume horaire total |
| `hoursCM` | int | Heures CM |
| `hoursTD` | int | Heures TD |
| `hoursTP` | int | Heures TP |
| `attendanceThreshold` | double | Seuil présence (défaut 80%) |

**Relations :** `@OneToMany` → `CourseResource`, `Evaluation`, `Enrollment`, `TeacherAssignment`

---

### `CourseResource` ← nouveau v3

Représente un support pédagogique attaché à une matière. Peut être un fichier uploadé (PDF, slides, vidéo) ou un lien externe.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `matiere` | Matiere FK | Matière concernée |
| `semester` | Semester FK | Semestre de mise à disposition |
| `title` | String | Titre du support |
| `description` | String | Description optionnelle |
| `type` | ResourceType | COURS, TD, TP, PROJET, LIEN, AUTRE |
| `fileUrl` | String | URL du fichier (S3/MinIO) ou lien externe |
| `fileName` | String | Nom original du fichier (null si lien) |
| `fileSize` | Long | Taille en octets (null si lien) |
| `mimeType` | String | Type MIME (null si lien) |
| `isPublished` | boolean | Visible par les étudiants |
| `uploadedBy` | String | ID de l'enseignant |
| `createdAt` | LocalDateTime | Date de création |
| `updatedAt` | LocalDateTime | Dernière modification |

**Règles métier :**
- Un support en `isPublished = false` (DRAFT) n'est visible que par l'enseignant qui l'a créé
- Un enseignant ne peut modifier ou supprimer que ses propres supports
- La taille maximale par fichier est de **50 Mo**
- Formats acceptés : PDF, DOCX, PPTX, XLSX, ZIP, MP4, MP3, PNG, JPEG
- Un lien externe ne nécessite pas de fichier (`fileUrl` = URL, `fileName` = null)
- Un support supprimé est conservé en base avec un flag `isDeleted` pour traçabilité

---

### `EvaluationAttachment` ← nouveau v3

Représente une pièce jointe liée à une évaluation spécifique. Typiquement un énoncé de devoir, un fichier TP, un barème ou tout document nécessaire à l'évaluation.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `evaluation` | Evaluation FK | Évaluation concernée |
| `title` | String | Titre de la pièce jointe |
| `description` | String | Description optionnelle |
| `fileUrl` | String | URL du fichier (S3/MinIO) |
| `fileName` | String | Nom original du fichier |
| `fileSize` | Long | Taille en octets |
| `mimeType` | String | Type MIME |
| `uploadedBy` | String | ID de l'enseignant |
| `createdAt` | LocalDateTime | Date d'upload |

**Règles métier :**
- Une évaluation peut avoir plusieurs pièces jointes (énoncé + barème par exemple)
- Les pièces jointes d'une évaluation sont **toujours visibles** par les étudiants inscrits dès leur upload - pas de mécanisme de brouillon (contrairement aux `CourseResource`)
- Seul l'enseignant qui a uploadé peut supprimer une pièce jointe
- La taille maximale par fichier est de **20 Mo**
- Formats acceptés : PDF, DOCX, XLSX, ZIP, PNG, JPEG

---

### `TeacherAssignment`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `teacherId` | String | Référence User Service |
| `matiere` | Matiere FK | Matière concernée |
| `role` | TeachingRole | CM, TD, TP |
| `semester` | Semester FK | Semestre |
| `assignedHours` | int | Heures allouées |

**Contrainte :** Unicité sur `(teacherId, matiere, role, semester)`

---

### `StudentGroup`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `name` | String | Nom (ex : Promo L1-INFO) |
| `type` | GroupType | PROMO, TD, TP |
| `levelId` | Long | Référence User Service |
| `filiereId` | Long | Référence User Service |
| `semester` | Semester FK | Semestre |
| `maxSize` | int | Capacité maximale |
| `studentIds` | List\<String\> | IDs étudiants - `@ElementCollection` |

---

### `Room`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `name` | String | Nom (ex : Amphi A) |
| `building` | String | Bâtiment |
| `capacity` | int | Capacité |
| `type` | RoomType | AMPHI, TD, TP, LABO, VISIO |
| `equipment` | List\<String\> | Équipements - `@ElementCollection` |
| `isAvailable` | boolean | Disponible pour planification |

---

### `PlannedSlot`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `matiere` | Matiere FK | Matière enseignée |
| `teacherId` | String | Enseignant assigné |
| `room` | Room FK | Salle assignée |
| `group` | StudentGroup FK | Groupe d'étudiants |
| `semester` | Semester FK | Semestre |
| `dayOfWeek` | DayOfWeek | Jour de la semaine |
| `startTime` | LocalTime | Heure de début |
| `endTime` | LocalTime | Heure de fin |
| `type` | SessionType | CM, TD, TP |
| `recurrent` | boolean | Récurrent chaque semaine |

**Règles - 3 conflits détectés :** salle, enseignant, groupe (chevauchement horaire)

---

### `Session`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `plannedSlot` | PlannedSlot FK | Slot d'origine |
| `teacherId` | String | Enseignant effectif |
| `matiere` | Matiere FK | Matière |
| `room` | Room FK | Salle effective |
| `date` | LocalDate | Date effective |
| `startTime` | LocalTime | Heure de début |
| `duration` | int | Durée en minutes |
| `type` | SessionType | CM, TD, TP |
| `status` | SessionStatus | SCHEDULED, DONE, CANCELLED, RESCHEDULED |
| `cancelReason` | String | Motif d'annulation |

---

### `Attendance`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `session` | Session FK | Séance concernée |
| `studentId` | String | Référence User Service |
| `present` | boolean | Présent ou absent |
| `justification` | String | Motif d'absence justifiée |
| `justifiedAt` | LocalDateTime | Date de justification |

**Contrainte :** Unicité sur `(session, studentId)`

---

### `Enrollment`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `studentId` | String | Référence User Service |
| `matiere` | Matiere FK | Matière |
| `group` | StudentGroup FK | Groupe |
| `semester` | Semester FK | Semestre |
| `enrolledAt` | LocalDate | Date d'inscription |
| `status` | EnrollStatus | ACTIVE, WITHDRAWN, BLOCKED |

**Contrainte :** Unicité sur `(studentId, matiere, semester)`

---

### `Evaluation`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `title` | String | Intitulé |
| `type` | EvalType | TP, TD, DEVOIR, PARTIEL, EXAMEN_FINAL, PROJET, ORAL |
| `matiere` | Matiere FK | Matière concernée |
| `semester` | Semester FK | Semestre |
| `date` | LocalDate | Date prévue |
| `coefficient` | double | Coefficient dans la note finale |
| `maxScore` | double | Note maximale (défaut 20.0) |
| `isPublished` | boolean | Notes publiées aux étudiants |

**Relations :** `@OneToMany` → `EvaluationAttachment`, `Grade`

---

### `Grade`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `studentId` | String | Référence User Service |
| `evaluation` | Evaluation FK | Évaluation concernée |
| `matiere` | Matiere FK | Matière (dénormalisé) |
| `semester` | Semester FK | Semestre (dénormalisé) |
| `score` | double | Note obtenue |
| `comment` | String | Commentaire enseignant |
| `gradedAt` | LocalDateTime | Date de saisie |
| `gradedBy` | String | ID de l'enseignant |

**Contrainte :** Unicité sur `(studentId, evaluation)`

---

### `StudentProgress`

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `studentId` | String | Référence User Service |
| `semester` | Semester FK | Semestre |
| `semesterAverage` | double | Moyenne générale |
| `creditsObtained` | int | Crédits ECTS validés |
| `status` | ProgressStatus | ADMIS, AJOURNE, EXCLUS |
| `mention` | Mention | TB, B, AB, P, INSUFFISANT |
| `rank` | int | Rang dans la promotion |
| `isAdmis` | boolean | Admis au niveau suivant |
| `computedAt` | LocalDateTime | Date de calcul |

**Contrainte :** Unicité sur `(studentId, semester)`

---

## 8. DTOs calculés - non persistés

### `WeeklySchedule`

Vue calculée de l'emploi du temps pour une semaine.

```
semester, weekNumber, startDate, endDate,
slots: List<PlannedSlotResponse>,
sessions: List<SessionResponse>
```

### `CourseDashboard` ← nouveau v3

Vue synthétique d'une matière pour un étudiant - combine inscriptions, notes, présences et supports en une seule réponse.

```
studentId, matiereId, matiereName, teachingUnitName
semester
currentAverage          ← moyenne provisoire toutes évals confondues
attendanceRate          ← taux de présence actuel
enrollStatus            ← ACTIVE, WITHDRAWN, BLOCKED
evaluations: [
  { title, type, date, coefficient, score (null si non publié), maxScore }
]
upcomingEvaluations: [  ← évaluations à venir avec leurs pièces jointes
  { title, type, date, attachments: [ title, fileUrl ] }
]
resourceCount           ← nombre de supports disponibles
recentResources: [      ← 3 derniers supports publiés
  { title, type, fileUrl, createdAt }
]
```

### `Transcript`

Relevé de notes complet pour un semestre ou une année.

```
studentId, academicYear, semester,
ues: List<UEResult>,
generalAverage, totalCredits, mention
```

### `ClassStats` - données ML

```
evaluationId, matiereId, average, min, max,
passRate, standardDeviation, totalStudents
```

### `TeacherLoad`

```
teacherId, semester, totalHours, cmHours, tdHours, tpHours,
matieres: List<MatiereResponse>
```

### `AttendanceStats` - données ML

```
studentId, matiereId, semester,
totalSessions, presentCount, absenceCount,
justifiedCount, attendanceRate, blocked
```

### `StudentProgressSummary` - données ML

```
semesterId, groupId, filiereCode,
promoAverage, promoMin, promoMax,
promoStandardDeviation, passRate,
results: List<StudentProgress>
```

---

## 9. Énumérations

### `SemesterStatus`
`UPCOMING` · `ACTIVE` · `CLOSED` · `VALIDATED`

### `SessionType`
`CM` · `TD` · `TP`

### `SessionStatus`
`SCHEDULED` · `DONE` · `CANCELLED` · `RESCHEDULED`

### `EvalType`
`TP` · `TD` · `DEVOIR` · `PARTIEL` · `EXAMEN_FINAL` · `PROJET` · `ORAL`

### `ProgressStatus`
`ADMIS` · `AJOURNE` · `EXCLUS`

### `Mention`

| Valeur | Seuil |
|---|---|
| `TRES_BIEN` | >= 16.0 |
| `BIEN` | >= 14.0 |
| `ASSEZ_BIEN` | >= 12.0 |
| `PASSABLE` | >= 10.0 |
| `INSUFFISANT` | < 10.0 |

### `EnrollStatus`
`ACTIVE` · `WITHDRAWN` · `BLOCKED`

### `RoomType`
`AMPHI` · `TD` · `TP` · `LABO` · `VISIO`

### `GroupType`
`PROMO` · `TD` · `TP`

### `TeachingRole`
`CM` · `TD` · `TP`

### `ResourceType` ← nouveau v3

| Valeur | Description |
|---|---|
| `COURS` | Support de cours magistral (PDF, slides) |
| `TD` | Feuille de TD, exercices |
| `TP` | Sujet de TP, fichiers TP |
| `PROJET` | Cahier des charges, ressources projet |
| `LIEN` | Lien externe (vidéo YouTube, documentation) |
| `AUTRE` | Autre document |

### `ConflictType`
`ROOM` · `TEACHER` · `GROUP`

---

## 10. Cas d'utilisation

### UC-CRS-001 - Initialiser groupes et inscriptions d'un étudiant

**Acteur :** Système · **Trigger :** Kafka `student.profile.created`

1. Reçoit `studentId`, `filiereId`, `levelId`
2. Identifie le semestre `ACTIVE`
3. Identifie ou crée le groupe `PROMO`
4. Ajoute `studentId` dans le groupe PROMO
5. Crée les `Enrollment ACTIVE` pour toutes les matières du niveau
6. Publie `student.enrolled`

---

### UC-CRS-002 - Affecter un enseignant à une matière

**Acteur :** ADMIN_SCHOLAR · **Endpoint :** `POST /courses/assignments`

1. Vérifie absence de doublon (409)
2. Appelle User Service HTTP - vérifie `maxHoursPerWeek` (422 si dépassement)
3. Crée le `TeacherAssignment`

---

### UC-CRS-003 - Planifier un créneau

**Acteur :** ADMIN_SCHOLAR · **Endpoint :** `POST /courses/slots`

1. Vérifie disponibilité salle (409 si conflit)
2. Vérifie disponibilité enseignant (409 si conflit)
3. Vérifie disponibilité groupe (409 si conflit)
4. Vérifie `capacity >= group.maxSize` (422 si insuffisant)
5. Crée `PlannedSlot` + génère les `Session`

---

### UC-CRS-004 - Marquer les présences

**Acteur :** TEACHER · **Endpoint :** `PUT /courses/sessions/{id}/attendance`

1. Vérifie que l'enseignant est affecté à la matière (403 sinon)
2. Crée les `Attendance`
3. Session → `DONE`
4. Recalcule `AttendanceStats`
5. Si taux < seuil → `Enrollment → BLOCKED` + publie `attendance.threshold.exceeded`

---

### UC-CRS-005 - Ajouter un support de cours ← nouveau v3

**Acteur :** TEACHER · **Endpoint :** `POST /courses/matieres/{id}/resources`

1. Vérifie que l'enseignant est affecté à la matière (403 sinon)
2. Valide le fichier (format + taille max 50 Mo)
3. Stocke le fichier (TODO : S3/MinIO - pour l'instant URL simulée)
4. Crée le `CourseResource` en `isPublished = false` (DRAFT)
5. L'enseignant peut modifier le titre, la description, le type
6. L'enseignant publie le support (`PUT /courses/resources/{id}/publish`)
7. Le support devient visible pour les étudiants inscrits à la matière

**Formats acceptés :** PDF, DOCX, PPTX, XLSX, ZIP, MP4, MP3, PNG, JPEG  
**Taille max :** 50 Mo

---

### UC-CRS-006 - Gérer ses supports de cours ← nouveau v3

**Acteur :** TEACHER · **Endpoints :** `GET /courses/resources/my`, `PUT /courses/resources/{id}`, `DELETE /courses/resources/{id}`

1. L'enseignant consulte ses propres supports (publiés + brouillons)
2. Il peut modifier titre, description, type d'un support
3. Il peut remplacer le fichier d'un support (nouvel upload)
4. Il peut dépublier un support (`PUT /courses/resources/{id}/unpublish`)
5. Il peut supprimer un support (soft delete - `isDeleted = true`)
6. Un support supprimé disparaît de la vue étudiant

---

### UC-CRS-007 - Consulter les supports d'une matière ← nouveau v3

**Acteur :** STUDENT, TEACHER, ADMIN_SCHOLAR · **Endpoint :** `GET /courses/matieres/{id}/resources`

**Vue Étudiant :**
1. Vérifie que l'étudiant est inscrit à la matière (403 sinon)
2. Retourne uniquement les supports `isPublished = true` et `isDeleted = false`
3. Triés par type, puis par date décroissante
4. Avec URL de téléchargement pour chaque fichier

**Vue Enseignant :**
1. Retourne tous ses supports (publiés + brouillons) pour cette matière
2. Avec indicateur `isPublished` pour distinguer les brouillons

---

### UC-CRS-008 - Ajouter une pièce jointe à une évaluation ← nouveau v3

**Acteur :** TEACHER · **Endpoint :** `POST /courses/evaluations/{id}/attachments`

1. Vérifie que l'enseignant est affecté à la matière de cette évaluation (403 sinon)
2. Valide le fichier (format + taille max 20 Mo)
3. Stocke le fichier
4. Crée l'`EvaluationAttachment`
5. La pièce jointe est **immédiatement visible** par les étudiants inscrits (pas de brouillon)
6. L'enseignant peut ajouter plusieurs pièces jointes (énoncé + barème par exemple)

**Formats acceptés :** PDF, DOCX, XLSX, ZIP, PNG, JPEG  
**Taille max :** 20 Mo

---

### UC-CRS-009 - Gérer les pièces jointes d'une évaluation ← nouveau v3

**Acteur :** TEACHER · **Endpoints :** `GET /courses/evaluations/{id}/attachments`, `DELETE /courses/evaluations/{id}/attachments/{attachId}`

1. L'enseignant consulte la liste des pièces jointes
2. Il peut supprimer une pièce jointe (soft delete)
3. Il ne peut supprimer que ses propres pièces jointes (403 sinon)

---

### UC-CRS-010 - Consulter le tableau de bord d'un cours ← nouveau v3

**Acteur :** STUDENT · **Endpoint :** `GET /courses/students/{id}/dashboard?semesterId=`

Retourne la liste de tous les cours du semestre avec pour chacun :
1. Nom de la matière et de l'UE
2. Moyenne provisoire actuelle
3. Taux de présence actuel
4. Statut d'inscription (ACTIVE, BLOCKED)
5. Évaluations à venir (avec pièces jointes si disponibles)
6. Nombre de supports disponibles + 3 supports récents

---

### UC-CRS-011 - Créer une évaluation

**Acteur :** TEACHER · **Endpoint :** `POST /courses/evaluations`

1. Vérifie affectation enseignant à la matière (403)
2. Vérifie que somme coefficients ≤ 1.0 (422 avec solde)
3. Crée l'`Evaluation`

---

### UC-CRS-012 - Saisir les notes

**Acteur :** TEACHER · **Endpoint :** `POST /courses/evaluations/{id}/grades`

1. Vérifie semestre `ACTIVE` (422 sinon)
2. Valide chaque score (0 ≤ score ≤ maxScore)
3. Crée les `Grade`
4. Recalcule `ClassStats`

---

### UC-CRS-013 - Calculer la progression semestrielle

**Acteur :** ADMIN_SCHOLAR · **Endpoint :** `POST /courses/semesters/{id}/compute-progress`

1. Note finale par matière = Σ (score/maxScore × 20) × coeff évaluation
2. Moyenne UE = Σ (note × coeff) / Σ coefficients
3. Crédits = Σ crédits UEs validées (moyenne >= 10)
4. Statut, mention, rang
5. `StudentProgress` créé ou mis à jour

---

### UC-CRS-014 - Valider un semestre

**Acteur :** SUPER_ADMIN · **Endpoint :** `PUT /courses/semesters/{id}/validate`

1. Vérifie semestre `CLOSED` et `StudentProgress` complets
2. Semestre → `VALIDATED`
3. Publie `semester.validated`

---

### UC-CRS-015 - Bloquer un étudiant pour impayé

**Acteur :** Système · **Trigger :** Kafka `student.payment.blocked`

1. Trouve tous les `Enrollment ACTIVE` de l'étudiant
2. Passe en `BLOCKED`

---

## 11. Règles métier transversales

### Calcul de la note finale d'une matière

```
Note matière = Σ (score / maxScore × 20) × coefficient_évaluation

Exemple :
  TP noté (coeff 0.3)   : 14/20
  Devoir  (coeff 0.3)   : 12/20
  Partiel (coeff 0.4)   : 16/20
  Note finale = 14×0.3 + 12×0.3 + 16×0.4 = 14.2/20
```

### Validation d'une UE

```
Moyenne UE = Σ (note matière × coeff matière) / Σ coefficients
UE validée si Moyenne UE >= 10/20
Crédits obtenus = Σ crédits des UEs validées
```

### Seuil d'absences

```
attendanceRate = (presentCount / totalSessions) × 100
Si attendanceRate < matiere.attendanceThreshold (défaut 80%) :
  Enrollment → BLOCKED
  attendance.threshold.exceeded publié
```

### Règles sur les supports (CourseResource)

```
DRAFT (isPublished = false) :
  Visible uniquement par l'enseignant auteur
  Modifiable librement

PUBLIÉ (isPublished = true) :
  Visible par tous les étudiants inscrits à la matière
  Modifiable uniquement par l'auteur
  La dépublication remet en DRAFT

SUPPRIMÉ (isDeleted = true) :
  Invisible pour les étudiants
  Conservé en base pour traçabilité
  Opération irréversible depuis l'API
```

### Règles sur les pièces jointes (EvaluationAttachment)

```
Visible immédiatement après upload par les étudiants inscrits
Pas de mécanisme de brouillon
Seul l'auteur peut supprimer (soft delete)
Suppression irréversible depuis l'API
```

### Immuabilité d'un semestre VALIDATED

```
Semester.status = VALIDATED →
  Grade, StudentProgress, Evaluation : lecture seule
  CourseResource : peut toujours être modifié (contenu indépendant du semestre)
  EvaluationAttachment : peut toujours être modifié
```

### Détection des conflits de planning

```
Chevauchement : startTime_A < endTime_B ET startTime_B < endTime_A
Conflit salle     : même roomId + même dayOfWeek + chevauchement
Conflit enseignant : même teacherId + même dayOfWeek + chevauchement
Conflit groupe    : même groupId + même dayOfWeek + chevauchement
```

---

## 12. Données pour le pipeline ML

Le Course Service est la **source de données principale** pour les modèles de détection d'anomalies.

### Métriques exposées par étudiant

| Métrique | Source | Modèle ML |
|---|---|---|
| `attendanceRate` par matière | `AttendanceStats` | Isolation Forest, One-class SVM |
| `semesterAverage` | `StudentProgress` | LSTM Autoencoder (série temporelle) |
| `rank` dans la promo | `StudentProgress` | MLP hybride |
| Évolution des notes | `Grade` historique | LSTM Autoencoder |

### Métriques exposées par matière / promotion

| Métrique | Source | Modèle ML |
|---|---|---|
| `average` / `standardDeviation` | `ClassStats` | Isolation Forest |
| `passRate` | `ClassStats` | One-class SVM |
| Distribution des notes | `Grade` agrégés | MLP |

### Endpoints dédiés ML

```
GET /courses/ml/attendance-stats?semesterId=&filiereId=
GET /courses/ml/grade-stats?semesterId=&matiereId=
GET /courses/ml/progress-summary?semesterId=&groupId=
```

---

## 13. Sécurité des routes

| Route | Méthode | Rôles |
|---|---|---|
| `/courses/semesters/**` | GET | Authentifié |
| `/courses/semesters` | POST | ADMIN_SCHOLAR, SUPER_ADMIN |
| `/courses/semesters/{id}/validate` | PUT | SUPER_ADMIN |
| `/courses/teaching-units/**` | GET | Authentifié |
| `/courses/teaching-units` | POST/PUT | ADMIN_SCHOLAR |
| `/courses/matieres/**` | GET | Authentifié |
| `/courses/matieres/{id}/resources` | GET | Authentifié (étudiant inscrit) |
| `/courses/matieres/{id}/resources` | POST | TEACHER |
| `/courses/resources/my` | GET | TEACHER |
| `/courses/resources/{id}` | PUT/DELETE | TEACHER (auteur uniquement) |
| `/courses/resources/{id}/publish` | PUT | TEACHER (auteur uniquement) |
| `/courses/slots` | POST/PUT/DELETE | ADMIN_SCHOLAR |
| `/courses/sessions/{id}/attendance` | PUT | TEACHER |
| `/courses/evaluations` | POST | TEACHER |
| `/courses/evaluations/{id}/grades` | POST | TEACHER |
| `/courses/evaluations/{id}/attachments` | POST | TEACHER |
| `/courses/evaluations/{id}/attachments/{id}` | DELETE | TEACHER (auteur) |
| `/courses/students/{id}/dashboard` | GET | Propriétaire, ADMIN_SCHOLAR |
| `/courses/students/{id}/progress` | GET | Propriétaire, ADMIN_SCHOLAR, TEACHER |
| `/courses/ml/**` | GET | ADMIN_SCHOLAR, SUPER_ADMIN |

---

## 14. Événements Kafka

### Consommés (5)

| Topic | Producteur | Action |
|---|---|---|
| `student.profile.created` | User Service | Crée groupes + enrollments |
| `student.promoted` | User Service | Met à jour groupes au nouveau niveau |
| `student.payment.blocked` | Payment Service | Bloque les Enrollment |
| `teacher.profile.created` | User Service | Rend l'enseignant disponible |
| `teacher.deactivated` | User Service | Retire des affectations futures |

### Publiés (5)

| Topic | Déclencheur | Consommateurs |
|---|---|---|
| `student.enrolled` | Inscription matières | Notification Service |
| `attendance.threshold.exceeded` | Seuil absences | Notification Service |
| `grades.published` | Notes publiées | Notification Service |
| `session.cancelled` | Séance annulée | Notification Service |
| `semester.validated` | Validation semestre | User Service, Payment Service, Document Service, Notification Service |

### Structure `SemesterValidated`

```json
{
  "semesterId": 1,
  "academicYear": "2026-2027",
  "semester": "S1",
  "isLastSemester": false,
  "results": [
    {
      "studentId": "uuid",
      "semesterAverage": 14.5,
      "creditsObtained": 30,
      "status": "ADMIS",
      "isAdmis": true,
      "rank": 3
    }
  ]
}
```

---

## 15. Dépendances cross-services

| Service | Type | Sens | Description |
|---|---|---|---|
| User Service | Kafka consumed | User → Course | `student.profile.created` |
| User Service | Kafka consumed | User → Course | `student.promoted` |
| User Service | Kafka consumed | User → Course | `teacher.profile.created` |
| User Service | HTTP sync | Course → User | `GET /users/teachers/{id}` |
| Payment Service | Kafka consumed | Payment → Course | `student.payment.blocked` |
| Payment Service | Kafka published | Course → Payment | `semester.validated` |
| User Service | Kafka published | Course → User | `semester.validated` |
| Document Service | Kafka published | Course → Document | `semester.validated` |
| Notification Service | Kafka published | Course → Notification | Tous les events |

---

## 16. Schedulers

| Cron | Méthode | Description |
|---|---|---|
| `0 0 8 * * MON` | `generateWeeklySessions()` | Sessions de la semaine à venir |
| `0 0 1 * * *` | `checkAttendanceThresholds()` | Seuils d'absences quotidiens |
| `0 0 6 * * *` | `updateSemesterStatus()` | UPCOMING → ACTIVE si startDate atteinte |

---

## 17. Endpoints API complets

### Semestres

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/semesters` | Lister | Authentifié |
| GET | `/courses/semesters/current` | Semestre actif | Authentifié |
| GET | `/courses/semesters/{id}` | Détail | Authentifié |
| POST | `/courses/semesters` | Créer | ADMIN_SCHOLAR |
| PUT | `/courses/semesters/{id}/close` | Clôturer | ADMIN_SCHOLAR |
| POST | `/courses/semesters/{id}/compute-progress` | Calculer progressions | ADMIN_SCHOLAR |
| PUT | `/courses/semesters/{id}/validate` | Valider | SUPER_ADMIN |

### Structure académique

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/teaching-units` | Lister UEs `?levelId=` | Authentifié |
| POST | `/courses/teaching-units` | Créer | ADMIN_SCHOLAR |
| PUT | `/courses/teaching-units/{id}` | Modifier | ADMIN_SCHOLAR |
| GET | `/courses/matieres` | Lister `?teachingUnitId=` | Authentifié |
| POST | `/courses/matieres` | Créer | ADMIN_SCHOLAR |
| PUT | `/courses/matieres/{id}` | Modifier | ADMIN_SCHOLAR |

### Supports de cours - CourseResource ← nouveau v3

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/matieres/{id}/resources` | Supports d'une matière | Authentifié (inscrit) |
| POST | `/courses/matieres/{id}/resources` | Ajouter un support (multipart) | TEACHER |
| GET | `/courses/resources/my` | Mes supports (publiés + brouillons) | TEACHER |
| GET | `/courses/resources/{id}` | Détail d'un support | Authentifié |
| PUT | `/courses/resources/{id}` | Modifier titre/description/type | TEACHER (auteur) |
| PUT | `/courses/resources/{id}/publish` | Publier un support | TEACHER (auteur) |
| PUT | `/courses/resources/{id}/unpublish` | Dépublier (retour DRAFT) | TEACHER (auteur) |
| DELETE | `/courses/resources/{id}` | Supprimer (soft delete) | TEACHER (auteur) |

### Pièces jointes évaluations - EvaluationAttachment ← nouveau v3

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/evaluations/{id}/attachments` | Liste des pièces jointes | Authentifié (inscrit) |
| POST | `/courses/evaluations/{id}/attachments` | Ajouter une pièce jointe (multipart) | TEACHER |
| DELETE | `/courses/evaluations/{id}/attachments/{attachId}` | Supprimer | TEACHER (auteur) |

### Salles

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/rooms` | Lister | Authentifié |
| POST | `/courses/rooms` | Créer | ADMIN_SCHOLAR |
| PUT | `/courses/rooms/{id}` | Modifier | ADMIN_SCHOLAR |
| GET | `/courses/rooms/available` | Salles disponibles `?day=&start=&end=` | ADMIN_SCHOLAR |

### Groupes

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/groups` | Lister `?semesterId=&filiereId=` | Authentifié |
| POST | `/courses/groups` | Créer | ADMIN_SCHOLAR |
| PUT | `/courses/groups/{id}/students` | Gérer membres | ADMIN_SCHOLAR |
| GET | `/courses/groups/{id}/students` | Liste membres | ADMIN_SCHOLAR, TEACHER |

### Affectations enseignants

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/assignments` | Lister `?teacherId=&semesterId=` | Authentifié |
| POST | `/courses/assignments` | Créer | ADMIN_SCHOLAR |
| DELETE | `/courses/assignments/{id}` | Supprimer | ADMIN_SCHOLAR |
| GET | `/courses/teachers/{id}/load` | Charge horaire `?semesterId=` | Authentifié |

### Emploi du temps

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/slots` | Créneaux `?semesterId=&groupId=` | Authentifié |
| POST | `/courses/slots` | Créer un créneau | ADMIN_SCHOLAR |
| PUT | `/courses/slots/{id}` | Modifier | ADMIN_SCHOLAR |
| DELETE | `/courses/slots/{id}` | Supprimer | ADMIN_SCHOLAR |
| GET | `/courses/schedule` | Emploi du temps `?week=&userId=` | Authentifié |

### Séances et présences

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/sessions` | Lister `?matiereId=&semesterId=` | Authentifié |
| GET | `/courses/sessions/{id}` | Détail | Authentifié |
| PUT | `/courses/sessions/{id}/attendance` | Marquer présences | TEACHER |
| PUT | `/courses/sessions/{id}/cancel` | Annuler | TEACHER, ADMIN_SCHOLAR |
| PUT | `/courses/sessions/{id}/reschedule` | Reporter | ADMIN_SCHOLAR |
| GET | `/courses/students/{id}/attendance` | Présences `?semesterId=` | Authentifié |

### Inscriptions

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/enrollments` | Lister `?studentId=&semesterId=` | Authentifié |
| PUT | `/courses/enrollments/{id}/status` | Changer statut | ADMIN_SCHOLAR |

### Évaluations et notes

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/evaluations` | Lister `?matiereId=&semesterId=` | Authentifié |
| POST | `/courses/evaluations` | Créer | TEACHER |
| PUT | `/courses/evaluations/{id}` | Modifier | TEACHER |
| DELETE | `/courses/evaluations/{id}` | Supprimer | TEACHER |
| PUT | `/courses/evaluations/{id}/publish` | Publier les notes | TEACHER |
| POST | `/courses/evaluations/{id}/grades` | Saisir notes | TEACHER |
| PUT | `/courses/evaluations/{id}/grades/{gId}` | Modifier une note | TEACHER |
| GET | `/courses/evaluations/{id}/stats` | Statistiques | Authentifié |
| GET | `/courses/students/{id}/grades` | Notes `?semesterId=` | Authentifié |

### Progression et tableau de bord étudiant

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/students/{id}/dashboard` | Tableau de bord `?semesterId=` | Propriétaire |
| GET | `/courses/students/{id}/progress` | Bilan semestriel `?semesterId=` | Authentifié |
| GET | `/courses/students/{id}/transcript` | Relevé de notes `?year=` | Authentifié |
| GET | `/courses/students/{id}/attendance-stats` | Stats présence | Authentifié |

### Pipeline ML

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/courses/ml/attendance-stats` | Stats présences `?semesterId=&filiereId=` | ADMIN_SCHOLAR |
| GET | `/courses/ml/grade-stats` | Stats notes `?semesterId=&matiereId=` | ADMIN_SCHOLAR |
| GET | `/courses/ml/progress-summary` | Synthèse promos `?semesterId=&groupId=` | ADMIN_SCHOLAR |

---

## 18. Configuration

### `application.yml` (local)

```yaml
spring:
  application:
    name: course-service
  config:
    import: optional:configserver:http://localhost:8761
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### `course-service.yaml` (Config Server)

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/coursedb
    username: course_service
    password: course_service#123@
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
  servlet:
    multipart:
      max-file-size: 55MB
      max-request-size: 60MB

eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false
```

### Base de données

```sql
CREATE DATABASE coursedb;
CREATE USER course_service WITH PASSWORD 'course_service#123@';
GRANT ALL PRIVILEGES ON DATABASE coursedb TO course_service;
```

### Gateway - routes à ajouter

```yaml
- id: course-route
  uri: http://localhost:8083
  predicates:
    - Path=/courses/**
```
