package util;

import model.ICalendarEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Interface for exporting calendar events to a specific format.
 */
public interface ICalendarExporter {

    /**
     * Exports the given list of calendar events to the provided Writer.
     *
     * @param events The list of events to export.
     * @param writer The Writer to output the formatted data to.
     * @throws IOException If an I/O error occurs during writing.
     * @throws ExportException If an error occurs during the export formatting process.
     */
    void export(List<ICalendarEvent> events, Writer writer) throws IOException, ExportException;
}
