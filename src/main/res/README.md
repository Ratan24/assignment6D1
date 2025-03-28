# Calendar Application

## Overview

This application is a command-line calendar system that allows you to:
- Create and edit single or recurring events
- Print events on a specific date or within a range
- Export events to CSV or Google CSV
- Check if the calendar is busy at a specific time
- Run in interactive or headless mode

The design is structured in an MVC-like pattern:
- `model` package contains data interfaces (`ICalendarEvent`, `ICalendarManager`
- , `IRecurringEventGenerator`) and their implementing classes (`CalendarEvent`, `CalendarManager`
- , `RecurringEventGenerator`).
- `controller` package includes `CalendarController` (the main controller) and `CommandParser` 
- (the command-line parser).
- `view` package has `OutputHandler` (handles printing).


## How to Run

1. **Compile** all Java files
2. Launch the main class:
java calendar.CalendarApp --mode 
interactive or java calendar.CalendarApp --mode headless <commandsFile.txt>
3. Interactive Mode
   In interactive mode, you type commands directly in the console.
   Type exit to quit.
4. Headless Mode
   In headless mode, you provide a text file with commands, one per line.
   The application processes them and stops at exit or end-of-file.
5. To Run JAR file: "java -jar Calendar.jar --mode interactive" or "java -jar Calendar.jar --mode headless <commandFile.txt>"

## Features That Work:
   Create events (timed or all-day)
   Edit events (single or bulk)
   Print events on a date or in a range
   Check status (busy/available) at a given time
   Export to CSV and Google CSV
   Recurring events (either with fixed occurrences or until a certain date)
   Conflict detection with optional auto-decline

## Which Features Do NOT Work:
   The system does not currently handle time zones or scheduling across day boundaries robustly.


## A rough distribution of each team member.
## Work Distribution

- **Multi-Calendar Framework**: Implementation of multiple calendar management and selection
- **Timezone Integration**: Adding timezone support to calendars and events
- **Event Operations**: Cross-calendar copying and conflict resolution
- **Command Processing**: Parser enhancements for new functionalities
- **Project Infrastructure**: Testing, documentation, and delivery

Member A focused primarily on the multi-calendar framework and cross-calendar event operations, 
while Member B concentrated on timezone integration and command processing enhancements. 
Both members collaborated on testing and documentation.


