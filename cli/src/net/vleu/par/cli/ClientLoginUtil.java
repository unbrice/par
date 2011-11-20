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
package net.vleu.par.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class uses the old ClientLogin interface. The reasons for not using
 * OAuth is that, at the time of writing (Nov 2011), Google App Engine's support
 * for OAuth is experimental and only supports OAuth 1.0 . Using an experimental
 * API for an obsolete protocol seemed worse than using a stable API, even for
 * if it is for an even older (but still supported) protocol.
 */
//@formatter:off
/*
 *  For future reference, here are a few pointers that I used while experimenting OAuth2.0:
 *  - To present the user with a window providing an temporary OAuth token :
 *    https://accounts.google.com/o/oauth2/auth? 
 *          scope=https://www.googleapis.com/auth/userinfo.email
 *          &redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code
 *          &client_id=440176119553.apps.googleusercontent.com
 *  - To convert this token in an approved one:
 *    curl https://accounts.google.com/o/oauth2/token 
 *           -d code=${TOKEN}
 *           -d client_id=440176119553.apps.googleusercontent.com
 *           -d grant_type=authorization_code
 *           -d redirect_uri=urn:ietf:wg:oauth:2.0:oob
 *           -d client_secret=${CLIENT_SECRET}
 *  - Reference:
 *    http://code.google.com/apis/accounts/docs/OAuth2InstalledApp.html
 *
 */
//@formatter:on
public class ClientLoginUtil {
    static final String CLIENTLOGIN_URL =
            "https://www.google.com/accounts/ClientLogin";

    static final Logger logger = Logger.getLogger(ClientLoginUtil.class
            .getName());

    static String getAuthTokenUsingGoogleClientLogin(final String email,
            final String password, final String applicationName)
            throws IOException {
        final URL url = new URL(CLIENTLOGIN_URL);
        final HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-type",
                "application/x-www-form-urlencoded");
        final PrintWriter w =
                new PrintWriter(new OutputStreamWriter(
                        connection.getOutputStream()));
        w.print("Email=" + URLEncoder.encode(email, "utf-8"));
        w.print("&Passwd=" + URLEncoder.encode(password, "utf-8"));
        w.print("&service=" + URLEncoder.encode("ah", "utf-8"));
        w.print("&source=" + URLEncoder.encode(applicationName, "utf-8"));
        w.print("&accountType="
            + URLEncoder.encode("GOOGLE", "utf-8"));
        w.flush();
        w.close();
        connection.connect();

        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Auth") == false)
                continue;
            return line.substring(line.indexOf('=') + 1);
        }
        throw new AssertionError();
    }

    /**
     * @param email
     * @param password
     * @param applicationName
     *            {yourname}-{applicationName}-{versionNumber}
     * @param applicationUrl
     *            url of your application e.g.
     *            {@literal http://shin1ogawa.appspot.com/}
     * @param continueUrl
     *            url of needs authenticate e.g.
     *            {@literal http://shin1ogawa.appspot.com/pathToNeedsAuthenticate}
     * @return cookie value {@literal ACSID=....} or {@literal SACSID=....}
     * @throws IOException
     */
    public static String getCookie(final String email, final String password,
            final String applicationName, final String applicationUrl,
            final String continueUrl) throws IOException {
        final String authToken =
                getAuthTokenUsingGoogleClientLogin(email, password,
                        applicationName);
        return getCookieUsingAppengineLogin(applicationUrl, continueUrl,
                authToken);
    }

    static String getCookieUsingAppengineLogin(final String applicationUrl,
            final String continueUrl, final String authToken)
            throws IOException {
        final StringBuilder _url =
                new StringBuilder(applicationUrl)
                        .append(applicationUrl.endsWith("/") ? "" : "/")
                        .append("_ah/login?").append("continue=")
                        .append(URLEncoder.encode(continueUrl, "utf-8"))
                        .append("&").append("auth=")
                        .append(URLEncoder.encode(authToken, "utf-8"));
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "GET " + _url);
        final URL url = new URL(_url.toString());
        final boolean followRedirects = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
        try {
            final HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("X-appcfg-api-version", "dummy");
            connection.connect();

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                        "responseCode=" + connection.getResponseCode());
            final Map<String, List<String>> headers =
                    connection.getHeaderFields();
            final Iterator<Map.Entry<String, List<String>>> i =
                    headers.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry<String, List<String>> next = i.next();
                final String key = next.getKey();
                if (key == null || key.equals("Set-Cookie") == false)
                    continue;
                final List<String> values = next.getValue();
                if (values.isEmpty() == false)
                    return values.get(0);
                else
                    return null;
            }
            return null;
        }
        finally {
            HttpURLConnection.setFollowRedirects(followRedirects);
        }
    }

}
