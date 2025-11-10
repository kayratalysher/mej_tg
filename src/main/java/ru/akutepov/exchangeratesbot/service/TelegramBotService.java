package ru.akutepov.exchangeratesbot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.akutepov.exchangeratesbot.entity.ContestResult;
import ru.akutepov.exchangeratesbot.entity.Contests;
import ru.akutepov.exchangeratesbot.entity.Users;
import ru.akutepov.exchangeratesbot.repositry.ContestResultRepository;
import ru.akutepov.exchangeratesbot.repositry.UsersRepositroy;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j

public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${bot.username:}")
    private String botUsername;

    @Value("${bot.token:}")
    private String botToken;

    private final UsersRepositroy usersRepositroy;
    private final ContestResultRepository contestResultRepository;

    // Храним, на каком шаге находится пользователь
    private final java.util.Map<Long, Integer> userStep = new java.util.HashMap<>();
    private final java.util.Map<Long, ContestResult> tempResults = new java.util.HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);



    @PostConstruct
    public void init() {
        System.out.println("=== BOT INITIALIZATION STARTED ===");
        System.out.println("Bot username from config: " + botUsername);
        System.out.println("Bot token length: " + (botToken != null ? botToken.length() : "null"));

        if (botToken == null || botToken.isEmpty() || botToken.equals("your_bot_token_here")) {
            System.err.println("ERROR: Bot token is not configured!");
            return;
        }

        if (botUsername == null || botUsername.isEmpty() || botUsername.equals("your_bot_username_here")) {
            System.err.println("ERROR: Bot username is not configured!");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            System.out.println("✅ SUCCESS: Telegram Bot registered successfully!");
            System.out.println("🤖 Bot: @" + botUsername);
            System.out.println("📡 Bot is listening for messages...");

            // Тестовая отправка сообщения себе (закомментируйте после теста)
            // sendTestMessage();

        } catch (TelegramApiException e) {
            System.err.println("❌ ERROR registering bot: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для тестовой отправки сообщения (раскомментируйте для теста)
    private void sendTestMessage() {
        try {
            SendMessage message = new SendMessage();
            message.setChatId("YOUR_CHAT_ID"); // Замените на ваш chat_id
            message.setText("🤖 Бот сәтті іске қосылды және жұмыс істеуге дайын!");
            execute(message);
            System.out.println("✅ Test message sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error sending test message: " + e.getMessage());
        }
    }

    @Transactional
    public void createOrUpdateUser(Update update) {
        if (update.getMessage() != null) {
            var fromUser = update.getMessage().getFrom();

            var userExis = usersRepositroy.findByUsername(fromUser.getUserName());
            if (userExis.isPresent()) {
                userExis.get().setLastSession(java.time.LocalDateTime.now());
                usersRepositroy.save(userExis.get());
                return;
            }


            usersRepositroy.save(Users.builder()
                    .created(LocalDateTime.now())
                    .email("")
                    .fio(fromUser.getFirstName() + " " + fromUser.getLastName())
                    .firstName(fromUser.getFirstName())
                    .lastName(fromUser.getLastName())
                    .username(fromUser.getUserName())
                    .chatId(update.getMessage().getChatId())
                    .build());
        }


    }


    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("=== NEW UPDATE RECEIVED ===");
        System.out.println("Update ID: " + update.getUpdateId());
        createOrUpdateUser(update);
        if (update.getMessage()!=null && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage().getChatId(), update.getMessage().getText());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(
                    update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getMessage().getMessageId(),
                    update.getCallbackQuery().getData(),
                    update.getCallbackQuery().getId()
            );
        }

        
    }

    private void handleCallbackQuery(Long chatId, Integer messageId, String callbackData, String callbackQueryId) {
        System.out.println("Handling callback: " + callbackData + " from chat: " + chatId);

        answerCallbackQuery(callbackQueryId);
        switch (callbackData) {
            case "main_menu":
                sendWelcomeMessage(chatId);
                break;
            case "active_contests":
                showActiveContests(chatId, messageId);
                break;
            case "contest_details_1":
                showContestDetails(chatId, messageId, 1);
                break;
            case "contest_details_2":
                showContestDetails(chatId, messageId, 2);
                break;
            case "contest_details_3":
                showContestDetails(chatId, messageId, 3);
                break;
            case "participate_contest":
                startParticipation(chatId);
                break;
            case "feedback":
                showFeedbackOptions(chatId, messageId);
                break;
            case "contact_email":
                showContactEmail(chatId, messageId);
                break;
            case "contact_phone":
                showContactPhone(chatId, messageId);
                break;
            case "contact_social":
                showSocialNetworks(chatId, messageId);
                break;
            case "download_contest_1":
                sendContestFile(chatId, 1);
                break;
            case  "BUY_CERTIFICATE":
                SendMessage paymentMessage = new SendMessage();
                paymentMessage.setChatId(chatId.toString());
                paymentMessage.setText("💳 Сіз сертификатты мына сілтеме арқылы сатып ала аласыз:\n" +
                        "https://pay.kaspi.kz/pay/v0iq41rc\n\n" +
                        "📸 Төлем жасаған соң, чекті осы чатқа жіберіңіз.\n" +
                        "Біздің менеджер төлемді растағаннан кейін сіздің сертификатыңыз дайындалады ✅");
                executeMessage(paymentMessage);
                executeMessage(paymentMessage);
                break;

            case "DECLINE_CERTIFICATE":
                SendMessage declineMessage = new SendMessage();
                declineMessage.setChatId(chatId.toString());
                declineMessage.setText("❌ Сатып алудан бас тарттыңыз.");
                executeMessage(declineMessage);
                break;
        }
    }

    private void handleTextMessage(Long chatId, String text) {
        // Если пользователь в процессе заполнения
        if (userStep.containsKey(chatId)) {
            processUserInput(chatId, text);
            return;
        }

        // Дальше старый код
        switch (text) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "/help":
                sendStartButton(chatId);
                break;
            default:
                sendUnknownCommand(chatId);
        }
    }


    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Сәлеметсіз бе, біз — Мәңгілік ел жастары командасымыз.\n" +
                "Сізді не қызықтырады?");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Список активных конкурсов"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton contestsButton = new InlineKeyboardButton();
        contestsButton.setText("📋 Қазір өтіп жатқан байқаулар тізімі");
        contestsButton.setCallbackData("active_contests");
        row1.add(contestsButton);

        // Кнопка "Обратная связь"
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton feedbackButton = new InlineKeyboardButton();
        feedbackButton.setText("📞 Кері байланыс");
        feedbackButton.setCallbackData("feedback");
        row2.add(feedbackButton);

        rows.add(row1);
        rows.add(row2);

        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);

        executeMessage(message);
    }

    private void showActiveContests(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText("🏆 **Активные конкурсы:**\n\n" +
                " \uD83C\uDFA4 *** I Республикалық “Ұлы арман” Махамбет оқулары***\n" +
                " ІІІ Республикалық  «Оян, қазақ!» атты Міржақып оқулары\n" +
                "Выберите конкурс для получения подробной информации:");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки для каждого конкурса
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton contest1 = new InlineKeyboardButton();
        contest1.setText(" Махамбет оқулары");
        contest1.setCallbackData("contest_details_1");
        row1.add(contest1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton contest2 = new InlineKeyboardButton();
        contest2.setText("«Оян, қазақ!»");
        contest2.setCallbackData("contest_details_2");
        row2.add(contest2);

        // Кнопка "Назад"
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("main_menu");
        row5.add(backButton);

        rows.add(row1);
        rows.add(row2);

        rows.add(row5);

        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);
        message.enableMarkdown(true);

        executeEditMessage(message);
    }


    private void showContestDetails(Long chatId, Integer messageId, int contestId) {
        String contestText = "";
        String contestTitle = "";

        switch (contestId) {
            case 1:
                contestTitle = "І Республикалық  «Ұлы арман» атты Махамбет оқулары";
                contestText = """
                📚 Құрметті ұстаздар мен оқушылар!
                
                “MÁŃGILIK EL JASTARY” қоғамдық қорының ұйымдастыруымен
                🎤 I Республикалық “Ұлы арман” Махамбет оқулары басталды!
                
                📅 Жұмыстарды жолдау тегін, қабылдау – 10 қараша 2025 жылға дейін (онлайн).
                
                🎁 Марапаттар:
                🏅 I, II, III орындар — дипломдар
                📜 10 жетекші — Құрмет грамотасы
                🌟 Үздік оқушылар — Алматыдағы марапаттау кешіне шақырылады
                
                📞 Ақпарат: +7 (777) 465 25 94
                
                ✨ Байқауға белсенді қатысыңыз!
                """;
                break;

            case 2:
                contestTitle = "ІІІ Республикалық «Оян, қазақ!» атты Міржақып оқулары";
                contestText = """
                **ІІІ Республикалық «Оян, қазақ!» атты Міржақып оқулары**

                **Сипаттама:**
                Міржақып Дулатұлының мұрасына арналған әдеби байқау.

                **Өткізу мерзімі:**
                📅 1 қараша - 20 желтоқсан 2024 жыл

                **Марапаттар:**
                🏅 1-3 орындар — дипломдар және құнды сыйлықтар
                📜 Үздік жұмыстар — жинақта басылу мүмкіндігі
                """;
                break;

            case 3:
                contestTitle = "✍️ Литературный конкурс";
                contestText = """
                **✍️ Литературный конкурс**

                **Описание:**
                Конкурс для молодых писателей и поэтов. Принимаются рассказы, стихи и эссе.

                **Сроки проведения:**
                📅 1 ноября - 31 декабря 2024 года

                **Призы:**
                🏅 Публикация в литературном сборнике
                🥈 Участие в творческом семинаре
                🥉 Книжные призы

                **Темы:**
                • Будущее Казахстана
                • Семейные ценности
                • Природа и экология
                """;
                break;
        }

        // 1️⃣ Отправляем баннер
        sendContestImage(chatId);

        // 2️⃣ Отправляем файл (если есть)
        sendContestFile(chatId, contestId);

        // 3️⃣ Отправляем текст с кнопками
        SendMessage textMessage = new SendMessage();
        textMessage.setChatId(chatId.toString());
        textMessage.setText(contestText);
        textMessage.enableMarkdown(true);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Участвовать"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton participate = new InlineKeyboardButton();
        participate.setText("✅ Байқауға қатысу");
        participate.setCallbackData("participate_contest");

        row1.add(participate);
        rows.add(row1);

        // Кнопка "Скачать положение" (только для конкурса 1)
        if (contestId == 1) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton downloadButton = new InlineKeyboardButton();
            downloadButton.setText("📥 Ережені жүктеу");
            downloadButton.setCallbackData("download_contest_1");
            row2.add(downloadButton);
            rows.add(row2);
        }

        // Кнопка "Назад к списку"
        List<InlineKeyboardButton> rowBack = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Байқаулар тізіміне оралу");
        backButton.setCallbackData("active_contests");
        rowBack.add(backButton);
        rows.add(rowBack);

        inlineKeyboard.setKeyboard(rows);
        textMessage.setReplyMarkup(inlineKeyboard);

        try {
            execute(textMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendContestImage(Long chatId) {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId.toString());
            photo.setCaption("🎨 Махамбет оқулары - Баннер");

            // Загрузка изображения из ресурсов
            InputStream imageStream = getClass().getClassLoader()
                    .getResourceAsStream("files/Маханбет.jpg");

            if (imageStream != null) {
                InputFile imageFile = new InputFile(imageStream, "makhambet_contest.jpg");
                photo.setPhoto(imageFile);
                execute(photo);
                System.out.println("✅ Изображение отправлено успешно");
            } else {
                System.err.println("❌ Файл изображения не найден в ресурсах: files/Маханбет.jpg");
                // Отправляем сообщение об ошибке
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId.toString());
                errorMessage.setText("❌ Изображение временно недоступно.");
                executeMessage(errorMessage);
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке изображения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendContestFile(Long chatId, int contestId) {
        try {
            SendDocument document = new SendDocument();
            document.setChatId(chatId.toString());
            document.setCaption("📄 Махамбет оқулары - Ережелер\n\n" +
                    "Бұл құжатта сіз таба аласыз:\n" +
                    "• Байқауға қатысу шарттары\n" +
                    "• Жұмыстарды тапсыру тәртібі\n" +
                    "• Бағалау критерийлері\n" +
                    "• Өтініш формасы");

            // Загрузка файла из ресурсов
            InputStream fileStream = getClass().getClassLoader()
                    .getResourceAsStream("files/МАХАМБЕТ ОҚУЛАРЫ ереже.docx");

            if (fileStream != null) {
                InputFile documentFile = new InputFile(fileStream, "makhambet_oregeler.docx");
                document.setDocument(documentFile);
                execute(document);
                System.out.println("✅ Файл отправлен успешно");
            } else {
                System.err.println("❌ Файл документа не найден в ресурсах: files/МАХАМБЕТ ОҚУЛАРЫ ереже.docx");
                // Отправляем сообщение об ошибке
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId.toString());
                errorMessage.setText("❌ Документ временно недоступен.");
                executeMessage(errorMessage);
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendContestFileFromResources(Long chatId, int contestId) {
        try {
            SendDocument document = new SendDocument();
            document.setChatId(chatId.toString());
            document.setCaption("📄 Положение о конкурсе молодых талантов");

            // Чтение файла из ресурсов
            InputStream fileStream = getClass().getClassLoader()
                    .getResourceAsStream("documents/contest_1_regulations.pdf");

            if (fileStream != null) {
                document.setDocument(new InputFile(fileStream, "contest_regulations.pdf"));
                execute(document);
            } else {
                // Если файл не найден в ресурсах
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId.toString());
                errorMessage.setText("❌ Файл временно недоступен. Пожалуйста, попробуйте позже.");
                executeMessage(errorMessage);
            }

        } catch (Exception e) {
            System.err.println("Ошибка при отправке файла из ресурсов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startParticipation(Long chatId) {
        userStep.put(chatId, 1);
        tempResults.put(chatId, new ContestResult());
        sendText(chatId, "Атыңыз-жөніңізді жазыңыз:");
    }

    private void processUserInput(Long chatId, String text) {
        Integer step = userStep.get(chatId);
        if (step == null) return;

        ContestResult result = tempResults.get(chatId);

        switch (step) {
            case 1 -> {
                result.setFullName(text);
                sendText(chatId, "Сыныбыңызды жазыңыз:");
                userStep.put(chatId, 2);
            }
            case 2 -> {
                result.setGrade(text);
                sendText(chatId, "Ұялы телефоныңыз:");
                userStep.put(chatId, 3);
            }
            case 3 -> {
                result.setPhone(text);
                sendText(chatId, "Жетекшіңіздің аты-жөні:");
                userStep.put(chatId, 4);
            }
            case 4 -> {
                result.setMentor(text);
                sendText(chatId, "Мектебіңіз:");
                userStep.put(chatId, 5);
            }
            case 5 -> {
                result.setSchool(text);
                sendText(chatId, "Жұмысыңызды сипаттаңыз (мысалы, шығарма, сурет, видео т.б.):");
                userStep.put(chatId, 6);
            }
            case 6 -> {
                result.setWorkDescription(text);
                result.setChatId(chatId);
                result.setCreatedAt(LocalDateTime.now());

                contestResultRepository.save(result);
                sendContestResultToChannel(result);
                //запуск таймера
                scheduleCertificateMessage(chatId);


                sendText(chatId, """
                    ✅ Рахмет!
                    Сіздің мәліметіңіз сәтті қабылданды.
                    Біз жақында сізбен хабарласамыз.
                    """);

                userStep.remove(chatId);
                tempResults.remove(chatId);
            }
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        executeMessage(message);
    }

    private void sendContestResultToChannel(ContestResult result) {
        Long channelId = -1003235201523L;  // ID твоего канала

        String text = "📢 *Жаңа қатысушы тіркелді!*\n\n" +
                "👤 *Аты-жөні:* " + result.getFullName() + "\n" +
                "🏫 *Мектебі:* " + result.getSchool() + "\n" +
                "📚 *Сыныбы:* " + result.getGrade() + "\n" +
                "📞 *Ұялы телефон:* " + result.getPhone() + "\n" +
                "👩‍🏫 *Жетекшісі:* " + result.getmentor() + "\n" +
                "📝 *Жұмысы:* " + result.getWorkDescription() + "\n\n" +
                "📅 Уақыты: " + LocalDateTime.now();

        SendMessage message = new SendMessage();
        message.setChatId(channelId.toString());
        //message.setChatId("@Работы участников (Мәңгілк ел жастары)");
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void scheduleCertificateMessage(Long chatId) {
        Runnable task = () -> {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🎉 Құттықтаймыз! Сіздің сертификатыңыз дайын ✅\n\n" +
                    "📜 Сертификатты алу үшін біздің сайтқа кіріңіз немесе хабарласыңыз: +7 777 123 4567");

            // Создание кнопок
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton buyButton = new InlineKeyboardButton();
            buyButton.setText("Сатып алу"); // Кнопка "Купить сертификат"
            buyButton.setCallbackData("BUY_CERTIFICATE"); // Callback data при нажатии

            InlineKeyboardButton declineButton = new InlineKeyboardButton();
            declineButton.setText("Бас тарту"); // Кнопка "Отказаться от покупки"
            declineButton.setCallbackData("DECLINE_CERTIFICATE");

            // Добавляем кнопки в одну строку
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buyButton);
            row.add(declineButton);

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(row);

            keyboardMarkup.setKeyboard(rows);
            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        };

        // Отправить сообщение через 5 минут
        scheduler.schedule(task, 5, TimeUnit.MINUTES);
    }




    private void showParticipationInfo(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText("**📝 Байқауға қалай қатысуға болады:**\n\n" +
                "1. **Қолайлы байқауды таңдаңыз**\n" +
                "2. **Жұмысты дайындаңыз** талаптарға сәйкес\n" +
                "3. **Өтінімді толтырыңыз**\n" +
                "4. **Материалдарды жіберіңіз** қабылдау мерзімінен бұрын\n\n" +
                "**Сұрақтар бойынша байланыс:**\n" +
                "📧 konkurs@manglik-el.kz\n" +
                "📞 +7 777 123 4567\n\n" +
                "Сіздің шығармашылығыңызды асыға күтеміз! 🎉");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Назад"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Байқауларға оралу");
        backButton.setCallbackData("active_contests");
        row1.add(backButton);

        rows.add(row1);

        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);
        message.enableMarkdown(true);

        executeEditMessage(message);
    }

    private void showFeedbackOptions(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText("**📞 Кері байланыс**\n\n" +
                "Сұрақтар, ұсыныстар немесе пікірлеріңізді бөлісуге әрдайым дайынбыз!\n\n" +
                "Қолайлы байланыс тәсілін таңдаңыз:");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка Email
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton emailButton = new InlineKeyboardButton();
        emailButton.setText("📧 Email");
        emailButton.setCallbackData("contact_email");
        row1.add(emailButton);

        // Кнопка Телефон
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton phoneButton = new InlineKeyboardButton();
        phoneButton.setText("📞 Телефон");
        phoneButton.setCallbackData("contact_phone");
        row2.add(phoneButton);

        // Кнопка Соцсети
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton socialButton = new InlineKeyboardButton();
        socialButton.setText("🌐 Әлеуметтік желілер");
        socialButton.setCallbackData("contact_social");
        row3.add(socialButton);

        // Кнопка "Назад"
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Басты менюге қайту");
        backButton.setCallbackData("main_menu");
        row4.add(backButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);
        message.enableMarkdown(true);

        executeEditMessage(message);
    }

    private void showContactEmail(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText("**📧 Электрондық пошта:**\n\n" +
                "Жалпы сұрақтар үшін:\n" +
                "📧 info@manglik-el.kz\n\n" +
                "Байқаулар бойынша:\n" +
                "📧 konkurs@manglik-el.kz\n\n" +
                "Серіктестік бойынша:\n" +
                "📧 partnership@manglik-el.kz\n\n" +
                "24 сағат ішінде жауап береміз!");

        addBackToFeedbackKeyboard(message);
        executeEditMessage(message);
    }

    private void showContactPhone(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        // Телефон
        message.setText("**📞 Телефон нөмірлері:**\n\n" +
                "Жалпы ақпарат:\n" +
                "📞 +7 7172 123 456\n\n" +
                "Байқаулар бөлімі:\n" +
                "📞 +7 777 123 4567\n\n" +
                "Жұмыс уақыты:\n" +
                "🕒 Дс-Пт: 9:00-18:00\n" +
                "🕒 Сен: 10:00-16:00\n" +
                "🌅 Жк: демалыс");

        addBackToFeedbackKeyboard(message);
        executeEditMessage(message);
    }

    private void showSocialNetworks(Long chatId, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText("**🌐 Біз әлеуметтік желілерде:**\n\n" +
                "Instagram:\n" +
                "📷 @manglik_el_jastary\n\n" +
                "Facebook:\n" +
                "👥 Мангилик Ел Жастары\n\n" +
                "Telegram канал:\n" +
                "📢 @manglik_el_news\n\n" +
                "YouTube:\n" +
                "🎥 Мангилик Ел Жастары\n\n" +
                "Жаңалықтардан қалмаңыз!");

        addBackToFeedbackKeyboard(message);
        executeEditMessage(message);
    }

    private void addBackToFeedbackKeyboard(EditMessageText message) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Байланысқа қайту");
        backButton.setCallbackData("feedback");
        row1.add(backButton);

        rows.add(row1);
        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);
        message.enableMarkdown(true);
    }

    private void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void executeEditMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Старые методы для Reply клавиатуры (оставьте для совместимости)
    private void sendActiveContests(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🏆 Қазіргі байқаулар:\n\n" +
                "1. Жас таланттар конкурсы\n\n" +
                "2. Фотоконкурс «Менің өлкем»\n\n" +
                "3. Әдеби конкурс\n\n" +
                "Қатысу үшін байқауды таңдап, нұсқауларды орындаңыз.");
        executeMessage(message);
    }

    private void sendFeedbackInfo(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📞 Кері байланыс:\n\n" +
                "Email: manglik-el@example.com\n" +
                "Телефон: +7 777 123 4567\n" +
                "Сіздердің сұрақтарыңыз бен ұсыныстарыңызға әрқашан қуаныштымыз!");
        executeMessage(message);
    }


    private void sendStartButton(Long chatId) {
        System.out.println("Sending start button to chat: " + chatId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👋 Қош келдіңіз! Жұмысты бастау үшін 'Старт' батырмасын басыңыз:");

        ReplyKeyboardMarkup keyboardMarkup = createKeyboard(List.of("🚀 Старт"));
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void sendUnknownCommand(Long chatId) {
        System.out.println("Sending unknown command response to chat: " + chatId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("❌ Белгісіз команда. Жұмысты бастау үшін /start пайдаланыңыз.");
        executeMessage(message);
    }

    private ReplyKeyboardMarkup createKeyboard(List<String> buttons) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        buttons.forEach(row::add);
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}