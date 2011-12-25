/*
 * Copyright ©2011 Brice Arnould
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.vleu.par.gwt.client.rpc;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.vleu.par.gwt.client.events.DeviceListChangedEvent;
import net.vleu.par.gwt.client.events.DeviceListRequestedEvent;
import net.vleu.par.gwt.client.events.DeviceListRequestedHandler;
import net.vleu.par.gwt.client.events.NewDirectiveEvent;
import net.vleu.par.gwt.client.events.NewDirectiveHandler;
import net.vleu.par.gwt.shared.Config;
import net.vleu.par.gwt.shared.Device;
import net.vleu.par.gwt.shared.DeviceId;
import net.vleu.par.gwt.shared.DeviceName;
import net.vleu.par.protocolbuffer.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayRequestData.EnumerateDevicesData;
import net.vleu.par.protocolbuffer.GatewayRequestData.QueueDirectiveData;
import net.vleu.par.protocolbuffer.GatewayResponseData;
import net.vleu.par.protocolbuffer.GatewayResponseData.DeviceDescriptionData;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Handler for communications requests that can be fired by the event bus
 */
public class Transceiver implements DeviceListRequestedHandler,
        NewDirectiveHandler {

    /**
     * This {@link RequestCallback} just parses its arguments and delegates to
     * {@link #onResponseReceived(Request, Response)},
     * {@link #onError(Request, Response)} or
     * {@link #onError(Request, Throwable)}
     */
    private class TransceiverRequestCallback implements RequestCallback {
        private final GatewayRequestData requestProto;

        public TransceiverRequestCallback(final GatewayRequestData requestProto) {
            this.requestProto = requestProto;
        }

        @Override
        public void onError(final Request request, final Throwable exception) {
            Transceiver.this.onError(this.requestProto, exception);
        }

        @Override
        public void onResponseReceived(final Request request,
                final Response response) {
            if (response.getStatusCode() == 200) {
                final String responseText = response.getText();
                final GatewayResponseData responseProto =
                        GatewayResponseData.parse(responseText);
                Transceiver.this.onResponseReceived(this.requestProto,
                        responseProto);
            }
            else
                Transceiver.this.onError(this.requestProto, response);
        }

    }

    private static final Logger LOG = Logger.getLogger("Transceiver");

    /**
     * We post events to this when responses come
     */
    private final EventBus eventBus;

    /**
     * Used for its {@link RequestBuilder#sendRequest(String, RequestCallback)}
     * method
     */
    private final RequestBuilder requestBuilder;

    /**
     * Builds a new {@link Transceiver}. It won't automatically register to the
     * eventBus, call {@link #registerHandlersToEventBus()} for that
     * 
     * @param eventBus
     *            Events like {@link DeviceListChangedEvent} will be posted to
     *            it
     */
    public Transceiver(final EventBus eventBus) {
        this.eventBus = eventBus;
        this.requestBuilder =
                new RequestBuilder(RequestBuilder.POST,
                        Config.SERVER_RPC_URL_JSON);
    }

    /**
     * This method is private because users are expected to post their requests
     * to the {@link #eventBus}
     * 
     * @param request
     *            This request will be sent to
     */
    private void fireRequest(final GatewayRequestData requestProto) {
        final TransceiverRequestCallback callback =
                new TransceiverRequestCallback(requestProto);
        final String requestJson = GatewayRequestData.stringify(requestProto);
        try {
            this.requestBuilder.sendRequest(requestJson, callback);
        }
        catch (final RequestException e) {
            onError(requestProto, e);
        }
    }

    @Override
    public void onDeviceListRequested(final DeviceListRequestedEvent event) {
        final GatewayRequestData requestProto = GatewayRequestData.create();
        final EnumerateDevicesData enumerateProto =
                EnumerateDevicesData.create();
        requestProto.setEnumerateDevices(enumerateProto);
        fireRequest(requestProto);
    }

    /**
     * Called when a {@link GatewayRequestData} does not complete normally. A
     * 404 error is one example of the type of error that a request may
     * encounter.
     * 
     * Currently, it just logs the error
     * 
     * @param request
     *            The request that triggered the exception
     * @param exception
     *            Can be a {@link RequestTimeoutException}, a
     *            {@link RequestException}, or some other exception
     */
    public void onError(final GatewayRequestData request,
            final Response httpResponse) {
        // TODO: Retry in case of 5xx errors
        if (LogConfiguration.loggingIsEnabled())
            LOG.severe("Request " + request.toString()
                + " failed with status: " + httpResponse.getStatusText());
    }

    /**
     * Called when a {@link GatewayRequestData} does not complete normally. A
     * {@link RequestTimeoutException} is one example of the type of error that
     * a request may encounter.
     * 
     * Currently, it just logs the error
     * 
     * @param request
     *            The request that triggered the exception
     * @param exception
     *            Can be a {@link RequestTimeoutException}, a
     *            {@link RequestException}, or some other exception
     */
    public void onError(final GatewayRequestData request,
            final Throwable exception) {
        if (LogConfiguration.loggingIsEnabled())
            LOG.severe("Request " + request.toString()
                + " failed with exception: " + exception.toString());
    }

    @Override
    public void onNewGatwayRequest(final NewDirectiveEvent event) {
        final QueueDirectiveData queueDirectiveProto =
                QueueDirectiveData.create();
        final GatewayRequestData requestProto = GatewayRequestData.create();
        queueDirectiveProto.setDirective(event.getDirective());
        queueDirectiveProto.setDeviceId(event.getDeviceId().value);
        requestProto.addQueueDirective(queueDirectiveProto);
        fireRequest(requestProto);
    }

    /**
     * The only field of the response we currently handle are
     * {@link DeviceDescriptionData}
     * 
     * @param request
     *            Ignored
     * @param response
     *            We only use
     *            {@link GatewayResponseData#getDeviceDescriptions(int)} and
     *            {@link GatewayResponseData#getDeviceDescriptionsCount()}
     */
    public void onResponseReceived(final GatewayRequestData request,
            final GatewayResponseData response) {
        final int devicesNumber = response.getDeviceDescriptionsCount();
        if (devicesNumber > 0) {
            final ArrayList<Device> devicesList =
                    new ArrayList<Device>(devicesNumber);
            for (int n = 0; n < devicesNumber; n++) {
                final DeviceDescriptionData deviceProto =
                        response.getDeviceDescriptions(n);
                final DeviceId deviceId =
                        new DeviceId(deviceProto.getDeviceId());
                final DeviceName deviceName =
                        new DeviceName(deviceProto.getFriendlyName());
                devicesList.add(new Device(deviceId, deviceName));
            }
            final DeviceListChangedEvent event =
                    new DeviceListChangedEvent(devicesList);
            this.eventBus.fireEventFromSource(event, this);
        }
    }

    /**
     * Registers this {@link Transceiver} to the event bus,
     */
    public void registerHandlersToEventBus() {
        this.eventBus.addHandler(DeviceListRequestedEvent.TYPE, this);
        this.eventBus.addHandler(NewDirectiveEvent.TYPE, this);
    }
}
