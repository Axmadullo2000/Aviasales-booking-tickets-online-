package com.monolit.booking.booking.enums;

import lombok.Getter;

@Getter
public enum CardType {
    UZCARD("UzCard", "5614"),
    HUMO("Humo", "9860"),
    VISA("Visa", "4"),
    MASTERCARD("MasterCard", "5"),
    MAESTRO("Maestro", "6"),
    MIR("МИР", "2"),
    AMERICAN_EXPRESS("American Express", "34", "37"),
    UNIONPAY("UnionPay", "62"),
    UNKNOWN("Unknown", "");

    private final String displayName;
    private final String[] prefixes;

    CardType(String displayName, String... prefixes) {
        this.displayName = displayName;
        this.prefixes = prefixes;
    }

    /**
     * Определяет тип карты по номеру
     */
    public static CardType detectCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return UNKNOWN;
        }

        String cleanNumber = cardNumber.replaceAll("\\s+", "");

        // ✅ UzCard (8600) - узбекские карты проверяем первыми
        if (cleanNumber.startsWith("5614")) {
            return UZCARD;
        }

        // ✅ Humo (9860)
        if (cleanNumber.startsWith("9860")) {
            return HUMO;
        }

        // Visa (4)
        if (cleanNumber.startsWith("4")) {
            return VISA;
        }

        // MasterCard (51-55, 2221-2720)
        if (cleanNumber.startsWith("5")) {
            int firstTwo = Integer.parseInt(cleanNumber.substring(0, 2));
            if (firstTwo >= 51 && firstTwo <= 55) {
                return MASTERCARD;
            }
        }
        if (cleanNumber.length() >= 4) {
            try {
                int firstFour = Integer.parseInt(cleanNumber.substring(0, 4));
                if (firstFour >= 2221 && firstFour <= 2720) {
                    return MASTERCARD;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Maestro (6)
        if (cleanNumber.startsWith("6")) {
            return MAESTRO;
        }

        // МИР (2200-2204)
        if (cleanNumber.length() >= 4) {
            try {
                int firstFour = Integer.parseInt(cleanNumber.substring(0, 4));
                if (firstFour >= 2200 && firstFour <= 2204) {
                    return MIR;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // American Express (34, 37)
        if (cleanNumber.startsWith("34") || cleanNumber.startsWith("37")) {
            return AMERICAN_EXPRESS;
        }

        // UnionPay (62)
        if (cleanNumber.startsWith("62")) {
            return UNIONPAY;
        }

        return UNKNOWN;
    }

    /**
     * Требуется ли валидация по алгоритму Луна для этого типа карты
     */
    public boolean requiresLuhnValidation() {
        return switch (this) {
            case VISA, MASTERCARD, MAESTRO, AMERICAN_EXPRESS, UNIONPAY -> true;
            case UZCARD, HUMO, MIR -> false; // Узбекские и российские карты имеют свою систему
            case UNKNOWN -> false;
        };
    }

    /**
     * Ожидаемая длина CVV для типа карты
     */
    public int getExpectedCvvLength() {
        return switch (this) {
            case AMERICAN_EXPRESS -> 4; // Amex использует 4-значный CVV
            default -> 3;
        };
    }

    /**
     * Минимальная длина номера карты
     */
    public int getMinLength() {
        return switch (this) {
            case AMERICAN_EXPRESS -> 15;
            case VISA, MASTERCARD, MAESTRO, UZCARD, HUMO, MIR, UNIONPAY -> 16;
            case UNKNOWN -> 13;
        };
    }

    /**
     * Максимальная длина номера карты
     */
    public int getMaxLength() {
        return switch (this) {
            case AMERICAN_EXPRESS -> 15;
            case VISA, MASTERCARD, MAESTRO, UZCARD, HUMO, MIR, UNIONPAY -> 16;
            case UNKNOWN -> 19;
        };
    }
}
