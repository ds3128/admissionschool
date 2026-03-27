# RH Service — Documentation Technique v1

**Projet :** AdmissionSchool  
**Service :** RH Service  
**Port :** 8085  
**Version :** 1.0.0  
**Dernière mise à jour :** Mars 2026

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Modèle de domaine — Description des entités](#2-modèle-de-domaine--description-des-entités)
3. [Énumérations](#3-énumérations)
4. [Cas d'utilisation](#4-cas-dutilisation)
5. [Règles métier transversales](#5-règles-métier-transversales)
6. [Dépendances cross-services](#6-dépendances-cross-services)
7. [Résumé des endpoints API](#7-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le RH Service gère les ressources humaines de l'établissement : contrats des enseignants et du personnel administratif, congés, absences, évaluations annuelles et paie. Il travaille en étroite collaboration avec le User Service pour les données personnelles et avec le Course Service pour la charge horaire des enseignants.

### Responsabilités

- Gestion des contrats (CDI, CDD, vacataire, stage)
- Gestion des congés et absences du personnel
- Gestion des évaluations annuelles du personnel
- Gestion de la paie (calcul, historique, fiches de paie)
- Suivi de la charge horaire des enseignants vacataires

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Profils personnels | User Service |
| Charge horaire académique | Course Service |
| Génération des documents (contrats, fiches de paie) | Document Service |
| Notifications | Notification Service |

---

## 2. Modèle de domaine — Description des entités

### 2.1 `Employee`

Représente un employé de l'établissement (enseignant ou personnel administratif). Lié au `UserProfile` du User Service.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Référence vers `Users.id` (Auth Service) |
| `profileId` | UUID | Référence vers `UserProfile.id` (User Service) |
| `employeeNumber` | String | Numéro d'employé unique |
| `position` | String | Intitulé du poste |
| `departmentId` | Long | Référence vers `Department.id` (User Service) |
| `hireDate` | LocalDate | Date d'embauche |
| `salary` | BigDecimal | Salaire de base mensuel brut |
| `contractType` | ContractType | Type de contrat courant |
| `isActive` | boolean | Employé actif ou non |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- `employeeNumber` est unique et correspond à celui généré par le User Service.
- Un employé `isActive = false` ne peut pas avoir de nouveau contrat.
- La paie est calculée sur la base du `salary` + primes éventuelles.

---

### 2.2 `Contract`

Représente un contrat de travail lié à un employé.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `type` | ContractType | CDI, CDD, VACATAIRE, STAGE |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin (null si CDI) |
| `salary` | BigDecimal | Salaire contractuel |
| `position` | String | Poste contractuel |
| `status` | ContractStatus | ACTIVE, EXPIRED, TERMINATED, PENDING |
| `signedAt` | LocalDate | Date de signature |
| `documentUrl` | String | URL du contrat signé |

**Règles métier :**
- Un employé ne peut avoir qu'un seul contrat `ACTIVE` à la fois.
- Un contrat `CDD` avec `endDate < today` passe automatiquement à `EXPIRED`.
- Un `CDI` a `endDate = null`.
- La résiliation d'un contrat passe le statut à `TERMINATED`.

---

### 2.3 `Leave`

Représente une demande de congé ou d'absence d'un employé.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `type` | LeaveType | Type de congé |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin |
| `days` | int | Nombre de jours ouvrés |
| `status` | LeaveStatus | PENDING, APPROVED, REJECTED, CANCELLED |
| `reason` | String | Motif (obligatoire pour certains types) |
| `approvedBy` | UUID | ID du responsable ayant approuvé |
| `approvedAt` | LocalDateTime | Date d'approbation |
| `documentUrl` | String | Justificatif (arrêt maladie, etc.) |

**Règles métier :**
- Le `days` est calculé automatiquement (jours ouvrés hors week-ends et jours fériés).
- Un congé `MALADIE` requiert un justificatif médical.
- Un congé ne peut être approuvé que par un responsable hiérarchique ou `ADMIN_RH`.
- Le solde de congés disponible est vérifié avant approbation.

---

### 2.4 `LeaveBalance`

Représente le solde de congés d'un employé pour une année donnée.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `year` | int | Année concernée |
| `totalDays` | int | Jours de congés accordés |
| `usedDays` | int | Jours déjà utilisés |
| `remainingDays` | int | Solde restant |
| `carriedOver` | int | Jours reportés de l'année précédente |

**Règles métier :**
- `remainingDays = totalDays + carriedOver - usedDays`.
- Le report de congés est limité à un maximum configurable (ex : 5 jours).
- `usedDays` est mis à jour à chaque approbation de congé.

---

### 2.5 `Evaluation_RH`

Représente l'évaluation annuelle d'un employé par son responsable.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `evaluatorId` | UUID | ID du responsable évaluateur |
| `period` | String | Période évaluée (ex : 2025) |
| `score` | int | Note globale sur 100 |
| `criteria` | List\<EvalCriteria\> | Critères détaillés |
| `strengths` | String | Points forts |
| `improvements` | String | Axes d'amélioration |
| `goals` | String | Objectifs pour la prochaine période |
| `status` | EvalStatus | DRAFT, SUBMITTED, ACKNOWLEDGED |
| `createdAt` | LocalDateTime | Date de création |
| `acknowledgedAt` | LocalDateTime | Date de prise de connaissance par l'employé |

**Règles métier :**
- Une seule évaluation par employé et par période.
- L'employé doit confirmer avoir pris connaissance de l'évaluation (`ACKNOWLEDGED`).
- Une évaluation `SUBMITTED` ne peut plus être modifiée par l'évaluateur.

---

### 2.6 `EvalCriteria`

Représente un critère d'évaluation individuel dans une évaluation RH.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `evaluationId` | Long | Référence vers `Evaluation_RH.id` |
| `name` | String | Nom du critère (ex : Ponctualité, Pédagogie) |
| `score` | int | Note sur 20 |
| `comment` | String | Commentaire sur ce critère |

---

### 2.7 `Payslip`

Représente une fiche de paie mensuelle.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `month` | int | Mois (1-12) |
| `year` | int | Année |
| `baseSalary` | BigDecimal | Salaire de base brut |
| `bonuses` | BigDecimal | Primes et indemnités |
| `deductions` | BigDecimal | Retenues (cotisations, absences) |
| `netSalary` | BigDecimal | Salaire net à payer |
| `paidAt` | LocalDate | Date de virement |
| `status` | PayslipStatus | DRAFT, VALIDATED, PAID |
| `documentUrl` | String | URL de la fiche de paie générée |

**Règles métier :**
- `netSalary = baseSalary + bonuses - deductions`.
- Une fiche `PAID` est immuable.
- Les absences non justifiées sont déduites du salaire (`deductions`).
- Pour les vacataires : `baseSalary` calculé depuis la charge horaire (`TeacherLoad`).

---

### 2.8 `VacataireHourlyRate`

Représente le taux horaire d'un enseignant vacataire, utilisé pour le calcul de la paie.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `employeeId` | UUID | Référence vers `Employee.id` |
| `ratePerHour` | BigDecimal | Taux horaire brut |
| `sessionType` | SessionType | CM, TD, TP (taux différents) |
| `effectiveFrom` | LocalDate | Date d'entrée en vigueur |

**Règles métier :**
- Un vacataire peut avoir des taux différents selon le type de séance (CM > TD > TP).
- La paie vacataire = Σ (heures par type × taux horaire correspondant).

---

## 3. Énumérations

### `ContractType`
| Valeur | Description |
|---|---|
| `CDI` | Contrat à durée indéterminée |
| `CDD` | Contrat à durée déterminée |
| `VACATAIRE` | Vacataire (rémunéré à l'heure) |
| `STAGE` | Convention de stage |

### `ContractStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente de signature |
| `ACTIVE` | Contrat en cours |
| `EXPIRED` | Contrat expiré (CDD) |
| `TERMINATED` | Résilié |

### `LeaveType`
| Valeur | Description |
|---|---|
| `CONGE_ANNUEL` | Congé annuel |
| `MALADIE` | Arrêt maladie |
| `MATERNITE` | Congé maternité |
| `PATERNITE` | Congé paternité |
| `FORMATION` | Congé formation |
| `SANS_SOLDE` | Congé sans solde |
| `EXCEPTIONNEL` | Congé exceptionnel (décès, mariage...) |

### `LeaveStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente d'approbation |
| `APPROVED` | Approuvé |
| `REJECTED` | Refusé |
| `CANCELLED` | Annulé par l'employé |

### `EvalStatus`
| Valeur | Description |
|---|---|
| `DRAFT` | Brouillon en cours de rédaction |
| `SUBMITTED` | Soumis à l'employé |
| `ACKNOWLEDGED` | Pris en connaissance par l'employé |

### `PayslipStatus`
| Valeur | Description |
|---|---|
| `DRAFT` | En cours de calcul |
| `VALIDATED` | Validé par le service RH |
| `PAID` | Virement effectué |

---

## 4. Cas d'utilisation

### UC-RH-001 — Créer un contrat

**Acteur :** Admin RH (`ADMIN_RH`)  
**Déclencheur :** Requête `POST /rh/contracts`  
**Préconditions :** L'employé existe et n'a pas de contrat `ACTIVE`

**Scénario principal :**
1. L'admin envoie `employeeId`, `type`, `startDate`, `endDate`, `salary`, `position`.
2. Le système vérifie l'absence de contrat `ACTIVE` existant.
3. Le système crée le contrat en statut `PENDING`.
4. Le Document Service génère le contrat PDF.
5. L'employé signe le contrat → statut `ACTIVE`.
6. Notification envoyée à l'employé.

---

### UC-RH-002 — Demander un congé

**Acteur :** Employé (tout rôle sauf `CANDIDATE`)  
**Déclencheur :** Requête `POST /rh/leaves`

**Scénario principal :**
1. L'employé envoie `type`, `startDate`, `endDate`, `reason`.
2. Le système calcule le nombre de jours ouvrés.
3. Le système vérifie le solde de congés disponible.
4. Le système crée la demande en statut `PENDING`.
5. Notification envoyée au responsable pour approbation.

**Scénarios alternatifs :**
- Solde insuffisant → `422 Unprocessable Entity`.
- Chevauchement avec un congé existant → `409 Conflict`.

---

### UC-RH-003 — Approuver ou rejeter un congé

**Acteur :** Admin RH (`ADMIN_RH`) ou responsable hiérarchique  
**Déclencheur :** Requête `PUT /rh/leaves/{id}/approve` ou `/reject`

**Scénario principal (approbation) :**
1. Le responsable approuve la demande.
2. Statut → `APPROVED`.
3. `LeaveBalance.usedDays` incrémenté.
4. Notification envoyée à l'employé.

**Scénario principal (rejet) :**
1. Le responsable rejette avec un motif.
2. Statut → `REJECTED`.
3. Notification envoyée à l'employé avec le motif.

---

### UC-RH-004 — Générer les fiches de paie mensuelles

**Acteur :** Admin RH (`ADMIN_RH`)  
**Déclencheur :** Requête `POST /rh/payslips/generate?month=&year=`

**Scénario principal :**
1. L'admin déclenche la génération pour un mois et une année.
2. Pour chaque employé actif :
   - Récupère le salaire de base depuis le contrat.
   - Calcule les déductions (absences non justifiées × taux journalier).
   - Pour les vacataires : récupère la charge horaire depuis le Course Service et calcule le brut.
   - Crée la `Payslip` en statut `DRAFT`.
3. L'admin valide → statut `VALIDATED`.
4. Virement effectué → statut `PAID`.
5. Document Service génère les PDF des fiches de paie.
6. Notification envoyée à chaque employé.

---

### UC-RH-005 — Créer une évaluation annuelle

**Acteur :** Responsable hiérarchique ou Admin RH  
**Déclencheur :** Requête `POST /rh/evaluations`

**Scénario principal :**
1. Le responsable crée l'évaluation en `DRAFT` avec les critères et notes.
2. Une fois complète → soumission : statut `SUBMITTED`.
3. L'employé reçoit une notification.
4. L'employé prend connaissance → statut `ACKNOWLEDGED`.

---

## 5. Règles métier transversales

### Calcul de la paie vacataire

```
Brut vacataire = Σ (heures_CM × tauxCM) + Σ (heures_TD × tauxTD) + Σ (heures_TP × tauxTP)
Données heures récupérées depuis Course Service (TeacherLoad)
```

### Solde de congés

```
Solde initial = 25 jours / an (configurable)
Report max    = 5 jours de l'année précédente
Décompte      = jours ouvrés (hors week-ends et jours fériés)
```

### Déduction pour absence

```
Taux journalier = salaire mensuel brut / 22 jours ouvrés
Déduction       = jours absents non justifiés × taux journalier
```

---

## 6. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **User Service** | HTTP synchrone | Récupère les données personnelles et professionnelles |
| **Course Service** | HTTP synchrone | Récupère `TeacherLoad` pour la paie des vacataires |
| **Document Service** | HTTP synchrone | Génère contrats et fiches de paie en PDF |
| **Notification Service** | Consommateur Kafka | Notifie les employés |

### Événements publiés (Kafka)

| Événement | Déclencheur | Consommateurs |
|---|---|---|
| `ContractCreated` | Création contrat | Notification Service, Document Service |
| `LeaveApproved` | Approbation congé | Notification Service |
| `LeaveRejected` | Rejet congé | Notification Service |
| `PayslipPaid` | Virement effectué | Notification Service, Document Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `TeacherProfileCreated` | User Service | Création `Employee` |
| `StaffProfileCreated` | User Service | Création `Employee` |
| `TeacherDeactivated` | User Service | Désactivation `Employee` |

---

## 7. Résumé des endpoints API

### Employés

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/rh/employees` | Lister les employés | `ADMIN_RH` |
| `GET` | `/rh/employees/{id}` | Détail d'un employé | `ADMIN_RH` |
| `GET` | `/rh/employees/me` | Mon dossier RH | Authentifié |

### Contrats

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/rh/contracts?employeeId=` | Contrats d'un employé | `ADMIN_RH` |
| `POST` | `/rh/contracts` | Créer un contrat | `ADMIN_RH` |
| `PUT` | `/rh/contracts/{id}/terminate` | Résilier un contrat | `ADMIN_RH` |

### Congés

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/rh/leaves?employeeId=` | Congés d'un employé | `ADMIN_RH` |
| `GET` | `/rh/leaves/me` | Mes demandes de congé | Authentifié |
| `POST` | `/rh/leaves` | Demander un congé | Authentifié |
| `PUT` | `/rh/leaves/{id}/approve` | Approuver | `ADMIN_RH` |
| `PUT` | `/rh/leaves/{id}/reject` | Rejeter | `ADMIN_RH` |
| `DELETE` | `/rh/leaves/{id}` | Annuler (PENDING uniquement) | Authentifié |
| `GET` | `/rh/leaves/balance?year=` | Solde de congés | Authentifié |

### Évaluations

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/rh/evaluations?employeeId=` | Évaluations d'un employé | `ADMIN_RH` |
| `POST` | `/rh/evaluations` | Créer une évaluation | `ADMIN_RH` |
| `PUT` | `/rh/evaluations/{id}/submit` | Soumettre à l'employé | `ADMIN_RH` |
| `PUT` | `/rh/evaluations/{id}/acknowledge` | Prendre connaissance | Authentifié |

### Paie

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/rh/payslips/me` | Mes fiches de paie | Authentifié |
| `POST` | `/rh/payslips/generate` | Générer les fiches du mois | `ADMIN_RH` |
| `PUT` | `/rh/payslips/{id}/validate` | Valider | `ADMIN_RH` |
| `PUT` | `/rh/payslips/{id}/pay` | Marquer comme payé | `ADMIN_RH` |
