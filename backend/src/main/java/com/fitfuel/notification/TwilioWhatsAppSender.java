package com.fitfuel.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TwilioWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppSender.class);

    private final String accountSid;
    private final String authToken;
    private final String whatsAppFrom;
    private final String contentSid;
    private final ObjectMapper objectMapper;
    private boolean configured;

    public TwilioWhatsAppSender(@Value("${fitfuel.twilio.account-sid:}") String accountSid,
                                @Value("${fitfuel.twilio.auth-token:}") String authToken,
                                @Value("${fitfuel.twilio.whatsapp-from:}") String whatsAppFrom,
                                @Value("${fitfuel.twilio.content-sid:}") String contentSid,
                                ObjectMapper objectMapper) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.whatsAppFrom = whatsAppFrom;
        this.contentSid = contentSid;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        if (accountSid.isBlank() || authToken.isBlank() || whatsAppFrom.isBlank()) {
            log.warn("Twilio credentials not configured (TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / "
                    + "TWILIO_WHATSAPP_FROM) — WhatsApp notifications are disabled.");
            configured = false;
            return;
        }
        Twilio.init(accountSid, authToken);
        configured = true;
        if (contentSid.isBlank()) {
            log.info("Twilio WhatsApp enabled in FREE-TEXT mode (sandbox). Set "
                    + "TWILIO_WHATSAPP_CONTENT_SID to send Meta-approved templates in production.");
        } else {
            log.info("Twilio WhatsApp enabled in TEMPLATE mode (content SID {}).", contentSid);
        }
    }

    @Override
    public void send(String toPhoneE164, String freeTextMessage, Map<String, String> templateVariables) {
        if (!configured) {
            log.debug("Skipping WhatsApp send to {} — Twilio is not configured.", toPhoneE164);
            return;
        }
        PhoneNumber to = new PhoneNumber("whatsapp:" + toPhoneE164);
        PhoneNumber from = new PhoneNumber("whatsapp:" + whatsAppFrom);
        try {
            if (contentSid.isBlank()) {
                // Sandbox / development: free-text body. Meta rejects free-text for
                // business-initiated production messages, so this path is dev-only.
                Message.creator(to, from, freeTextMessage).create();
            } else {
                // Production: send a Meta-approved template. templateVariables are keyed
                // "1","2",... to match {{1}},{{2}},... in the registered template body.
                Message.creator(to, from, (String) null)
                        .setContentSid(contentSid)
                        .setContentVariables(objectMapper.writeValueAsString(templateVariables))
                        .create();
            }
        } catch (Exception e) {
            // A flaky WhatsApp send must never block or fail the order flow that triggered it.
            log.warn("Failed to send WhatsApp message to {}: {}", toPhoneE164, e.getMessage());
        }
    }
}
