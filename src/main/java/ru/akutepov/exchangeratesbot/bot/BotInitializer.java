package ru.akutepov.exchangeratesbot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.TelegramBot;

@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramBot telegramBot;
    private final LongPollingBot longPollingBot;
    private final TelegramBotsApi telegramBotsApi;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            telegramBotsApi.registerBot(longPollingBot);
            System.out.println("Telegram бот успешно запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}