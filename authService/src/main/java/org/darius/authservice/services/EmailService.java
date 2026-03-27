package org.darius.authservice.services;

//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class EmailService {
//    private final JavaMailSender mailSender;
//
//    @Value("${app.base-url}")
//    private String baseUrl;
//
//    public void sendVerificationEmail(String email, String token) {
//        String verificationLink = baseUrl + "/auth/verify?token=" + token;
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(email);
//        message.setSubject("Confirm your account");
//        message.setText(
//                "Bonjour,\n\n" +
//                        "Merci pour votre inscription. Cliquez sur le lien ci-dessous pour activer votre compte :\n\n" +
//                        verificationLink + "\n\n" +
//                        "Ce lien est valide pendant 24h.\n\n" +
//                        "L'équipe"
//        );
//
//        mailSender.send(message);
//    }
//
//    public void sendResetPasswordEmail(String toEmail, String token) {
//        String resetLink = baseUrl + "/auth/reset-password?token=" + token;
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(toEmail);
//        message.setSubject("Reset your password");
//        message.setText(
//                "Bonjour,\n\n" +
//                        "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :\n\n" +
//                        resetLink + "\n\n" +
//                        "Ce lien est valide pendant 24h.\n\n" +
//                        "Si vous n'avez pas fait cette demande, ignorez ce mail.\n\n" +
//                        "L'équipe"
//        );
//        mailSender.send(message);
//    }
//}

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    // ── Email d'activation de compte ──────────────────────────────────────────

    public void sendVerificationEmail(String email, String token) {
        String verificationLink = baseUrl + "/auth/verify?token=" + token;

        Context ctx = new Context();
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("email", email);

        String html = renderTemplate("mail/verify-account", ctx);
        sendHtml(email, "Activez votre compte AdmissionSchool", html);
    }

    // ── Email de réinitialisation du mot de passe ─────────────────────────────

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/auth/reset-password?token=" + token;

        Context ctx = new Context();
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("email", toEmail);

        String html = renderTemplate("mail/reset-password", ctx);
        sendHtml(toEmail, "Réinitialisation de votre mot de passe - AdmissionSchool", html);
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private String renderTemplate(String templateName, Context ctx) {
        try {
            return templateEngine.process(templateName, ctx);
        } catch (Exception ex) {
            log.warn("Template '{}' introuvable - utilisation du fallback texte : {}",
                    templateName, ex.getMessage());
            // Fallback : texte brut si le template est absent
            return "<p>Bonjour,</p><p>Veuillez cliquer sur le lien reçu.</p>";
        }
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML

            mailSender.send(message);
            log.info("Email envoyé → {} : {}", to, subject);

        } catch (Exception ex) {
            log.error("Échec envoi email → {} : {}", to, ex.getMessage());
            // On ne propage pas l'exception pour ne pas bloquer l'inscription
            // L'utilisateur peut demander le renvoi via /auth/resend-activation
        }
    }
}
