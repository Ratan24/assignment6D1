# Calendar Application GUI Usage Guide

This guide explains how to use the graphical user interface (GUI) for the Calendar Application.

## Starting the GUI

You can start the GUI in two ways:

1.  **Double-clicking the JAR:** If you have the `CalendarApp.jar` file, simply double-clicking it should launch the GUI.
2.  **Command Line:** Open a terminal or command prompt, navigate to the directory containing `CalendarApp.jar`, and run:
    ```bash
    java -jar CalendarApp.jar
    ```
    (Note: If you run from within an IDE like IntelliJ without arguments, it should also default to GUI mode).

## Main Window

The main window displays a month view of the selected calendar.

*   **Month Navigation:** Use the `<` and `>` buttons next to the month/year label to navigate to the previous or next month.
*   **Calendar Selection:**
    *   The dropdown menu shows the currently selected calendar.
    *   Use the dropdown to switch between existing calendars. The month view will update to show events for the selected calendar.
*   **Creating a New Calendar:** Click the "New Calendar" button or use the "Calendar" -> "Create New Calendar..." menu item. A dialog will appear asking for a unique name and a timezone.
*   **Editing the Current Calendar:** Use the "Calendar" -> "Edit Current Calendar..." menu item. A dialog will appear allowing you to change the name and/or timezone of the currently selected calendar.
*   **Status Bar:** The bottom bar displays status messages (e.g., "Ready", "Using calendar: ...").

## Interacting with Days and Events

*   **Viewing Daily Events:** Click on any day number in the month grid. A dialog will pop up showing all events scheduled for that specific day in the selected calendar.
*   **Creating an Event:**
    1.  Click on the day you want to add an event to. The "Events on [Date]" dialog appears.
    2.  Click the "Add Event" button.
    3.  The "Create New Event" dialog appears. Fill in the details:
        *   **Name:** The event's subject/title (required).
        *   **All Day Event:** Check this box if the event lasts the entire day. Start/End times will be disabled.
        *   **Start/End Date & Time:** Enter the date (YYYY-MM-DD) and time (HH:MM, 24-hour format). End time must be after start time. Disabled if "All Day" is checked.
        *   **Description:** Optional longer description.
        *   **Location:** Optional location.
        *   **Public Event:** Check if the event is public (uncheck for private).
        *   **Recurring Event:** Check this to set up recurrence rules.
            *   **Repeat on:** Select the days of the week (Mon-Sun) the event should repeat on.
            *   **Termination:** Choose either "For N times" (specify number) or "Until Date" (specify end date YYYY-MM-DD).
    4.  Click "Save". The event will be added if there are no conflicts.
*   **Editing an Event:**
    1.  Click on the day the event occurs. The "Events on [Date]" dialog appears.
    2.  Select the event you want to edit from the list.
    3.  Click the "Edit" button (or double-click the event in the list).
    4.  The "Edit Event" dialog appears.
    5.  Modify the desired properties (Name, Description, Location, Public status). **Note:** Editing date/time or recurrence rules for existing events via the GUI is currently limited.
    6.  Click "Save".
*   **Deleting an Event:**
    1.  Click on the day the event occurs. The "Events on [Date]" dialog appears.
    2.  Select the event you want to delete from the list.
    3.  Click the "Delete" button.
    4.  Confirm the deletion when prompted.

## File Operations (Menu Bar)

*   **Import from CSV:**
    1.  Go to "File" -> "Import from CSV...".
    2.  A file chooser dialog appears. Select a Google Calendar compatible CSV file.
    3.  Click "Import". Events from the file will be added to the *currently selected* calendar. Conflicts will be rejected.
    4.  The main view will refresh automatically.
*   **Export to CSV:**
    1.  Go to "File" -> "Export to CSV...".
    2.  A file chooser dialog appears. Choose a location and enter a file name (ensure it ends with `.csv`).
    3.  Click "Export". The *currently selected* calendar's events will be saved in a Google Calendar compatible format.
*   **Exit:** Go to "File" -> "Exit" to close the application.

## Help Menu

*   **About:** Go to "Help" -> "About..." to see basic information about the application.
