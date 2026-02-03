package ru.akutepov.exchangeratesbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import ru.akutepov.exchangeratesbot.adapter.MinioAdapter;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.entity.ContestResult;
import ru.akutepov.exchangeratesbot.entity.ContestType;
import ru.akutepov.exchangeratesbot.entity.ParticipantStatus;
import ru.akutepov.exchangeratesbot.entity.Users;
import ru.akutepov.exchangeratesbot.repositry.ContestResultRepository;
import ru.akutepov.exchangeratesbot.repositry.UsersRepositroy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.util.Map;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;


@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("mangilik_el_jastary_mektep_bot")
    private String botUsername;

    //@Value("8584001024:AAG_nL0hK4LYTUZdrVAUeqdH604boqmk5CM")
    @Value("${bots.mektep.token:8584001024:AAG_nL0hK4LYTUZdrVAUeqdH604boqmk5CM}")
    private String botToken;

    private final UsersRepositroy usersRepositroy;
    private final ContestResultRepository contestResultRepository;
    private final DiplomGenerateAdapter diplomGenerateAdapter;
    private final MinioAdapter minioAdapter;
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
                handleFileMessage(chatId, message.getDocument().getFileId(), message.getDocument().getFileSize(), message.getDocument().getMimeType());
            } else if (message.hasPhoto()) {
                String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
                handleFileMessage(chatId, fileId, null, null);
            } else if (message.hasVideo()) {
                handleFileMessage(chatId, message.getVideo().getFileId(), message.getVideo().getFileSize(), message.getVideo().getMimeType());
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
        log.info("📨 handleTextMessage | chatId={}, text={}", chatId, text);

        if (userStep.containsKey(chatId)) {
            log.info("📝 User in registration flow | chatId={}, step={}", chatId, userStep.get(chatId));
            processUserInput(chatId, text);
            return;
        }

        log.info("⚡ Processing command: {}", text);
        switch (text) {
            case "/start" -> {
                log.info("🚀 /start command | chatId={}", chatId);
                sendWelcomeMessage(chatId);
            }
            case "/help" -> {
                log.info("❓ /help command | chatId={}", chatId);
                sendText(chatId, "Команды: /start");
            }
            case "/test_channel" -> {
                log.info("🧪 /test_channel command");
                testChannel();
            }
            default -> {
                log.warn("⚠️ Unknown command: {} | chatId={}", text, chatId);
                sendText(chatId, "Неизвестная команда");
            }
        }
    }
    private void sendText(Long chatId, String text) {
        log.info("📤 sendText | chatId={}, text={}", chatId, text);
        try {
            execute(new SendMessage(chatId.toString(), text));
            log.info("✅ Text sent successfully | chatId={}", chatId);
        } catch (Exception e) {
            log.error("❌ Send text error | chatId={}", chatId, e);
        }
    }


    private void showDiplomaButtons(Long chatId, Integer messageId, String data) {
        log.info("🎓 showDiplomaButtons | chatId={}, messageId={}, data={}", chatId, messageId, data);

        Long participantId = Long.parseLong(
                data.replace("payment_ok_", "")
        );
        log.info("👤 Parsed participantId={}", participantId);

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
            log.info("✅ Diploma buttons shown successfully | participantId={}", participantId);
        } catch (Exception e) {
            log.error("❌ Edit keyboard error | chatId={}, messageId={}", chatId, messageId, e);
        }
    }


    private void handleCallbackQuery(Long chatId, Integer messageId, String data, String callbackId) {
        log.info("🔔 handleCallbackQuery | chatId={}, messageId={}, data={}, callbackId={}", chatId, messageId, data, callbackId);

        if (data.startsWith("payment_ok_")) {
            log.info("💳 Payment OK callback | data={}", data);
            showDiplomaButtons(chatId, messageId, data);
            return;
        }

        if (data.startsWith("payment_failed_")) {
            Long id = Long.parseLong(data.replace("payment_failed_", ""));
            log.info("❌ Payment failed callback | id={}", id);
            handlePaymentFailed(id);
            return;
        }
        answerCallbackQuery(callbackId);

        log.info("🎯 Processing callback data: {}", data);
        switch (data) {
            case "main_menu" -> {
                log.info("🏠 Main menu callback");
                sendWelcomeMessage(chatId);
            }
            case "active_contests" -> {
                log.info("📋 Active contests callback");
                showActiveContests(chatId, messageId);
            }
            case "contest_details_1" -> {
                log.info("📖 Contest details 1 callback");
                showContestDetails(chatId, messageId, 1);
            }
            case "contest_details_2" -> {
                log.info("📖 Contest details 2 callback");
                showContestDetails(chatId, messageId, 2);
            }
            case "participate_contest" -> {
                log.info("✍️ Participate contest callback");
                startParticipation(chatId);
            }
            case "download_contest_1" -> {
                log.info("📥 Download contest file callback");
                sendContestFile(chatId);
            }
        }

        // Обработка выбора диплома
        if (data.startsWith("set_diploma_")) {
            log.info("🎓 Set diploma callback | data={}", data);
            handleSetDiploma(data);
            return; // больше ничего не делаем для этого колбэка
        }

        if (data.startsWith("certificate_paid_")) {
            Long id = Long.parseLong(data.replace("certificate_paid_", ""));
            log.info("✅ Certificate paid callback | id={}", id);
            handleCertificatePaidById(id);
            return;
        }

        if (data.startsWith("certificate_reject_")) {
            Long id = Long.parseLong(data.replace("certificate_reject_", ""));
            log.info("🚫 Certificate reject callback | id={}", id);
            handleRejectById(id);
            return;
        }
    }

    private void handlePaymentFailed(Long id) {
        log.info("💔 handlePaymentFailed | id={}", id);
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) {
            log.warn("⚠️ ContestResult not found | id={}", id);
            return;
        }

        log.info("🔄 Changing status to REJECTED | id={}", id);
        // меняем статус (можно REJECTED или новый)
        r.setStatus(ParticipantStatus.REJECTED);
        contestResultRepository.save(r);

        // обновляем сообщение в группе (убираем кнопки)
        log.info("📝 Updating group message | id={}", id);
        updateGroupMessage(r);

        // ❗ сообщение пользователю
        log.info("📨 Sending payment failed message to user | chatId={}", r.getChatId());
        sendText(
                r.getChatId(),
                "❌ Төлем өтпеді.\n\n" +
                        "Мүмкін қате болды немесе төлем расталмады.\n" +
                        "Қайта төлем жасап көріңіз немесе администраторға хабарласыңыз 🙏"
        );
        log.info("✅ Payment failed handled | id={}", id);
    }


    private void handleCertificatePaidById(Long id) {
        log.info("💰 handleCertificatePaidById | id={}", id);
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) {
            log.warn("⚠️ ContestResult not found | id={}", id);
            return;
        }

        log.info("🔄 Changing status to PAID_PENDING | id={}", id);
        r.setStatus(ParticipantStatus.PAID_PENDING);
        contestResultRepository.save(r);

        log.info("📝 Updating group message | id={}", id);
        updateGroupMessage(r);

        log.info("📨 Sending confirmation message to user | chatId={}", r.getChatId());
        sendText(r.getChatId(),
                "⏳ Төлем қабылданды.\n" +
                        "Тексерілген соң сертификат жіберіледі 📜");
        log.info("✅ Certificate paid handled | id={}", id);
    }

    private void handleRejectById(Long id) {
        log.info("🚫 handleRejectById | id={}", id);
        ContestResult r = contestResultRepository.findById(id).orElse(null);
        if (r == null) {
            log.warn("⚠️ ContestResult not found | id={}", id);
            return;
        }

        log.info("🔄 Changing status to REJECTED | id={}", id);
        r.setStatus(ParticipantStatus.REJECTED);
        contestResultRepository.save(r);

        log.info("📝 Updating group message | id={}", id);
        updateGroupMessage(r);

        log.info("📨 Sending rejection confirmation to user | chatId={}", r.getChatId());
        sendText(r.getChatId(), "Жарайды 👍 Егер ойыңыз өзгерсе — хабарласыңыз");
        log.info("✅ Rejection handled | id={}", id);
    }

    private void handleSetDiploma(String data) {
        log.info("🎓 handleSetDiploma | data={}", data);
        // data = set_diploma_1_123
        String[] parts = data.split("_");
        int diplomaCategory = Integer.parseInt(parts[2]);
        Long participantId = Long.parseLong(parts[3]);

        log.info("📊 Parsed diploma info | category={}, participantId={}", diplomaCategory, participantId);

        contestResultRepository.findById(participantId).ifPresent(r -> {
            log.info("✅ ContestResult found | id={}", participantId);
            r.setDiplomaCategory(diplomaCategory); // сохраняем категорию
            contestResultRepository.save(r);
            log.info("💾 Diploma category saved | id={}, category={}", participantId, diplomaCategory);

            // Запрос сертификата через API
            log.info("📥 Fetching certificate for participant | id={}", participantId);
            fetchAndSendCertificate(r);
        });

        if (!contestResultRepository.findById(participantId).isPresent()) {
            log.warn("⚠️ ContestResult not found | participantId={}", participantId);
        }
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
        log.info("📜 fetchAndSendCertificate | resultId={}, chatId={}", r.getId(), r.getChatId());
        try {
            log.info("🔽 Downloading diploma | fullName={}, mentor={}, category={}",
                    r.getFullName(), r.getMentor(), r.getDiplomaCategory());
            byte[] diplomaBytes = diplomGenerateAdapter.downloadDiploma(r.getFullName(),r.getMentor(), DiplomTemplates.MUKAGALI_SCHOOL,r.getDiplomaCategory());

            if (diplomaBytes == null || diplomaBytes.length == 0) {
                log.error("❌ Diploma bytes are empty | resultId={}", r.getId());
                throw new RuntimeException("Диплом пришёл пустой");
            }
            log.info("✅ Diploma downloaded | size={} bytes", diplomaBytes.length);

            InputStream certificateStream = new ByteArrayInputStream(diplomaBytes);

            log.info("📤 Sending diploma to user | chatId={}", r.getChatId());
            execute(new SendDocument(
                    r.getChatId().toString(),
                    new InputFile(certificateStream, "diplom.pdf")
            ));
            log.info("✅ Diploma sent successfully | resultId={}", r.getId());

            //диплом руководителю
            log.info("🔽 Downloading algys diploma for mentor | mentor={}", r.getMentor());
            byte[] diplomaBytesHead = diplomGenerateAdapter.downloadDiplomAlgis(r.getMentor(),DiplomTemplates.ALGYS_SCHOOL);

            if (diplomaBytesHead == null || diplomaBytesHead.length == 0) {
                log.error("❌ Algys diploma bytes are empty | resultId={}", r.getId());
                throw new RuntimeException("Диплом пришёл пустой");
            }
            log.info("✅ Algys diploma downloaded | size={} bytes", diplomaBytesHead.length);

            InputStream certificateStreamHead = new ByteArrayInputStream(diplomaBytesHead);

            log.info("📤 Sending algys diploma to user | chatId={}", r.getChatId());
            execute(new SendDocument(
                    r.getChatId().toString(),
                    new InputFile(certificateStreamHead, "algys_xat.pdf")
            ));
            log.info("✅ Algys diploma sent successfully | resultId={}", r.getId());

            sendText(r.getChatId(), "📜 Диплом дайын!");

            // меняем статус
            log.info("🔄 Changing status to APPROVED | resultId={}", r.getId());
            r.setStatus(ParticipantStatus.APPROVED);
            contestResultRepository.save(r);

            // убираем кнопки в группе
            if (r.getChannelMessageId() != null) {
                log.info("📝 Updating channel message | channelMessageId={}", r.getChannelMessageId());
                EditMessageText edit = new EditMessageText();
                edit.setChatId("-1003235201523");
                edit.setMessageId(r.getChannelMessageId());
                edit.setText(buildGroupText(r));
                edit.setReplyMarkup(null);
                execute(edit);
                log.info("✅ Channel message updated | resultId={}", r.getId());
            }

        } catch (Exception e) {
            log.error("❌ Diploma download error | resultId={}", r.getId(), e);
            sendText(r.getChatId(), "❌ Диплом жүктелмеді, кейінірек қайталап көріңіз");
        }
    }

    private void testChannel() {
        log.info("🧪 testChannel | Testing channel message sending");
        try {
            Message m = execute(
                    new SendMessage("-1003235201523", "TEST CHANNEL MESSAGE")
            );
            log.info("✅ TEST SENT successfully | messageId={}", m.getMessageId());
        } catch (Exception e) {
            log.error("❌ TEST FAILED", e);
        }
    }

    private void showContestDetails(Long chatId, Integer messageId, int contestId) {
        log.info("📖 showContestDetails | chatId={}, messageId={}, contestId={}", chatId, messageId, contestId);

        // 1) редактируем старое сообщение, чтобы убрать меню
        log.info("🔄 Editing message to show loading | messageId={}", messageId);
        EditMessageText progress = new EditMessageText();
        progress.setChatId(chatId.toString());
        progress.setMessageId(messageId);
        progress.setText("Жүктелуде...");
        executeEditMessage(progress);

        // 2) Подгружаем афишу
        log.info("🖼️ Loading contest poster image");
        InputStream imageStream = getClass().getClassLoader()
                .getResourceAsStream("files/mukagali.jpg");

        if (imageStream == null) {
            log.error("❌ Contest poster not found | chatId={}", chatId);
            sendText(chatId, "❌ Афиша табылмады");
            return;
        }
        log.info("✅ Poster loaded successfully");

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
        log.info("📤 Sending contest poster with buttons | chatId={}", chatId);
        try {
            execute(photo);
            log.info("✅ Contest details sent successfully | chatId={}", chatId);
        } catch (Exception e) {
            log.error("❌ Send photo error | chatId={}", chatId, e);
        }
    }


    // ===================== PARTICIPATION =====================

    private void startParticipation(Long chatId) {
        log.info("✍️ startParticipation | chatId={}", chatId);
        userStep.put(chatId, 1);
        tempResults.put(chatId, new ContestResult());
        log.info("📝 Registration flow started | chatId={}, step=1", chatId);
        sendText(chatId, "Қатысушының аты-жөні");
    }

    private void processUserInput(Long chatId, String text) {
        log.info("📝 processUserInput | chatId={}, text={}", chatId, text);
        ContestResult result = tempResults.get(chatId);
        result.setContestType(ContestType.MEKTEP_MAKATAEV);
        Integer step = userStep.get(chatId);

        if (result == null || step == null) {
            log.warn("⚠️ Result or step is null | chatId={}", chatId);
            return;
        }

        log.info("🔢 Processing step {} | chatId={}", step, chatId);
        switch (step) {
            case 1 -> {
                log.info("👤 Saving fullName | chatId={}", chatId);
                result.setFullName(text);
                userStep.put(chatId, 2);
                sendText(chatId, "Сыныбыңыз:");
            }
            case 2 -> {
                log.info("🎓 Saving grade | chatId={}", chatId);
                result.setGrade(text);
                userStep.put(chatId, 3);
                sendText(chatId, "Телефон:");
            }
            case 3 -> {
                log.info("📞 Saving phone | chatId={}", chatId);
                result.setPhone(text);
                userStep.put(chatId, 4);
                sendText(chatId, "Жетекші аты:");
            }
            case 4 -> {
                log.info("👨‍🏫 Saving mentor | chatId={}", chatId);
                result.setMentor(text);
                userStep.put(chatId, 5);
                sendText(chatId, "Мектеп:");
            }
            case 5 -> {
                log.info("🏫 Saving school | chatId={}", chatId);
                result.setSchool(text);
                userStep.put(chatId, 6);
                log.info("⏭️ Moving to step 6 (waiting for file) | chatId={}", chatId);
                sendText(chatId, "Жұмысыңызды жіберіңіз (файл түрінде):");
            }
            case 6 -> {
                log.info("⚠️ User sent text instead of file | chatId={}", chatId);
                sendText(chatId, "Жұмысыңызды жіберіңіз (файл түрінде):");
            }
        }
    }

    private void handleFileMessage(Long chatId, String fileId, Long fileSize, String mimeType) {
        log.info("📎 handleFileMessage | chatId={}, fileId={}, fileSize={}, mimeType={}", chatId, fileId, fileSize, mimeType);
        Integer step = userStep.get(chatId);
        ContestResult result = tempResults.get(chatId);

        if (step == null || result == null) {
            log.warn("⚠️ No active registration | chatId={}", chatId);
            return;
        }

        if (step == 6) {
            log.info("📤 Processing file upload | chatId={}, step=6", chatId);

            // Константы ограничений Telegram Bot API
            final long TELEGRAM_BOT_API_FILE_LIMIT = 20 * 1024 * 1024; // 20 МБ - лимит Bot API
            final long LARGE_VIDEO_THRESHOLD = 50 * 1024 * 1024; // 50 МБ - порог для MinIO

            try {
                String originalFileName = "file";
                String extension = "dat";
                String fileUrl = null;
                boolean isTooBigForBotApi = fileSize != null && fileSize > TELEGRAM_BOT_API_FILE_LIMIT;
                boolean shouldUploadToMinio = fileSize != null && fileSize > LARGE_VIDEO_THRESHOLD &&
                                               mimeType != null && mimeType.startsWith("video/");

                log.info("📊 File analysis | size={} MB, isTooBigForBotApi={}, shouldUploadToMinio={}",
                        fileSize != null ? fileSize / (1024 * 1024) : "unknown", isTooBigForBotApi, shouldUploadToMinio);

                // Пытаемся получить информацию о файле (только если файл <= 20 МБ)
                if (!isTooBigForBotApi) {
                    try {
                        log.info("🔍 Getting file info from Telegram | fileId={}", fileId);
                        var file = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
                        fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
                        log.info("📥 File URL obtained | url={}", fileUrl);

                        originalFileName = file.getFilePath();
                        int dotIndex = originalFileName.lastIndexOf('.');
                        if (dotIndex > 0) {
                            extension = originalFileName.substring(dotIndex + 1);
                        }
                    } catch (Exception e) {
                        log.error("❌ Failed to get file info from Telegram | fileId={}", fileId, e);
                        // Продолжаем работу с file_id
                    }
                } else {
                    log.warn("⚠️ File too big for Bot API | size={} MB, using file_id only", fileSize / (1024 * 1024));
                    // Пытаемся определить расширение из mime type
                    if (mimeType != null) {
                        if (mimeType.startsWith("video/")) {
                            extension = mimeType.replace("video/", "");
                        } else if (mimeType.startsWith("image/")) {
                            extension = mimeType.replace("image/", "");
                        } else if (mimeType.contains("pdf")) {
                            extension = "pdf";
                        } else if (mimeType.contains("document")) {
                            extension = "doc";
                        }
                    }
                }

                String savedFileName = result.getFullName().replaceAll("\\s+", "_") + "_work." + extension;
                log.info("💾 Generated filename | savedFileName={}, extension={}", savedFileName, extension);

                String minioUrl = null;

                // Загружаем в MinIO если это большое видео И файл доступен для скачивания
                if (shouldUploadToMinio && !isTooBigForBotApi && fileUrl != null) {
                    log.info("☁️ Uploading large video to MinIO | filename={}, size={} MB",
                            savedFileName, fileSize / (1024 * 1024));
                    try (InputStream videoStream = new java.net.URL(fileUrl).openStream()) {
                        minioUrl = fileService.uploadFileVideo(videoStream, savedFileName, fileSize);
                        log.info("✅ Video uploaded to MinIO successfully | url={}", minioUrl);
                    } catch (Exception minioEx) {
                        log.error("❌ Failed to upload to MinIO | filename={}", savedFileName, minioEx);
                        sendText(chatId, "❌ Видео файл үлкен, жүктеу кезінде қате пайда болды.");
                        return;
                    }
                }

                // Сохраняем в объекте
                result.setWorkFileId(fileId);
                result.setWorkFileName(savedFileName);
                result.setCreatedAt(LocalDateTime.now());
                result.setChatId(chatId);
                result.setStatus(ParticipantStatus.NEW);

                // Сохраняем результат
                log.info("💾 Saving contest result to DB | chatId={}", chatId);
                ContestResult saved = contestResultRepository.save(result);
                log.info("✅ Contest result saved | id={}", saved.getId());

                // 🔹 Отправка в канал
                if (isTooBigForBotApi) {
                    // Файл слишком большой - отправляем только информацию
                    log.info("📤 Sending large file info to channel | savedId={}, fileId={}", saved.getId(), fileId);
                    String fileInfo = "📦 Большой файл (не доступен для скачивания через Bot API)\n" +
                                     "📁 Файл: " + savedFileName + "\n" +
                                     "💾 Размер: " + (fileSize != null ? (fileSize / (1024 * 1024)) + " МБ" : "неизвестен") + "\n" +
                                     "🆔 File ID: " + fileId + "\n" +
                                     "📋 Type: " + (mimeType != null ? mimeType : "unknown");

                    if (minioUrl != null) {
                        fileInfo += "\n🔗 MinIO: " + minioUrl;
                    } else {
                        fileInfo += "\n⚠️ Файл НЕ загружен в MinIO (недоступен для Bot API)";
                    }

                    SendMessage bigFileMsg = new SendMessage("-1003235201523", fileInfo);
                    execute(bigFileMsg);
                    log.info("✅ Large file info sent to channel");

                } else if (shouldUploadToMinio && minioUrl != null) {
                    // Видео загружено в MinIO
                    log.info("📤 Sending MinIO link to channel | savedId={}, minioUrl={}", saved.getId(), minioUrl);
                    SendMessage minioMsg = new SendMessage("-1003235201523",
                        "🎥 Видео файл (большой размер)\n" +
                        "📁 Файл: " + savedFileName + "\n" +
                        "💾 Размер: " + (fileSize / (1024 * 1024)) + " МБ\n" +
                        "🔗 MinIO: " + minioUrl);
                    execute(minioMsg);
                    log.info("✅ MinIO link sent to channel successfully");

                } else {
                    // Обычный файл - отправляем напрямую
                    log.info("📤 Sending file to channel | savedId={}", saved.getId());
                    SendDocument sendDoc = new SendDocument();
                    sendDoc.setChatId("-1003235201523");
                    sendDoc.setDocument(new InputFile(new java.net.URL(fileUrl).openStream(), savedFileName));
                    execute(sendDoc);
                    log.info("✅ File sent to channel successfully");
                }

                // 🔹 Отправка отдельного текстового сообщения с кнопками
                log.info("📤 Sending info message to channel | savedId={}", saved.getId());
                SendMessage msg = new SendMessage("-1003235201523", buildGroupText(saved));
                Message textMessage = execute(msg);
                log.info("✅ Info message sent | channelMessageId={}", textMessage.getMessageId());

                saved.setChannelMessageId(textMessage.getMessageId());
                contestResultRepository.save(saved);
                log.info("💾 Channel messageId saved | channelMessageId={}", textMessage.getMessageId());

                // Подтверждение участнику
                log.info("📨 Sending confirmation to user | chatId={}", chatId);
                sendText(chatId, "✅ Мәлімет сақталды, рақмет!");

                if (isTooBigForBotApi) {
                    sendText(chatId, "✔ Жұмысыңыз қабылданды! (Файл өте үлкен - file_id сақталды)\n📜 Сертификат 2–3 сағат ішінде дайын болады.");
                } else {
                    sendText(chatId, "✔ Жұмысыңыз қабылданды!\n📜 Сертификат 2–3 сағат ішінде дайын болады.");
                }

                saved.setStatus(ParticipantStatus.AWAITING_CHECK);
                saved.setCertificateNotifyAt(LocalDateTime.now().plusHours(2));
                contestResultRepository.save(saved);
                log.info("🔄 Status changed to AWAITING_CHECK | id={}, notifyAt={}", saved.getId(), saved.getCertificateNotifyAt());

                userStep.remove(chatId);
                tempResults.remove(chatId);
                log.info("🧹 Cleared user session | chatId={}", chatId);
                log.info("✅ File upload completed successfully | resultId={}, uploadedToMinio={}", saved.getId(), shouldUploadToMinio);

            } catch (Exception e) {
                log.error("❌ File send to channel error | chatId={}", chatId, e);
                sendText(chatId, "❌ Жұмысыңызды жіберу кезінде қате пайда болды, кейінірек қайта көріңіз.");
            }
        } else {
            log.warn("⚠️ File sent at wrong step | chatId={}, step={}", chatId, step);
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


    @Scheduled(fixedDelay = 60000)
    public void certificateJob() {
        List<ContestResult> list =
                contestResultRepository.findAllByStatusAndContestTypeAndCertificateNotifyAtBefore(
                        ParticipantStatus.AWAITING_CHECK,
                        ContestType.MEKTEP_MAKATAEV,
                        LocalDateTime.now()
                );

        for (ContestResult r : list) {
            sendCertificateMessage(r);
            r.setStatus(ParticipantStatus.WANT_TO_BUY);
            contestResultRepository.save(r);
            log.info("⏰ CERTIFICATE JOB | resultId={} status=WANT_TO_BUY", r.getId());
        }
    }

    private void sendCertificateMessage(ContestResult r) {

        SendMessage msg = new SendMessage();
        msg.setChatId(r.getChatId().toString());
        msg.setText(
                "📜ДИПЛОМ мен АЛҒЫС ХАТЫҢЫЗ дайын✅\n\n" +
                        "Жүктеп алу үшін төлем жасауыңыз керек. Төлем жарнасы 1900 теңге.\n" +
                        "\uD83D\uDCCE Егер бір педагогтың жетекшілігімен 10 қатысушыдан артық тіркелетін болса, менеджерге хабарласыңыз!\n" +
                        " Арнайы жеңілдік қарастырылған\uD83E\uDD73 \n" +
                        "🟥🟥🟥 ЕСКЕРТУ 🟥🟥🟥\n" +
                        "Төлем жасағанда каспи-комментариге М" + r.getId() + " жіберуіңізді сұраймыз 👇"
        );
        //String payUrl = "https://pay.example.com/certificate?chatId=" + chatId;
        String payUrl = "https://pay.kaspi.kz/pay/v0iq41rc";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(
                List.of(payUrlButton("💳 Сертификатты төлеу", payUrl)),
                List.of(callbackButton(
                        "✅ Сертификат төленді",
                        "certificate_paid_" + r.getId()
                )),
                List.of(callbackButton(
                        "❌ Бас тарту",
                        "certificate_reject_" + r.getId()
                ))
        ));

        msg.setReplyMarkup(keyboard);
        executeMessage(msg);
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
            case AWAITING_CHECK -> "🔍 Тексерілуде";
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
