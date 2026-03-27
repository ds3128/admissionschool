# Course Service — User Stories complètes v2

**Projet :** AdmissionSchool  
**Service :** Course Service  
**Version :** 2.0.0  
**Date :** Mars 2026

**Changelog v2 :**
- EPIC 8 ajouté — Supports de cours (CourseResource)
- EPIC 9 ajouté — Pièces jointes évaluations (EvaluationAttachment)
- EPIC 5 enrichi — Vue étudiant : tableau de bord, progression par cours, accès supports
- Total : 35 user stories (vs 27 en v1)

---

## Acteurs

| Acteur | Rôle système | Description |
|---|---|---|
| **Super Admin** | `SUPER_ADMIN` | Valide les semestres |
| **Scolarité** | `ADMIN_SCHOLAR` | Gère structure académique, emploi du temps, groupes |
| **Enseignant** | `TEACHER` | Présences, évaluations, notes, supports de cours |
| **Étudiant** | `STUDENT` | Consulte cours, supports, notes, progression |
| **Système** | `system` | Kafka consumers + schedulers |

---

## EPIC 1 — Structure académique

### US-CRS-001 — Créer une unité d'enseignement

**En tant que** Scolarité  
**Je veux** créer une UE  
**Afin de** structurer les matières par blocs pédagogiques

**Critères d'acceptation :**
- [ ] Je peux saisir : code unique, nom, crédits ECTS, niveau (studyLevelId), numéro semestre, coefficient
- [ ] Code vérifié unique (409 si doublon)

**Endpoint :** `POST /courses/teaching-units`

---

### US-CRS-002 — Créer une matière

**En tant que** Scolarité  
**Je veux** créer une matière dans une UE  
**Afin de** définir les enseignements du niveau

**Critères d'acceptation :**
- [ ] Je peux saisir : code unique, nom, UE, département, coefficient, heures CM/TD/TP, seuil de présence
- [ ] `totalHours = hoursCM + hoursTD + hoursTP` calculé automatiquement
- [ ] Seuil de présence configurable par matière (défaut 80%)
- [ ] Une matière avec des `Enrollment` actifs ne peut pas être supprimée (422)

**Endpoint :** `POST /courses/matieres`

---

### US-CRS-003 — Créer et gérer un semestre

**En tant que** Scolarité  
**Je veux** créer un semestre académique

**Critères d'acceptation :**
- [ ] Je peux saisir : label, année, dates, indicateur dernier semestre de l'année
- [ ] Un seul semestre `isCurrent = true` à la fois
- [ ] Scheduler UPCOMING → ACTIVE automatiquement quand `startDate` atteinte
- [ ] Semestre `VALIDATED` immuable

**Endpoint :** `POST /courses/semesters`

---

### US-CRS-004 — Gérer les salles

**En tant que** Scolarité  
**Je veux** gérer les salles disponibles

**Critères d'acceptation :**
- [ ] Créer une salle avec : nom, bâtiment, capacité, type, équipements
- [ ] Marquer une salle indisponible (`isAvailable = false`)
- [ ] Consulter les salles disponibles sur un créneau horaire (`GET /courses/rooms/available`)
- [ ] Salle indisponible ne peut pas être assignée à un créneau (422)

**Endpoints :** `POST /courses/rooms`, `GET /courses/rooms/available`

---

## EPIC 2 — Groupes et affectations

### US-CRS-005 — Initialisation automatique groupes et inscriptions

**En tant que** Système  
**Je veux** créer automatiquement groupes et inscriptions quand un étudiant est confirmé

**Critères d'acceptation :**
- [ ] Consomme `student.profile.created` depuis Kafka
- [ ] Identifie le semestre `ACTIVE` pour le niveau et la filière
- [ ] Identifie ou crée le groupe `PROMO`
- [ ] Ajoute `studentId` dans le groupe PROMO
- [ ] Crée un `Enrollment ACTIVE` pour chaque matière du niveau
- [ ] Publie `student.enrolled`
- [ ] Si aucun semestre `ACTIVE` → log d'avertissement, pas d'erreur bloquante

**Trigger :** Kafka `student.profile.created`

---

### US-CRS-006 — Créer des groupes TD et TP

**En tant que** Scolarité  
**Je veux** créer des sous-groupes pour les séances pratiques

**Critères d'acceptation :**
- [ ] Créer groupe TD ou TP avec : nom, niveau, filière, semestre, taille max
- [ ] Ajouter ou retirer des étudiants dans le groupe
- [ ] Étudiant peut appartenir à 1 PROMO + 1 TD + 1 TP simultanément
- [ ] Taille max vérifiée avant ajout (422 si dépassement)

**Endpoints :** `POST /courses/groups`, `PUT /courses/groups/{id}/students`

---

### US-CRS-007 — Affecter un enseignant à une matière

**En tant que** Scolarité  
**Je veux** affecter un enseignant à une matière pour un semestre

**Critères d'acceptation :**
- [ ] Créer une affectation avec : teacherId, matiereId, role (CM/TD/TP), semesterId, heures
- [ ] Vérification doublon (409 si même enseignant + matière + rôle + semestre)
- [ ] Appel HTTP User Service pour vérifier `maxHoursPerWeek` (422 si dépassement)
- [ ] Consulter la charge horaire d'un enseignant (`TeacherLoad` DTO)

**Endpoints :** `POST /courses/assignments`, `GET /courses/teachers/{id}/load`

---

## EPIC 3 — Emploi du temps

### US-CRS-008 — Planifier un créneau

**En tant que** Scolarité  
**Je veux** créer un créneau dans l'emploi du temps

**Critères d'acceptation :**
- [ ] Créer avec : matière, enseignant, salle, groupe, semestre, jour, heure, type, récurrent
- [ ] 3 conflits détectés (409 avec détail) :
  - Conflit salle : même salle + même jour + chevauchement
  - Conflit enseignant : même enseignant + même jour + chevauchement
  - Conflit groupe : même groupe + même jour + chevauchement
- [ ] `capacity >= group.maxSize` vérifié (422 si insuffisant)
- [ ] `recurrent = true` → Sessions générées pour toutes les semaines du semestre
- [ ] `recurrent = false` → Une seule Session générée

**Endpoint :** `POST /courses/slots`

---

### US-CRS-009 — Consulter l'emploi du temps

**En tant que** Étudiant, Enseignant ou Administrateur  
**Je veux** voir l'emploi du temps d'une semaine

**Critères d'acceptation :**
- [ ] Filtrer par semaine (numéro ou date)
- [ ] Vue Étudiant : uniquement son groupe (PROMO + TD + TP)
- [ ] Vue Enseignant : uniquement ses matières assignées
- [ ] Vue Admin : tout l'emploi du temps
- [ ] Sessions annulées/reportées visibles avec statut

**Endpoint :** `GET /courses/schedule?week=&userId=`

---

### US-CRS-010 — Annuler ou reporter une séance

**En tant que** Enseignant ou Scolarité

**Critères d'acceptation :**
- [ ] Annuler avec motif → Session → CANCELLED
- [ ] Reporter avec nouvelle date/heure/salle → Session → RESCHEDULED + nouvelle Session créée
- [ ] `session.cancelled` publié → Notification Service notifie les étudiants

**Endpoints :** `PUT /courses/sessions/{id}/cancel`, `PUT /courses/sessions/{id}/reschedule`

---

## EPIC 4 — Présences

### US-CRS-011 — Marquer les présences

**En tant que** Enseignant  
**Je veux** marquer les présences après une séance

**Critères d'acceptation :**
- [ ] Soumettre la liste `[{ studentId, present, justification }]`
- [ ] Enseignant doit être affecté à la matière (403 sinon)
- [ ] `Attendance` créée pour chaque étudiant du groupe
- [ ] Session → DONE
- [ ] `AttendanceStats` recalculé par étudiant
- [ ] Si taux < seuil : `Enrollment → BLOCKED` + `attendance.threshold.exceeded` publié

**Endpoint :** `PUT /courses/sessions/{id}/attendance`

---

### US-CRS-012 — Justifier une absence

**En tant que** Étudiant ou Scolarité

**Critères d'acceptation :**
- [ ] Soumettre une justification pour une absence (`Attendance.justification`)
- [ ] Session doit être `DONE`
- [ ] `AttendanceStats.justifiedCount` mis à jour

---

### US-CRS-013 — Consulter les statistiques de présence

**En tant que** Étudiant, Enseignant ou Scolarité

**Critères d'acceptation :**
- [ ] Vois : total séances, présences, absences, justifiées, taux, statut BLOCKED
- [ ] Vue Étudiant : uniquement ses propres stats
- [ ] Vue Enseignant : ses matières uniquement
- [ ] Vue Admin : toutes les stats

**Endpoint :** `GET /courses/students/{id}/attendance-stats?semesterId=`

---

### US-CRS-014 — Blocage automatique pour impayé

**En tant que** Système  
**Trigger :** Kafka `student.payment.blocked`

**Critères d'acceptation :**
- [ ] Tous les `Enrollment ACTIVE` de l'étudiant → `BLOCKED`
- [ ] Étudiant exclu des examens

---

## EPIC 5 — Vue étudiant enrichie ← enrichi v3

### US-CRS-015 — Consulter le tableau de bord de mes cours

**En tant que** Étudiant  
**Je veux** avoir une vue synthétique de tous mes cours du semestre  
**Afin de** suivre ma progression globale en un coup d'œil

**Critères d'acceptation :**
- [ ] Je vois la liste de toutes mes matières du semestre avec pour chacune :
  - Nom de la matière et de l'UE d'appartenance
  - Enseignant(s) affecté(s)
  - Ma moyenne provisoire actuelle (calculée sur les notes publiées)
  - Mon taux de présence actuel
  - Mon statut d'inscription (ACTIVE, BLOCKED)
  - Nombre de supports disponibles publiés
  - Les 3 derniers supports publiés (titre + type + date)
  - Prochaine évaluation à venir (titre + date + type + pièces jointes si disponibles)
- [ ] Je ne vois que mes propres données
- [ ] La liste est filtrée par semestre (défaut : semestre courant)

**Endpoint :** `GET /courses/students/{id}/dashboard?semesterId=`

---

### US-CRS-016 — Consulter mes notes

**En tant que** Étudiant  
**Je veux** consulter mes notes par matière et par semestre

**Critères d'acceptation :**
- [ ] Je vois uniquement les évaluations publiées (`isPublished = true`)
- [ ] Je vois : note, commentaire enseignant, date de saisie, coefficient de l'évaluation
- [ ] Je vois ma moyenne provisoire par matière
- [ ] Je peux filtrer par semestre et par matière

**Endpoint :** `GET /courses/students/{id}/grades?semesterId=`

---

### US-CRS-017 — Consulter ma progression semestrielle

**En tant que** Étudiant  
**Je veux** consulter mon bilan semestriel officiel

**Critères d'acceptation :**
- [ ] Je vois : moyenne générale, crédits obtenus, statut, mention, rang dans la promotion
- [ ] Ce bilan n'est visible qu'après validation du semestre (`VALIDATED`)

**Endpoint :** `GET /courses/students/{id}/progress?semesterId=`

---

### US-CRS-018 — Consulter mon relevé de notes

**En tant que** Étudiant  
**Je veux** consulter mon relevé de notes complet

**Critères d'acceptation :**
- [ ] Je vois la structure UE → matières → évaluations → notes
- [ ] Je vois la moyenne par UE, les crédits, la mention
- [ ] Filtrable par année académique ou par semestre

**Endpoint :** `GET /courses/students/{id}/transcript?year=`

---

## EPIC 6 — Évaluations et notes

### US-CRS-019 — Créer une évaluation

**En tant que** Enseignant  
**Je veux** créer une évaluation dans ma matière

**Critères d'acceptation :**
- [ ] Créer avec : titre, type, matière, semestre, date, coefficient, note max
- [ ] Enseignant doit être affecté à la matière (403)
- [ ] Somme coefficients ≤ 1.0 (422 avec solde disponible si dépassement)
- [ ] Évaluation avec `Grade` existants ne peut pas être supprimée (422)

**Endpoint :** `POST /courses/evaluations`

---

### US-CRS-020 — Saisir les notes

**En tant que** Enseignant  
**Je veux** saisir les notes des étudiants

**Critères d'acceptation :**
- [ ] Soumettre `[{ studentId, score, comment }]`
- [ ] Semestre doit être `ACTIVE` (422 sinon)
- [ ] Chaque score validé : 0 ≤ score ≤ maxScore (400 avec liste erreurs si invalide)
- [ ] `Grade` créé ou mis à jour par étudiant
- [ ] `ClassStats` recalculé après saisie
- [ ] Moyenne provisoire de chaque étudiant mise à jour
- [ ] Grade dans semestre `VALIDATED` immuable (422)

**Endpoint :** `POST /courses/evaluations/{id}/grades`

---

### US-CRS-021 — Publier les notes

**En tant que** Enseignant  
**Je veux** rendre les notes visibles aux étudiants

**Critères d'acceptation :**
- [ ] Avant publication : étudiants ne voient pas leurs notes
- [ ] Après publication (`isPublished = true`) : notes visibles
- [ ] `grades.published` publié → Notification Service notifie les étudiants

**Endpoint :** `PUT /courses/evaluations/{id}/publish`

---

### US-CRS-022 — Consulter les statistiques d'une évaluation

**En tant que** Enseignant ou Administrateur

**Critères d'acceptation :**
- [ ] Vois : moyenne, min, max, taux de réussite, écart-type, nombre d'étudiants
- [ ] Ces données alimentent le pipeline ML

**Endpoint :** `GET /courses/evaluations/{id}/stats`

---

## EPIC 7 — Progression semestrielle

### US-CRS-023 — Calculer la progression semestrielle

**En tant que** Scolarité  
**Je veux** calculer la progression de tous les étudiants

**Critères d'acceptation :**
- [ ] Semestre doit être `CLOSED` (422 sinon)
- [ ] Pour chaque étudiant :
  - Note finale par matière = Σ (score/maxScore × 20) × coeff évaluation
  - Moyenne UE = Σ (note × coeff) / Σ coefficients
  - Crédits = Σ crédits UEs validées (moyenne >= 10)
  - Statut : ADMIS / AJOURNE / EXCLUS
  - Mention : TB(>=16) / B(>=14) / AB(>=12) / P(>=10) / INSUFFISANT
  - Rang dans la promotion
- [ ] `StudentProgress` créé ou mis à jour
- [ ] Calcul relançable avant validation

**Endpoint :** `POST /courses/semesters/{id}/compute-progress`

---

### US-CRS-024 — Valider un semestre

**En tant que** Super Admin  
**Je veux** valider officiellement un semestre

**Critères d'acceptation :**
- [ ] Semestre doit être `CLOSED` et `StudentProgress` complets (422 sinon)
- [ ] Semestre → `VALIDATED` (immuable)
- [ ] `semester.validated` publié avec tous les résultats + `isLastSemester`
- [ ] User Service → promotion étudiants ADMIS
- [ ] Payment Service → renouvellement bourses mérite (si `isLastSemester = true`)
- [ ] Document Service → bulletins PDF
- [ ] Notification Service → résultats aux étudiants

**Endpoint :** `PUT /courses/semesters/{id}/validate`

---

### US-CRS-025 — Mise à jour automatique après promotion

**En tant que** Système  
**Trigger :** Kafka `student.promoted`

**Critères d'acceptation :**
- [ ] Retire l'étudiant des groupes du niveau précédent
- [ ] Ajoute l'étudiant au groupe PROMO du nouveau niveau
- [ ] Crée les `Enrollment` pour les matières du nouveau niveau

---

## EPIC 8 — Supports de cours (CourseResource) ← nouveau v3

### US-CRS-026 — Ajouter un support de cours

**En tant que** Enseignant  
**Je veux** ajouter des supports pédagogiques à ma matière  
**Afin de** partager mes documents de cours avec les étudiants

**Critères d'acceptation :**
- [ ] Je dois être affecté à la matière (403 sinon)
- [ ] Je peux uploader un fichier (PDF, DOCX, PPTX, XLSX, ZIP, MP4, MP3, PNG, JPEG) — max 50 Mo
- [ ] Je peux aussi ajouter un lien externe (YouTube, documentation officielle, etc.)
- [ ] Je choisis le type : COURS, TD, TP, PROJET, LIEN, AUTRE
- [ ] Le support est créé en **DRAFT** (`isPublished = false`) — invisible pour les étudiants
- [ ] Je peux ajouter un titre et une description
- [ ] Un message d'erreur clair si le format ou la taille n'est pas respecté (422)

**Endpoint :** `POST /courses/matieres/{id}/resources` (multipart)

---

### US-CRS-027 — Gérer mes supports de cours

**En tant que** Enseignant  
**Je veux** consulter, modifier et supprimer mes supports  
**Afin de** maintenir mes contenus pédagogiques à jour

**Critères d'acceptation :**
- [ ] Je vois tous mes supports (publiés ET brouillons) pour mes matières
- [ ] Je peux modifier le titre, la description et le type d'un support
- [ ] Je peux remplacer le fichier d'un support existant (nouvel upload)
- [ ] Je ne peux modifier que mes propres supports (403 si autre auteur)
- [ ] Je peux supprimer un support (soft delete — disparaît de la vue étudiant, conservé en base)
- [ ] Je vois clairement le statut de chaque support (DRAFT / PUBLIÉ)

**Endpoints :** `GET /courses/resources/my`, `PUT /courses/resources/{id}`, `DELETE /courses/resources/{id}`

---

### US-CRS-028 — Publier ou dépublier un support

**En tant que** Enseignant  
**Je veux** contrôler la visibilité de mes supports  
**Afin de** les partager au bon moment avec les étudiants

**Critères d'acceptation :**
- [ ] Je peux publier un support DRAFT → visible par les étudiants inscrits
- [ ] Je peux dépublier un support publié → retour en DRAFT, invisible pour les étudiants
- [ ] Seul l'auteur du support peut publier/dépublier (403 sinon)
- [ ] Un support publié reste accessible aux étudiants inscrits même après la fin du semestre

**Endpoints :** `PUT /courses/resources/{id}/publish`, `PUT /courses/resources/{id}/unpublish`

---

### US-CRS-029 — Consulter les supports d'une matière (côté étudiant)

**En tant que** Étudiant  
**Je veux** accéder aux supports de cours de mes matières  
**Afin de** étudier et préparer mes évaluations

**Critères d'acceptation :**
- [ ] Je dois être inscrit à la matière (403 sinon)
- [ ] Je vois uniquement les supports publiés (`isPublished = true`, `isDeleted = false`)
- [ ] Les supports sont triés par type puis par date de publication décroissante
- [ ] Je vois pour chaque support : titre, type, description, date, taille du fichier
- [ ] Je peux télécharger le fichier ou accéder au lien externe
- [ ] Je ne vois jamais les brouillons de l'enseignant

**Endpoint :** `GET /courses/matieres/{id}/resources`

---

## EPIC 9 — Pièces jointes aux évaluations (EvaluationAttachment) ← nouveau v3

### US-CRS-030 — Ajouter une pièce jointe à une évaluation

**En tant que** Enseignant  
**Je veux** ajouter des documents à une évaluation  
**Afin de** fournir aux étudiants l'énoncé, le barème ou tout document nécessaire

**Critères d'acceptation :**
- [ ] Je dois être affecté à la matière de cette évaluation (403 sinon)
- [ ] Je peux uploader un fichier (PDF, DOCX, XLSX, ZIP, PNG, JPEG) — max 20 Mo
- [ ] La pièce jointe est **immédiatement visible** par les étudiants inscrits (pas de brouillon)
- [ ] Je peux ajouter plusieurs pièces jointes à une même évaluation (ex : énoncé + barème)
- [ ] Je peux ajouter un titre et une description optionnelle pour chaque pièce jointe
- [ ] Un message d'erreur clair si format ou taille non respecté (422)

**Endpoint :** `POST /courses/evaluations/{id}/attachments` (multipart)

---

### US-CRS-031 — Gérer les pièces jointes d'une évaluation

**En tant que** Enseignant  
**Je veux** consulter et supprimer les pièces jointes de mes évaluations

**Critères d'acceptation :**
- [ ] Je vois la liste de toutes les pièces jointes d'une évaluation
- [ ] Je peux supprimer une pièce jointe (soft delete)
- [ ] Je ne peux supprimer que mes propres pièces jointes (403 sinon)
- [ ] Après suppression, la pièce jointe disparaît de la vue étudiant

**Endpoints :** `GET /courses/evaluations/{id}/attachments`, `DELETE /courses/evaluations/{id}/attachments/{attachId}`

---

### US-CRS-032 — Consulter les pièces jointes d'une évaluation (côté étudiant)

**En tant que** Étudiant  
**Je veux** accéder aux documents d'une évaluation  
**Afin de** télécharger l'énoncé ou tout document utile

**Critères d'acceptation :**
- [ ] Je dois être inscrit à la matière de l'évaluation (403 sinon)
- [ ] Je vois toutes les pièces jointes disponibles (pas de brouillon — toujours visibles)
- [ ] Je vois : titre, description, nom du fichier, taille, date d'upload
- [ ] Je peux télécharger le fichier
- [ ] Les pièces jointes apparaissent aussi dans mon tableau de bord (US-CRS-015) pour les évaluations à venir

**Endpoint :** `GET /courses/evaluations/{id}/attachments`

---

## EPIC 10 — Données ML et statistiques

### US-CRS-033 — Exposer les données de présence pour le pipeline ML

**En tant que** Scolarité ou Système ML  
**Je veux** accéder aux statistiques de présence agrégées

**Critères d'acceptation :**
- [ ] Filtrable par semestre, filière, niveau, groupe
- [ ] Retourne `attendanceRate` par étudiant par matière
- [ ] Format compatible avec ingestion directe (JSON structuré pour Pandas)

**Endpoint :** `GET /courses/ml/attendance-stats?semesterId=&filiereId=`

---

### US-CRS-034 — Exposer les statistiques de notes pour le pipeline ML

**En tant que** Scolarité ou Système ML

**Critères d'acceptation :**
- [ ] Filtrable par semestre, matière, groupe
- [ ] Retourne : moyenne, écart-type, min, max, taux de réussite
- [ ] Utilisable directement par Isolation Forest et One-class SVM

**Endpoint :** `GET /courses/ml/grade-stats?semesterId=&matiereId=`

---

### US-CRS-035 — Exposer la synthèse de progression pour le pipeline ML

**En tant que** Scolarité ou Système ML

**Critères d'acceptation :**
- [ ] Filtrable par semestre, groupe, filière
- [ ] Retourne : moyenne promo, écart-type, taux de réussite, distribution statuts
- [ ] Données historiques multi-semestres pour séries temporelles LSTM

**Endpoint :** `GET /courses/ml/progress-summary?semesterId=&groupId=`

---

## Matrice des user stories par acteur

| Epic | US | Super Admin | Scolarité | Enseignant | Étudiant | Système |
|---|---|---|---|---|---|---|
| Structure | US-CRS-001 | | ✅ | | | |
| Structure | US-CRS-002 | | ✅ | | | |
| Structure | US-CRS-003 | ✅ | ✅ | | | |
| Structure | US-CRS-004 | | ✅ | | | |
| Groupes | US-CRS-005 | | | | | ✅ |
| Groupes | US-CRS-006 | | ✅ | | | |
| Groupes | US-CRS-007 | | ✅ | | | |
| EDT | US-CRS-008 | | ✅ | | | |
| EDT | US-CRS-009 | ✅ | ✅ | ✅ | ✅ | |
| EDT | US-CRS-010 | | ✅ | ✅ | | |
| Présences | US-CRS-011 | | | ✅ | | |
| Présences | US-CRS-012 | | ✅ | | ✅ | |
| Présences | US-CRS-013 | ✅ | ✅ | ✅ | ✅ | |
| Présences | US-CRS-014 | | | | | ✅ |
| Vue étudiant | US-CRS-015 | | | | ✅ | |
| Vue étudiant | US-CRS-016 | | | | ✅ | |
| Vue étudiant | US-CRS-017 | | | | ✅ | |
| Vue étudiant | US-CRS-018 | ✅ | ✅ | | ✅ | |
| Évaluations | US-CRS-019 | | | ✅ | | |
| Évaluations | US-CRS-020 | | | ✅ | | |
| Évaluations | US-CRS-021 | | | ✅ | | |
| Évaluations | US-CRS-022 | ✅ | ✅ | ✅ | | |
| Progression | US-CRS-023 | | ✅ | | | |
| Progression | US-CRS-024 | ✅ | | | | |
| Progression | US-CRS-025 | | | | | ✅ |
| Supports | US-CRS-026 | | | ✅ | | |
| Supports | US-CRS-027 | | | ✅ | | |
| Supports | US-CRS-028 | | | ✅ | | |
| Supports | US-CRS-029 | | | | ✅ | |
| PJ Évals | US-CRS-030 | | | ✅ | | |
| PJ Évals | US-CRS-031 | | | ✅ | | |
| PJ Évals | US-CRS-032 | | | | ✅ | |
| ML | US-CRS-033 | ✅ | ✅ | | | |
| ML | US-CRS-034 | ✅ | ✅ | | | |
| ML | US-CRS-035 | ✅ | ✅ | | | |

**Total : 35 user stories réparties sur 10 epics**

---

## Matrice d'intégration cross-services

| US | Service | Type | Direction | Données |
|---|---|---|---|---|
| US-CRS-005 | User Service | Kafka async | User → Course | `student.profile.created` |
| US-CRS-007 | User Service | HTTP sync | Course → User | `GET /users/teachers/{id}` |
| US-CRS-011 | Notification | Kafka async | Course → Notif | `attendance.threshold.exceeded` |
| US-CRS-014 | Payment | Kafka async | Payment → Course | `student.payment.blocked` |
| US-CRS-021 | Notification | Kafka async | Course → Notif | `grades.published` |
| US-CRS-024 | User Service | Kafka async | Course → User | `semester.validated` → promotion |
| US-CRS-024 | Payment | Kafka async | Course → Payment | `semester.validated` → bourses |
| US-CRS-024 | Document | Kafka async | Course → Document | `semester.validated` → bulletins |
| US-CRS-024 | Notification | Kafka async | Course → Notif | `semester.validated` → résultats |
| US-CRS-025 | User Service | Kafka async | User → Course | `student.promoted` |
