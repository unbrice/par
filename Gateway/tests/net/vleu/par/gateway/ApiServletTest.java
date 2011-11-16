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
package net.vleu.par.gateway;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.vleu.par.C2dmToken;
import net.vleu.par.gateway.datastore.DeviceEntityTest;
import net.vleu.par.gateway.models.UserIdTest;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiServletTest {

    private static class DebugServletInputStream extends ServletInputStream {
        final InputStream backing;
        private int readBytes = 0;

        public DebugServletInputStream(final byte[] bytes) {
            this.backing = new ByteArrayInputStream(bytes);
        }

        public int getReadBytes() {
            return this.readBytes;
        }

        @Override
        public int read() throws IOException {
            this.readBytes++;
            return this.backing.read();
        }

    }

    private static final C2dmToken DUMMY_C2DM_REGISTRATION_ID = new C2dmToken(
            "Dummy Google Auth Id");

    @Mock
    private DeviceRegistrar deviceRegistrar;
    @Mock
    private DeviceWaker deviceWaker;
    @Mock
    private DirectiveStore directiveStore;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletHelper servletHelper;

    private GatewayRequestData buildDummyRequest() {
        final DeviceIdData deviceIdData =
                DeviceEntityTest.DUMMY_DEVICE_ID.asProtocolBuffer();
        final RegisterDeviceData registerDeviceData =
                RegisterDeviceData
                        .newBuilder()
                        .setDeviceId(deviceIdData)
                        .setC2DmRegistrationId(DUMMY_C2DM_REGISTRATION_ID.value)
                        .build();
        final GatewayRequestData requestData =
                GatewayRequestData.newBuilder()
                        .addRegisterDevice(registerDeviceData).build();
        return requestData;
    }

    private ApiServlet makeInjectedApiServlet() {
        return new ApiServlet(this.directiveStore, this.deviceRegistrar,
                this.deviceWaker, this.servletHelper);
    }

    private HttpServletRequest makeStubedRequest(final byte[] body)
            throws IOException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ServletInputStream inputStream =
                new DebugServletInputStream(body);

        stub(request.getInputStream()).toReturn(inputStream);
        return request;
    }

    @Test
    public void testAuth() throws IOException {
        final ApiServlet tested = makeInjectedApiServlet();
        final HttpServletRequest request = makeStubedRequest(new byte[50]);
        tested.doPost(request, this.response);
        verify(this.servletHelper).getCurrentUser();
        verify(this.response).sendError(eq(HttpCodes.HTTP_FORBIDDEN_STATUS),
                any(String.class));
    }

    @Test
    public void testEmptyRequest() throws IOException {
        final HttpServletRequest request = makeStubedRequest(new byte[0]);
        final ApiServlet tested = makeInjectedApiServlet();

        stub(this.servletHelper.getCurrentUser()).toReturn(
                UserIdTest.DUMMY_USER_ID);
        tested.doPost(request, this.response);
    }

    @Test
    public void testHugeInput() throws IOException {
        final ApiServlet tested = makeInjectedApiServlet();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final DebugServletInputStream inputStream =
                new DebugServletInputStream(
                        new byte[ApiServlet.MAX_COMMAND_SIZE * 2]);
        stub(this.servletHelper.getCurrentUser()).toReturn(
                UserIdTest.DUMMY_USER_ID);
        stub(request.getInputStream()).toReturn(inputStream);
        tested.doPost(request, this.response);
        verify(this.response).sendError(eq(HttpCodes.HTTP_BAD_REQUEST_STATUS),
                any(String.class));
        assertEquals(ApiServlet.MAX_COMMAND_SIZE, inputStream.getReadBytes());
    }

    /**
     * Checks that feeding the Servlet with null bytes results in a 5XX error.
     * 
     * @throws IOException
     *             Test failed.
     */
    @Test
    public void testNotAProtocolBuffer() throws IOException {
        final ApiServlet tested = makeInjectedApiServlet();
        final HttpServletRequest request = makeStubedRequest(new byte[50]);
        stub(this.servletHelper.getCurrentUser()).toReturn(
                UserIdTest.DUMMY_USER_ID);
        tested.doPost(request, this.response);
        verify(this.response).sendError(eq(HttpCodes.HTTP_BAD_REQUEST_STATUS),
                any(String.class));
    }

    @Test
    public void testRegisterDevice() throws IOException {
        final ApiServlet tested = makeInjectedApiServlet();
        final HttpServletRequest request =
                makeStubedRequest(buildDummyRequest().toByteArray());
        final ServletOutputStream outputStream =
                mock(ServletOutputStream.class);
        stub(this.servletHelper.getCurrentUser()).toReturn(
                UserIdTest.DUMMY_USER_ID);
        stub(this.response.getOutputStream()).toReturn(outputStream);
        tested.doPost(request, this.response);
        verify(this.response, never()).sendError(anyInt(), any(String.class));
        verify(this.deviceRegistrar).registerDevice(UserIdTest.DUMMY_USER_ID,
                DeviceEntityTest.DUMMY_DEVICE_ID,
                DeviceEntityTest.DUMMY_DEVICE_NAME, DUMMY_C2DM_REGISTRATION_ID);
        verifyZeroInteractions(this.deviceWaker);
        verifyZeroInteractions(this.directiveStore);
    }
}
