package util;

import model.ICalendarEvent;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports calendar events to a Google Calendar compatible CSV format.
 */
public class GoogleCsvExporter implements ICalendarExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a"); // AM/PM format

    @Override
    public void export(List<ICalendarEvent> events, Writer writer) throws IOException, ExportException {
        try {
            // Write header
            writer.write("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");

            // Write event data
            for (ICalendarEvent event : events) {
                writer.write("\"" + escapeCsv(event.getEventName()) + "\","); // Subject
                LocalDate startDate = event.getStart().toLocalDate();
                LocalDate endDate = event.getEnd().toLocalDate(); // Use end date directly

                if (event.isAllDay()) {
                    // Google expects end date to be the day *before* the exclusive end date for multi-day all-day events
                    // For single all-day events, start and end date are the same.
                    LocalDate googleEndDate = event.getEnd().toLocalDate();
                    if (event.getEnd().toLocalTime().equals(LocalTime.MIDNIGHT) && !event.getEnd().toLocalDate().equals(startDate)) {
                        googleEndDate = googleEndDate.minusDays(1); // Adjust if our model uses exclusive end date midnight
                    }
                    writer.write(startDate.format(DATE_FORMATTER) + ","); // Start Date
                    writer.write(","); // Start Time (empty for all-day)
                    writer.write(googleEndDate.format(DATE_FORMATTER) + ","); // End Date
                    writer.write(","); // End Time (empty for all-day)
                    writer.write("True,"); // All Day Event
                } else {
                    writer.write(startDate.format(DATE_FORMATTER) + ","); // Start Date
                    writer.write(event.getStart().format(TIME_FORMATTER) + ","); // Start Time
                    // Google's end date is inclusive for timed events
                    writer.write(endDate.format(DATE_FORMATTER) + ","); // End Date
                    writer.write(event.getEnd().format(TIME_FORMATTER) + ","); // End Time
                    writer.write("False,"); // All Day Event
                }
                writer.write("\"" + escapeCsv(event.getDescription()) + "\","); // Description
                writer.write("\"" + escapeCsv(event.getLocation()) + "\","); // Location
                writer.write(event.isPublic() ? "False" : "True"); // Private (opposite of our isPublic)
                writer.write("\n");
            }
            writer.flush(); // Ensure all data is written
        } catch (IOException e) {
            throw e; // Re-throw IOExceptions
        } catch (Exception e) {
            // Wrap other exceptions (e.g., formatting errors) in ExportException
            throw new ExportException("Error formatting event data for Google CSV export: " + e.getMessage(), e);
        }
    }

    // Helper to escape quotes in CSV fields
    private String escapeCsv(String value) {
        if (value == null) return "";
        // Replace existing quotes with double quotes
        return value.replace("\"", "\"\"");
    }
}
