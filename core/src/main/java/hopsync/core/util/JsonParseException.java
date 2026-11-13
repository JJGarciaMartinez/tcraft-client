package hopsync.core.util;

import java.io.IOException;

/**
 * Thrown when a document is not valid JSON. Extends IOException so it flows through
 * the same error handling as network failures.
 */
public class JsonParseException extends IOException {
    private static final long serialVersionUID = 1L;

    private final int position;

    public JsonParseException(String message, int position) {
        super("JSON inválido en la posición " + position + ": " + message);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
