# Payment Service — Documentation Technique v2

**Projet :** AdmissionSchool  
**Service :** Payment Service  
**Port :** 8086  
**Version :** 2.0.0  
**Dernière mise à jour :** Mars 2026  
**Stack :** Spring Boot 4.0.3, Java 21, Spring Cloud 2025.1.0, PostgreSQL, Kafka, MapStruct 1.6.3, SpringDoc 2.8.6

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture technique](#2-architecture-technique)
3. [Contexte d'intégration](#3-contexte-dintégration)
4. [Flux de paiement des frais de dossier](#4-flux-de-paiement-des-frais-de-dossier)
5. [Flux de facturation des frais de scolarité](#5-flux-de-facturation-des-frais-de-scolarité)
6. [Flux des bourses étudiantes](#6-flux-des-bourses-étudiantes)
7. [Modèle de domaine — Entités](#7-modèle-de-domaine--entités)
8. [Énumérations](#8-énumérations)
9. [Cas d'utilisation](#9-cas-dutilisation)
10. [Règles métier transversales](#10-règles-métier-transversales)
11. [Sécurité des routes](#11-sécurité-des-routes)
12. [Événements Kafka](#12-événements-kafka)
13. [Dépendances cross-services](#13-dépendances-cross-services)
14. [Schedulers](#14-schedulers)
15. [Endpoints API complets](#15-endpoints-api-complets)
16. [Configuration](#16-configuration)

---

## 1. Vue d'ensemble

Le Payment Service gère tous les flux financiers liés à la vie universitaire : frais de dossier d'admission, frais de scolarité, bourses et remboursements. Il est le pivot financier du système — aucun étudiant ne peut s'inscrire sans paiement validé, aucune bourse ne peut être versée sans son orchestration.

### Responsabilités

- Traitement des paiements (frais de dossier, frais de scolarité, inscription)
- Gestion des factures et des échéanciers de paiement
- Gestion des bourses étudiantes (attribution, versements, renouvellement)
- Suivi de l'historique complet des transactions
- Détection et signalement des impayés
- Publication des événements financiers vers les autres services

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Intégration passerelle de paiement externe | Infrastructure (Stripe, PayDunya, etc.) |
| Paie du personnel enseignant et administratif | RH Service |
| Génération des reçus et factures PDF | Document Service |
| Envoi des notifications aux étudiants | Notification Service |
| Création du profil étudiant | User Service |
| Gestion des inscriptions aux cours | Course Service |

---

## 2. Architecture technique

### Structure du projet

```
payment-service/
├── config/
│   ├── SwaggerConfig.java
│   ├── JacksonConfig.java          ← ObjectMapper avec JavaTimeModule
│   └── SchedulerConfig.java        ← jobs @Scheduled
├── controllers/
│   ├── PaymentController.java
│   ├── InvoiceController.java
│   ├── ScholarshipController.java
│   └── AdminFinanceController.java
├── dtos/
│   ├── requests/
│   └── responses/
├── entities/
│   ├── Payment.java
│   ├── Invoice.java
│   ├── PaymentSchedule.java
│   ├── Installment.java
│   ├── Scholarship.java
│   └── ScholarshipDisbursement.java
├── enums/
├── events/
│   ├── consumed/
│   └── published/
├── exceptions/
│   └── GlobalExceptionHandler.java
├── kafka/
│   ├── KafkaConfig.java
│   ├── PaymentEventProducer.java
│   └── PaymentEventConsumer.java
├── mapper/
│   └── PaymentMapper.java
├── repositories/
├── security/
│   ├── PaymentSecurityFilter.java
│   └── SecurityConfig.java
└── services/
    └── impl/
```

### Flux de sécurité

```
Client
  ↓ JWT Bearer token
API Gateway :8888
  ↓ Valide JWT, extrait email/role/userId
  ↓ Injecte X-User-Email, X-User-Role, X-User-Id
Payment Service :8086
  ↓ PaymentSecurityFilter lit les headers
  ↓ Peuple SecurityContext avec ROLE_{role}
  ↓ Controllers lisent @RequestHeader("X-User-Id")
```

---

## 3. Contexte d'intégration

### Position dans l'architecture globale

```
Candidat
  ↓ POST /admissions/applications/{id}/payment
Admission Service
  → Crée AdmissionPayment PENDING
  → Retourne { paymentId, amount, currency }
  ↓ (candidat initie le paiement externe)
Payment Service
  → Crée Payment PENDING
  → Appelle passerelle externe (Stripe/PayDunya)
  → Confirme → publie PaymentCompleted
  ↓ Kafka payment.completed
Admission Service
  → AdmissionPayment → COMPLETED
  → Application → PAID
  → Candidat peut soumettre son dossier
```

### Dépendance critique avec l'Admission Service

L'Admission Service **attend impérativement** le topic `payment.completed` avec cette structure :

```json
{
  "paymentReference": "PAY-2026-00042",
  "applicationId": "uuid-de-l-application",
  "amount": 50.00,
  "currency": "EUR"
}
```

Le champ `applicationId` est la clé de corrélation — l'Admission Service l'utilise pour retrouver l'`AdmissionPayment` et le passer en `COMPLETED`.

---

## 4. Flux de paiement des frais de dossier

```
[Candidat]
  1. POST /payments/admission-fees
     { applicationId, amount, currency, method }
  
  2. Payment Service crée Payment PENDING
     → Génère paymentReference = PAY-{year}-{seq}
  
  3. Payment Service appelle passerelle externe
     (Stripe, PayDunya, etc.)
  
  4a. Paiement confirmé par la passerelle :
       → Payment → COMPLETED
       → PaymentCompletedEvent publié sur Kafka
         { paymentReference, applicationId, amount, currency }
       → Admission Service consomme
         → AdmissionPayment → COMPLETED
         → Application → PAID
  
  4b. Paiement échoué :
       → Payment → FAILED
       → PaymentFailedEvent publié sur Kafka
       → Le candidat peut relancer un nouveau paiement
```

**Note sur la passerelle externe :**  
Dans la version actuelle (développement), la confirmation est simulée via un endpoint `POST /payments/{id}/simulate-confirm`. En production, la passerelle appellera un webhook `POST /payments/webhook`.

---

## 5. Flux de facturation des frais de scolarité

```
[Fin de campagne d'admission / Début de semestre]
  1. Admin Finance déclenche la génération
     POST /payments/invoices/generate
     { academicYear, semester }
  
  2. Payment Service interroge User Service
     GET /users/students?status=ACTIVE (appel HTTP)
  
  3. Pour chaque étudiant actif :
     a. Récupère le tarif selon la filière
     b. Vérifie si une bourse ACTIVE existe
        → Si oui : déduit le montant de la bourse
     c. Crée Invoice (PENDING ou PARTIAL si bourse)
  
  4. Publie InvoiceGeneratedEvent pour chaque étudiant
     → Notification Service notifie l'étudiant par email

[Étudiant]
  5. Consulte ses factures : GET /payments/invoices/me
  6. Paie sa facture : POST /payments/invoices/{id}/pay
     { amount, method, ... }
  7. Payment créé → confirmé → Invoice.paidAmount mis à jour
     → Si paidAmount >= amount → Invoice PAID → InvoicePaidEvent
     → Si paiement partiel → Invoice PARTIAL

[Scheduler quotidien]
  8. Vérifie les factures dont dueDate est dépassée
     → Invoice → OVERDUE
     → InvoiceOverdueEvent publié
     → Notification Service alerte l'étudiant
     → Après 30j impayé : StudentPaymentBlockedEvent
       → User Service : restreint l'accès
       → Course Service : Enrollment BLOCKED
```

---

## 6. Flux des bourses étudiantes

```
[Admin Finance]
  1. Attribue une bourse : POST /payments/scholarships
     { studentId, type, amount, academicYear, conditions }
  
  2. Scholarship créée en PENDING
  
  3. Active la bourse : PUT /payments/scholarships/{id}/activate
     → Scholarship → ACTIVE
     → Génère les ScholarshipDisbursement pour chaque période
     → Publication ScholarshipActivatedEvent

[Scheduler mensuel]
  4. Vérifie les ScholarshipDisbursement SCHEDULED pour cette période
     → Traite le versement
     → ScholarshipDisbursement → PAID
     → ScholarshipDisbursedEvent publié
     → Notification Service notifie l'étudiant

[Fin d'année académique — Bourse mérite]
  5. Scheduler annuel vérifie le renouvellement
     → Interroge Course Service pour la moyenne annuelle
     → Si moyenne >= 14.0 → bourse renouvelée pour l'année suivante
     → Si moyenne < 14.0 → bourse SUSPENDED + notification
```

---

## 7. Modèle de domaine — Entités

### `Payment`

Transaction de paiement individuelle. Représente un mouvement financier unique.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID PK | Clé primaire |
| `userId` | String | Payeur — référence Auth Service |
| `applicationId` | String | Référence Admission Service (nullable — uniquement pour FRAIS_DOSSIER) |
| `invoiceId` | UUID | Référence Invoice (nullable — pour FRAIS_SCOLARITE) |
| `amount` | BigDecimal | Montant payé |
| `currency` | String | Devise (EUR, XAF, USD) |
| `type` | PaymentType | FRAIS_DOSSIER, FRAIS_SCOLARITE, INSCRIPTION, BOURSE, REMBOURSEMENT, AUTRE |
| `status` | PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED |
| `method` | PaymentMethod | CARTE, VIREMENT, MOBILE_MONEY, ESPECES |
| `reference` | String UNIQUE | Référence interne `PAY-{year}-{seq}` |
| `externalReference` | String | Référence retournée par la passerelle externe |
| `description` | String | Libellé du paiement |
| `failureReason` | String | Motif en cas d'échec |
| `paidAt` | LocalDateTime | Date de confirmation |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Un paiement `COMPLETED` est immuable — seul un remboursement crée un nouveau paiement `REMBOURSEMENT`
- La `reference` est générée en interne au format `PAY-{year}-{séquence 5 chiffres}`
- Un paiement `FAILED` peut être retesté — un nouveau `Payment` est créé
- `applicationId` est renseigné uniquement pour les paiements de type `FRAIS_DOSSIER`

---

### `Invoice`

Facture émise à un étudiant pour les frais de scolarité ou d'inscription.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID PK | Clé primaire |
| `studentId` | String | Étudiant — référence User Service |
| `academicYear` | String | Année académique (ex : 2026-2027) |
| `semester` | String | Semestre (S1, S2, ANNUEL) |
| `type` | InvoiceType | SCOLARITE, INSCRIPTION, AUTRE |
| `amount` | BigDecimal | Montant total brut |
| `scholarshipDeduction` | BigDecimal | Déduction bourse (0 si aucune) |
| `netAmount` | BigDecimal | Montant net à payer (amount - scholarshipDeduction) |
| `paidAmount` | BigDecimal | Montant déjà réglé |
| `remainingAmount` | BigDecimal | Solde restant (netAmount - paidAmount) |
| `dueDate` | LocalDate | Date limite de paiement |
| `status` | InvoiceStatus | PENDING, PARTIAL, PAID, OVERDUE, CANCELLED |
| `hasSchedule` | boolean | Échéancier actif |
| `createdAt` | LocalDateTime | Date de création |
| `updatedAt` | LocalDateTime | Dernière modification |

**Règles métier :**
- `netAmount = amount - scholarshipDeduction`
- `remainingAmount = netAmount - paidAmount`
- `status = PAID` quand `paidAmount >= netAmount`
- `status = PARTIAL` quand `paidAmount > 0 && paidAmount < netAmount`
- `status = OVERDUE` quand `dueDate < today && status != PAID`
- Une facture `PAID` ne peut pas être annulée
- Unicité sur `(studentId, academicYear, semester, type)`

---

### `PaymentSchedule`

Échéancier de paiement permettant de fractionner une facture.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `invoiceId` | UUID FK | Facture fractionnée |
| `totalInstallments` | int | Nombre total d'échéances |
| `paidInstallments` | int | Nombre d'échéances payées |
| `totalAmount` | BigDecimal | Montant total de l'échéancier |
| `createdBy` | String | Admin qui a créé l'échéancier |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Un seul échéancier actif par facture
- La somme des `Installment.amount` = `Invoice.netAmount`
- Quand `paidInstallments = totalInstallments` → facture PAID

---

### `Installment`

Échéance individuelle dans un plan de paiement.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `schedule_id` | Long FK | Échéancier parent |
| `installmentNumber` | int | Numéro de l'échéance (1, 2, 3...) |
| `amount` | BigDecimal | Montant de cette échéance |
| `dueDate` | LocalDate | Date limite de paiement |
| `status` | InstallmentStatus | PENDING, PAID, OVERDUE |
| `paidAt` | LocalDateTime | Date de paiement effectif |
| `paymentId` | UUID | Référence Payment associé |

---

### `Scholarship`

Bourse accordée à un étudiant pour une année académique.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `studentId` | String | Bénéficiaire — référence User Service |
| `academicYear` | String | Année académique |
| `amount` | BigDecimal | Montant annuel de la bourse |
| `monthlyAmount` | BigDecimal | Montant par versement |
| `disbursementFrequency` | DisbursementFrequency | MONTHLY, QUARTERLY, SEMESTER |
| `type` | ScholarshipType | MERITE, SOCIALE, EXCELLENCE, SPORTIVE |
| `status` | ScholarshipStatus | PENDING, ACTIVE, SUSPENDED, TERMINATED |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin |
| `conditions` | String | Conditions de maintien |
| `minimumGrade` | BigDecimal | Moyenne minimale requise (nullable) |
| `suspensionReason` | String | Motif de suspension |
| `createdBy` | String | Admin qui a attribué la bourse |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Une seule bourse active par étudiant par année académique et par type
- `monthlyAmount = amount / 12` (MONTHLY), `amount / 4` (QUARTERLY), `amount / 2` (SEMESTER)
- Bourse MERITE : `minimumGrade >= 14.0` requis au renouvellement
- Bourse SOCIALE : critères sociaux — revérification annuelle manuelle
- Une bourse SUSPENDED ne génère plus de versements mais reste associée à l'étudiant

---

### `ScholarshipDisbursement`

Versement individuel d'une bourse.

| Champ | Type | Description |
|---|---|---|
| `id` | Long PK | Clé primaire |
| `scholarship_id` | Long FK | Bourse parente |
| `amount` | BigDecimal | Montant du versement |
| `period` | String | Période (ex : `2026-01` pour janvier 2026) |
| `scheduledDate` | LocalDate | Date prévue du versement |
| `status` | DisbursementStatus | SCHEDULED, PAID, FAILED, CANCELLED |
| `paymentId` | UUID | Référence Payment créé au versement |
| `paidAt` | LocalDateTime | Date de versement effectif |
| `failureReason` | String | Motif en cas d'échec |

---

## 8. Énumérations

### `PaymentType`

| Valeur | Description |
|---|---|
| `FRAIS_DOSSIER` | Frais de dossier d'admission |
| `FRAIS_SCOLARITE` | Frais de scolarité semestriels/annuels |
| `INSCRIPTION` | Frais d'inscription administrative |
| `BOURSE` | Versement de bourse |
| `REMBOURSEMENT` | Remboursement d'un paiement |
| `AUTRE` | Autre paiement |

### `PaymentStatus`

| Valeur | Description |
|---|---|
| `PENDING` | En attente de confirmation passerelle |
| `COMPLETED` | Confirmé — irrévocable |
| `FAILED` | Échec de la transaction |
| `REFUNDED` | Remboursé |

### `PaymentMethod`

| Valeur | Description |
|---|---|
| `CARTE` | Carte bancaire (Visa, Mastercard) |
| `VIREMENT` | Virement bancaire |
| `MOBILE_MONEY` | Mobile Money (Orange, MTN, Wave) |
| `ESPECES` | Espèces à la caisse de l'université |

### `InvoiceType`

| Valeur | Description |
|---|---|
| `SCOLARITE` | Frais de scolarité |
| `INSCRIPTION` | Frais d'inscription administrative |
| `AUTRE` | Autre facturation |

### `InvoiceStatus`

| Valeur | Description |
|---|---|
| `PENDING` | En attente de paiement |
| `PARTIAL` | Partiellement payée |
| `PAID` | Intégralement payée |
| `OVERDUE` | En retard de paiement |
| `CANCELLED` | Annulée |

### `InstallmentStatus`

| Valeur | Description |
|---|---|
| `PENDING` | Échéance à venir |
| `PAID` | Payée |
| `OVERDUE` | En retard |

### `ScholarshipType`

| Valeur | Description |
|---|---|
| `MERITE` | Bourse au mérite — moyenne >= 14.0 |
| `SOCIALE` | Bourse sociale — critères socio-économiques |
| `EXCELLENCE` | Bourse d'excellence — meilleurs étudiants de la promotion |
| `SPORTIVE` | Bourse sportive — sportifs de haut niveau |

### `ScholarshipStatus`

| Valeur | Description |
|---|---|
| `PENDING` | En attente d'activation |
| `ACTIVE` | En cours — versements actifs |
| `SUSPENDED` | Suspendue — versements arrêtés temporairement |
| `TERMINATED` | Terminée — définitivement clôturée |

### `DisbursementFrequency`

| Valeur | Description |
|---|---|
| `MONTHLY` | Versement mensuel (12 fois/an) |
| `QUARTERLY` | Versement trimestriel (4 fois/an) |
| `SEMESTER` | Versement semestriel (2 fois/an) |

### `DisbursementStatus`

| Valeur | Description |
|---|---|
| `SCHEDULED` | Programmé — pas encore versé |
| `PAID` | Versé avec succès |
| `FAILED` | Échec du versement |
| `CANCELLED` | Annulé (bourse suspendue/terminée) |

---

## 9. Cas d'utilisation

### UC-PAY-001 — Initier le paiement des frais de dossier

**Acteur :** Candidat  
**Déclencheur :** `POST /payments/admission-fees`  
**Préconditions :** L'`AdmissionPayment` en `PENDING` existe dans l'Admission Service

**Scénario principal :**
1. Le candidat envoie `{ applicationId, amount, currency, method }`
2. Le système vérifie qu'un `AdmissionPayment PENDING` existe pour cet `applicationId`
3. Le système vérifie qu'aucun paiement `COMPLETED` n'existe déjà pour cet `applicationId`
4. Le système crée un `Payment` en `PENDING` avec `reference = PAY-{year}-{seq}`
5. Le système simule ou appelle la passerelle externe
6. Sur confirmation → `Payment → COMPLETED`, `PaymentCompleted` publié sur Kafka
7. Sur échec → `Payment → FAILED`, `PaymentFailed` publié sur Kafka

**Règles :**
- Un seul paiement `COMPLETED` autorisé par `applicationId`
- En cas d'échec, le candidat peut relancer (nouveau `Payment` créé)

---

### UC-PAY-002 — Confirmer un paiement via webhook

**Acteur :** Passerelle externe (Stripe/PayDunya)  
**Déclencheur :** `POST /payments/webhook`

**Scénario principal :**
1. La passerelle envoie la confirmation avec `externalReference`
2. Le système retrouve le `Payment` via `externalReference`
3. `Payment → COMPLETED`, `paidAt = now`
4. `PaymentCompleted` publié sur Kafka
5. Si `type = FRAIS_SCOLARITE` → met à jour la `Invoice` associée

---

### UC-PAY-003 — Simuler une confirmation (développement)

**Acteur :** Développeur / Admin  
**Déclencheur :** `POST /payments/{id}/simulate-confirm`

**Scénario principal :**
1. Le système passe le `Payment` en `COMPLETED`
2. Publie les mêmes événements Kafka que la confirmation réelle
3. **Endpoint désactivé en production** (`@Profile("!prod")`)

---

### UC-PAY-004 — Générer les factures de scolarité

**Acteur :** Admin Finance  
**Déclencheur :** `POST /payments/invoices/generate`  
**Préconditions :** Aucune facture existante pour `(year, semester)`

**Scénario principal :**
1. L'admin envoie `{ academicYear, semester, dueDate }`
2. Le système appelle `GET /users/students?status=ACTIVE` via HTTP
3. Pour chaque étudiant :
   a. Calcule le montant selon la filière (tarif configurable)
   b. Vérifie si une bourse `ACTIVE` existe → calcule `scholarshipDeduction`
   c. Crée l'`Invoice` avec `netAmount = amount - scholarshipDeduction`
4. Publie `InvoiceGeneratedEvent` pour chaque étudiant
5. Retourne le nombre de factures générées

**Gestion des erreurs :**
- Si le User Service est indisponible → 503 avec message explicite
- Les étudiants sans filière connue sont ignorés avec log d'avertissement

---

### UC-PAY-005 — Payer une facture de scolarité

**Acteur :** Étudiant  
**Déclencheur :** `POST /payments/invoices/{id}/pay`

**Scénario principal :**
1. L'étudiant envoie `{ amount, method }`
2. Le système vérifie que la facture appartient à l'étudiant connecté
3. Le système vérifie que `amount <= remainingAmount`
4. Crée un `Payment` en `PENDING`
5. Confirmation → `Payment → COMPLETED`
6. `Invoice.paidAmount += amount`, `Invoice.remainingAmount` recalculé
7. Si `remainingAmount <= 0` → `Invoice → PAID`, `InvoicePaidEvent` publié
8. Sinon → `Invoice → PARTIAL`

---

### UC-PAY-006 — Créer un échéancier de paiement

**Acteur :** Admin Finance  
**Déclencheur :** `POST /payments/invoices/{id}/schedule`

**Scénario principal :**
1. L'admin envoie `{ installments: [{ amount, dueDate }, ...] }`
2. Le système vérifie que la somme des échéances = `Invoice.netAmount`
3. Le système vérifie que la facture n'a pas déjà un échéancier
4. Crée le `PaymentSchedule` et les `Installment`
5. `Invoice.hasSchedule = true`
6. Notification envoyée à l'étudiant

---

### UC-PAY-007 — Attribuer une bourse

**Acteur :** Admin Finance  
**Déclencheur :** `POST /payments/scholarships`

**Scénario principal :**
1. L'admin envoie `{ studentId, type, amount, academicYear, disbursementFrequency, conditions }`
2. Le système vérifie qu'aucune bourse du même type n'est déjà active pour cet étudiant et cette année
3. Crée la `Scholarship` en `PENDING`
4. L'admin active la bourse : `PUT /payments/scholarships/{id}/activate`
5. `Scholarship → ACTIVE`
6. Génère les `ScholarshipDisbursement` selon la fréquence
7. Publie `ScholarshipActivatedEvent`

---

### UC-PAY-008 — Versement automatique des bourses

**Acteur :** Système (scheduler)  
**Déclencheur :** Scheduler mensuel (1er du mois à 08:00)

**Scénario principal :**
1. Le scheduler récupère tous les `ScholarshipDisbursement` en `SCHEDULED` dont `scheduledDate <= today`
2. Pour chaque versement :
   a. Vérifie que la bourse est encore `ACTIVE`
   b. Crée un `Payment` de type `BOURSE`
   c. Traite le paiement
   d. `ScholarshipDisbursement → PAID`
   e. Publie `ScholarshipDisbursedEvent`
3. En cas d'échec : `ScholarshipDisbursement → FAILED`, alerte admin

---

### UC-PAY-009 — Renouvellement annuel des bourses mérite

**Acteur :** Système (scheduler annuel)  
**Déclencheur :** Scheduler annuel (1er juillet à 06:00)

**Scénario principal :**
1. Récupère toutes les bourses `ACTIVE` de type `MERITE`
2. Pour chaque bourse :
   a. Appelle `GET /courses/students/{id}/progress?year={year}` (Course Service)
   b. Si `annualAverage >= minimumGrade` → crée une nouvelle `Scholarship` pour l'année suivante
   c. Si `annualAverage < minimumGrade` → `Scholarship → SUSPENDED`, publie notification
3. Publie `ScholarshipRenewalProcessedEvent`

---

### UC-PAY-010 — Détection des impayés

**Acteur :** Système (scheduler quotidien)  
**Déclencheur :** Scheduler quotidien (02:00)

**Scénario principal :**
1. Récupère toutes les factures `PENDING` ou `PARTIAL` dont `dueDate < today`
2. Passe ces factures en `OVERDUE`
3. Publie `InvoiceOverdueEvent` pour chaque facture
4. Pour les factures `OVERDUE` depuis plus de 30 jours :
   a. Publie `StudentPaymentBlockedEvent`
   b. User Service restreint l'accès de l'étudiant
   c. Course Service bloque les inscriptions
5. Vérifie aussi les `Installment` expirés → `OVERDUE`

---

### UC-PAY-011 — Rembourser un paiement

**Acteur :** Admin Finance  
**Déclencheur :** `POST /payments/{id}/refund`

**Scénario principal :**
1. L'admin envoie `{ reason }`
2. Le système vérifie que le `Payment` est `COMPLETED`
3. Crée un nouveau `Payment` de type `REMBOURSEMENT` avec montant négatif
4. `Payment original → REFUNDED`
5. Si lié à une facture → `Invoice.paidAmount` ajusté
6. Publie `PaymentRefundedEvent`

---

### UC-PAY-012 — Suspension d'une bourse

**Acteur :** Admin Finance  
**Déclencheur :** `PUT /payments/scholarships/{id}/suspend`

**Scénario principal :**
1. L'admin envoie `{ reason }`
2. `Scholarship → SUSPENDED`
3. Les `ScholarshipDisbursement` futurs passent en `CANCELLED`
4. Publie `ScholarshipSuspendedEvent`
5. Notification à l'étudiant

---

## 10. Règles métier transversales

### Génération de la référence de paiement

```
Format : PAY-{année}-{séquence 5 chiffres}
Exemple : PAY-2026-00042
Implémentation : AtomicLong threadsafe (comme StudentNumberService)
```

### Calcul de la facture avec bourse

```
Montant brut (amount)        = tarif standard de la filière
Déduction bourse             = Scholarship.amount / nbSemestres
Montant net (netAmount)      = amount - scholarshipDeduction
Montant restant              = netAmount - paidAmount
```

### Règle de blocage étudiant pour impayé

```
Facture OVERDUE depuis > 30 jours
  → StudentPaymentBlockedEvent publié
  → User Service : restreint accès aux services non-essentiels
  → Course Service : Enrollment.status = BLOCKED
  → Notification répétée tous les 7 jours
Déblocage : paiement de la facture ou arrangement admin
```

### Renouvellement bourse mérite

```
Bourse MERITE active en fin d'année :
  → Récupère annualAverage du Course Service
  → Si annualAverage >= minimumGrade (défaut 14.0) :
      Crée nouvelle Scholarship pour l'année suivante
  → Si annualAverage < minimumGrade :
      Scholarship courante → SUSPENDED
      Notification à l'étudiant + admin
```

### Paiement partiel et échéancier

```
Sans échéancier :
  → Paiement partiel autorisé tant que amount <= remainingAmount
  → Invoice → PARTIAL jusqu'au solde complet

Avec échéancier :
  → Chaque Installment doit être payé à sa dueDate
  → Paiement d'un installment met à jour Invoice.paidAmount
  → Un installment peut être payé par anticipation
```

---

## 11. Sécurité des routes

| Route | Méthode | Rôles |
|---|---|---|
| `/payments/admission-fees` | POST | CANDIDATE, STUDENT |
| `/payments/{id}` | GET | ADMIN_FINANCE, propriétaire |
| `/payments/history` | GET | ADMIN_FINANCE, propriétaire |
| `/payments/{id}/simulate-confirm` | POST | ADMIN_FINANCE (dev uniquement) |
| `/payments/webhook` | POST | Public (IP whitelist en prod) |
| `/payments/invoices/me` | GET | STUDENT, CANDIDATE |
| `/payments/invoices/{id}` | GET | ADMIN_FINANCE, propriétaire |
| `/payments/invoices/{id}/pay` | POST | STUDENT |
| `/payments/invoices/generate` | POST | ADMIN_FINANCE |
| `/payments/invoices/{id}/schedule` | POST | ADMIN_FINANCE |
| `/payments/scholarships/me` | GET | STUDENT |
| `/payments/scholarships` | GET | ADMIN_FINANCE |
| `/payments/scholarships` | POST | ADMIN_FINANCE |
| `/payments/scholarships/{id}/activate` | PUT | ADMIN_FINANCE |
| `/payments/scholarships/{id}/suspend` | PUT | ADMIN_FINANCE |
| `/payments/scholarships/{id}/terminate` | PUT | ADMIN_FINANCE |
| `/payments/admin/stats` | GET | ADMIN_FINANCE, SUPER_ADMIN |
| `/payments/admin/overdue` | GET | ADMIN_FINANCE |

---

## 12. Événements Kafka

### Consommés (2)

| Topic | Producteur | Action |
|---|---|---|
| `student.profile.created` | User Service | Optionnel — créer une facture d'inscription pour le nouveau semestre |
| `semester.validated` | Course Service | Déclenche vérification renouvellement bourses mérite |

### Publiés (8)

| Topic | Déclencheur | Consommateurs |
|---|---|---|
| `payment.completed` | Paiement confirmé | **Admission Service** (critique), Notification Service, Document Service |
| `payment.failed` | Paiement échoué | Notification Service |
| `payment.refunded` | Remboursement | Notification Service, Document Service |
| `invoice.generated` | Génération facture | Notification Service |
| `invoice.paid` | Facture soldée | Notification Service, Document Service |
| `invoice.overdue` | Facture en retard | Notification Service |
| `student.payment.blocked` | Impayé > 30j | **User Service**, **Course Service**, Notification Service |
| `scholarship.disbursed` | Versement bourse | Notification Service, Document Service |

### Structure détaillée des events critiques

#### `payment.completed` (critique pour l'Admission Service)
```json
{
  "paymentReference": "PAY-2026-00042",
  "applicationId": "uuid-ou-null-si-non-frais-dossier",
  "invoiceId": "uuid-ou-null",
  "userId": "uuid-utilisateur",
  "amount": 50.00,
  "currency": "EUR",
  "type": "FRAIS_DOSSIER",
  "paidAt": "2026-03-20T10:30:00"
}
```

#### `student.payment.blocked` (critique pour User Service et Course Service)
```json
{
  "studentId": "uuid-etudiant",
  "userId": "uuid-utilisateur",
  "invoiceId": "uuid-facture",
  "amount": 850.00,
  "overdueDays": 32,
  "academicYear": "2026-2027"
}
```

---

## 13. Dépendances cross-services

| Service | Type | Sens | Description |
|---|---|---|---|
| **Admission Service** | Kafka consumer | ← Payment Service publie | Consomme `payment.completed` → Application PAID |
| **User Service** | HTTP synchrone | → Payment Service appelle | `GET /users/students?status=ACTIVE` pour génération factures |
| **User Service** | Kafka consumer | ← Payment Service publie | Consomme `student.payment.blocked` → restreint accès |
| **Course Service** | HTTP synchrone | → Payment Service appelle | `GET /courses/students/{id}/progress` pour renouvellement bourses |
| **Course Service** | Kafka consumer | ← Payment Service publie | Consomme `student.payment.blocked` → bloque inscriptions |
| **Course Service** | Kafka producer | → Payment Service consomme | `semester.validated` → renouvellement bourses mérite |
| **Notification Service** | Kafka consumer | ← Payment Service publie | Consomme tous les events pour notifier les utilisateurs |
| **Document Service** | Kafka consumer | ← Payment Service publie | Consomme `payment.completed`, `invoice.paid`, `scholarship.disbursed` → génère PDF |

---

## 14. Schedulers

| Cron | Méthode | Description |
|---|---|---|
| `0 0 2 * * *` | `processOverdueInvoices()` | Détection impayés quotidienne (02:00) |
| `0 0 8 1 * *` | `processDisbursements()` | Versements bourses (1er du mois 08:00) |
| `0 0 6 1 7 *` | `processScholarshipRenewals()` | Renouvellement bourses mérite (1er juillet 06:00) |
| `0 0 9 * * MON` | `sendPaymentReminders()` | Rappels factures dues dans 7 jours (lundi 09:00) |

---

## 15. Endpoints API complets

### Paiements

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| POST | `/payments/admission-fees` | Initier paiement frais de dossier | CANDIDATE |
| POST | `/payments/webhook` | Confirmation passerelle externe | Public |
| POST | `/payments/{id}/simulate-confirm` | Simuler confirmation (dev) | ADMIN_FINANCE |
| POST | `/payments/{id}/refund` | Rembourser un paiement | ADMIN_FINANCE |
| GET | `/payments/{id}` | Détail d'un paiement | ADMIN_FINANCE, propriétaire |
| GET | `/payments/history` | Historique — `?userId=&type=&from=&to=` | ADMIN_FINANCE, propriétaire |
| GET | `/payments/me` | Mes paiements | Authentifié |

### Factures

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/payments/invoices/me` | Mes factures | STUDENT |
| GET | `/payments/invoices/{id}` | Détail d'une facture | ADMIN_FINANCE, propriétaire |
| POST | `/payments/invoices/{id}/pay` | Payer une facture | STUDENT |
| POST | `/payments/invoices/generate` | Générer les factures — `?year=&semester=` | ADMIN_FINANCE |
| POST | `/payments/invoices/{id}/schedule` | Créer un échéancier | ADMIN_FINANCE |
| GET | `/payments/invoices/{id}/schedule` | Consulter l'échéancier | ADMIN_FINANCE, propriétaire |
| PUT | `/payments/invoices/{id}/cancel` | Annuler une facture | ADMIN_FINANCE |

### Bourses

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/payments/scholarships/me` | Ma bourse | STUDENT |
| GET | `/payments/scholarships/{id}` | Détail d'une bourse | ADMIN_FINANCE |
| GET | `/payments/scholarships` | Lister les bourses — `?studentId=&type=&status=` | ADMIN_FINANCE |
| POST | `/payments/scholarships` | Attribuer une bourse | ADMIN_FINANCE |
| PUT | `/payments/scholarships/{id}/activate` | Activer | ADMIN_FINANCE |
| PUT | `/payments/scholarships/{id}/suspend` | Suspendre | ADMIN_FINANCE |
| PUT | `/payments/scholarships/{id}/terminate` | Terminer définitivement | ADMIN_FINANCE |
| GET | `/payments/scholarships/{id}/disbursements` | Historique des versements | ADMIN_FINANCE, propriétaire |

### Administration

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| GET | `/payments/admin/stats` | Statistiques financières globales | ADMIN_FINANCE, SUPER_ADMIN |
| GET | `/payments/admin/overdue` | Factures en retard — paginées | ADMIN_FINANCE |
| GET | `/payments/admin/blocked-students` | Étudiants bloqués pour impayé | ADMIN_FINANCE |

---

## 16. Configuration

### `application.yml` (local)

```yaml
spring:
  application:
    name: payment-service
  config:
    import: optional:configserver:http://localhost:8761
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### `payment-service.yaml` (Config Server)

```yaml
server:
  port: 8086

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/paymentdb
    username: payment_service
    password: payment_service#123@
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  kafka:
    bootstrap-servers: localhost:9092

eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false
```

### Base de données

```sql
CREATE DATABASE paymentdb;
CREATE USER payment_service WITH PASSWORD 'payment_service#123@';
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment_service;
```

### Gateway — routes à ajouter

```yaml
- id: payment-route
  uri: http://localhost:8086
  predicates:
    - Path=/payments/**

- id: payment-service-swagger
  uri: http://localhost:8086
  predicates:
    - Path=/payment-service/**
  filters:
    - StripPrefix=1
```

### `shouldNotFilter` Gateway — routes publiques à ajouter

```java
|| path.equals("/payments/webhook")  // webhook passerelle — sans JWT
```
