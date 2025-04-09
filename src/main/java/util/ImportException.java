package util;

/**
 * Exception thrown during the calendar import process, indicating an issue
 * with parsing, interpreting, or handling the imported data, distinct from standard I/O errors.
 */
public class ImportException extends Exception {

    /**
     * Constructs a new ImportException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ImportException(String message) {
        super(message);
    }

    /**
     * Constructs a new ImportException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
