package ru.akutepov.exchangeratesbot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.akutepov.exchangeratesbot.adapter.DiplomGenerateAdapter;
import ru.akutepov.exchangeratesbot.entity.ContestResult;
import ru.akutepov.exchangeratesbot.entity.ParticipantStatus;
import ru.akutepov.exchangeratesbot.entity.Users;
import ru.akutepov.exchangeratesbot.repositry.ContestResultRepository;
import ru.akutepov.exchangeratesbot.repositry.UsersRepositroy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;


@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
@ConditionalOnProperty(name = "bot.enabled", havingValue = "true")
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("mangilik_el_jastary_mektep_bot")
    private String botUsername;

    @Value("8584001024:AAG_nL0hK4LYTUZdrVAUeqdH604boqmk5CM")
    private String botToken;

    private final UsersRepositroy usersRepositroy;
    private final ContestResultRepository contestResultRepository;
    private final DiplomGenerateAdapter diplomGenerateAdapter;
    private final FileService fileService;
    private final Map<Long, Integer> userStep = new ConcurrentHashMap<>();
    private final Map<Long, ContestResult> tempResults = new ConcurrentHashMap<>();

    // ====== SERVICE ONLY — NO BOT STARTUP HERE ======


    public void onUpdateReceived(Update update) {
        if (update == null) return;

        log.info("Update received: {}", update.getUpdateId());

        createOrUpdateUser(update);

        if (update.hasMessage()) {
            var message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasText()) {
                handleTextMessage(chatId, message.getText());
            } else if (message.hasDocument()) {
                handleFileMessage(chatId, message.getDocument().getFileId());
            } else if (message.hasPhoto()) {
                String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
                handleFileMessage(chatId, fileId);
            }
        }

        if (update.hasCallbackQuery()) {
            var q = update.getCallbackQuery();
            handleCallbackQuery(
                    q.getMessage().getChatId(),
                    q.getMessage().getMessageId(),
                    q.getData(),
                    q.getId()
            );
        }
    }


    public class HttpClientHelper {

        public static byte[] downloadFile(String url, Map<String, String> params) throws Exception {

            StringBuilder query = new StringBuilder("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (query.length() > 1) query.append("&");
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + query))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ошибка API: " + response.statusCode());
            }

            return response.body();
        }


    }

    // ===================== USER =====================

    private void createOrUpdateUser(Update update) {
        if (update.getMessage() == null) return;

        var from = update.getMessage().getFrom();

        usersRepositroy.findByUsername(from.getUserName()).ifPresentOrElse(
                user -> {
                    user.setLastSession(LocalDateTime.now());
                    usersRepositroy.save(user);
                },
                () -> usersRepositroy.save(
                        Users.builder()
                                .created(LocalDateTime.now())
                                .email("")
                                .fio(from.getFirstName() + " " + from.getLastName())
                                .firstName(from.getFirstName())
                                .lastName(from.getLastName())
                                .username(from.getUserName())
                                .chatId(update.getMessage().getChatId())
                                .build()
                )
        );
    }

    // ===================== MESSAGES =====================

    private void handleTextMessage(Long chatId, String text) {

        if (userStep.containsKey(chatId)) {
            processUserInput(chatId, text);
            return;
        }

        switch (text) {
            case "/start" -> sendWelcomeMessage(chatId);
            case "/help" -> sendText(chatId, "Команды: /start");
            case "/test_channel" -> testChannel();
            default -> sendText(chatId, "Неизвестная команда");
        }
    }
    private void sendText(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (Exception e) {
            log.error("Send text error", e);
        }
    }


    private void showDiplomaButtons(Long chatId, Integer messageId, String data) {

        Long participantId = Long.parseLong(
                data.replace("payment_ok_", "")
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                List.of(button("🥇 1 дәрежелі", "set_diploma_1_" + participantId)),
                List.of(button("🥈 2 дәрежелі", "set_diploma_2_" + participantId)),
                List.of(button("🥉 3 дәрежелі", "set_diploma_3_" + participantId))
        ));

        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setReplyMarkup(keyboard);

        try {
            execute(edit);
        } catch (Exception e) {
            log.error("Edit keyboard error", e);
        }
    }


    private void handleCallbackQuery(Long chatId, Integer messageId, String data, String callbackId) {
        if (data.startsWith("payment_ok_")) {
            showDiplomaButtons(chatId, messageId, data);
            return;
        }

        if (data.startsWith("payment_failed_")) {
            Long id = Long.parseLong(data.replace("payment_failed_", ""));
            handlePaymentFailed(id);
            return;
        }
        answerCallbackQuery(callbackId);

        switch (data) {
            case "main_menu" -> sendWelcomeMessage(chatId);
            case "active_contests" -> showActiveContests(chatId, messageId);
            case "contest_details_1" -> showContestDetails(chatId, messageId, 1);
            case "contest_details_2" -> showContestDetails(chatId, messageId, 2);
            case "participate_contest" -> startParticipation(chatId);
            case "download_contest_1" -> sendContestFile(chatId);
        }

        // Обработка выбора диплома
        if (data.startsWith("set_diploma_")) {
            handleSetDiploma(data);
            return; // больше ничего не делаем для этого колбэка
        }

        if (data.startsWith("certificate_paid_")) {
            Long id = Long.parseLong(data.replace("certificate_paid_", ""));
            handleCertificatePaidById(id);
            return;
        }

        if (data.startsWith("certificate_reject_")) {
            Long id = Long.parseLong(data.replace("certificate_reject_", ""));
            handleRejectById(id);
            return;
        }
    }

    private void handlePaymentFailed(Long id) {
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) return;

        // меняем статус (можно REJECTED или новый)
        r.setStatus(ParticipantStatus.REJECTED);
        contestResultRepository.save(r);

        // обновляем сообщение в группе (убираем кнопки)
        updateGroupMessage(r);

        // ❗ сообщение пользователю
        sendText(
                r.getChatId(),
                "❌ Төлем өтпеді.\n\n" +
                        "Мүмкін қате болды немесе төлем расталмады.\n" +
                        "Қайта төлем жасап көріңіз немесе администраторға хабарласыңыз 🙏"
        );
    }


    private void handleCertificatePaidById(Long id) {
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) return;

        r.setStatus(ParticipantStatus.PAID_PENDING);
        contestResultRepository.save(r);

        updateGroupMessage(r);

        sendText(r.getChatId(),
                "⏳ Төлем қабылданды.\n" +
                        "Тексерілген соң сертификат жіберіледі 📜");
    }

    private void handleRejectById(Long id) {
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) return;

        r.setStatus(ParticipantStatus.REJECTED);
        contestResultRepository.save(r);

        updateGroupMessage(r);
        sendText(r.getChatId(), "Жарайды 👍 Егер ойыңыз өзгерсе — хабарласыңыз");
    }

    private void handleSetDiploma(String data) {
        // data = set_diploma_1_123
        String[] parts = data.split("_");
        int diplomaCategory = Integer.parseInt(parts[2]);
        Long participantId = Long.parseLong(parts[3]);

        contestResultRepository.findById(participantId).ifPresent(r -> {
            r.setDiplomaCategory(diplomaCategory); // сохраняем категорию
            contestResultRepository.save(r);

            // Запрос сертификата через API
            fetchAndSendCertificate(r);
        });
    }
    private int resolveScoreByDiploma(int diplomaCategory) {
        return switch (diplomaCategory) {
            case 1 -> 100;
            case 2 -> 70;
            case 3 -> 50;
            default -> 0;
        };
    }

    private void fetchAndSendCertificate(ContestResult r) {
        try {
            String typeHandler=r.getDiplomaCategory() ==1 ? null : r.getDiplomaCategory()==2 ? "SECOND" : "THIRD";
            byte[] diplomaBytes = diplomGenerateAdapter.downloadDiploma(r.getFullName(),typeHandler,60);

            if (diplomaBytes == null || diplomaBytes.length == 0) {
                throw new RuntimeException("Диплом пришёл пустой");
            }

            InputStream certificateStream = new ByteArrayInputStream(diplomaBytes);

            execute(new SendDocument(
                    r.getChatId().toString(),
                    new InputFile(certificateStream, "diplom.pdf")
            ));
            //диплом руководителю
            String typeHandlerHead=r.getDiplomaCategory() ==1 ? null : r.getDiplomaCategory()==2 ? "SECOND" : "THIRD";
            byte[] diplomaBytesHead = diplomGenerateAdapter.downloadDiploma(r.getMentor(),typeHandler,60);

            if (diplomaBytesHead == null || diplomaBytesHead.length == 0) {
                throw new RuntimeException("Диплом пришёл пустой");
            }

            InputStream certificateStreamHead = new ByteArrayInputStream(diplomaBytesHead);

            execute(new SendDocument(
                    r.getChatId().toString(),
                    new InputFile(certificateStreamHead, "algys_xat.pdf")
            ));

            sendText(r.getChatId(), "📜 Диплом дайын!");

            // меняем статус
            r.setStatus(ParticipantStatus.APPROVED);
            contestResultRepository.save(r);

            // убираем кнопки в группе
            if (r.getChannelMessageId() != null) {
                EditMessageText edit = new EditMessageText();
                edit.setChatId("-1003235201523");
                edit.setMessageId(r.getChannelMessageId());
                edit.setText(buildGroupText(r));
                edit.setReplyMarkup(null);
                execute(edit);
            }

        } catch (Exception e) {
            log.error("❌ Diploma download error", e);
            sendText(r.getChatId(), "❌ Диплом жүктелмеді, кейінірек қайталап көріңіз");
        }
    }

    private void testChannel() {
        try {
            Message m = execute(
                    new SendMessage("-1003235201523", "TEST CHANNEL MESSAGE")
            );
            log.error("🧪 TEST SENT, messageId={}", m.getMessageId());
        } catch (Exception e) {
            log.error("🧪 TEST FAILED", e);
        }
    }

    private void showContestDetails(Long chatId, Integer messageId, int contestId) {

        // 1) редактируем старое сообщение, чтобы убрать меню
        EditMessageText progress = new EditMessageText();
        progress.setChatId(chatId.toString());
        progress.setMessageId(messageId);
        progress.setText("Жүктелуде...");
        executeEditMessage(progress);

        // 2) Подгружаем афишу
        InputStream imageStream = getClass().getClassLoader()
                .getResourceAsStream("files/mukagali.jpg");

        if (imageStream == null) {
            sendText(chatId, "❌ Афиша табылмады");
            return;
        }

        String caption =
                "📘 МАҚАТАЕВ ОҚУЛАРЫ\n\n" +
                        "МҰҚАҒАЛИ МАҚАТАЕВТЫҢ ТУҒАНЫНА  95 ЖЫЛ ТОЛУЫНА ОРАЙ ӨТКІЗІЛЕТІН\n\n" +
                        "• Барлық қатысушыларға «MÁŃGLIK EL JASTARY»қоғамдық қорының арнайы лауреаттық дипломдары беріледі.\n" +
                        "• Шәкірт дайындаған жетекшілерге «Алғыс хат»табысталады.\n" +
                        "• Үздік деп танылған 100 оқушыға брендталған «Premium» бокс беріледі.\n\n" +
                        "“Бас жүлде” бір жылға шәкіртақы!\n" +
                        "Жетекшісіне “Құрмет грамотасы”\n" +
                        "“Бас жүлде” бір жылға шәкіртақы!\n" +
                        "“МҰҚАҒАЛИ МАҚАТАЕВ 95 жыл” медалі мен куәлігі салтанатты түрде табысталады.\n" +
                        "Оқушылар мен жетекшілерге Алматы қаласының танымдық жерлеріне  саяхат.\n\n" +
                        "Қатысу үшін басыңыз:";

        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId.toString());
        photo.setPhoto(new InputFile(imageStream, "mukagali.jpg"));
        photo.setCaption(caption);

        // 3) Добавляем кнопки
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                List.of(button("✅ Қатысу", "participate_contest")),
                List.of(button("📄 Ереже", "download_contest_1")),
                List.of(button("⬅ Артқа", "active_contests"))
        ));
        photo.setReplyMarkup(keyboard);

        // 4) Отправляем афишу + кнопки
        try {
            execute(photo);
        } catch (Exception e) {
            log.error("Send photo error", e);
        }
    }


    // ===================== PARTICIPATION =====================

    private void startParticipation(Long chatId) {
        userStep.put(chatId, 1);
        tempResults.put(chatId, new ContestResult());
        sendText(chatId, "Атыңыз-жөніңіз:");
    }

    private void processUserInput(Long chatId, String text) {
        ContestResult result = tempResults.get(chatId);
        Integer step = userStep.get(chatId);

        if (result == null || step == null) return;

        switch (step) {
            case 1 -> { result.setFullName(text); userStep.put(chatId, 2); sendText(chatId, "Сыныбыңыз:"); }
            case 2 -> { result.setGrade(text); userStep.put(chatId, 3); sendText(chatId, "Телефон:"); }
            case 3 -> { result.setPhone(text); userStep.put(chatId, 4); sendText(chatId, "Жетекші аты:"); }
            case 4 -> { result.setMentor(text); userStep.put(chatId, 5); sendText(chatId, "Мектеп:"); }
            case 5 -> {
                result.setSchool(text);
                userStep.put(chatId, 6);  // бот ждёт файл
                sendText(chatId, "Жұмысыңызды жіберіңіз (файл түрінде):");
                // НЕ сохраняем в БД на этом шаге
            }
            case 6 -> {
                sendText(chatId, "Жұмысыңызды жіберіңіз (файл түрінде):");
            }
        }
    }

    private void handleFileMessage(Long chatId, String fileId) {
        Integer step = userStep.get(chatId);
        ContestResult result = tempResults.get(chatId);

        if (step == null || result == null) return;

        if (step == 6) {
            try {
                // Получаем файл
                var file = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
                String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();

                String originalFileName = file.getFilePath();
                String extension = "dat";
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = originalFileName.substring(dotIndex + 1);
                }
                String savedFileName = result.getFullName().replaceAll("\\s+", "_") + "_work." + extension;

                // Сохраняем в объекте
                result.setWorkFileId(fileId);
                result.setWorkFileName(savedFileName);
                result.setCreatedAt(LocalDateTime.now());
                result.setChatId(chatId);
                result.setStatus(ParticipantStatus.NEW);

                // Сохраняем результат
                ContestResult saved = contestResultRepository.save(result);

                // 🔹 Отправка файла в канал
                SendDocument sendDoc = new SendDocument();
                sendDoc.setChatId("-1003235201523");
                sendDoc.setDocument(new InputFile(new java.net.URL(fileUrl).openStream(), savedFileName));
                execute(sendDoc);

                // 🔹 Отправка отдельного текстового сообщения с кнопками
                SendMessage msg = new SendMessage("-1003235201523", buildGroupText(saved));
                Message textMessage = execute(msg);

                saved.setChannelMessageId(textMessage.getMessageId());
                contestResultRepository.save(saved);

                // Подтверждение участнику
                sendText(chatId, "✅ Мәлімет сақталды, рахмет!");
                sendText(chatId, "✔ Жұмысыңыз қабылданды!\n📜 Сертификат 2–3 сағат ішінде дайын болады.");

                startCertificateTimer(saved.getId(), chatId);

                userStep.remove(chatId);
                tempResults.remove(chatId);

            } catch (Exception e) {
                log.error("File send to channel error", e);
                sendText(chatId, "❌ Жұмысыңызды жіберу кезінде қате пайда болды, кейінірек қайта көріңіз.");
            }
        }
    }


    private void sendContestResultToChannelTextOnly(ContestResult r) {
        SendMessage msg = new SendMessage();
        msg.setChatId("-1003235201523");
        msg.setText(buildGroupText(r));

        try {
            Message sent = execute(msg);
            if (sent != null) {
                r.setChannelMessageId(sent.getMessageId());
                contestResultRepository.save(r);
            }
        } catch (Exception e) {
            log.error("Send text to channel error", e);
        }
    }

    private InputStream fileIdToInputStream(String fileId) throws Exception {
        var file = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
        var fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
        return new java.net.URL(fileUrl).openStream();
    }



    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private void startCertificateTimer(Long contestResultId, Long chatId){
        scheduler.schedule(() -> {

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText(
                    "📜 Сертификат дайын!\n\n" +
                            "Егер сертификатты алғыңыз келсе,\n" +
                            "төлем жасап сатып ала аласыз \n" +
                            "төлем жасаганда комментриге " + contestResultId +
                            " санын жіберуіңізді сураймыз 👇"
            );
            //String payUrl = "https://pay.example.com/certificate?chatId=" + chatId;
            String payUrl = "https://pay.kaspi.kz/pay/v0iq41rc";

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                    List.of(payUrlButton("💳 Сертификатты төлеу", payUrl)),
                    List.of(callbackButton(
                            "✅ Сертификат төленді",
                            "certificate_paid_" + contestResultId
                    )),
                    List.of(callbackButton(
                            "❌ Бас тарту",
                            "certificate_reject_" + contestResultId
                    ))
            ));

            msg.setReplyMarkup(keyboard);
            executeMessage(msg);

        }, 1, TimeUnit.MINUTES);
    }

    private void updateGroupMessage(ContestResult r) {
        if (r == null || r.getId() == null) return;

        ContestResult fresh = contestResultRepository.findById(r.getId()).orElse(null);
        if (fresh == null || fresh.getChannelMessageId() == null) return;

        log.info("⚠ updateGroupMessage: channelMessageId={}", fresh.getChannelMessageId());

        try {
            switch (fresh.getStatus()) {

                case PAID_PENDING -> {
                    // ✅ меняем ТОЛЬКО кнопки
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                            List.of(
                                    button("💳 Оплата прошла", "payment_ok_" + fresh.getId()),
                                    button("❌ Оплата не прошла", "payment_failed_" + fresh.getId())
                            )
                    ));

                    EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                    edit.setChatId("-1003235201523");
                    edit.setMessageId(fresh.getChannelMessageId());
                    edit.setReplyMarkup(keyboard);

                    execute(edit);
                    log.info("✅ GROUP KEYBOARD UPDATED (PAID_PENDING)");
                }

                case REJECTED -> {
                    // ✅ меняем текст и убираем кнопки
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId("-1003235201523");
                    edit.setMessageId(fresh.getChannelMessageId());
                    edit.setText(buildGroupText(fresh));
                    edit.setReplyMarkup(null);

                    execute(edit);
                    log.info("✅ GROUP MESSAGE UPDATED (REJECTED)");
                }

                default -> {
                    // ничего не делаем
                }
            }
        } catch (Exception e) {
            log.error("❌ updateGroupMessage FAILED", e);
        }
    }

    private InlineKeyboardButton payUrlButton(String text, String url) {
        InlineKeyboardButton b = new InlineKeyboardButton(text);
        b.setText(text);
        b.setUrl(url); // ⚠️ именно URL, не callback
        return b;
    }

    private InlineKeyboardButton callbackButton(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(data);
        return b;
    }

    // ===================== FILE =====================

    private void sendContestFile(Long chatId) {
        UUID erezheDocId = UUID.fromString("e631fd99-3d0e-4b70-9663-0bb7d16eeab2");
        var erezheBytes = fileService.downloadFileBytes(erezheDocId);
        if (erezheBytes != null) {
            try (InputStream erezheStream = new ByteArrayInputStream(erezheBytes)) {
                execute(new SendDocument(chatId.toString(),
                        new InputFile(erezheStream, "makataev_rules.docx")));
                return;
            } catch (Exception e) {
                log.error("File send error", e);
            }

        }
    }


    // ===================== UI HELPERS =====================

    private void sendWelcomeMessage(Long chatId) {
        SendMessage msg = new SendMessage(chatId.toString(),
                "Сәлеметсіз бе! " +
                        "\uD83C\uDFC6 Өтіп жатқан байкаулар:\n" +
                        "\n" +
                        " \uD83C\uDFA4  МАҚАТАЕВ ОҚУЛАРЫ \n" +
                        "\n" +
                        "Толықырақ білу үшін байқауды таңдаңыз:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                List.of(button("📋 Байқаулар", "active_contests"))
        ));

        msg.setReplyMarkup(keyboard);
        executeMessage(msg);
    }

    private void showActiveContests(Long chatId, Integer messageId) {
        EditMessageText msg = new EditMessageText();
        msg.setChatId(chatId.toString());
        msg.setMessageId(messageId);
        msg.setText("Байқаулар тізімі:");

        msg.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(button("МАҚАТАЕВ ОҚУЛАРЫ", "contest_details_1"))
        )));

        executeEditMessage(msg);
    }

    private InlineKeyboardButton button(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton(text);
        b.setCallbackData(data);
        return b;
    }

    // ===================== UTILS =====================

    private void answerCallbackQuery(String id) {
        try { execute(new AnswerCallbackQuery(id)); }
        catch (Exception e) { log.error("Callback reply error", e); }
    }

    private void executeMessage(SendMessage msg) {
        try { execute(msg); }
        catch (Exception e) { log.error("Message error", e); }
    }

    private void executeEditMessage(EditMessageText msg) {
        try { execute(msg); }
        catch (Exception e) { log.error("Edit error", e); }
    }

    private String statusText(ParticipantStatus status) {
        return switch (status) {
            case NEW -> "🆕 Жаңа қатысушы";
            case WANT_TO_BUY -> "🛒 Сертификат алғысы келеді";
            case PAID_PENDING -> "⏳ Төлем тексерілуде";
            case APPROVED -> "✅ Төлем расталды";
            case REJECTED -> "❌ Бас тартты";
        };
    }

    private String buildGroupText(ContestResult r) {
        return
                "📢 Жаңа қатысушы\n\n" +
                        "ID: " + r.getId() + "\n" +
                        "👤 " + r.getFullName() + "\n" +
                        "🏫 " + r.getSchool() + "\n" +
                        "📚 " + r.getGrade() + "\n" +
                        "📞 " + r.getPhone() + "\n" +
                        "👩‍🏫 " + r.getMentor() + "\n\n" +
                        "📌 Статус: " + statusText(r.getStatus());
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }
}
