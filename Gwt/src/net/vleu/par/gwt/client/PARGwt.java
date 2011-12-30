package net.vleu.par.gwt.client;

import net.vleu.par.gwt.client.activities.SelectDirectiveActivity;
import net.vleu.par.gwt.client.activities.SelectDeviceTinyPresenter;
import net.vleu.par.gwt.client.events.DeviceListRequestedEvent;
import net.vleu.par.gwt.client.rpc.Transceiver;
import net.vleu.par.gwt.client.storage.AppLocalCache;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PARGwt implements EntryPoint {
    private final ActivityManager activityManager;
    private final SimpleEventBus eventBus;
    private final PlaceController placeController;
    private final Transceiver transceiver;

    public PARGwt() {
        final DesktopActivityMapper mapper = new DesktopActivityMapper();
        this.eventBus = new SimpleEventBus();
        this.activityManager = new ActivityManager(mapper, this.eventBus);
        this.placeController = new PlaceController(this.eventBus);
        this.transceiver = new Transceiver(eventBus);
    }

    /**
     * Creates a {@link PlaceHistoryHandler} associated to the {@link AppPlaceHistoryMapper}
     * @return The unregistered {@link PlaceHistoryHandler}
     */
    private PlaceHistoryHandler createHistoryHandler() {
        final AppPlaceHistoryMapper historyMapper= GWT.create(AppPlaceHistoryMapper.class);
        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        return historyHandler;
    }
    
    private void setupDesktopUi(AppLocalCache appLocalCache) {
        final DesktopBaseUi ui = new DesktopBaseUi();
        final SelectDirectiveActivity directiveSelector =
                new SelectDirectiveActivity(this.placeController);
        final SelectDeviceTinyPresenter deviceSelector =
                new SelectDeviceTinyPresenter(appLocalCache, eventBus, this.placeController);
        directiveSelector.start(ui.getLeftPanel(), this.eventBus);
        deviceSelector.start(ui.getTopPanel(), this.eventBus);
        this.activityManager.setDisplay(ui.getMainPanel());
        RootPanel.get("rootContainer").add(ui);
    }
    
    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        AppLocalCache appLocalCache = new AppLocalCache();
        PlaceHistoryHandler historyHandler = createHistoryHandler();
        
        /* Registers handlers */
        appLocalCache.registerHandlers(eventBus);
        transceiver.registerHandlersToEventBus();
        historyHandler.register(placeController, eventBus, new DefaultPlace());
        
        /* Selects the UI (well, right now there is only "Desktop") */
        setupDesktopUi(appLocalCache);
        
        /* Fires events to fill the UI */
        eventBus.fireEvent(new DeviceListRequestedEvent());
        historyHandler.handleCurrentHistory();
    }
}
