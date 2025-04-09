package controller;

import model.IModelEventListener;
import view.IOutputHandler; // Need this for the getter

/**
 * Enhanced controller interface that extends the base controller
 * and adds model event listening capabilities. This controller is designed
 * to properly mediate between the model and view layers, adhering strictly
 * to MVC principles.
 */
public interface IEnhancedCalendarController extends ICalendarController, IModelEventListener {

    /**
     * Initializes the controller, setting up necessary connections like
     * registering as a listener to the model.
     */
    void initialize();

    /**
     * Gets the output handler associated with this controller, which can be used
     * for logging messages or displaying information (e.g., in a console or status bar).
     *
     * @return The output handler instance used by this controller.
     */
    IOutputHandler getOutputHandler();

    // Inherits all methods from ICalendarController
    // Inherits onModelEvent(ModelEvent event) from IModelEventListener
}
