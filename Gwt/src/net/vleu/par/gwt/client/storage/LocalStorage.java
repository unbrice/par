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
package net.vleu.par.gwt.client.storage;

import java.util.HashMap;

import com.google.gwt.storage.client.Storage;

/**
 * Allows to retain value, either in a persistent store provided by
 * {@link PersistentStorage} or in memory
 */
final class LocalStorage {

    /**
     * Can be null. If not null, its values will take precedence over
     * {@link #volatileStorage}
     */
    private final PersistentStorage persistentStore;

    /**
     * {@link #persistentStore} has precedence over its values
     */
    private final HashMap<String, String> volatileStorage;

    /**
     * Creates a new {@link LocalStorage} possibly backed by its first argument
     * 
     * @param persistentStore
     *            Won't be used if null
     */
    public LocalStorage(final PersistentStorage backingStore) {
        this.persistentStore = backingStore;
        this.volatileStorage = new HashMap<String, String>();
    }

    /**
     * Tries getting the value associated with this key from the persistent
     * store. If it fails, tries with the non-persistent store
     * 
     * @param key
     *            the key to a value in the Storage
     * @return The value, null if there is none
     */
    public String getFromAnyStore(final String key) {
        String res = getFromPersistentStore(key);
        if (res == null)
            res = this.volatileStorage.get(key);
        return res;
    }

    /**
     * Calls {@link Storage#getItem(String)} on the {@link #persistentStore} if
     * it is not null, else returns null
     * 
     * @param key
     *            the key to a value in the Storage
     * @return The value from the backing store, null if there is none
     */
    private String getFromPersistentStore(final String key) {
        if (this.persistentStore != null)
            return this.persistentStore.getItem(key);
        else
            return null;
    }

    /**
     * Calls {@link Storage#setItem(String, String)} on the
     * {@link #persistentStore} if it is not null, else does nothing.
     * 
     * @param key
     *            the key to a value in the Storage
     * @param data
     *            the value associated with the key
     */
    private void putInPersistentStore(final String key, final String data) {
        if (this.persistentStore != null)
            this.persistentStore.setItem(key, data);
    }

    /**
     * Tries setting the value associated with this key in the persistent store,
     * if it fails, tries again with the non-persistent store.
     * 
     * @param key
     *            the key to a value in the Storage
     * @param value
     *            the value associated with the key
     */
    public void putPersistingIfPossible(final String key, final String value) {
        if (this.persistentStore != null)
            putInPersistentStore(key, value);
        else
            this.volatileStorage.put(key, value);

    }
}
