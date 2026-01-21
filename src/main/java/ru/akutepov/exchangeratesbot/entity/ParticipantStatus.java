package ru.akutepov.exchangeratesbot.entity;

public enum ParticipantStatus {
    NEW,            // 🆕 Новый участник (отправил работу)
    WANT_TO_BUY,    // 🛒 Хочет купить сертификат
    PAID_PENDING,  // ⏳ Оплатил — проверить
    APPROVED,       // ✅ Подтверждено
    REJECTED        // ❌ Отказался
}
