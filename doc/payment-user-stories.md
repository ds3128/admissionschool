# Payment Service - User Stories complètes

**Projet :** AdmissionSchool  
**Service :** Payment Service  
**Version :** 1.0.0  
**Date :** Mars 2026

---

## Acteurs

| Acteur | Rôle système | Description |
|---|---|---|
| **Candidat** | `CANDIDATE` | Utilisateur non encore inscrit - paie ses frais de dossier |
| **Étudiant** | `STUDENT` | Utilisateur inscrit - paie ses frais de scolarité |
| **Admin Finance** | `ADMIN_FINANCE` | Gestionnaire financier - gère factures et bourses |
| **Super Admin** | `SUPER_ADMIN` | Accès total aux statistiques |
| **Système** | `system` | Jobs schedulés - actions automatiques |
| **Passerelle** | externe | Confirmation des paiements via webhook |

---

## EPIC 1 - Paiement des frais de dossier

---

### US-PAY-001 - Initier le paiement de mes frais de dossier

**En tant que** Candidat  
**Je veux** payer mes frais de dossier d'admission  
**Afin de** pouvoir soumettre ma candidature

**Critères d'acceptation :**
- [ ] Je peux initier le paiement depuis mon espace candidat
- [ ] Je dois fournir : `applicationId`, `amount`, `currency`, `method`
- [ ] Le système vérifie qu'un `AdmissionPayment PENDING` existe dans l'Admission Service pour cet `applicationId`
- [ ] Le système rejette si un paiement `COMPLETED` existe déjà pour cet `applicationId` (409 Conflict)
- [ ] Un `Payment` est créé en `PENDING` avec une référence `PAY-{year}-{seq}`
- [ ] La référence générée est unique et au format `PAY-2026-XXXXX`
- [ ] Je reçois en réponse : `{ paymentId, reference, status, amount }`

**Endpoint :** `POST /payments/admission-fees`  
**Intégration :** Admission Service - `AdmissionPayment` doit exister

---

### US-PAY-002 - Confirmation automatique après paiement

**En tant que** Système (passerelle externe)  
**Je veux** confirmer un paiement via webhook  
**Afin de** finaliser la transaction et débloquer la soumission du dossier

**Critères d'acceptation :**
- [ ] La passerelle appelle `POST /payments/webhook` avec `externalReference`
- [ ] Le système retrouve le `Payment` via `externalReference`
- [ ] Le `Payment` passe en `COMPLETED` avec `paidAt = now`
- [ ] L'événement `PaymentCompleted` est publié sur le topic `payment.completed` avec :
  - `paymentReference`, `applicationId`, `userId`, `amount`, `currency`, `type`, `paidAt`
- [ ] L'Admission Service consomme cet événement et passe l'`Application` en `PAID`
- [ ] En cas d'échec : `Payment → FAILED`, `PaymentFailed` publié

**Endpoint :** `POST /payments/webhook`  
**Intégration critique :** Admission Service consomme `payment.completed`

---

### US-PAY-003 - Simuler une confirmation (développement)

**En tant que** Admin Finance (environnement dev)  
**Je veux** simuler manuellement la confirmation d'un paiement  
**Afin de** tester le flux complet sans passerelle réelle

**Critères d'acceptation :**
- [ ] Je peux confirmer un paiement `PENDING` via `POST /payments/{id}/simulate-confirm`
- [ ] Le comportement est identique à une vraie confirmation (mêmes events Kafka publiés)
- [ ] Cet endpoint est **désactivé en production** (`@Profile("!prod")`)

**Endpoint :** `POST /payments/{id}/simulate-confirm`

---

### US-PAY-004 - Relancer un paiement échoué

**En tant que** Candidat  
**Je veux** pouvoir relancer un paiement après un échec  
**Afin de** ne pas être bloqué par une erreur de transaction

**Critères d'acceptation :**
- [ ] Si mon paiement est en `FAILED`, je peux initier un nouveau paiement
- [ ] Un nouveau `Payment` est créé en `PENDING` avec une nouvelle référence
- [ ] L'ancien paiement `FAILED` reste dans l'historique
- [ ] Je ne peux pas relancer si un paiement `COMPLETED` existe déjà (409)

---

### US-PAY-005 - Consulter l'historique de mes paiements

**En tant que** Candidat ou Étudiant  
**Je veux** consulter l'historique de mes paiements  
**Afin de** avoir une traçabilité de mes transactions

**Critères d'acceptation :**
- [ ] Je vois tous mes paiements triés par date décroissante
- [ ] Je vois pour chaque paiement : référence, montant, statut, méthode, date
- [ ] Je ne vois que mes propres paiements (filtré par `userId`)
- [ ] Un admin finance peut voir les paiements de n'importe quel utilisateur

**Endpoint :** `GET /payments/me`, `GET /payments/history?userId=`

---

## EPIC 2 - Facturation des frais de scolarité

---

### US-PAY-006 - Générer les factures de scolarité pour une cohorte

**En tant que** Admin Finance  
**Je veux** générer les factures de scolarité pour tous les étudiants actifs  
**Afin de** leur permettre de s'acquitter de leurs frais

**Critères d'acceptation :**
- [ ] Je peux déclencher la génération pour une `academicYear` + `semester` + `dueDate`
- [ ] Le système récupère tous les étudiants actifs via `GET /users/students?status=ACTIVE`
- [ ] Pour chaque étudiant : une facture est créée avec le montant de sa filière
- [ ] Si l'étudiant a une bourse `ACTIVE` : la déduction est calculée automatiquement
  - `scholarshipDeduction = scholarship.amount / nbSemestres`
  - `netAmount = amount - scholarshipDeduction`
- [ ] Aucune facture en doublon n'est créée si `(studentId, year, semester)` existe déjà (422)
- [ ] L'événement `InvoiceGenerated` est publié pour chaque étudiant (notification par email)
- [ ] Le système retourne : `{ generated: N, skipped: M }`
- [ ] Si le User Service est indisponible → 503 avec message explicite

**Endpoint :** `POST /payments/invoices/generate`  
**Intégration :** User Service (HTTP synchrone)

---

### US-PAY-007 - Consulter mes factures

**En tant que** Étudiant  
**Je veux** voir mes factures de scolarité  
**Afin de** connaître les montants à payer et les délais

**Critères d'acceptation :**
- [ ] Je vois toutes mes factures triées par date d'échéance
- [ ] Je vois pour chaque facture : montant brut, déduction bourse, montant net, montant payé, solde restant, statut, date limite
- [ ] Je vois si un échéancier est actif sur ma facture
- [ ] Je ne vois que mes propres factures

**Endpoint :** `GET /payments/invoices/me`

---

### US-PAY-008 - Payer ma facture de scolarité

**En tant que** Étudiant  
**Je veux** payer tout ou partie de ma facture de scolarité  
**Afin de** régulariser ma situation administrative

**Critères d'acceptation :**
- [ ] Je peux payer le montant intégral ou un montant partiel
- [ ] Le montant payé ne peut pas dépasser `remainingAmount` (422)
- [ ] La facture doit m'appartenir (403 sinon)
- [ ] La facture ne doit pas être `PAID` ou `CANCELLED` (422)
- [ ] Un `Payment` est créé et confirmé
- [ ] `Invoice.paidAmount` est mis à jour après confirmation
- [ ] Si `paidAmount >= netAmount` → `Invoice → PAID`, `InvoicePaidEvent` publié
- [ ] Si paiement partiel → `Invoice → PARTIAL`
- [ ] Si facture avec échéancier → l'`Installment` correspondant est marqué `PAID`

**Endpoint :** `POST /payments/invoices/{id}/pay`

---

### US-PAY-009 - Créer un échéancier de paiement pour un étudiant

**En tant que** Admin Finance  
**Je veux** créer un échéancier de paiement pour fractionner une facture  
**Afin de** aider les étudiants en difficulté financière

**Critères d'acceptation :**
- [ ] Je peux définir la liste des échéances avec `{ amount, dueDate }` pour chacune
- [ ] La somme des échéances doit être égale à `Invoice.netAmount` (422 sinon)
- [ ] Une facture ne peut avoir qu'un seul échéancier actif (422 si doublon)
- [ ] La facture ne doit pas être `PAID` ou `CANCELLED`
- [ ] Après création : `Invoice.hasSchedule = true`
- [ ] L'étudiant est notifié (event Kafka)

**Endpoint :** `POST /payments/invoices/{id}/schedule`

---

### US-PAY-010 - Consulter l'échéancier d'une facture

**En tant que** Étudiant ou Admin Finance  
**Je veux** voir les détails de l'échéancier de ma facture  
**Afin de** connaître les dates et montants de chaque versement

**Critères d'acceptation :**
- [ ] Je vois toutes les échéances avec leur numéro, montant, date limite et statut
- [ ] Je vois les échéances déjà payées avec leur date de paiement effective
- [ ] Je vois les échéances en retard (`OVERDUE`)

**Endpoint :** `GET /payments/invoices/{id}/schedule`

---

### US-PAY-011 - Annuler une facture

**En tant que** Admin Finance  
**Je veux** annuler une facture  
**Afin de** corriger une erreur de génération

**Critères d'acceptation :**
- [ ] Je ne peux annuler qu'une facture `PENDING` ou `PARTIAL` (422 si `PAID`)
- [ ] La facture passe en `CANCELLED`
- [ ] L'étudiant est notifié de l'annulation

**Endpoint :** `PUT /payments/invoices/{id}/cancel`

---

### US-PAY-012 - Détection automatique des factures en retard

**En tant que** Système  
**Je veux** détecter automatiquement les factures dont la date limite est dépassée  
**Afin de** alerter les étudiants et l'administration

**Critères d'acceptation :**
- [ ] Un scheduler s'exécute chaque jour à 02:00
- [ ] Toutes les factures `PENDING` ou `PARTIAL` dont `dueDate < today` passent en `OVERDUE`
- [ ] `InvoiceOverdueEvent` est publié pour chaque facture → Notification Service alerte l'étudiant
- [ ] Les `Installment` expirés passent également en `OVERDUE`

---

### US-PAY-013 - Blocage automatique des étudiants avec impayé critique

**En tant que** Système  
**Je veux** bloquer l'accès aux services d'un étudiant avec un impayé critique  
**Afin de** inciter à la régularisation de la situation

**Critères d'acceptation :**
- [ ] Une facture `OVERDUE` depuis plus de 30 jours déclenche un blocage
- [ ] `StudentPaymentBlockedEvent` est publié avec `{ studentId, userId, invoiceId, amount, overdueDays }`
- [ ] User Service consomme l'event → restreint l'accès de l'étudiant aux services non-essentiels
- [ ] Course Service consomme l'event → bloque les nouvelles inscriptions aux cours
- [ ] Notification répétée tous les 7 jours jusqu'à régularisation
- [ ] Le déblocage se fait automatiquement quand la facture est payée

---

### US-PAY-014 - Envoyer des rappels de paiement

**En tant que** Système  
**Je veux** envoyer des rappels automatiques aux étudiants dont la facture arrive à échéance  
**Afin de** prévenir les retards de paiement

**Critères d'acceptation :**
- [ ] Un scheduler s'exécute chaque lundi à 09:00
- [ ] Les étudiants dont la facture est due dans 7 jours ou moins reçoivent un rappel
- [ ] Le rappel mentionne le montant restant et la date limite

---

## EPIC 3 - Bourses étudiantes

---

### US-PAY-015 - Attribuer une bourse à un étudiant

**En tant que** Admin Finance  
**Je veux** attribuer une bourse à un étudiant  
**Afin de** lui fournir une aide financière pour ses études

**Critères d'acceptation :**
- [ ] Je peux créer une bourse avec : `studentId`, `type`, `amount`, `academicYear`, `disbursementFrequency`, `conditions`
- [ ] La bourse est créée en `PENDING`
- [ ] Je ne peux pas créer deux bourses du même type pour le même étudiant et la même année (409)
- [ ] Pour une bourse `MERITE` : je dois définir `minimumGrade` (défaut 14.0)

**Endpoint :** `POST /payments/scholarships`

---

### US-PAY-016 - Activer une bourse

**En tant que** Admin Finance  
**Je veux** activer une bourse  
**Afin de** déclencher les versements automatiques

**Critères d'acceptation :**
- [ ] La bourse doit être en `PENDING` (422 sinon)
- [ ] `Scholarship → ACTIVE`
- [ ] Le système génère automatiquement les `ScholarshipDisbursement` selon la fréquence :
  - `MONTHLY` → 12 versements
  - `QUARTERLY` → 4 versements
  - `SEMESTER` → 2 versements
- [ ] `monthlyAmount` est calculé : `amount / nbVersements`
- [ ] `ScholarshipActivatedEvent` est publié → Notification Service notifie l'étudiant
- [ ] La déduction apparaît automatiquement sur les prochaines factures générées

**Endpoint :** `PUT /payments/scholarships/{id}/activate`

---

### US-PAY-017 - Consulter ma bourse

**En tant que** Étudiant  
**Je veux** consulter les détails de ma bourse  
**Afin de** connaître le montant, la fréquence et les conditions de maintien

**Critères d'acceptation :**
- [ ] Je vois le type, montant annuel, montant par versement, fréquence, statut, date de début/fin
- [ ] Je vois les conditions de maintien
- [ ] Je vois l'historique de mes versements reçus

**Endpoint :** `GET /payments/scholarships/me`

---

### US-PAY-018 - Versements automatiques des bourses

**En tant que** Système  
**Je veux** effectuer automatiquement les versements de bourse à chaque période  
**Afin de** assurer la régularité des aides financières

**Critères d'acceptation :**
- [ ] Un scheduler s'exécute le 1er de chaque mois à 08:00
- [ ] Les `ScholarshipDisbursement SCHEDULED` dont `scheduledDate <= today` sont traités
- [ ] Pour chaque versement :
  - La bourse doit être `ACTIVE` (versement `CANCELLED` sinon)
  - Un `Payment` de type `BOURSE` est créé et confirmé
  - `ScholarshipDisbursement → PAID`
  - `ScholarshipDisbursedEvent` publié
- [ ] En cas d'échec : `ScholarshipDisbursement → FAILED`, alerte admin

---

### US-PAY-019 - Suspendre une bourse

**En tant que** Admin Finance  
**Je veux** suspendre une bourse temporairement  
**Afin de** arrêter les versements en cas de manquement aux conditions

**Critères d'acceptation :**
- [ ] La bourse doit être `ACTIVE` (422 sinon)
- [ ] Je dois fournir un motif de suspension
- [ ] `Scholarship → SUSPENDED`
- [ ] Les `ScholarshipDisbursement` futurs passent en `CANCELLED`
- [ ] `ScholarshipSuspendedEvent` publié → Notification Service notifie l'étudiant
- [ ] La bourse peut être réactivée par l'admin

**Endpoint :** `PUT /payments/scholarships/{id}/suspend`

---

### US-PAY-020 - Terminer définitivement une bourse

**En tant que** Admin Finance  
**Je veux** clôturer définitivement une bourse  
**Afin de** mettre fin à une aide financière de façon irréversible

**Critères d'acceptation :**
- [ ] `Scholarship → TERMINATED`
- [ ] Tous les `ScholarshipDisbursement` futurs passent en `CANCELLED`
- [ ] La bourse ne peut plus être réactivée
- [ ] L'étudiant est notifié

**Endpoint :** `PUT /payments/scholarships/{id}/terminate`

---

### US-PAY-021 - Renouvellement automatique des bourses mérite

**En tant que** Système  
**Je veux** renouveler automatiquement les bourses mérite selon les résultats académiques  
**Afin de** récompenser les étudiants méritants et retirer l'aide à ceux qui ne remplissent plus les conditions

**Critères d'acceptation :**
- [ ] Un scheduler s'exécute le 1er juillet à 06:00
- [ ] Pour chaque bourse `MERITE` `ACTIVE` :
  - Le système interroge le Course Service : `GET /courses/students/{id}/progress?year={year}`
  - Si `annualAverage >= minimumGrade` → nouvelle bourse créée pour l'année suivante + notification
  - Si `annualAverage < minimumGrade` → `Scholarship → SUSPENDED` + notification étudiant + alerte admin
- [ ] `ScholarshipRenewalProcessedEvent` publié avec les résultats globaux

**Intégration :** Course Service (HTTP synchrone)

---

### US-PAY-022 - Traitement du renouvellement depuis Kafka

**En tant que** Système  
**Je veux** déclencher la vérification des bourses mérite quand les notes sont validées  
**Afin de** réagir en temps réel aux résultats académiques

**Critères d'acceptation :**
- [ ] Le Payment Service consomme l'événement `semester.validated` du Course Service
- [ ] Pour les étudiants concernés, la vérification des conditions de bourse est déclenchée
- [ ] Si le semestre validé est le dernier de l'année → renouvellement traité immédiatement

**Intégration :** Course Service publie `semester.validated`

---

## EPIC 4 - Remboursements

---

### US-PAY-023 - Rembourser un paiement

**En tant que** Admin Finance  
**Je veux** rembourser un paiement  
**Afin de** corriger une erreur ou traiter une annulation d'inscription

**Critères d'acceptation :**
- [ ] Le paiement doit être `COMPLETED` (422 sinon)
- [ ] Je dois fournir un motif de remboursement
- [ ] Un nouveau `Payment` de type `REMBOURSEMENT` est créé avec montant négatif
- [ ] Le paiement original passe en `REFUNDED`
- [ ] Si lié à une `Invoice` → `Invoice.paidAmount` est ajusté, `Invoice → PARTIAL` ou `PENDING`
- [ ] `PaymentRefundedEvent` publié → Notification Service notifie l'étudiant
- [ ] Document Service génère un justificatif de remboursement

**Endpoint :** `POST /payments/{id}/refund`

---

## EPIC 5 - Administration et reporting

---

### US-PAY-024 - Consulter les statistiques financières

**En tant que** Admin Finance ou Super Admin  
**Je veux** voir un tableau de bord financier  
**Afin de** suivre la santé financière de l'établissement

**Critères d'acceptation :**
- [ ] Je vois le total des paiements confirmés (par type, par période)
- [ ] Je vois le nombre de factures par statut (PENDING, PARTIAL, PAID, OVERDUE)
- [ ] Je vois le nombre d'étudiants bloqués pour impayé
- [ ] Je vois le montant total des bourses actives
- [ ] Je peux filtrer par année académique et semestre

**Endpoint :** `GET /payments/admin/stats`

---

### US-PAY-025 - Consulter les factures en retard

**En tant que** Admin Finance  
**Je veux** voir la liste des factures en retard de paiement  
**Afin de** contacter les étudiants concernés et prendre les mesures nécessaires

**Critères d'acceptation :**
- [ ] Les factures sont triées par nombre de jours de retard décroissant
- [ ] Je vois : nom étudiant, montant restant, date limite, jours de retard, statut blocage
- [ ] Je peux filtrer par seuil de jours de retard
- [ ] Les résultats sont paginés

**Endpoint :** `GET /payments/admin/overdue`

---

### US-PAY-026 - Consulter les étudiants bloqués pour impayé

**En tant que** Admin Finance  
**Je veux** voir la liste des étudiants bloqués pour impayé critique  
**Afin de** gérer les cas individuellement et débloquer les situations

**Critères d'acceptation :**
- [ ] Je vois les étudiants dont le `StudentPaymentBlockedEvent` a été publié
- [ ] Je vois depuis combien de jours ils sont bloqués
- [ ] Je vois le montant de la dette
- [ ] Quand un étudiant paie sa facture, il est automatiquement débloqué

**Endpoint :** `GET /payments/admin/blocked-students`

---

## Matrice des user stories par acteur

| Epic | US | Candidat | Étudiant | Admin Finance | Super Admin | Système |
|---|---|---|---|---|---|---|
| Frais dossier | US-PAY-001 | ✅ | | | | |
| Frais dossier | US-PAY-002 | | | | | ✅ (passerelle) |
| Frais dossier | US-PAY-003 | | | ✅ (dev) | | |
| Frais dossier | US-PAY-004 | ✅ | | | | |
| Frais dossier | US-PAY-005 | ✅ | ✅ | ✅ | | |
| Scolarité | US-PAY-006 | | | ✅ | | |
| Scolarité | US-PAY-007 | | ✅ | | | |
| Scolarité | US-PAY-008 | | ✅ | | | |
| Scolarité | US-PAY-009 | | | ✅ | | |
| Scolarité | US-PAY-010 | | ✅ | ✅ | | |
| Scolarité | US-PAY-011 | | | ✅ | | |
| Scolarité | US-PAY-012 | | | | | ✅ |
| Scolarité | US-PAY-013 | | | | | ✅ |
| Scolarité | US-PAY-014 | | | | | ✅ |
| Bourses | US-PAY-015 | | | ✅ | | |
| Bourses | US-PAY-016 | | | ✅ | | |
| Bourses | US-PAY-017 | | ✅ | | | |
| Bourses | US-PAY-018 | | | | | ✅ |
| Bourses | US-PAY-019 | | | ✅ | | |
| Bourses | US-PAY-020 | | | ✅ | | |
| Bourses | US-PAY-021 | | | | | ✅ |
| Bourses | US-PAY-022 | | | | | ✅ |
| Remboursements | US-PAY-023 | | | ✅ | | |
| Admin | US-PAY-024 | | | ✅ | ✅ | |
| Admin | US-PAY-025 | | | ✅ | | |
| Admin | US-PAY-026 | | | ✅ | | |

**Total : 26 user stories réparties sur 5 epics**

---

## Matrice d'intégration cross-services

| US | Service appelé | Type | Direction | Données échangées |
|---|---|---|---|---|
| US-PAY-002 | Admission Service | Kafka async | Payment → Admission | `payment.completed` : `paymentReference`, `applicationId`, `amount` |
| US-PAY-006 | User Service | HTTP sync | Payment → User | `GET /users/students?status=ACTIVE` |
| US-PAY-012 | Notification Service | Kafka async | Payment → Notification | `invoice.overdue` |
| US-PAY-013 | User Service | Kafka async | Payment → User | `student.payment.blocked` |
| US-PAY-013 | Course Service | Kafka async | Payment → Course | `student.payment.blocked` |
| US-PAY-021 | Course Service | HTTP sync | Payment → Course | `GET /courses/students/{id}/progress` |
| US-PAY-022 | Course Service | Kafka async | Course → Payment | `semester.validated` |
| US-PAY-023 | Document Service | Kafka async | Payment → Document | `payment.refunded` |
