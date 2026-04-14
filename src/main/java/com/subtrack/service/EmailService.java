package com.subtrack.service;

import com.subtrack.dao.SystemConfigDAO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@ApplicationScoped
public class EmailService {

    @Inject
    private SystemConfigDAO configDAO;

    public void sendEmail(String toAddress, String subject, String body) {
        String host = configDAO.getValue("SMTP_HOST", "");
        String port = configDAO.getValue("SMTP_PORT", "587");
        String username = configDAO.getValue("SMTP_USER", "");
        String password = configDAO.getValue("SMTP_PASS", "");

        if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
            System.err.println(">>> Error: SMTP credentials are not fully configured in SystemConfig. Email not sent.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        // Required for newer Mail APIs if TLS is not properly resolving
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println(">>> Email sent successfully to: " + toAddress);
            
        } catch (MessagingException e) {
            System.err.println(">>> Failed to send email to " + toAddress);
            e.printStackTrace();
        }
    }
}
