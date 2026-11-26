package it.whodas.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carica il template email XML (template_email.xml) e lo invia tramite
 * whodas-mailserver, sostituendo i placeholder.
 */
@Service
public class TemplateEmailService {

    private static final Logger log = LoggerFactory.getLogger(TemplateEmailService.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z_]+)}");

    private final MailServerEmailService mailServerEmailService;

    @Value("${app.email.from:whodas_info@yahoo.com}")
    private String fromEmail;

    @Value("${app.email.template-path:classpath:email/template_email.xml}")
    private String templatePath;

    @Value("${app.frontend.url:http://localhost}")
    private String frontendUrl;

    public TemplateEmailService(MailServerEmailService mailServerEmailService) {
        this.mailServerEmailService = mailServerEmailService;
    }

    public void sendResetPasswordEmail(String to, String firstName, String lastName, String username, String token) {
        String[] template = loadTemplate();
        String subject = "Richiesta di Reset Password WHODAS 2.0";
        String html = substitute(template[1], firstName, lastName, username, token);
        String text = substitute(template[2], firstName, lastName, username, token);
        mailServerEmailService.send(fromEmail, to, subject, html, text);
        log.info("Email reset password (template) inviata a {} per account {}", to, username);
    }

    private String substitute(String content, String firstName, String lastName, String username, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + (token != null ? token : "");
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        if (fullName.isBlank()) {
            fullName = "Utente";
        }
        String accountName = (username != null && !username.isBlank()) ? username : fullName;
        Matcher m = PLACEHOLDER.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String value;
            switch (key) {
                case "RESET_LINK" -> value = resetLink;
                case "EXPIRY_HOURS" -> value = "24";
                case "USER_FIRST_NAME" -> value = firstName != null ? firstName : "";
                case "USER_LAST_NAME" -> value = lastName != null ? lastName : "";
                case "USER_NAME" -> value = fullName;
                case "APP_NAME" -> value = "WHODAS 2.0";
                case "ACCOUNT_NAME" -> value = accountName;
                case "SUPPORT_EMAIL" -> value = "whodas_info@yahoo.com";
                case "YEAR" -> value = "2026";
                default -> value = m.group(0);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** @return [subject, htmlBody, textBody] */
    private String[] loadTemplate() {
        try {
            Resource resource = templatePath.startsWith("classpath:")
                    ? new ClassPathResource(templatePath.substring("classpath:".length()))
                    : new FileSystemResource(templatePath);
            try (InputStream in = resource.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(in);
                String html = textOf(doc, "html-body");
                String text = textOf(doc, "text-body");
                return new String[]{textOf(doc, "subject"), html, text};
            }
        } catch (Exception e) {
            log.error("Impossibile caricare il template email da '{}': {}", templatePath, e.getMessage(), e);
            throw new RuntimeException("Template email non disponibile", e);
        }
    }

    private String textOf(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }
}