package com.example.Homework;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class HomeworkBot implements SpringLongPollingBot {
    private final UpdateConsumer updateConsumer;

    @Value("${token}")
    private String token;
    public HomeworkBot(UpdateConsumer updateConsumer) {
        this.updateConsumer = updateConsumer;
    }
    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
