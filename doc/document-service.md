# Document Service - Documentation Technique v1

**Projet :** AdmissionSchool  
**Service :** Document Service  
**Port :** 8089  
**Version :** 1.0.0  
**Dernière mise à jour :** Mars 2026

---

## Table des matières

1. [Vue d'ensemble du service](#1-vue-densemble-du-service)
2. [Modèle de domaine - Description des entités](#2-modèle-de-domaine--description-des-entités)
3. [Énumérations](#3-énumérations)
4. [Cas d'utilisation](#4-cas-dutilisation)
5. [Règles métier transversales](#5-règles-métier-transversales)
6. [Dépendances cross-services](#6-dépendances-cross-services)
7. [Résumé des endpoints API](#7-résumé-des-endpoints-api)

---

## 1. Vue d'ensemble du service

Le Document Service est responsable de la génération, du stockage et de la mise à disposition de tous les documents officiels de l'établissement : bulletins de notes, diplômes, attestations de scolarité, contrats, fiches de paie et lettres d'admission. Il consomme des événements Kafka pour générer automatiquement les documents au bon moment du cycle de vie.

### Responsabilités

- Génération de documents PDF (bulletins, diplômes, attestations, contrats)
- Stockage et archivage des documents générés
- Mise à disposition sécurisée des documents (liens temporaires)
- Vérification de l'authenticité des documents (QR code / hash)
- Gestion des templates de documents

### Hors périmètre

| Responsabilité | Service concerné |
|---|---|
| Calcul des notes et moyennes | Course Service |
| Données personnelles | User Service |
| Données de paie | RH Service |
| Envoi des notifications | Notification Service |

---

## 2. Modèle de domaine - Description des entités

### 2.1 `GeneratedDocument`

Représente un document généré et stocké dans le système.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `ownerId` | UUID | Propriétaire du document (userId) |
| `type` | DocumentType | Type de document |
| `title` | String | Titre du document |
| `fileUrl` | String | URL de stockage (S3 ou équivalent) |
| `fileSize` | Long | Taille en octets |
| `hash` | String | Empreinte SHA-256 pour vérification |
| `qrCode` | String | Contenu du QR code de vérification |
| `status` | DocStatus | GENERATING, READY, FAILED, ARCHIVED |
| `academicYear` | String | Année académique concernée |
| `semester` | String | Semestre (si applicable) |
| `generatedAt` | LocalDateTime | Date de génération |
| `expiresAt` | LocalDateTime | Expiration du lien d'accès (null = permanent) |
| `templateId` | Long | Template utilisé |

**Règles métier :**
- Les documents officiels (diplômes, bulletins validés) sont permanents.
- Les attestations à la demande ont un lien d'accès valide 30 jours.
- Le `hash` permet la vérification de l'authenticité du document.
- Un document `FAILED` peut être régénéré.

---

### 2.2 `DocumentTemplate`

Représente un template de document utilisé pour la génération PDF.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `name` | String | Nom du template |
| `type` | DocumentType | Type de document |
| `content` | String | Template HTML/Thymeleaf |
| `variables` | List\<String\> | Variables attendues |
| `version` | String | Version du template |
| `isActive` | boolean | Template actif |
| `createdAt` | LocalDateTime | Date de création |

---

### 2.3 `DocumentAccessLog`

Trace les accès aux documents pour audit.

| Champ | Type | Description |
|---|---|---|
| `id` | Long | Clé primaire |
| `documentId` | UUID | Document consulté |
| `accessedBy` | UUID | Utilisateur ayant accédé |
| `accessedAt` | LocalDateTime | Date d'accès |
| `ipAddress` | String | Adresse IP |
| `accessType` | AccessType | VIEW, DOWNLOAD, VERIFY |

---

### 2.4 `Diploma`

Représente un diplôme officiel émis à un étudiant.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `documentId` | UUID | Référence vers `GeneratedDocument.id` |
| `studentId` | UUID | Étudiant diplômé |
| `filiereName` | String | Filière (dénormalisé) |
| `levelName` | String | Niveau obtenu (dénormalisé) |
| `mention` | String | Mention obtenue |
| `generalAverage` | double | Moyenne générale |
| `issuedAt` | LocalDate | Date d'émission |
| `diplomaNumber` | String | Numéro unique du diplôme |

**Règles métier :**
- Un diplôme ne peut être émis que si le semestre est `VALIDATED` et l'étudiant `GRADUATED`.
- Le `diplomaNumber` est unique et généré au format `DIP-YYYY-FILIERE-XXXXX`.
- Un diplôme émis est immuable.

---

### 2.5 `Bulletin`

Représente un bulletin de notes semestriel.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `documentId` | UUID | Référence vers `GeneratedDocument.id` |
| `studentId` | UUID | Étudiant concerné |
| `semesterLabel` | String | Libellé du semestre (dénormalisé) |
| `academicYear` | String | Année académique |
| `generalAverage` | double | Moyenne générale |
| `mention` | String | Mention |
| `creditsObtained` | int | Crédits ECTS obtenus |
| `rank` | int | Rang dans la promotion |
| `status` | BulletinStatus | DRAFT, VALIDATED, PUBLISHED |

**Règles métier :**
- Un bulletin `DRAFT` est généré à la clôture du semestre.
- Il passe à `VALIDATED` après vérification par l'administration.
- Il passe à `PUBLISHED` et devient accessible à l'étudiant.

---

### 2.6 `Attestation`

Représente une attestation administrative générée à la demande.

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Clé primaire |
| `documentId` | UUID | Référence vers `GeneratedDocument.id` |
| `userId` | UUID | Bénéficiaire |
| `type` | AttestationType | SCOLARITE, PRESENCE, REUSSITE, STAGE, INSCRIPTION |
| `academicYear` | String | Année concernée |
| `requestedAt` | LocalDateTime | Date de demande |
| `validUntil` | LocalDate | Date de validité |

---

## 3. Énumérations

### `DocumentType`
| Valeur | Description |
|---|---|
| `BULLETIN` | Bulletin de notes semestriel |
| `RELEVE_NOTES` | Relevé de notes complet |
| `DIPLOME` | Diplôme officiel |
| `ATTESTATION_SCOLARITE` | Attestation de scolarité |
| `ATTESTATION_PRESENCE` | Attestation de présence |
| `ATTESTATION_REUSSITE` | Attestation de réussite |
| `LETTRE_ADMISSION` | Lettre d'admission |
| `CONTRAT` | Contrat de travail |
| `FICHE_PAIE` | Fiche de paie |
| `RECU_PAIEMENT` | Reçu de paiement |
| `CARTE_ETUDIANT` | Carte étudiant |

### `DocStatus`
| Valeur | Description |
|---|---|
| `GENERATING` | Génération en cours |
| `READY` | Document prêt |
| `FAILED` | Génération échouée |
| `ARCHIVED` | Archivé |

### `BulletinStatus`
| Valeur | Description |
|---|---|
| `DRAFT` | Brouillon - non visible par l'étudiant |
| `VALIDATED` | Validé par l'administration |
| `PUBLISHED` | Publié - accessible à l'étudiant |

### `AttestationType`
| Valeur | Description |
|---|---|
| `SCOLARITE` | Attestation d'inscription |
| `PRESENCE` | Attestation de présence |
| `REUSSITE` | Attestation de réussite |
| `STAGE` | Attestation de stage |
| `INSCRIPTION` | Confirmation d'inscription |

### `AccessType`
| Valeur | Description |
|---|---|
| `VIEW` | Consultation en ligne |
| `DOWNLOAD` | Téléchargement |
| `VERIFY` | Vérification d'authenticité |

---

## 4. Cas d'utilisation

### UC-DOC-001 - Générer les bulletins de notes

**Acteur :** Système (consommateur Kafka)  
**Déclencheur :** Réception de l'événement `SemesterValidated`

**Scénario principal :**
1. Le système reçoit `SemesterValidated` avec les résultats de tous les étudiants.
2. Pour chaque étudiant :
   - Récupère le `Transcript` depuis le Course Service.
   - Récupère le profil depuis le User Service.
   - Applique le template `BULLETIN`.
   - Génère le PDF.
   - Crée un `Bulletin` en `DRAFT`.
   - Crée un `GeneratedDocument` en `READY`.
3. L'administrateur valide les bulletins → `VALIDATED`.
4. Publication en masse → `PUBLISHED`.
5. Notification Service notifie les étudiants.

---

### UC-DOC-002 - Émettre un diplôme

**Acteur :** Système (consommateur Kafka)  
**Déclencheur :** Réception de l'événement `StudentGraduated`

**Scénario principal :**
1. Le système reçoit `StudentGraduated` avec `studentId`, `filiereId`, `mention`, `average`.
2. Récupère le profil complet depuis le User Service.
3. Génère le numéro de diplôme unique.
4. Applique le template `DIPLOME`.
5. Génère le PDF avec QR code de vérification.
6. Crée le `Diploma` et le `GeneratedDocument`.
7. Notification envoyée à l'étudiant.

---

### UC-DOC-003 - Générer une attestation de scolarité

**Acteur :** Étudiant  
**Déclencheur :** Requête `POST /documents/attestations`

**Scénario principal :**
1. L'étudiant demande une attestation de scolarité.
2. Le système vérifie que l'étudiant est actif (User Service).
3. Le système applique le template `ATTESTATION_SCOLARITE`.
4. Génère le PDF avec QR code.
5. Retourne le document avec un lien valide 30 jours.

---

### UC-DOC-004 - Vérifier l'authenticité d'un document

**Acteur :** Tout utilisateur (public)  
**Déclencheur :** Requête `GET /documents/verify/{hash}` ou scan du QR code

**Scénario principal :**
1. L'utilisateur envoie le hash ou scanne le QR code.
2. Le système recherche le document correspondant.
3. Si trouvé → retourne les informations de vérification (type, date, propriétaire anonymisé).
4. Trace l'accès dans `DocumentAccessLog`.

---

### UC-DOC-005 - Générer la lettre d'admission

**Acteur :** Système (consommateur Kafka)  
**Déclencheur :** Réception de l'événement `ApplicationAccepted`

**Scénario principal :**
1. Le système reçoit `ApplicationAccepted` avec les données du candidat et la filière confirmée.
2. Applique le template `LETTRE_ADMISSION`.
3. Génère le PDF.
4. Stocke le document et retourne l'URL.
5. L'URL est incluse dans l'email de bienvenue envoyé par le Notification Service.

---

### UC-DOC-006 - Générer les fiches de paie

**Acteur :** Système (consommateur Kafka)  
**Déclencheur :** Réception de l'événement `PayslipPaid`

**Scénario principal :**
1. Le système reçoit `PayslipPaid` avec les données de paie.
2. Applique le template `FICHE_PAIE`.
3. Génère le PDF.
4. Crée le `GeneratedDocument` accessible à l'employé.

---

## 5. Règles métier transversales

### Numérotation des diplômes

```
Format : DIP-YYYY-FILIERE-XXXXX
Exemple : DIP-2025-LINF-00042

YYYY    = année d'émission
FILIERE = code de la filière
XXXXX   = séquentiel sur 5 chiffres
```

### Vérification d'authenticité

```
Hash = SHA-256 (documentId + ownerId + generatedAt + content)
QR code contient : URL de vérification + hash
Vérification publique : GET /documents/verify/{hash}
  → Retourne : valide/invalide + type de document + date d'émission
  → NE retourne PAS les données personnelles complètes
```

### Politique de rétention

```
Diplômes          : permanents (jamais supprimés)
Bulletins         : 50 ans
Attestations      : 5 ans + lien temporaire 30 jours
Fiches de paie    : 10 ans
Contrats          : durée contrat + 10 ans
Reçus paiement    : 10 ans
```

---

## 6. Dépendances cross-services

| Service | Type | Description |
|---|---|---|
| **Course Service** | HTTP synchrone | Récupère `Transcript` pour génération des bulletins |
| **User Service** | HTTP synchrone | Récupère les données personnelles |
| **RH Service** | Consommateur Kafka | Génère contrats et fiches de paie |
| **Paiement Service** | Consommateur Kafka | Génère les reçus de paiement |
| **Notification Service** | Publication indirecte | Le Document Service stocke, le Notification Service envoie l'URL |

### Événements consommés (Kafka)

| Événement | Producteur | Action |
|---|---|---|
| `SemesterValidated` | Course Service | Génération des bulletins |
| `StudentGraduated` | User Service | Émission du diplôme |
| `ApplicationAccepted` | Admission Service | Génération de la lettre d'admission |
| `InvoicePaid` | Paiement Service | Génération du reçu de paiement |
| `PayslipPaid` | RH Service | Génération de la fiche de paie |
| `ContractCreated` | RH Service | Génération du contrat PDF |

---

## 7. Résumé des endpoints API

### Documents

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/documents/me` | Mes documents | Authentifié |
| `GET` | `/documents/{id}` | Consulter un document | Propriétaire, `ADMIN_SCHOLAR` |
| `GET` | `/documents/{id}/download` | Télécharger un document | Propriétaire, `ADMIN_SCHOLAR` |
| `GET` | `/documents/verify/{hash}` | Vérifier l'authenticité | Public |

### Bulletins

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/documents/bulletins?studentId=&year=` | Bulletins d'un étudiant | `USER_READ` |
| `PUT` | `/documents/bulletins/{id}/validate` | Valider un bulletin | `ADMIN_SCHOLAR` |
| `PUT` | `/documents/bulletins/{id}/publish` | Publier un bulletin | `ADMIN_SCHOLAR` |

### Diplômes

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/documents/diplomas?studentId=` | Diplômes d'un étudiant | `USER_READ` |
| `GET` | `/documents/diplomas/{number}` | Diplôme par numéro | Public |

### Attestations

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `POST` | `/documents/attestations` | Demander une attestation | Authentifié |
| `GET` | `/documents/attestations/me` | Mes attestations | Authentifié |

### Templates

| Méthode | Endpoint | Description | Rôle |
|---|---|---|---|
| `GET` | `/documents/templates` | Lister les templates | `SUPER_ADMIN` |
| `POST` | `/documents/templates` | Créer un template | `SUPER_ADMIN` |
| `PUT` | `/documents/templates/{id}` | Modifier un template | `SUPER_ADMIN` |
