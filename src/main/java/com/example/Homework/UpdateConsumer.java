package com.example.Homework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    public UpdateConsumer(@Value("${token}") String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        StringBuilder state = new StringBuilder();
        File file = new File("src/main/java/com/example/Homework/isStarted");
        try (FileReader fr = new FileReader(file)) {
            int c;
            while ((c = fr.read()) != -1) {
                state.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StringBuilder state1 = new StringBuilder();
        File file1 = new File("src/main/java/com/example/Homework/isRemoving");
        try (FileReader fr = new FileReader(file1)) {
            int c;
            while ((c = fr.read()) != -1) {
                state1.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
           }
        System.out.println(state);
        if (update.hasMessage() && update.getMessage().getText().equals("/start") && state.toString().equals("false")) {
            Long chatId = update.getMessage().getChatId();
            sendMessage(chatId, """
                    /stop - закончить работу программы❌
                    /show - показать все записи📖
                    /remove - удалить запись🗑""");
            sendMessage(chatId, "Приветствую😊 Напишите ваше домашнее задание📖✍");

            try (FileWriter fw = new FileWriter(file)) {
                fw.write("true");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (FileWriter fw1 = new FileWriter(file1)) {
                fw1.write("false");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }  else if (update.hasMessage() && update.getMessage().getText().equals("/stop")) {
            Long chatId = update.getMessage().getChatId();
            sendMessage(chatId, "До встречи!👋");
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("false");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (FileWriter fw1 = new FileWriter(file1)) {
                fw1.write("false");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else if(update.hasMessage() && update.getMessage().getText().equals("/show") && (state.toString().equals("true") || state.toString().equals("false"))) {
            Long chatId = update.getMessage().getChatId();
            try {
                showHomework(chatId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (state.toString().equals("true") || state1.toString().equals("true")) {
                sendMessage(chatId, "Можете дальше писать✍✍");
            }
        } else if (update.hasMessage() && update.getMessage().getText().equals("/remove") && (state.toString().equals("true") || state.toString().equals("false"))) {
            Long chatId = update.getMessage().getChatId();
            try {
                showHomework(chatId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("false");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (FileWriter fw1 = new FileWriter(file1)) {
                fw1.write("true");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sendMessage(chatId, "Впишите номер домашнего задания для удаления💌");
        } else if(update.hasMessage() && state.toString().equals("false") && state1.toString().equals("true")) {
            Long chatId = update.getMessage().getChatId();
            String choose = update.getMessage().getText();
            if (isDigit(choose) ) {
                try {
                    if (checkHomework()) {
                        try {
                            deleteHomework(chatId, Integer.parseInt(choose));
                            sendMessage(chatId, "Удалил😙");
                            try {
                                showHomework(chatId);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        deleteHomework(chatId, Integer.parseInt(choose));
                        sendMessage(chatId, "Я🤨... пожалуй САМ закончу удаление🤨...");
                        try (FileWriter fw1 = new FileWriter(file1)) {
                            fw1.write("false");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }



            } else {
                sendMessage(chatId, "😡😡😡ТЫ ЗАЧЕМ ТЕКСТ ВПИСАЛ?");
            }





        }else if (update.hasMessage() && state.toString().equals("true") && state1.toString().equals("false")) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            System.out.println(chatId);
            try {
                writeHomeWork(messageText);
                sendMessage(chatId, "Всё✍ Можете вписывать дальше");
            } catch (IOException e) {
                sendMessage(chatId, "Ошибка❌");
                throw new RuntimeException(e);
            }


        } else if (update.hasMessage() && state.toString().equals("false")) {
            Long chatId = update.getMessage().getChatId();
            sendMessage(chatId, "‼Что бы начать записывать введите команду /start‼");

        }
    }
    public boolean isDigit(String s) throws NumberFormatException {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public void deleteHomework(Long chatId, int choose) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        List<String> homeW = mapper.readValue(Paths.get("src/main/homework.json").toFile(), List.class);
        if (choose <= homeW.size() && choose > 0) {
            homeW.remove(choose - 1);
        } else if (!checkHomework()) {
            sendMessage(chatId, "Дневник ведь пустой, ты чего🤨");
        }else {
            sendMessage(chatId, "Вы вписали некорректное число⭕⭕");
        }
        String json = mapper.writeValueAsString(homeW);
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream("src/main/homework.json"),
                StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mapper.disable(SerializationFeature.INDENT_OUTPUT);
    }
    public void showHomework(Long chatId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        List<String> homeW = mapper.readValue(Paths.get("src/main/homework.json").toFile(), List.class);
        StringBuilder all = null;
        for (int i = 0; i < homeW.size(); i++) {
            if (all == null) {
                all = new StringBuilder(i + 1 + ". " + homeW.get(i) +"\n");
            } else if(i == homeW.size() - 1) {
                all.append(i + 1).append(". ").append(homeW.get(i));
            }else {
                all.append(i + 1).append(". ").append(homeW.get(i)).append("\n");
            }
        }
        if(all != null) {
            sendMessage(chatId, String.valueOf(all));
        } else {
            sendMessage(chatId, "Дневник пуст!😭😭😭");

        }
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

    }
    public boolean checkHomework() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        List<String> homeW = mapper.readValue(Paths.get("src/main/homework.json").toFile(), List.class);
        StringBuilder all = null;
        for (int i = 0; i < homeW.size(); i++) {
            if (all == null) {
                all = new StringBuilder(i + 1 + ". " + homeW.get(i) +"\n");
            } else if(i == homeW.size() - 1) {
                all.append(i + 1).append(". ").append(homeW.get(i));
            }else {
                all.append(i + 1).append(". ").append(homeW.get(i)).append("\n");
            }
        }
        return all != null;
    }
    public void writeHomeWork(String homework) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        List<String> homeW = mapper.readValue(Paths.get("src/main/homework.json").toFile(), List.class);
        homeW.add(homework);
        String json = mapper.writeValueAsString(homeW);
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream("src/main/homework.json"),
                StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mapper.disable(SerializationFeature.INDENT_OUTPUT);
    }
    public void sendMessage(Long chatId, String answer) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}