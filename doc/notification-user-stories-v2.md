# Notification Service — User Stories complètes v2

**Projet :** AdmissionSchool  
**Service :** Notification Service  
**Version :** 2.0.0  
**Stack :** Java / Spring Boot  
**Date :** Mars 2026

---

## Acteurs

| Acteur | Rôle système | Description |
|---|---|---|
| **Candidat** | En cours de candidature | Reçoit les notifications du processus d'admission |
| **Étudiant** | `STUDENT` | Reçoit les notifications académiques et financières |
| **Enseignant** | `TEACHER` | Reçoit les demandes d'encadrement de thèse |
| **Admin** | `ADMIN_SCHOLAR`, `SUPER_ADMIN` | Consulte les stats, force les renvois |
| **Système** | Kafka consumers + `@Scheduled` | Traitement automatique des notifications |

---

## EPIC 1 — Notifications Admission

### US-NOTIF-001 — Confirmer la soumission d'un dossier

**En tant que** Candidat  
**Je veux** recevoir un email de confirmation quand je soumets mon dossier  
**Afin de** avoir la preuve que ma candidature a bien été reçue

**Critères d'acceptation :**
- [ ] Déclenché par `application.submitted`
- [ ] Email envoyé à `personalEmail` inclus dans l'event
- [ ] Contient : numéro de candidature, année académique, date de soumission
- [ ] Contient les prochaines étapes du processus
- [ ] Notification enregistrée en base avec statut `SENT`

**Topic :** `application.submitted`  
**Template :** `admission/application-submitted`

---

### US-NOTIF-002 — Informer d'une mise à jour administrative

**En tant que** Candidat  
**Je veux** être notifié quand mon dossier est traité administrativement  
**Afin de** savoir si mon dossier est conforme ou s'il manque des pièces

**Critères d'acceptation :**
- [ ] Déclenché par `application.admin.review`
- [ ] Si validé → email positif avec passage à la commission
- [ ] Si refusé → email avec motif et demande de compléments

**Topic :** `application.admin.review`  
**Template :** `admission/application-admin-review`

---

### US-NOTIF-003 — Informer de la transmission à la commission

**En tant que** Candidat  
**Je veux** savoir que mon dossier a été transmis à la commission pédagogique

**Critères d'acceptation :**
- [ ] Déclenché par `application.pending.commission`
- [ ] Email informatif simple avec délai indicatif de traitement

**Topic :** `application.pending.commission`  
**Template :** `admission/application-pending-commission`

---

### US-NOTIF-004 — Notifier un entretien planifié

**En tant que** Candidat  
**Je veux** recevoir les informations de mon entretien par email

**Critères d'acceptation :**
- [ ] Déclenché par `interview.scheduled`
- [ ] Contient : date, heure, durée, lieu ou lien visio
- [ ] Type précisé (PRESENTIEL ou VISIO)
- [ ] Documents à apporter mentionnés

**Topic :** `interview.scheduled`  
**Template :** `admission/interview-scheduled`

---

### US-NOTIF-005 — Notifier un directeur de thèse

**En tant que** Enseignant HDR  
**Je veux** être notifié quand un candidat souhaite que je dirige sa thèse

**Critères d'acceptation :**
- [ ] Déclenché par `thesis.approval.requested`
- [ ] `directorId` reçu → appel HTTP `GET /users/{directorId}` → User Service
- [ ] Email avec résumé du projet de recherche et délai de réponse (15 jours)

**Topic :** `thesis.approval.requested`  
**Template :** `admission/thesis-approval-requested`

---

### US-NOTIF-006 — Notifier les choix acceptés et le délai de confirmation

**En tant que** Candidat  
**Je veux** être notifié quand au moins un de mes choix est accepté

**Critères d'acceptation :**
- [ ] Déclenché par `application.awaiting.confirmation`
- [ ] Liste des formations acceptées affichée
- [ ] Délai exact de confirmation (`expiresAt`) clairement indiqué

**Topic :** `application.awaiting.confirmation`  
**Template :** `admission/application-awaiting-confirmation`

---

### US-NOTIF-007 — Accueillir le nouvel étudiant

**En tant que** Nouvel étudiant  
**Je veux** recevoir un email de bienvenue avec mon numéro matricule

**Critères d'acceptation :**
- [ ] Déclenché par `application.accepted`
- [ ] **Notification non désactivable** — toujours envoyée
- [ ] Contient : numéro matricule, filière, prochaines étapes (paiement scolarité)

**Topic :** `application.accepted`  
**Template :** `admission/application-accepted`

---

### US-NOTIF-008 — Informer d'un refus de candidature

**En tant que** Candidat  
**Je veux** être notifié si tous mes choix ont été refusés

**Critères d'acceptation :**
- [ ] Déclenché par `application.rejected`
- [ ] Email respectueux et empathique
- [ ] Mentionne la possibilité de candidater lors de la prochaine campagne

**Topic :** `application.rejected`  
**Template :** `admission/application-rejected`

---

### US-NOTIF-009 — Notifier une promotion depuis la liste d'attente

**En tant que** Candidat en liste d'attente  
**Je veux** être notifié immédiatement quand une place se libère

**Critères d'acceptation :**
- [ ] Déclenché par `waitlist.promoted`
- [ ] Délai de 48h clairement indiqué
- [ ] Filière concernée et `expiresAt` visibles

**Topic :** `waitlist.promoted`  
**Template :** `admission/waitlist-promoted`

---

### US-NOTIF-010 — Informer d'une confirmation automatique

**En tant que** Étudiant  
**Je veux** être notifié que ma formation a été confirmée automatiquement après expiration du délai

**Critères d'acceptation :**
- [ ] Déclenché par `choice.auto.confirmed`
- [ ] Email explique que la priorité 1 a été confirmée automatiquement
- [ ] Contient le nom de la formation confirmée

**Topic :** `choice.auto.confirmed`  
**Template :** `admission/choice-auto-confirmed`

---

## EPIC 2 — Notifications Payment

### US-NOTIF-011 — Confirmer un paiement

**En tant que** Candidat ou Étudiant  
**Je veux** recevoir un reçu de paiement par email

**Critères d'acceptation :**
- [ ] Déclenché par `payment.completed`
- [ ] Contient : référence, montant, devise, méthode, date
- [ ] Template différent selon `type` : FRAIS_DOSSIER / FRAIS_SCOLARITE / BOURSE
- [ ] Si `userId` sans email → résolution via User Service

**Topic :** `payment.completed`  
**Template :** `payment/payment-completed`

---

### US-NOTIF-012 — Alerter d'un paiement échoué

**En tant que** Candidat ou Étudiant  
**Je veux** être alerté quand mon paiement échoue

**Critères d'acceptation :**
- [ ] Déclenché par `payment.failed`
- [ ] Email urgent avec motif d'échec et instructions pour relancer

**Topic :** `payment.failed`  
**Template :** `payment/payment-failed`

---

### US-NOTIF-013 — Confirmer un remboursement

**En tant que** Étudiant  
**Je veux** être notifié d'un remboursement effectué

**Critères d'acceptation :**
- [ ] Déclenché par `payment.refunded`
- [ ] Contient : référence originale, référence remboursement, montant, motif

**Topic :** `payment.refunded`  
**Template :** `payment/payment-refunded`

---

### US-NOTIF-014 — Notifier la génération d'une facture

**En tant que** Étudiant  
**Je veux** être notifié quand une nouvelle facture est créée

**Critères d'acceptation :**
- [ ] Déclenché par `invoice.generated`
- [ ] Contient : année académique, semestre, montant net, déduction bourse, date limite
- [ ] `studentId` → résolution email via User Service

**Topic :** `invoice.generated`  
**Template :** `payment/invoice-generated`

---

### US-NOTIF-015 — Confirmer le règlement d'une facture

**En tant que** Étudiant  
**Je veux** être notifié quand ma facture est entièrement réglée

**Critères d'acceptation :**
- [ ] Déclenché par `invoice.paid`
- [ ] Email de confirmation positif

**Topic :** `invoice.paid`  
**Template :** `payment/invoice-paid`

---

### US-NOTIF-016 — Alerter d'une facture en retard

**En tant que** Étudiant  
**Je veux** être alerté quand ma facture est en retard

**Critères d'acceptation :**
- [ ] Déclenché par `invoice.overdue`
- [ ] **Notification non désactivable**
- [ ] Contient : montant restant, jours de retard, conséquences possibles
- [ ] `studentId` → résolution email via User Service

**Topic :** `invoice.overdue`  
**Template :** `payment/invoice-overdue`

---

### US-NOTIF-017 — Alerter d'un blocage pour impayé critique

**En tant que** Étudiant  
**Je veux** être notifié de la restriction de mon accès

**Critères d'acceptation :**
- [ ] Déclenché par `student.payment.blocked`
- [ ] **Notification non désactivable**
- [ ] Contient le montant dû et les démarches de régularisation

**Topic :** `student.payment.blocked`  
**Template :** `payment/student-payment-blocked`

---

### US-NOTIF-018 — Confirmer un versement de bourse

**En tant que** Étudiant boursier  
**Je veux** être notifié de chaque versement de bourse

**Critères d'acceptation :**
- [ ] Déclenché par `scholarship.disbursed`
- [ ] Contient : montant, période, référence du paiement

**Topic :** `scholarship.disbursed`  
**Template :** `payment/scholarship-disbursed`

---

## EPIC 3 — Notifications Course

### US-NOTIF-019 — Confirmer les inscriptions aux cours

**En tant que** Étudiant  
**Je veux** recevoir la confirmation de mes inscriptions aux matières

**Critères d'acceptation :**
- [ ] Déclenché par `student.enrolled`
- [ ] Liste des matières, semestre, année académique
- [ ] `studentId` → résolution email via User Service

**Topic :** `student.enrolled`  
**Template :** `course/student-enrolled`

---

### US-NOTIF-020 — Alerter d'un seuil d'absences dépassé

**En tant que** Étudiant  
**Je veux** être alerté quand mon taux de présence est trop bas

**Critères d'acceptation :**
- [ ] Déclenché par `attendance.threshold.exceeded`
- [ ] Email avec taux actuel, seuil requis, matière concernée, conséquences
- [ ] Email envoyé également à l'admin (ADMIN_SCHOLAR)

**Topic :** `attendance.threshold.exceeded`  
**Template :** `course/attendance-threshold-exceeded`

---

### US-NOTIF-021 — Notifier la disponibilité des notes

**En tant que** Étudiant  
**Je veux** être notifié quand mes notes sont publiées

**Critères d'acceptation :**
- [ ] Déclenché par `grades.published`
- [ ] **Envoi bulk** — un email par étudiant dans `studentIds`
- [ ] Contient le nom de l'évaluation et de la matière
- [ ] Ne contient pas la note elle-même (confidentialité)

**Topic :** `grades.published`  
**Template :** `course/grades-published`

---

### US-NOTIF-022 — Alerter d'une séance annulée

**En tant que** Étudiant  
**Je veux** être notifié rapidement quand une séance est annulée

**Critères d'acceptation :**
- [ ] Déclenché par `session.cancelled`
- [ ] **Envoi bulk** — un email par étudiant dans `affectedStudentIds`
- [ ] Contient : matière, date, heure, motif

**Topic :** `session.cancelled`  
**Template :** `course/session-cancelled`

---

### US-NOTIF-023 — Notifier les résultats du semestre

**En tant que** Étudiant  
**Je veux** recevoir mes résultats semestriels par email

**Critères d'acceptation :**
- [ ] Déclenché par `semester.validated`
- [ ] **Envoi bulk** — un email personnalisé par étudiant dans `results`
- [ ] ADMIS → template avec félicitations, moyenne, mention, rang
- [ ] AJOURNE → template avec informations rattrapage

**Topic :** `semester.validated`  
**Template :** `course/semester-validated-admis` ou `course/semester-validated-ajourne`

---

## EPIC 4 — Historique et préférences

### US-NOTIF-024 — Consulter mon historique

**En tant que** Utilisateur authentifié  
**Je veux** voir toutes les notifications que j'ai reçues

**Critères d'acceptation :**
- [ ] Liste paginée triée par date décroissante
- [ ] Filtrable par type (ADMISSION, PAYMENT, COURSE)
- [ ] Uniquement mes propres notifications (`X-User-Id`)

**Endpoint :** `GET /notifications/me?page=&size=&type=`

---

### US-NOTIF-025 — Consulter mes notifications non lues

**En tant que** Utilisateur authentifié  
**Je veux** voir rapidement mes notifications non lues

**Critères d'acceptation :**
- [ ] Retourne uniquement les notifications `readAt = null`
- [ ] `GET /notifications/me/count` retourne le nombre entier

**Endpoints :** `GET /notifications/me/unread`, `GET /notifications/me/count`

---

### US-NOTIF-026 — Marquer des notifications comme lues

**En tant que** Utilisateur authentifié  
**Je veux** marquer mes notifications comme lues

**Critères d'acceptation :**
- [ ] Marquer une : `PUT /notifications/{id}/read`
- [ ] Tout marquer : `PUT /notifications/me/read-all`
- [ ] 403 si tentative sur une notification d'un autre utilisateur

---

### US-NOTIF-027 — Gérer mes préférences

**En tant que** Utilisateur authentifié  
**Je veux** contrôler quelles notifications je reçois

**Critères d'acceptation :**
- [ ] Désactiver les emails ou par catégorie
- [ ] Préférences créées automatiquement avec valeurs par défaut si inexistantes
- [ ] Notifications critiques toujours envoyées (`INVOICE_OVERDUE`, `STUDENT_PAYMENT_BLOCKED`, `APPLICATION_ACCEPTED`)

**Endpoints :** `GET /notifications/preferences`, `PUT /notifications/preferences`

---

## EPIC 5 — Fiabilité et administration

### US-NOTIF-028 — Retry automatique des notifications échouées

**En tant que** Système  
**Je veux** retenter l'envoi des notifications échouées

**Critères d'acceptation :**
- [ ] Scheduler `@Scheduled(fixedDelay = 300000)` — toutes les 5 minutes
- [ ] Retente uniquement les `FAILED` avec `retryCount < 3`
- [ ] Incrémente `retryCount` à chaque tentative
- [ ] Après 3 échecs → `FAILED` définitif + log `WARN`

---

### US-NOTIF-029 — Consulter les statistiques d'envoi

**En tant que** Admin  
**Je veux** voir les statistiques d'envoi des notifications

**Critères d'acceptation :**
- [ ] Nombre total, envoyé, échoué, en attente
- [ ] Taux de succès en pourcentage

**Endpoint :** `GET /notifications/admin/stats`

---

### US-NOTIF-030 — Forcer le renvoi d'une notification

**En tant que** Admin  
**Je veux** forcer le renvoi d'une notification échouée

**Critères d'acceptation :**
- [ ] Remet `retryCount = 0` et statut `PENDING`
- [ ] Le scheduler reprend au prochain cycle

**Endpoint :** `POST /notifications/admin/{id}/resend`

---

## Matrice des user stories par acteur

| Epic | US | Candidat | Étudiant | Enseignant | Admin | Système |
|---|---|---|---|---|---|---|
| Admission | US-001 | ✅ | | | | ✅ |
| Admission | US-002 | ✅ | | | | ✅ |
| Admission | US-003 | ✅ | | | | ✅ |
| Admission | US-004 | ✅ | | | | ✅ |
| Admission | US-005 | | | ✅ | | ✅ |
| Admission | US-006 | ✅ | | | | ✅ |
| Admission | US-007 | | ✅ | | | ✅ |
| Admission | US-008 | ✅ | | | | ✅ |
| Admission | US-009 | ✅ | | | | ✅ |
| Admission | US-010 | | ✅ | | | ✅ |
| Payment | US-011 | ✅ | ✅ | | | ✅ |
| Payment | US-012 | ✅ | ✅ | | | ✅ |
| Payment | US-013 | | ✅ | | | ✅ |
| Payment | US-014 | | ✅ | | | ✅ |
| Payment | US-015 | | ✅ | | | ✅ |
| Payment | US-016 | | ✅ | | | ✅ |
| Payment | US-017 | | ✅ | | | ✅ |
| Payment | US-018 | | ✅ | | | ✅ |
| Course | US-019 | | ✅ | | | ✅ |
| Course | US-020 | | ✅ | | ✅ | ✅ |
| Course | US-021 | | ✅ | | | ✅ |
| Course | US-022 | | ✅ | | | ✅ |
| Course | US-023 | | ✅ | | | ✅ |
| Historique | US-024 | ✅ | ✅ | ✅ | | |
| Historique | US-025 | ✅ | ✅ | ✅ | | |
| Historique | US-026 | ✅ | ✅ | ✅ | | |
| Historique | US-027 | ✅ | ✅ | ✅ | | |
| Fiabilité | US-028 | | | | | ✅ |
| Fiabilité | US-029 | | | | ✅ | |
| Fiabilité | US-030 | | | | ✅ | |

**Total : 30 user stories — 5 epics**

---

## Matrice d'intégration cross-services

| US | Service | Type | Direction | Données |
|---|---|---|---|---|
| US-001 à US-010 | Admission Service | Kafka async | Admission → Notification | 10 events admission |
| US-011 à US-018 | Payment Service | Kafka async | Payment → Notification | 8 events payment |
| US-019 à US-023 | Course Service | Kafka async | Course → Notification | 5 events course |
| US-004 | User Service | HTTP sync | Notification → User | `GET /users/{directorId}` |
| US-014, US-016, US-019 | User Service | HTTP sync | Notification → User | `GET /users/{studentId}` |
