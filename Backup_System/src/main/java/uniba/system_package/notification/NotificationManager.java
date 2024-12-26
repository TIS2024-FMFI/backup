package uniba.system_package.notification;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.List;
import java.util.Properties;

public class NotificationManager {
    private static final Logger logger = LogManager.getLogger(NotificationManager.class);

    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private String fromAddress;
    private List<String> toAddresses;

    public NotificationManager(String smtpHost, int smtpPort, String username, String password, String fromAddress, List<String> toAddresses) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses;
    }

    /**
     * Sends a notification email.
     *
     * @param subject The subject of the email.
     * @param message The content of the email.
     */
    public void sendEmail(String subject, String message) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromAddress));
            for (String toAddress : toAddresses) {
                email.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            }
            email.setSubject(subject);
            email.setText(message);

            Transport.send(email);
            logger.info("Notification email sent successfully: {}", subject);
        } catch (MessagingException e) {
            logger.error("Failed to send notification email: {}", e.getMessage(), e);
        }
    }

    /**
     * Formats a notification message based on backup results.
     */
    public String formatBackupResultMessage(String backupType, String targetName, String status, String location) {
        return String.format(
                "Backup Type: %s%n" +
                        "Target: %s%n" +
                        "Status: %s%n" +
                        "Location: %s%n",
                backupType, targetName, status, location
        );
    }
}
