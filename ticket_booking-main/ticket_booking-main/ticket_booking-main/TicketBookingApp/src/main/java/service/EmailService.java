package service;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import java.io.File;
import java.util.Properties;

public class EmailService {
    private String username = "oopg1t7.is442@gmail.com";
    private String password = "p@ssword_is442";

    public boolean sendEmail(String toEmail, String subject, String htmlContent, String pdfFilePath, int numOfTickets) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Get the Session object
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // Create a default MimeMessage object
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            String enhancedHtmlContent = htmlContent + "<br><p>This QR code represents " + numOfTickets + " ticket(s).</p>";

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            String basePath = System.getProperty("user.dir"); // Get the user working directory
            String qrCodeFilePath = basePath + "/image/qrcode.png"; // Path to the QR Code image

            // Part two is attachments
            MimeBodyPart pdfAttachment = new MimeBodyPart();
            DataSource source = new FileDataSource(pdfFilePath);
            pdfAttachment.setDataHandler(new DataHandler(source));
            pdfAttachment.setFileName(new File(pdfFilePath).getName());

            MimeBodyPart qrAttachment = new MimeBodyPart();
            qrAttachment.attachFile(qrCodeFilePath);
            multipart.addBodyPart(qrAttachment);

            // Part three is setting the HTML content
            BodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(enhancedHtmlContent, "text/html");
            multipart.addBodyPart(htmlBodyPart);

            // Send the complete message parts
            message.setContent(multipart);
            // Send message
            Transport.send(message);

            System.out.println("Sent message successfully....");
            return true;
        } catch (MessagingException | java.io.IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
