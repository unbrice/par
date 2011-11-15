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
package net.vleu.par.utils;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.validateCertificate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.C2dmToken;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;

@ThreadSafe
public class C2dmRequestFactory {
    /** A HTTPHeader with the Content-Type of an UTF-8 form */
    private static final HTTPHeader CONTENT_TYPE_HTTPHEADER = new HTTPHeader(
            "Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    /**
     * All messages for version 0 are collapsed, because they have no useful
     * payload.
     */
    private static final String POST_COLLAPSE_KEY = "collapse_key=v0";
    /** The payload is just a version number */
    private static final String POST_PAYLOAD = "data.v=0";
    /** "registration_id", used for building POST requests */
    private static final String POST_REGISTRATION_ID_NAME = "registration_id";
    /** A URL representing "https://android.clients.google.com/c2dm/send" */
    private static final URL URL;
    /** "UTF-8" */
    private static final String UTF8 = "UTF-8";

    /* Initializes URL */
    static {
        try {
            URL = new URL("https://android.clients.google.com/c2dm/send");
        }
        catch (final MalformedURLException e) {
            /* This cannot happen because the URL text is hardcoded */
            throw new ExceptionInInitializerError(e);
        }
    }

    private static byte[] buildPostData(final C2dmToken registrationId)
            throws UnsupportedEncodingException {
        final StringBuilder postDataBuilder = new StringBuilder();
        postDataBuilder.append(POST_REGISTRATION_ID_NAME).append('=')
                .append(registrationId.value);
        postDataBuilder.append('&').append(POST_COLLAPSE_KEY);
        postDataBuilder.append('&').append(POST_PAYLOAD);
        return postDataBuilder.toString().getBytes(UTF8);
    }

    /** A HTTPHeader with the authorization token */
    private final HTTPHeader authTokenHTTPHeader;

    public C2dmRequestFactory(final String authToken) {
        this.authTokenHTTPHeader =
                new HTTPHeader("Authorization", "GoogleLogin auth=" + authToken);
    }

    public HTTPRequest buildRequest(final C2dmToken registrationId)
            throws IOException {
        final HTTPRequest request =
                new HTTPRequest(URL, HTTPMethod.POST, validateCertificate());
        final byte[] postData = buildPostData(registrationId);
        request.setHeader(CONTENT_TYPE_HTTPHEADER);
        request.setHeader(this.authTokenHTTPHeader);
        request.setHeader(new HTTPHeader("Content-Length", Integer
                .toString(postData.length)));
        request.setPayload(postData);
        return request;
    }
}
