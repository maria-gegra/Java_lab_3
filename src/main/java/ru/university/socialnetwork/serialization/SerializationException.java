package ru.university.socialnetwork.serialization;

import java.io.IOException;

public class SerializationException extends IOException {
    public enum Code {
        EMPTY_FILE,
        CORRUPTED_FILE,
        UNSUPPORTED_VERSION,
        VALIDATION_FAILED,
        WRONG_FORMAT
    }

    private final Code code;

    public SerializationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public SerializationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
