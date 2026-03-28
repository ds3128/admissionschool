package org.darius.notification.mail;

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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    // ── Envoi principal ────────────────────────────────────────────────────────

    public void send(
            String              to,
            String              subject,
            String              templateName,
            Map<String, Object> templateData
    ) throws Exception {

        // Rendu du template Thymeleaf
        String html = renderTemplate(templateName, templateData);

        // Construction du message MIME
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
        log.debug("Email envoyé → {} : {}", to, subject);
    }

    // ── Rendu Thymeleaf ───────────────────────────────────────────────────────

    public String renderTemplate(
            String              templateName,
            Map<String, Object> data
    ) {
        Context context = new Context();
        if (data != null) {
            data.forEach(context::setVariable);
        }

        try {
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            log.warn("Template manquant '{}' — utilisation du fallback : {}",
                    templateName, ex.getMessage());
            return renderFallback(data);
        }
    }

    // ── Template de secours ───────────────────────────────────────────────────

    private String renderFallback(Map<String, Object> data) {
        String firstName = data != null
                ? (String) data.getOrDefault("firstName", "Utilisateur")
                : "Utilisateur";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <style>
                body { font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f5f5f5; }
                .container { max-width: 600px; margin: 40px auto; background: white;
                             border-radius: 8px; overflow: hidden; }
                .header { background: #1e40af; color: white; padding: 30px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; }
                .body { padding: 30px; color: #374151; line-height: 1.6; }
                .footer { background: #f9fafb; padding: 20px; text-align: center;
                          color: #9ca3af; font-size: 12px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>AdmissionSchool</h1>
                </div>
                <div class="body">
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Vous avez une nouvelle notification de l'université.</p>
                  <p>Connectez-vous à votre espace pour plus de détails.</p>
                  <p>Cordialement,<br/><strong>L'équipe AdmissionSchool</strong></p>
                </div>
                <div class="footer">
                  <p>© 2026 AdmissionSchool — Tous droits réservés</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstName);
    }
}
