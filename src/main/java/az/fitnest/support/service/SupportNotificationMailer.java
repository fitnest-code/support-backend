package az.fitnest.support.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SupportNotificationMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final String supportTo;

    public SupportNotificationMailer(
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${SPRING_MAIL_FROM:${spring.mail.username:noreply@fitnest.az}}") String from,
            @Value("${FITNEST_SUPPORT_EMAIL:support@fitnest.az}") String supportTo
    ) {
        this.mailSender = mailSender;
        this.from = (from == null || from.isBlank()) ? username : from;
        this.supportTo = supportTo;
    }

    @Async
    public void sendPartnerApplication(
            String gymName,
            String contactName,
            String phone,
            String email,
            String activity
    ) {
        if (mailSender == null || supportTo == null || supportTo.isBlank()) {
            log.info("Partner application email skipped; mail is not configured");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from == null || from.isBlank() ? "noreply@fitnest.az" : from);
            message.setTo(supportTo);
            message.setSubject("Yeni partnyor müraciəti: " + gymName);
            message.setText("""
                    Yeni partnyor müraciəti

                    Zal: %s
                    Əlaqədar şəxs: %s
                    Telefon: %s
                    Email: %s
                    Fəaliyyət növü: %s
                    """.formatted(
                    nullToDash(gymName),
                    nullToDash(contactName),
                    nullToDash(phone),
                    nullToDash(email),
                    nullToDash(activity)
            ));
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Partner application email failed: {}", ex.getMessage());
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
