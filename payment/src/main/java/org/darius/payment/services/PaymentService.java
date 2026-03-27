package org.darius.payment.services;

import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;

public interface PaymentService {

    /** Initie le paiement des frais de dossier (candidat). */
    PaymentResponse initiateAdmissionFee(String userId, InitiateAdmissionFeeRequest request);

    /** Traite la confirmation d'une passerelle externe via webhook. */
    void processWebhook(WebhookRequest request);

    /** Simule une confirmation — dev uniquement. */
    PaymentResponse simulateConfirm(String paymentId);

    /** Rembourse un paiement COMPLETED. */
    PaymentResponse refund(String paymentId, String adminId, RefundRequest request);

    /** Détail d'un paiement. */
    PaymentResponse getPaymentById(String paymentId, String requesterId);

    /** Historique des paiements d'un utilisateur. */
    PageResponse<PaymentResponse> getPaymentHistory(String userId, int page, int size);

    /** Mes paiements (utilisateur connecté). */
    PageResponse<PaymentResponse> getMyPayments(String userId, int page, int size);
}