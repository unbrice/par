package net.vleu.par.gwt.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PARGwt implements EntryPoint {
    private final ActivityManager activityManager;
    private final SimpleEventBus eventBus;

    public PARGwt() {
        final DesktopActivityMapper mapper = new DesktopActivityMapper();
        this.eventBus = new SimpleEventBus();
        this.activityManager = new ActivityManager(mapper, this.eventBus);
    }

    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        final DesktopBaseUi ui = new DesktopBaseUi();
        this.activityManager.setDisplay(ui.getMainPanel());
        RootPanel.get().add(ui);
    }

}
