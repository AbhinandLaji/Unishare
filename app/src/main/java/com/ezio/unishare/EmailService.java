package com.ezio.unishare;

import java.util.Properties;
import java.util.Random;
// FINAL FIX: Changed all imports from "jakarta.mail" to "javax.mail"
// This is the correct package name for the Android mail libraries.
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * A reusable service to send emails using a dedicated Gmail account to users.
 * This version uses the correct imports for the Android mail libraries.
 */
public class EmailService {

    // IMPORTANT: Your credentials are correct. Do not change them.
    private static final String SENDER_EMAIL = "tkmcerentalbot@gmail.com";
    private static final String SENDER_PASSWORD = "ibyjstemhejesqco";

    /**
     * Sends an email with a randomly generated 6-digit OTP.
     * @param recipientEmail The email address to send the OTP to.
     * @return The 6-digit OTP as a String if successful, otherwise null.
     */
    public String sendOtp(String recipientEmail) {
        // This logic is all correct and does not need to be changed.
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Your Verification Code");
            message.setText("Your One-Time Password (OTP) is: " + otp);

            Transport.send(message);

            System.out.println("OTP email sent successfully to " + recipientEmail);
            return otp;

        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

