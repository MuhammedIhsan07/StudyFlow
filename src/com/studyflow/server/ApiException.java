package com.studyflow.server;

/** An expected, safe-to-display request failure. */
final class ApiException extends RuntimeException {
    final int status;
    ApiException(int status, String message) { super(message); this.status = status; }
    static void require(boolean condition, int status, String message) {
        if (!condition) throw new ApiException(status, message);
    }
}
