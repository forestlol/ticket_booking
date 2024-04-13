package service;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import java.io.File;
import java.util.Properties;
import javax.mail.MessagingException;
import java.util.logging.Logger;
import java.util.logging.Level;


public class EmailService {
    private String username = "oopg1t7.is442@gmail.com";
    private String password = "tvvh ookq untm losm";

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    public boolean sendEmail(String toEmail, String subject, String htmlContent, String pdfFilePath, String qrCodeFilePath, int numOfTickets) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            Multipart multipart = new MimeMultipart();

            // Add PDF attachment
            MimeBodyPart pdfAttachment = new MimeBodyPart();
            DataSource pdfSource = new FileDataSource(pdfFilePath);
            pdfAttachment.setDataHandler(new DataHandler(pdfSource));
            pdfAttachment.setFileName(new File(pdfFilePath).getName());
            multipart.addBodyPart(pdfAttachment);

            // Add QR Code attachment
            MimeBodyPart qrCodeAttachment = new MimeBodyPart();
            DataSource qrSource = new FileDataSource(qrCodeFilePath);
            qrCodeAttachment.setDataHandler(new DataHandler(qrSource));
            qrCodeAttachment.setFileName(new File(qrCodeFilePath).getName());
            multipart.addBodyPart(qrCodeAttachment);

            // Setup HTML content
            BodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(htmlContent, "text/html");
            multipart.addBodyPart(htmlBodyPart);

            message.setContent(multipart);
            Transport.send(message);

            LOGGER.log(Level.INFO, "Email sent successfully to: {0}", toEmail);
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email to: " + toEmail, e);
            return false;
        }
    }
}
