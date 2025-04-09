package util;

/**
 * Exception thrown during the calendar export process, indicating an issue
 * with formatting or handling the export data, distinct from standard I/O errors.
 */
public class ExportException extends Exception {

    /**
     * Constructs a new ExportException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExportException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
