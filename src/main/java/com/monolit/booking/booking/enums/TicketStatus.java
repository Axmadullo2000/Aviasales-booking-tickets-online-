package com.monolit.booking.booking.enums;

public enum TicketStatus {
    RESERVED,    // Билет забронирован, но ещё не оплачен
    PAID,        // Билет оплачен,
    CHECKED_IN,  // Пассажир прошёл регистрацию (check-in)
    BOARDING,    // Пассажир идёт на посадку
    CONFIRMED,
    ISSUED,
    BOARDED,
    COMPLETED,   // Полёт завершён
    VOIDED,
    CANCELLED,   // Билет отменён
    NO_SHOW      // Пассажир не явился на рейс
}
