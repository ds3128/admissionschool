# Paiement Service — Documentation Technique v1

**Projet :** AdmissionSchool  
**Service :** Paiement Service  
**Port :** 8086  
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

Le Paiement Service gère tous les flux financiers liés aux étudiants et à l'établissement : frais de scolarité, frais de dossier d'admission, bourses et remboursements. Il est appelé de manière synchrone par l'Admission Service pour les frais de dossier, et publie des événements Kafka pour notifier les autres services des paiements confirmés.

### Responsabilités

- Traitement des paiements (frais de dossier, frais de scolarité)
- Gestion des factures et des échéanciers
- Gestion des bourses étudiantes
- Historique des transactions
- Génération des reçus et justificatifs de paiement

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Intégration avec une passerelle de paiement externe | Infrastructure / externe |
| Paie du personnel | RH Service |
| Génération des documents financiers (reçus PDF) | Document Service |
| Notifications | Notification Service |

---

## 2. Modèle de domaine — Description des entités

### 2.1 `Payment`

Représente une transaction de paiement effectuée.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `userId` | UUID | Payeur (référence Auth Service) |
| `invoiceId` | UUID | Facture associée (nullable si paiement direct) |
| `amount` | BigDecimal | Montant payé |
| `currency` | String | Devise (ex : EUR, XAF) |
| `type` | PaymentType | FRAIS_DOSSIER, FRAIS_SCOLARITE, BOURSE, REMBOURSEMENT |
| `status` | PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED |
| `method` | PaymentMethod | CARTE, VIREMENT, MOBILE_MONEY, ESPECES |
| `reference` | String | Référence externe de la transaction |
| `description` | String | Libellé du paiement |
| `paidAt` | LocalDateTime | Date et heure de confirmation |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- Un paiement `COMPLETED` ne peut pas être supprimé, uniquement remboursé.
- La `reference` est générée par la passerelle externe et garantit l'unicité.
- Un paiement `FAILED` peut être retesté (nouveau `Payment` créé).

---

### 2.2 `Invoice`

Représente une facture émise à un étudiant pour les frais de scolarité.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `studentId` | UUID | Étudiant concerné (référence User Service) |
| `academicYear` | String | Année académique |
| `amount` | BigDecimal | Montant total de la facture |
| `currency` | String | Devise |
| `dueDate` | LocalDate | Date limite de paiement |
| `paid` | boolean | Payée intégralement |
| `paidAmount` | BigDecimal | Montant déjà payé |
| `remainingAmount` | BigDecimal | Solde restant |
| `type` | InvoiceType | SCOLARITE, INSCRIPTION, AUTRE |
| `status` | InvoiceStatus | PENDING, PARTIAL, PAID, OVERDUE |
| `semester` | String | Semestre concerné |
| `createdAt` | LocalDateTime | Date de création |

**Règles métier :**
- `remainingAmount = amount - paidAmount`.
- `status = OVERDUE` si `dueDate < today` et `paid = false`.
- `status = PARTIAL` si `paidAmount > 0` et `paid = false`.
- Un étudiant avec une facture `OVERDUE` peut être bloqué (accès limité aux services).
- Le paiement partiel est autorisé si un échéancier est défini.

---

### 2.3 `PaymentSchedule`

Représente un échéancier de paiement permettant de fractionner les frais de scolarité.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `invoiceId` | UUID | Facture associée |
| `installments` | List\<Installment\> | Liste des échéances |
| `totalAmount` | BigDecimal | Montant total |
| `createdAt` | LocalDateTime | Date de création |

---

### 2.4 `Installment`

Représente une échéance dans un plan de paiement.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `scheduleId` | Long | Référence vers `PaymentSchedule.id` |
| `amount` | BigDecimal | Montant de l'échéance |
| `dueDate` | LocalDate | Date limite |
| `paid` | boolean | Payée ou non |
| `paidAt` | LocalDateTime | Date de paiement effectif |

---

### 2.5 `Scholarship`

Représente une bourse accordée à un étudiant.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `studentId` | UUID | Étudiant bénéficiaire |
| `academicYear` | String | Année académique |
| `amount` | BigDecimal | Montant annuel de la bourse |
| `type` | ScholarshipType | MERITE, SOCIALE, EXCELLENCE, SPORTIVE |
| `status` | ScholarshipStatus | PENDING, ACTIVE, SUSPENDED, TERMINATED |
| `startDate` | LocalDate | Date de début |
| `endDate` | LocalDate | Date de fin |
| `conditions` | String | Conditions de maintien |

**Règles métier :**
- Une bourse `MERITE` requiert une moyenne >= 14/20 pour être renouvelée.
- Une bourse `SOCIAL` est attribuée sur critères sociaux — revérifiée annuellement.
- Une bourse `SUSPENDED` suspend les versements sans annuler la bourse.

---

### 2.6 `ScholarshipDisbursement`

Représente un versement de bourse mensuel ou trimestriel.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `scholarshipId` | Long | Référence vers `Scholarship.id` |
| `amount` | BigDecimal | Montant du versement |
| `period` | String | Période (ex : 2025-10) |
| `status` | DisbursementStatus | SCHEDULED, PAID, FAILED |
| `paidAt` | LocalDateTime | Date de versement effectif |

---

## 3. Énumérations

### `PaymentType`
| Valeur | Description |
|---|---|
| `FRAIS_DOSSIER` | Frais de dossier d'admission |
| `FRAIS_SCOLARITE` | Frais de scolarité |
| `INSCRIPTION` | Frais d'inscription |
| `BOURSE` | Versement de bourse |
| `REMBOURSEMENT` | Remboursement |
| `AUTRE` | Autre paiement |

### `PaymentStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente de confirmation |
| `COMPLETED` | Paiement confirmé |
| `FAILED` | Paiement échoué |
| `REFUNDED` | Remboursé |

### `PaymentMethod`
| Valeur | Description |
|---|---|
| `CARTE` | Carte bancaire |
| `VIREMENT` | Virement bancaire |
| `MOBILE_MONEY` | Mobile Money |
| `ESPECES` | Espèces (à la caisse) |

### `InvoiceType`
| Valeur | Description |
|---|---|
| `SCOLARITE` | Frais de scolarité semestriels / annuels |
| `INSCRIPTION` | Frais d'inscription administrative |
| `AUTRE` | Autre facturation |

### `InvoiceStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente de paiement |
| `PARTIAL` | Partiellement payée |
| `PAID` | Intégralement payée |
| `OVERDUE` | En retard de paiement |

### `ScholarshipType`
| Valeur | Description |
|---|---|
| `MERITE` | Bourse au mérite académique |
| `SOCIALE` | Bourse sociale |
| `EXCELLENCE` | Bourse d'excellence |
| `SPORTIVE` | Bourse sportive |

### `ScholarshipStatus`
| Valeur | Description |
|---|---|
| `PENDING` | En attente d'attribution |
| `ACTIVE` | Bourse en cours |
| `SUSPENDED` | Suspendue temporairement |
| `TERMINATED` | Terminée |

---

## 4. Cas d'utilisation

### UC-PAY-001 — Traiter un paiement de frais de dossier

**Acteur :** Admission Service (appel synchrone)  
**Déclencheur :** Requête HTTP `POST /payments/process`

**Scénario principal :**
1. L'Admission Service envoie `userId`, `amount`, `currency`, `type = FRAIS_DOSSIER`, `applicationId`.
2. Le système crée un `Payment` en `PENDING`.
3. Le système appelle la passerelle de paiement externe.
4. Sur confirmation → statut `COMPLETED`, publication `PaymentCompleted`.
5. Sur échec → statut `FAILED`, publication `PaymentFailed`.

---

### UC-PAY-002 — Générer les factures de scolarité

**Acteur :** Admin Finance (`ADMIN_FINANCE`)  
**Déclencheur :** Requête `POST /payments/invoices/generate?year=&semester=`

**Scénario principal :**
1. L'admin déclenche la génération pour une cohorte (année + semestre).
2. Le système récupère la liste des étudiants actifs depuis le User Service.
3. Pour chaque étudiant → création d'une `Invoice`.
4. Si bourse active → déduction automatique du montant de la bourse.
5. Notification envoyée à chaque étudiant.

---

### UC-PAY-003 — Payer une facture de scolarité

**Acteur :** Étudiant  
**Déclencheur :** Requête `POST /payments/invoices/{id}/pay`

**Scénario principal :**
1. L'étudiant sélectionne la méthode de paiement et initie le paiement.
2. Le système crée un `Payment` en `PENDING`.
3. Paiement confirmé → `Payment.status = COMPLETED`.
4. `Invoice.paidAmount` mis à jour.
5. Si `paidAmount >= amount` → `Invoice.paid = true`, statut `PAID`.
6. Publication `InvoicePaid`.

---

### UC-PAY-004 — Attribuer une bourse

**Acteur :** Admin Finance (`ADMIN_FINANCE`)  
**Déclencheur :** Requête `POST /payments/scholarships`

**Scénario principal :**
1. L'admin crée la bourse avec `studentId`, `type`, `amount`, `academicYear`.
2. Le système crée la `Scholarship` en `PENDING`.
3. L'admin active la bourse → statut `ACTIVE`.
4. Le système génère les `ScholarshipDisbursement` pour chaque période.
5. Notification envoyée à l'étudiant.

---

### UC-PAY-005 — Créer un échéancier de paiement

**Acteur :** Admin Finance (`ADMIN_FINANCE`)  
**Déclencheur :** Requête `POST /payments/invoices/{id}/schedule`

**Scénario principal :**
1. L'admin définit le nombre de versements et les dates.
2. Le système calcule les montants par échéance.
3. Le système crée le `PaymentSchedule` avec ses `Installment`.
4. Notification envoyée à l'étudiant.

---

## 5. Règles métier transversales

### Calcul de la facture avec bourse

```
Montant facture brut = frais de scolarité standards
Déduction bourse     = Scholarship.amount / 2 (par semestre)
Montant net étudiant = Montant brut - Déduction bourse
```

### Blocage étudiant pour impayé

```
Si Invoice.status = OVERDUE et dueDate + 30 jours < today :
  Publication StudentPaymentBlocked
  User Service → restriction d'accès aux services
  Course Service → Enrollment.status = BLOCKED
```

### Renouvellement de bourse mérite

```
Chaque fin d'année :
  Récupère StudentProgress du Course Service
  Si semesterAverage >= 14.0 → bourse renouvelée
  Si semesterAverage < 14.0 → bourse SUSPENDED + notification
```

---

## 6. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **User Service** | HTTP synchrone | Récupère la liste des étudiants pour génération des factures |
| **Course Service** | HTTP synchrone | Récupère `StudentProgress` pour renouvellement des bourses |
| **Document Service** | Consommateur Kafka | Génère les reçus et factures PDF |
| **Notification Service** | Consommateur Kafka | Notifie les étudiants |

### Événements publiés (Kafka)

| Événement | Déclencheur | Consommateurs |
|---|---|---|
| `PaymentCompleted` | Paiement confirmé | Admission Service, Notification Service |
| `PaymentFailed` | Paiement échoué | Admission Service, Notification Service |
| `InvoicePaid` | Facture soldée | Document Service, Notification Service |
| `InvoiceOverdue` | Facture en retard | Notification Service |
| `StudentPaymentBlocked` | Impayé critique | User Service, Course Service, Notification Service |
| `ScholarshipDisbursed` | Versement bourse | Notification Service, Document Service |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `StudentProfileCreated` | User Service | Création potentielle d'une Invoice pour le semestre courant |
| `SemesterValidated` | Course Service | Vérification renouvellement des bourses mérite |

---

## 7. Résumé des endpoints API

### Paiements

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `POST` | `/payments/process` | Traiter un paiement | Système / Authentifié |
| `GET` | `/payments/history?userId=` | Historique des paiements | `ADMIN_FINANCE`, Propriétaire |
| `GET` | `/payments/{id}` | Détail d'un paiement | `ADMIN_FINANCE`, Propriétaire |

### Factures

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/payments/invoices?studentId=` | Factures d'un étudiant | `ADMIN_FINANCE`, Propriétaire |
| `POST` | `/payments/invoices/generate` | Générer les factures | `ADMIN_FINANCE` |
| `POST` | `/payments/invoices/{id}/pay` | Payer une facture | Authentifié |
| `POST` | `/payments/invoices/{id}/schedule` | Créer un échéancier | `ADMIN_FINANCE` |
| `GET` | `/payments/invoices/me` | Mes factures | Authentifié |

### Bourses

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/payments/scholarships?studentId=` | Bourses d'un étudiant | `ADMIN_FINANCE` |
| `POST` | `/payments/scholarships` | Attribuer une bourse | `ADMIN_FINANCE` |
| `PUT` | `/payments/scholarships/{id}/activate` | Activer | `ADMIN_FINANCE` |
| `PUT` | `/payments/scholarships/{id}/suspend` | Suspendre | `ADMIN_FINANCE` |
| `GET` | `/payments/scholarships/me` | Ma bourse | Authentifié |
