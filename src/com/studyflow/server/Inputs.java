package com.studyflow.server;

import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;

final class Inputs {
    private Inputs() { }
    static String text(JsonObject body, String key, int max) {
        String value = optional(body, key, "").trim();
        ApiException.require(!value.isEmpty() && value.length() <= max, 400,
                key + " is required and must be at most " + max + " characters.");
        ApiException.require(value.chars().noneMatch(c -> c < 32 && c != 10 && c != 13 && c != 9), 400, "Invalid characters in " + key + ".");
        return value;
    }
    static String optional(JsonObject body, String key, String fallback) {
        if (!body.has(key) || body.get(key).isJsonNull()) return fallback;
        ApiException.require(body.get(key).isJsonPrimitive() && body.get(key).getAsJsonPrimitive().isString(), 400,
                key + " must be text.");
        return body.get(key).getAsString();
    }
    static String email(JsonObject body) {
        String value = text(body, "email", 254).toLowerCase(Locale.ROOT);
        ApiException.require(value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"), 400, "Enter a valid email address.");
        return value;
    }
    static int number(JsonObject body, String key, int min, int max) {
        try {
            ApiException.require(body.has(key) && body.get(key).isJsonPrimitive()
                    && body.get(key).getAsJsonPrimitive().isNumber(), 400, key + " must be a number.");
            int value = body.get(key).getAsBigDecimal().intValueExact();
            ApiException.require(value >= min && value <= max, 400, key + " must be between " + min + " and " + max + ".");
            return value;
        } catch (ArithmeticException | NumberFormatException exception) { throw new ApiException(400, "Invalid " + key + "."); }
    }
    static boolean bool(JsonObject body, String key) {
        ApiException.require(body.has(key) && body.get(key).isJsonPrimitive()
                && body.get(key).getAsJsonPrimitive().isBoolean(), 400, key + " must be true or false.");
        return body.get(key).getAsBoolean();
    }
    static String choice(JsonObject body, String key, String... options) {
        String value = text(body, key, 30);
        ApiException.require(Set.of(options).contains(value), 400, "Invalid " + key + ".");
        return value;
    }
    static String date(JsonObject body, String key) {
        String value = text(body, key, 10);
        try {
            LocalDate date = LocalDate.parse(value);
            ApiException.require(date.getYear() >= 2000 && date.getYear() <= 2100, 400, "Use a date between 2000 and 2100.");
            return date.toString();
        } catch (java.time.DateTimeException exception) { throw new ApiException(400, "Invalid " + key + "."); }
    }
    static String time(JsonObject body) {
        String value = text(body, "start_time", 5);
        try {
            ApiException.require(value.matches("\\d{2}:\\d{2}"), 400, "Use a time in HH:mm format.");
            return LocalTime.parse(value).toString();
        } catch (java.time.DateTimeException exception) { throw new ApiException(400, "Invalid start time."); }
    }
}
