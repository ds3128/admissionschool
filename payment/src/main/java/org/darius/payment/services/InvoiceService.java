package org.darius.payment.services;

import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;

import java.util.List;
import java.util.Map;

public interface InvoiceService {

    /**
     * Génère les factures pour tous les étudiants actifs d'une cohorte.
     * Appelle le User Service en HTTP. Retourne { generated, skipped }.
     */
    Map<String, Integer> generateInvoices(GenerateInvoicesRequest request);

    /** Mes factures (étudiant connecté). */
    List<InvoiceResponse> getMyInvoices(String studentId);

    /** Détail d'une facture. */
    InvoiceResponse getInvoiceById(String invoiceId, String requesterId);

    /**
     * Paie tout ou partie d'une facture.
     * Met à jour paidAmount et recalcule le statut.
     */
    PaymentResponse payInvoice(String invoiceId, String studentId, PayInvoiceRequest request);

    /**
     * Crée un échéancier de paiement.
     * La somme des échéances doit être égale à Invoice.netAmount.
     */
    PaymentScheduleResponse createSchedule(String invoiceId, String adminId, CreateScheduleRequest request);

    /** Consulte l'échéancier d'une facture. */
    PaymentScheduleResponse getSchedule(String invoiceId, String requesterId);

    /** Annule une facture PENDING ou PARTIAL. */
    InvoiceResponse cancelInvoice(String invoiceId, String adminId);

    /** Factures en retard paginées — pour l'admin. */
    PageResponse<InvoiceResponse> getOverdueInvoices(int page, int size);

    /** Job schedulé — détecte les impayés quotidiennement. */
    void processOverdueInvoices();

    /** Job schedulé — envoie les rappels de paiement. */
    void sendPaymentReminders();
}