package it.whodas.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@whodas.local}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetLink, int expiryHours) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("WHODAS 2.0 - Reset Password");
            message.setText("""
                    Gentile utente,

                    Hai richiesto il reset della password per il tuo account WHODAS 2.0.

                    Clicca sul link seguente per impostare una nuova password:
                    %s

                    Il link scadrà tra %d ore.

                    Se non hai richiesto questo reset, ignora questa email.

                    Cordiali saluti,
                    Team WHODAS 2.0
                    """.formatted(resetLink, expiryHours));

            mailSender.send(message);
            log.info("Email reset password inviata a: {}", to);
        } catch (Exception e) {
            log.error("Errore invio email reset password a {}: {}", to, e.getMessage(), e);
        }
    }
}