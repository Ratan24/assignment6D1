package util;

import model.ICalendarEvent;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Interface for importing calendar events from a specific format.
 */
public interface ICalendarImporter {

    /**
     * Imports calendar events from the provided Reader.
     *
     * @param reader The Reader to read the formatted data from.
     * @return A list of ICalendarEvent objects parsed from the input.
     * @throws IOException If an I/O error occurs during reading.
     * @throws ImportException If an error occurs during parsing or interpreting the input data.
     */
    List<ICalendarEvent> importEvents(Reader reader) throws IOException, ImportException;
}
