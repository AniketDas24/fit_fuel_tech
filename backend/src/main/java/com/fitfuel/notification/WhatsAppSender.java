package com.fitfuel.notification;

import java.util.Map;

public interface WhatsAppSender {

    /**
     * Sends a WhatsApp notification.
     *
     * @param toPhoneE164        recipient in E.164 format
     * @param freeTextMessage    used in sandbox/free-text mode (no template configured)
     * @param templateVariables  used in production template mode (keyed "1", "2", ... to match {{1}}, {{2}}, ...)
     */
    void send(String toPhoneE164, String freeTextMessage, Map<String, String> templateVariables);
}
