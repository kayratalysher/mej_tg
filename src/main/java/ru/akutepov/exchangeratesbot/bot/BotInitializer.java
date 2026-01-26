package ru.akutepov.exchangeratesbot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramBotsApi telegramBotsApi;
    private final List<LongPollingBot> bots;

    @EventListener(ContextRefreshedEvent.class)
    public void init() throws TelegramApiException {
        for (LongPollingBot bot : bots) {
            telegramBotsApi.registerBot(bot);
            System.out.println("Бот запущен: " + bot.getBotUsername());
        }
    }
}
