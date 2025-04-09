package view;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat; // Added import
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId; // Added import
import java.time.ZonedDateTime; // Added for RRULE UNTIL format
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner; // For joining weekdays
// Removed CommandParser import
// import model.CalendarEvent; // Removed concrete class import
import model.ICalendarEvent;
// import model.MultiCalendarManager; // Removed model import
// import model.RecurringEventGenerator; // Removed direct generator import
import controller.ICalendarController; // Added controller import

/**
 * Dialog for creating and editing calendar events (single and recurring).
 */
public class EventDialog extends JDialog {

    // private MultiCalendarManager calendarManager; // Removed model reference
    private ICalendarController controller; // Added controller reference
    private ICalendarEvent eventToEdit; // Null if creating new event
    private LocalDate initialDate; // Date context from where the dialog was opened

    // --- UI Components ---
    private JTextField nameField;
    private JCheckBox allDayCheckBox;
    private JFormattedTextField startDateField;
    private JFormattedTextField startTimeField;
    private JFormattedTextField endDateField;
    private JFormattedTextField endTimeField;
    private JTextArea descriptionArea;
    private JTextField locationField;
    private JCheckBox publicCheckBox; // Assuming true = public, false = private

    // Recurrence Components
    private JCheckBox recurringCheckBox;
    private JPanel recurrencePanel;
    private JPanel weekdaysPanel;
    private JCheckBox[] weekdayCheckBoxes;
    private ButtonGroup terminationGroup;
    private JRadioButton forNTimesRadioButton;
    private JSpinner occurrencesSpinner;
    private JRadioButton untilDateRadioButton;
    private JFormattedTextField untilDateField;

    private JButton saveButton;
    private JButton cancelButton;

    private boolean saved = false;

    // Formatters
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    // Formatters for the simple recurrence rule expected by the generator
    private static final DateTimeFormatter RECUR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RECUR_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");


    /**
     * Constructor for creating a new event.
     *
     * @param parent          The parent frame/dialog
     * @param controller      The controller
     * @param initialDate     The date for which to initially set the dialog
     */
    public EventDialog(Window parent, ICalendarController controller, LocalDate initialDate) { // Accept controller
        this(parent, controller, initialDate, null);
    }

    /**
     * Constructor for editing an existing event.
     *
     * @param parent          The parent frame/dialog
     * @param controller      The controller
     * @param initialDate     The date context (can be ignored if editing)
     * @param eventToEdit     The event to edit
     */
    public EventDialog(Window parent, ICalendarController controller, LocalDate initialDate, ICalendarEvent eventToEdit) { // Accept controller
        super(parent, (eventToEdit == null ? "Create New Event" : "Edit Event"), ModalityType.APPLICATION_MODAL);
        // this.calendarManager = calendarManager; // Removed model assignment
        this.controller = controller; // Store controller
        this.initialDate = initialDate;
        this.eventToEdit = eventToEdit;

        initializeComponents();
        layoutComponents();
        registerListeners();
        populateFields(); // Populate if editing

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initializeComponents() {
        // Standard Fields
        nameField = new JTextField(25);
        allDayCheckBox = new JCheckBox("All Day Event");
        // Use SimpleDateFormat for Swing's DateFormatter
        startDateField = new JFormattedTextField(new DefaultFormatterFactory(new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"))));
        startTimeField = new JFormattedTextField(new DefaultFormatterFactory(new DateFormatter(new SimpleDateFormat("HH:mm"))));
        endDateField = new JFormattedTextField(new DefaultFormatterFactory(new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"))));
        endTimeField = new JFormattedTextField(new DefaultFormatterFactory(new DateFormatter(new SimpleDateFormat("HH:mm"))));
        descriptionArea = new JTextArea(3, 25);
        locationField = new JTextField(25);
        publicCheckBox = new JCheckBox("Public Event", true); // Default to public

        // Set initial values (will be overwritten if editing)
        // Convert java.time objects to java.util.Date for Swing formatters
        ZoneId defaultZone = ZoneId.systemDefault();
        startDateField.setValue(java.util.Date.from(initialDate.atStartOfDay(defaultZone).toInstant()));
        LocalTime initialStartTime = LocalTime.of(9, 0);
        startTimeField.setValue(java.util.Date.from(initialDate.atTime(initialStartTime).atZone(defaultZone).toInstant()));
        endDateField.setValue(java.util.Date.from(initialDate.atStartOfDay(defaultZone).toInstant()));
        LocalTime initialEndTime = LocalTime.of(10, 0);
        endTimeField.setValue(java.util.Date.from(initialDate.atTime(initialEndTime).atZone(defaultZone).toInstant()));


        // Recurrence Fields
        recurringCheckBox = new JCheckBox("Recurring Event");
        recurrencePanel = new JPanel(); // Layout will be set later
        recurrencePanel.setBorder(BorderFactory.createTitledBorder("Recurrence Rules"));
        recurrencePanel.setVisible(false); // Initially hidden

        weekdaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        weekdayCheckBoxes = new JCheckBox[7];
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            weekdayCheckBoxes[i] = new JCheckBox(days[i]);
            weekdaysPanel.add(weekdayCheckBoxes[i]);
        }

        terminationGroup = new ButtonGroup();
        forNTimesRadioButton = new JRadioButton("For");
        occurrencesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1)); // Default 5 times
        occurrencesSpinner.setEnabled(false); // Disabled initially
        untilDateRadioButton = new JRadioButton("Until");
        // Use SimpleDateFormat for Swing's DateFormatter
        untilDateField = new JFormattedTextField(new DefaultFormatterFactory(new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"))));
        // Convert LocalDate to java.util.Date for the formatter
        untilDateField.setValue(java.util.Date.from(initialDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        untilDateField.setEnabled(false); // Disabled initially

        terminationGroup.add(forNTimesRadioButton);
        terminationGroup.add(untilDateRadioButton);

        // Buttons
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Name
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nameField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        // Row 1: All Day
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3;
        mainPanel.add(allDayCheckBox, gbc);
        gbc.gridwidth = 1;

        // Row 2: Start Date/Time
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Start:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        mainPanel.add(startDateField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        mainPanel.add(startTimeField, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; // Spacer
        mainPanel.add(Box.createHorizontalStrut(50), gbc);


        // Row 3: End Date/Time
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("End:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        mainPanel.add(endDateField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        mainPanel.add(endTimeField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; // Reset

        // Row 4: Description
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new JScrollPane(descriptionArea), gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST; // Reset

        // Row 5: Location
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(locationField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        // Row 6: Public & Recurring Checkboxes
        gbc.gridx = 1; gbc.gridy = 6;
        mainPanel.add(publicCheckBox, gbc);
        gbc.gridx = 2;
        mainPanel.add(recurringCheckBox, gbc);

        // Row 7: Recurrence Panel (initially hidden)
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        layoutRecurrencePanel(); // Setup layout for recurrence panel
        mainPanel.add(recurrencePanel, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

     private void layoutRecurrencePanel() {
        recurrencePanel.setLayout(new GridBagLayout());
        GridBagConstraints rgc = new GridBagConstraints();
        rgc.insets = new Insets(2, 2, 2, 2);
        rgc.anchor = GridBagConstraints.WEST;

        // Row 0: Weekdays
        rgc.gridx = 0; rgc.gridy = 0; rgc.gridwidth = 4;
        recurrencePanel.add(new JLabel("Repeat on:"), rgc);
        rgc.gridy = 1;
        recurrencePanel.add(weekdaysPanel, rgc);

        // Row 2: Termination options
        rgc.gridy = 2; rgc.gridwidth = 1;
        recurrencePanel.add(forNTimesRadioButton, rgc);
        rgc.gridx = 1;
        recurrencePanel.add(occurrencesSpinner, rgc);
        rgc.gridx = 2;
        recurrencePanel.add(new JLabel("times"), rgc);

        rgc.gridx = 0; rgc.gridy = 3;
        recurrencePanel.add(untilDateRadioButton, rgc);
        rgc.gridx = 1; rgc.gridwidth = 2; rgc.fill = GridBagConstraints.HORIZONTAL;
        recurrencePanel.add(untilDateField, rgc);
    }


    private void registerListeners() {
        // Cancel Button
        cancelButton.addActionListener(e -> dispose());

        // Save Button
        saveButton.addActionListener(e -> saveEvent());

        // All Day Checkbox
        allDayCheckBox.addActionListener(e -> {
            boolean isAllDay = allDayCheckBox.isSelected();
            startTimeField.setEnabled(!isAllDay);
            endTimeField.setEnabled(!isAllDay);
            // If switching to all-day, maybe clear/disable recurrence? (TBD based on requirements)
            // If switching off all-day, ensure end time is after start time
            if (!isAllDay) {
                try {
                    // Get time from fields (might be null if invalid)
                    java.util.Date utilStartTime = (java.util.Date) startTimeField.getValue();
                    java.util.Date utilEndTime = (java.util.Date) endTimeField.getValue();
                    if (utilStartTime != null) {
                        LocalTime start = utilStartTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
                        LocalTime end = (utilEndTime != null) ? utilEndTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime() : null;
                        if (end == null || !end.isAfter(start)) {
                             // Convert back to java.util.Date to set value
                            endTimeField.setValue(java.util.Date.from(start.plusHours(1).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
                        }
                    }
                } catch (Exception ex) { /* Ignore parsing/casting errors here */ }
            }
        });

        // Recurring Checkbox
        recurringCheckBox.addActionListener(e -> {
            recurrencePanel.setVisible(recurringCheckBox.isSelected());
            pack(); // Adjust dialog size
        });

        // Recurrence Termination Radio Buttons
        ActionListener terminationListener = e -> {
            occurrencesSpinner.setEnabled(forNTimesRadioButton.isSelected());
            untilDateField.setEnabled(untilDateRadioButton.isSelected());
        };
        forNTimesRadioButton.addActionListener(terminationListener);
        untilDateRadioButton.addActionListener(terminationListener);

        // Ensure end date is not before start date
        FocusAdapter dateFocusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateDates();
            }
        };
        startDateField.addFocusListener(dateFocusAdapter);
        endDateField.addFocusListener(dateFocusAdapter);
        startTimeField.addFocusListener(dateFocusAdapter);
        endTimeField.addFocusListener(dateFocusAdapter);
    }

    private void validateDates() {
        try {
            // Convert java.util.Date from fields back to java.time objects for comparison
            ZoneId defaultZone = ZoneId.systemDefault();
            java.util.Date utilStartDate = (java.util.Date) startDateField.getValue();
            java.util.Date utilStartTime = (java.util.Date) startTimeField.getValue();
            java.util.Date utilEndDate = (java.util.Date) endDateField.getValue();
            java.util.Date utilEndTime = (java.util.Date) endTimeField.getValue();

            if (utilStartDate == null || utilEndDate == null) return; // Not enough info yet

            LocalDate startDate = utilStartDate.toInstant().atZone(defaultZone).toLocalDate();
            LocalTime startTime = (utilStartTime != null) ? utilStartTime.toInstant().atZone(defaultZone).toLocalTime() : LocalTime.MIDNIGHT;
            LocalDate endDate = utilEndDate.toInstant().atZone(defaultZone).toLocalDate();
            LocalTime endTime = (utilEndTime != null) ? utilEndTime.toInstant().atZone(defaultZone).toLocalTime() : LocalTime.MIDNIGHT;

            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (endDateTime.isBefore(startDateTime)) {
                // If end is before start, reset end to start date/time + 1 hour
                endDateField.setValue(utilStartDate); // Reset end date field to start date
                LocalTime newEndTime = startTime.plusHours(1);
                // Convert back to java.util.Date to set value
                endTimeField.setValue(java.util.Date.from(startDate.atTime(newEndTime).atZone(defaultZone).toInstant()));
            }
        } catch (Exception e) {
            // Ignore errors during intermediate validation (e.g., ClassCastException if field is empty/invalid)
        }
    }


    /**
     * Populates the dialog fields if editing an existing event.
     */
    private void populateFields() {
        if (eventToEdit != null) {
            ZoneId defaultZone = ZoneId.systemDefault(); // Needed for conversion

            nameField.setText(eventToEdit.getEventName());
            allDayCheckBox.setSelected(eventToEdit.isAllDay());

            // Convert java.time to java.util.Date for JFormattedTextFields
            LocalDateTime startDateTime = eventToEdit.getStart();
            LocalDateTime endDateTime = eventToEdit.getEnd();

            startDateField.setValue(java.util.Date.from(startDateTime.atZone(defaultZone).toInstant()));
            startTimeField.setValue(java.util.Date.from(startDateTime.atZone(defaultZone).toInstant())); // Formatter extracts time

            // Handle potential end date/time issues for all-day or specific model logic
            // For simplicity, use the stored end date/time directly for population
            endDateField.setValue(java.util.Date.from(endDateTime.atZone(defaultZone).toInstant()));
            endTimeField.setValue(java.util.Date.from(endDateTime.atZone(defaultZone).toInstant())); // Formatter extracts time


            descriptionArea.setText(eventToEdit.getDescription());
            locationField.setText(eventToEdit.getLocation());
            publicCheckBox.setSelected(eventToEdit.isPublic());

            // Disable fields not editable for existing events (e.g., recurrence)
            // For now, we assume recurrence cannot be edited once created.
            recurringCheckBox.setEnabled(false);

            // Trigger state update for all-day checkbox
            allDayCheckBox.getActionListeners()[0].actionPerformed(null);
        }
    }

    /**
     * Handles the save action. Validates input and calls the model.
     */
    private void saveEvent() {
        // --- Input Validation ---
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showValidationError("Event name cannot be empty.");
            return;
        }

        LocalDate startDate, endDate;
        LocalTime startTime = LocalTime.MIDNIGHT, endTime = LocalTime.MIDNIGHT; // Defaults for all-day
        ZoneId defaultZone = ZoneId.systemDefault();

        // Convert java.util.Date from fields back to java.time objects
        try {
            java.util.Date utilStartDate = (java.util.Date) startDateField.getValue();
            java.util.Date utilEndDate = (java.util.Date) endDateField.getValue();
            if (utilStartDate == null || utilEndDate == null) throw new NullPointerException("Date fields cannot be null.");
            startDate = utilStartDate.toInstant().atZone(defaultZone).toLocalDate();
            endDate = utilEndDate.toInstant().atZone(defaultZone).toLocalDate();
        } catch (Exception e) {
            showValidationError("Invalid start or end date format (YYYY-MM-DD). Check fields.");
            return;
        }

        boolean isAllDay = allDayCheckBox.isSelected();
        if (!isAllDay) {
            try {
                java.util.Date utilStartTime = (java.util.Date) startTimeField.getValue();
                java.util.Date utilEndTime = (java.util.Date) endTimeField.getValue();
                if (utilStartTime == null || utilEndTime == null) throw new NullPointerException("Time fields cannot be null for non-all-day events.");
                startTime = utilStartTime.toInstant().atZone(defaultZone).toLocalTime();
                endTime = utilEndTime.toInstant().atZone(defaultZone).toLocalTime();
            } catch (Exception e) {
                showValidationError("Invalid start or end time format (HH:MM). Check fields.");
                return;
            }
        }

        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        // For all-day events, the end time is often exclusive (start of next day) or handled by model.
        // Let's keep it consistent with start time for now if all-day.
        LocalDateTime endDateTime = LocalDateTime.of(endDate, isAllDay ? startTime : endTime);

        if (!isAllDay && !endDateTime.isAfter(startDateTime)) {
            showValidationError("End date/time must be after start date/time.");
            return;
        }
        // For all-day events, the model usually handles setting the end time (e.g., start of next day)
        // But we ensure the dates are valid at least.
        if (isAllDay && endDate.isBefore(startDate)) {
             showValidationError("End date must be on or after start date for all-day events.");
            return;
        }


        // --- Recurrence Validation (if applicable) ---
        String recurrenceRule = null;
        if (recurringCheckBox.isSelected() && eventToEdit == null) { // Only allow setting recurrence on create
            String selectedWeekdays = getSimpleWeekdayString(weekdayCheckBoxes); // Use simple format
            if (selectedWeekdays.isEmpty()) {
                showValidationError("Select at least one day for recurrence.");
                return;
            }

            if (forNTimesRadioButton.isSelected()) {
                int occurrences = (int) occurrencesSpinner.getValue();
                // Construct simple rule: "MWF for 5 times"
                recurrenceRule = String.format("%s for %d times", selectedWeekdays, occurrences);
            } else if (untilDateRadioButton.isSelected()) {
                LocalDate untilDate;
                try {
                    // Convert java.util.Date from field back to LocalDate
                    java.util.Date utilUntilDate = (java.util.Date) untilDateField.getValue();
                    if (utilUntilDate == null) throw new NullPointerException("Recurrence until date cannot be null.");
                    untilDate = utilUntilDate.toInstant().atZone(defaultZone).toLocalDate();
                } catch (Exception e) {
                    showValidationError("Invalid recurrence until date format (YYYY-MM-DD). Check field.");
                    return;
                }
                 if (untilDate.isBefore(startDate)) {
                    showValidationError("Recurrence until date cannot be before the event start date.");
                    return;
                }
                // Construct simple rule: "WR until YYYY-MM-DD" or "WR until YYYY-MM-DDTHH:MM"
                String untilFormatted;
                if (isAllDay) {
                    untilFormatted = untilDate.format(RECUR_DATE_FORMAT);
                } else {
                    // Use the event's start time for the 'until' datetime boundary
                    // Or should it be end of day? Generator logic uses start of next day for all-day
                    // and exact time for timed. Let's use exact time.
                    LocalDateTime untilDateTime = untilDate.atTime(startTime); // Use start time
                    untilFormatted = untilDateTime.format(RECUR_DATETIME_FORMAT);
                }
                recurrenceRule = String.format("%s until %s", selectedWeekdays, untilFormatted);

            } else {
                showValidationError("Select a recurrence termination condition ('for N times' or 'until date').");
                return;
            }
        }


        // --- Call Model ---
        try {
            if (eventToEdit == null) {
                // --- Create New Event ---
                // Check if the RECURRING checkbox is selected, NOT just if the rule string was built
                if (recurringCheckBox.isSelected()) {
                    if (recurrenceRule == null) {
                        // This should not happen if the checkbox is checked and validation passed, but handle defensively
                         showValidationError("Recurrence rule is missing despite checkbox being selected.");
                         return;
                    }
                    // Call controller to add recurring event
                    controller.addRecurringEvent(
                        name, startDateTime, endDateTime, recurrenceRule, isAllDay,
                        descriptionArea.getText().trim(),
                        locationField.getText().trim(),
                        publicCheckBox.isSelected()
                    );
                    JOptionPane.showMessageDialog(this, "Recurring event created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

                } else {
                    // Create SINGLE event via controller, passing raw data
                    controller.createNewSingleEvent(
                        name, startDateTime, endDateTime, isAllDay,
                        descriptionArea.getText().trim(),
                        locationField.getText().trim(),
                        publicCheckBox.isSelected()
                    );
                    JOptionPane.showMessageDialog(this, "Event created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }

            } else {
                // --- Edit Existing Event ---
                // Part 3 requires supporting different edit modes (single instance, series from date, all series).
                // This simple dialog doesn't capture that intent.
                // For now, implement basic property editing for the single instance shown.
                // WARNING: This bypasses complex recurring edit logic and conflict checks on edit.

                // Use model's edit methods if possible, but need to identify which one.
                // Simplest case: edit properties of the specific instance using model methods.
                boolean updated = false;
                String originalName = eventToEdit.getEventName(); // Store original name for identification
                LocalDateTime originalStart = eventToEdit.getStart();
                LocalDateTime originalEnd = eventToEdit.getEnd();

                // --- Edit properties using model's editSingleEvent ---
                // Note: This assumes editSingleEvent can handle these properties.
                // It also assumes we only edit THIS specific instance.

                String newDescription = descriptionArea.getText().trim();
                if (!newDescription.equals(eventToEdit.getDescription())) {
                    updated |= controller.editSingleEvent("description", originalName, originalStart, originalEnd, newDescription);
                }

                String newLocation = locationField.getText().trim();
                if (!newLocation.equals(eventToEdit.getLocation())) {
                     updated |= controller.editSingleEvent("location", originalName, originalStart, originalEnd, newLocation);
                }

                boolean newPublicStatus = publicCheckBox.isSelected();
                 if (newPublicStatus != eventToEdit.isPublic()) {
                     updated |= controller.editSingleEvent("public", originalName, originalStart, originalEnd, String.valueOf(newPublicStatus));
                 }

                // --- Edit All-Day Status ---
                if (isAllDay != eventToEdit.isAllDay()) {
                    // Need to adjust times if changing all-day status
                    if (isAllDay) {
                        startDateTime = startDateTime.toLocalDate().atStartOfDay();
                        endDateTime = endDateTime.toLocalDate().plusDays(1).atStartOfDay(); // Model expects exclusive end
                    } // else: startDateTime/endDateTime already have times from fields
                    // Edit allDay property first? Or time properties? Let's assume time edit handles allDay implicitly if needed by model.
                    // For now, just edit times if they changed.
                    // updated |= controller.editSingleEvent("allDay", originalName, originalStart, originalEnd, String.valueOf(isAllDay));
                }

                // --- Edit Start Time ---
                if (!startDateTime.equals(originalStart)) {
                     // Format using ISO standard which editSingleEvent likely expects if parsing
                     updated |= controller.editSingleEvent("start", originalName, originalStart, originalEnd, startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                     originalStart = startDateTime; // Update identifier for subsequent edits in this save operation
                }

                // --- Edit End Time ---
                 if (!endDateTime.equals(originalEnd)) {
                     // Format using ISO standard
                     updated |= controller.editSingleEvent("end", originalName, originalStart, originalEnd, endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                     originalEnd = endDateTime; // Update identifier
                 }

                // Edit name LAST, as it's used for identification in previous edits
                if (!name.equals(originalName)) {
                    // Controller's editSingleEvent should handle finding the event correctly,
                    // even if other properties were changed in the same save operation.
                    updated |= controller.editSingleEvent("name", originalName, originalStart, originalEnd, name);
                }


                // --- Final Feedback ---
                 if (updated) {
                     JOptionPane.showMessageDialog(this, "Event updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                 } else {
                     JOptionPane.showMessageDialog(this, "No changes detected or update failed.", "Info", JOptionPane.INFORMATION_MESSAGE);
                     return; // Don't close if nothing changed
                 }
            }

            saved = true;
            dispose(); // Close dialog on successful save

        } catch (Exception ex) {
            // Catch exceptions from model (e.g., conflicts) or parsing
            showValidationError("Error saving event: " + ex.getMessage());
            ex.printStackTrace(); // For debugging
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns true if the event was saved successfully.
     *
     * @return true if saved, false otherwise
     */
    public boolean wasSaved() {
        return saved;
    }

    /**
     * Static method to show the dialog for creating/editing an event.
     *
     * @param parent          The parent window
     * @param controller      The controller
     * @param date            The initial date context
     * @param eventToEdit     The event to edit (null for new event)
     * @return true if the event was saved, false otherwise
     */
    public static boolean showDialog(Window parent, ICalendarController controller, LocalDate date, ICalendarEvent eventToEdit) { // Accept controller
        EventDialog dialog = new EventDialog(parent, controller, date, eventToEdit);
        dialog.setVisible(true);
        return dialog.wasSaved();
    }

     /**
      * Helper method to get simple weekday string (e.g., "MWR")
      * from the checkbox states.
      */
     private String getSimpleWeekdayString(JCheckBox[] checkBoxes) {
         StringBuilder sb = new StringBuilder();
         // Generator uses M, T, W, R, F, S, U
         char[] dayCodes = {'M', 'T', 'W', 'R', 'F', 'S', 'U'};
         for (int i = 0; i < 7; i++) {
             if (checkBoxes[i].isSelected()) {
                 sb.append(dayCodes[i]);
             }
         }
         return sb.toString(); // Returns empty string if none selected
     }
}
//
//</final_file_content>
//
//IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.
//
//
//
//New problems detected after saving the file:
//src/main/java/view/EventDialog.java
//- [Java Error] Line 474: The method editSingleEvent(String, String, LocalDateTime, LocalDateTime, String) in the type ICalendarController is not applicable for the arguments (String, String, LocalDateTime, LocalDateTime, boolean)
//- [Java Error] Line 480: DateTimeFormatter.ISO_LOCAL_DATE_TIME cannot be resolved or is not a field
//- [Java Error] Line 486: DateTimeFormatter.ISO_LOCAL_DATE_TIME cannot be resolved or is not a field
//- [Java Error] Line 546: The method getCommaSeparatedWeekdays(JCheckBox[]) is undefined for the type EventDialog<environment_details>
//# VSCode Visible Files
//src/main/java/view/EventDialog.java
//
//# VSCode Open Tabs
//src/main/res/USEME.md
//src/main/java/view/ColorManager.java
//src/main/java/view/MonthViewPanel.java
//src/main/java/view/FileOperationDialog.java
//src/main/java/view/ICalendarGUI.java
//src/main/java/view/CreateCalendarDialog.java
//src/main/java/model/ModelEvent.java
//src/main/java/model/IModelEventListener.java
//src/main/java/controller/IEnhancedCalendarController.java
//src/main/java/calendar/CalendarApp.java
//src/main/java/model/CalendarConflictException.java
//src/main/java/model/EventNotFoundException.java
//src/main/java/model/InvalidDataException.java
//src/main/java/util/ICalendarExporter.java
//src/main/java/util/ExportException.java
//src/main/java/util/ICalendarImporter.java
//src/main/java/util/ImportException.java
//src/main/java/util/GoogleCsvExporter.java
//src/main/java/model/ICalendarManager.java
//src/main/java/controller/EnhancedCalendarController.java
//src/main/java/view/DayEventsDialog.java
//pom.xml
//src/test/java/tests/CommandParserTest.java
//src/test/java/tests/CommandParserCreateCalendarTest.java
//src/test/java/tests/CommandParserEditCalendarTest.java
//src/test/java/tests/CalendarControllerInteractiveTest.java
//src/main/java/util/GoogleCsvImporter.java
//src/test/java/tests/CalendarManagerTest.java
//src/main/java/model/RecurringEventGenerator.java
//src/main/java/view/CalendarGUI.java
//src/main/java/model/CalendarManager.java
//src/main/java/model/MultiCalendarManager.java
//src/test/java/tests/MultiCalendarTest.java
//src/test/java/tests/CommandParserMultiTest.java
//src/test/java/tests/CalendarAppTest.java
//src/test/java/tests/EnhancedCalendarControllerTest.java
//src/test/java/tests/CalendarAppMainTest.java
//src/main/java/controller/ICalendarController.java
//src/main/java/controller/CalendarController.java
//src/main/java/controller/CommandParser.java
//src/main/java/view/EventDialog.java
//src/main/java/model/CalendarEvent.java
//
//# Current Time
//4/8/2025, 9:28:00 PM (America/New_York, UTC-4:00)
//
//# Current Mode
//ACT MODE
//</environment_details>
