package util;

import model.CalendarEvent;
import model.ICalendarEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports calendar events from a Google Calendar compatible CSV format.
 */
import java.time.format.DateTimeFormatterBuilder; // Import Builder
import java.time.temporal.ChronoField; // Import ChronoField

/**
 * Imports calendar events from a Google Calendar compatible CSV format.
 */
public class GoogleCsvImporter implements ICalendarImporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    // Use DateTimeFormatterBuilder for flexible AM/PM time parsing
    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive() // Handle am/pm
            .appendPattern("h:mm") // Hour (1-12), minutes
            .appendLiteral(' ') // Space before AM/PM
            .appendText(ChronoField.AMPM_OF_DAY) // AM/PM text
            .toFormatter(java.util.Locale.ENGLISH);

    @Override
    public List<ICalendarEvent> importEvents(Reader reader) throws IOException, ImportException {
        List<ICalendarEvent> importedEvents = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine(); // Read header line
            lineNumber++;

            if (line == null || !line.toLowerCase().contains("subject") || !line.toLowerCase().contains("start date")) {
                throw new ImportException("Invalid Google CSV format: Header row missing or incorrect.");
            }

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                // Basic CSV parsing (doesn't handle quotes within fields perfectly, assumes standard Google format)
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (fields.length < 9) {
                   System.err.println("Skipping malformed CSV line " + lineNumber + ": " + line);
                   // Optionally throw ImportException or collect errors
                   continue;
                }

                try {
                    // Trim quotes and whitespace from fields
                    String subject = fields[0].trim().replaceAll("^\"|\"$", "");
                    String startDateStr = fields[1].trim().replaceAll("^\"|\"$", "");
                    String startTimeStr = fields[2].trim().replaceAll("^\"|\"$", "");
                    String endDateStr = fields[3].trim().replaceAll("^\"|\"$", "");
                    String endTimeStr = fields[4].trim().replaceAll("^\"|\"$", "");
                    String allDayStr = fields[5].trim().replaceAll("^\"|\"$", "");
                    String description = fields[6].trim().replaceAll("^\"|\"$", "");
                    String location = fields[7].trim().replaceAll("^\"|\"$", "");
                    String privateStr = fields[8].trim().replaceAll("^\"|\"$", "");

                    // Basic validation
                    if (subject.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                        throw new ImportException("Missing required fields (Subject, Start Date, End Date) on line " + lineNumber);
                    }

                    boolean isAllDay = Boolean.parseBoolean(allDayStr);
                    boolean isPrivate = Boolean.parseBoolean(privateStr); // Google uses "Private" column (True=Private)

                    LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                    LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                    LocalDateTime startDateTime;
                    LocalDateTime endDateTime;

                    if (isAllDay) {
                        startDateTime = startDate.atStartOfDay();
                        // Google CSV uses inclusive end date for all-day events.
                        // Our model expects exclusive end (start of next day).
                        endDateTime = endDate.plusDays(1).atStartOfDay();
                    } else {
                         if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
                             throw new ImportException("Missing start/end time for non-all-day event on line " + lineNumber);
                         }
                         // Handle potential single-digit hour without leading zero if parser needs it
                         LocalTime startTime = LocalTime.parse(startTimeStr.toUpperCase(), TIME_FORMATTER);
                         LocalTime endTime = LocalTime.parse(endTimeStr.toUpperCase(), TIME_FORMATTER);
                         startDateTime = LocalDateTime.of(startDate, startTime);
                         endDateTime = LocalDateTime.of(endDate, endTime);
                    }

                    // Create the event object (using concrete CalendarEvent for now)
                    CalendarEvent newEvent = new CalendarEvent(subject, startDateTime, endDateTime, isAllDay);
                    newEvent.setDescription(description);
                    newEvent.setLocation(location);
                    newEvent.setPublic(!isPrivate); // Our model uses isPublic, Google uses isPrivate

                    importedEvents.add(newEvent);

                } catch (DateTimeParseException e) {
                    throw new ImportException("Error parsing date/time on line " + lineNumber + ": " + e.getMessage(), e);
                } catch (ImportException e) {
                    // Re-throw specific import exceptions
                    throw e;
                } catch (Exception e) {
                   // Wrap other potential errors during parsing/creation
                   throw new ImportException("Error processing line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw e; // Re-throw IOExceptions
        }
        return importedEvents;
    }
}
