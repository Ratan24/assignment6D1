package model;

/**
 * Exception thrown when invalid data is provided to a model operation,
 * such as an invalid timezone string, an unrecognized property name,
 * or improperly formatted input.
 */
public class InvalidDataException extends Exception {

    /**
     * Constructs a new InvalidDataException with the specified detail message.
     *
     * @param message the detail message.
     */
    public InvalidDataException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidDataException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
