package model;

/**
 * Exception thrown when an attempt is made to add or modify an event
 * in a way that conflicts with an existing event in the calendar.
 */
public class CalendarConflictException extends Exception {

    /**
     * Constructs a new CalendarConflictException with the specified detail message.
     *
     * @param message the detail message.
     */
    public CalendarConflictException(String message) {
        super(message);
    }

    /**
     * Constructs a new CalendarConflictException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public CalendarConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
