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

import java.util.HashMap;

import com.google.gwt.storage.client.Storage;

/**
 * Allows to retain value. If the HTML5 LocalStorage API is supported, the value
 * will persist, else they won't.
 */
public class LocalStore {

    /**
     * {@link #backingStore} has precedence over its values
     */
    private final HashMap<String, String> backingMap;

    /**
     * Can be null. If not null, its values will take precedence over
     * {@link #backingMap}
     */
    private final Storage backingStore;

    /**
     * Creates a new {@link LocalStore} possibly backed by its first argument
     * 
     * @param backingStore
     *            Won't be used if null
     */
    public LocalStore(final Storage backingStore) {
        this.backingStore = backingStore;
        this.backingMap = new HashMap<String, String>();
    }

    /**
     * Calls {@link Storage#getItem(String)} on the {@link #backingStore} if it
     * is not null, else returns null
     * 
     * @param key
     *            the key to a value in the Storage
     * @return The value from the backing store, null if there is none
     */
    private String getFromBackingStore(final String key) {
        if (hasBackingStore())
            return this.backingStore.getItem(key);
        else
            return null;
    }

    /**
     * @return False if {@link #backingStore} is null, else true
     */
    private final boolean hasBackingStore() {
        return this.backingStore != null;
    }

    /**
     * Calls {@link Storage#setItem(String, String)} on the
     * {@link #backingStore} if it is not null, else does nothing.
     * 
     * @param key
     *            the key to a value in the Storage
     * @param data
     *            the value associated with the key
     */
    private void putInBackingStore(final String key, final String data) {
        if (hasBackingStore())
            this.backingStore.setItem(key, data);
    }
}
