package org.darius.authservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String email, String token) {
        String verificationLink = baseUrl + "/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Confirm your account");
        message.setText(
                "Bonjour,\n\n" +
                        "Merci pour votre inscription. Cliquez sur le lien ci-dessous pour activer votre compte :\n\n" +
                        verificationLink + "\n\n" +
                        "Ce lien est valide pendant 24h.\n\n" +
                        "L'équipe"
        );

        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/auth/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your password");
        message.setText(
                "Bonjour,\n\n" +
                        "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :\n\n" +
                        resetLink + "\n\n" +
                        "Ce lien est valide pendant 24h.\n\n" +
                        "Si vous n'avez pas fait cette demande, ignorez ce mail.\n\n" +
                        "L'équipe"
        );
        mailSender.send(message);
    }
}
