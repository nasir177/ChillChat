package com.example.chillchat.models;

import java.util.Date;

public class ChatMessage {
    public String senderId;
    public String receiverId;
    public String message;
    public String translatedMessage;
    public boolean isSeen;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String language;

    public String getTranslatedMessage() {
        return translatedMessage;
    }

    public void setTranslatedMessage(String translatedMessage) {
        this.translatedMessage = translatedMessage;
    }

    public boolean isTranslated() {
        return isTranslated;
    }

    public void setTranslated(boolean translated) {
        isTranslated = translated;
    }

    private boolean isTranslated;
    public String dateTime;  // This is for the formatted date string
    public Date dateObject;  // This is for the raw Date object
    public String conversionId;
    public String conversionName;
    public String conversionImage;
}

