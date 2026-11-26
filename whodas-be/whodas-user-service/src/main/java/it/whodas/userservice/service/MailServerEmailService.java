package it.whodas.userservice.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Invio email tramite il container SMTP whodas-mailserver (relay Brevo).
 * Usa un JavaMailSender dedicato (host/porta configurabili) per non interferire
 * con il mail sender principale dell'applicazione.
 */
@Service
public class MailServerEmailService {

    private static final Logger log = LoggerFactory.getLogger(MailServerEmailService.class);

    private final JavaMailSenderImpl mailSender;
    private final String replyTo;

    public MailServerEmailService(
            @Value("${app.mailserver.host:mailserver}") String host,
            @Value("${app.mailserver.port:25}") int port,
            @Value("${app.email.reply-to:whodas_info@yahoo.com}") String replyTo) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        this.mailSender = sender;
        this.replyTo = replyTo;
    }

    public void send(String from, String to, String subject, String htmlBody, String textBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setReplyTo(replyTo);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(message);
            log.info("Email inviata a {} tramite whodas-mailserver", to);
        } catch (Exception e) {
            log.error("Errore invio email a {}: {}", to, rootMessage(e), e);
            throw new RuntimeException(rootMessage(e), e);
        }
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : cause.getClass().getSimpleName();
    }
}