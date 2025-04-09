package model;

/**
 * Exception thrown when an operation attempts to access or modify an event
 * that does not exist in the calendar based on the provided criteria.
 */
public class EventNotFoundException extends Exception {

    /**
     * Constructs a new EventNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EventNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new EventNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public EventNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
