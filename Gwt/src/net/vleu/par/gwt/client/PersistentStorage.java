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
package net.vleu.par.gwt.client;

import java.util.Calendar;
import java.util.Date;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;

/**
 * Allows to persist value. Tries using Cookies, SessionStorage
 */
public final class PersistentStorage {
    private static final String COOKIE_NAME =
            "PARGwtClient_PersistentStorage_Cookie";

    private static PersistentStorage singleton;

    /**
     * @return A date one year from now
     */
    private static Date computeCookieExpiryDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return calendar.getTime();
    }

    /**
     * @return true if cookies or HTML5's LocalStorage are supported
     */
    public static boolean isSupported() {
        return Cookies.isCookieEnabled() || Storage.isLocalStorageSupported();
    }

    /**
     * This JSON Object is serialized in cookies as a substitute to HTML5
     * LocalStorage if that one is not supported
     */
    private final JSONObject cookiesJson;

    /**
     * Can be null. If not null, its values will take precedence over
     * {@link #backingMap}
     */
    private final Storage localStorage;

    /**
     * Creates or fetches a {@link PersistentStorage} backed by Cookies or
     * (preferably) by the HTML5 storage API are supported
     */
    private PersistentStorage() {
        final JSONObject existingCookiesJson = readCookies();
        this.localStorage = Storage.getLocalStorageIfSupported();
        if (existingCookiesJson == null)
            this.cookiesJson = new JSONObject();
        else
            this.cookiesJson = existingCookiesJson;
    }

    /**
     * Try to get the value associated with this key, first in the
     * {@link #localStorage}, then in the cookies
     * 
     * @param key
     *            the key to a value in the Storage
     * @return The value from the backing store, null if there is none
     */
    public String getItem(final String key) {
        if (this.localStorage != null)
            return this.localStorage.getItem(key);
        else if (this.cookiesJson.containsKey(key)) {
            final JSONString jsonStr = this.cookiesJson.get(key).isString();
            if (jsonStr == null)
                return null;
            else
                return jsonStr.stringValue();
        }
        else
            return null;
    }

    /**
     * Creates or fetches a {@link PersistentStorage} backed by Cookies or
     * (preferably) by the HTML5 storage API are supported
     * 
     * @return the new instance, or null if neither Cookies or the HTML5 storage
     *         API are supported
     */
    public PersistentStorage getSingleton() {
        if (singleton == null && isSupported())
            singleton = new PersistentStorage();
        return singleton;
    }

    /**
     * Reads {@link #cookiesJson} save by {@link #writeCookies()} in the
     * browser's cookie jar
     * 
     * @return The unserialized object, or null if there weren't any
     */
    private JSONObject readCookies() {
        final String serializedCookies = Cookies.getCookie(COOKIE_NAME);
        JSONValue jsonValue;
        if (serializedCookies == null)
            return null;
        try {
            jsonValue = JSONParser.parseLenient(serializedCookies);
        }
        catch (final Exception _) {
            return null;
        }
        return jsonValue.isObject();
    }

    /**
     * Sets the value in the Storage associated with the specified key to the
     * specified data. Note: The empty string may not be used as a key.
     * 
     * @param key
     *            the key to a value in the Storage, must not be null or empty
     * @param data
     *            the value associated with the key, must not be null
     */
    public void setItem(final String key, final String data) {
        if (this.localStorage != null)
            this.localStorage.setItem(key, data);
        else {
            final JSONString jsonValue = new JSONString(data);
            this.cookiesJson.put(key, jsonValue);
            writeCookies();
        }
    }

    /**
     * Writes {@link #cookiesJson} in the browser's cookie jar
     */
    private void writeCookies() {
        final String serializedCookies = this.cookiesJson.toString();
        Cookies.setCookie(COOKIE_NAME, serializedCookies,
                computeCookieExpiryDate(), null, null, true);
    }

}
