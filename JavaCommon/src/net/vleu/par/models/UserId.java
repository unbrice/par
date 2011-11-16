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
package net.vleu.par.models;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class UserId {
    /**
     * Creates a UserID from a Google ID as per
     * {@link com.google.appengine.api.users.User#getUserId()

     */
    public static UserId fromGoogleAuthId(final String googleAuthId) {
        return new UserId(googleAuthId);
    }

    /**
     * A Google ID as per
     * {@link com.google.appengine.api.users.User#getUserId()

     */
    private final String googleAuthId;

    /**
     * Creates a UserID from a Google ID as per
     * {@link com.google.appengine.api.users.User#getUserId()

     */
    private UserId(final String googleAuthId) {
        this.googleAuthId = googleAuthId;
    }

    /** @return An opaque string representing the user */
    public String asString() {
        return this.googleAuthId;
    }
}
