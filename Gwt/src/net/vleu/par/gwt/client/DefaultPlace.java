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

import com.google.gwt.place.shared.PlaceTokenizer;

/**
 * A place representing the default state of the application
 */
public class DefaultPlace extends AppPlace {
    /**
     * Used by the {@link AppPlaceHistoryMapper}. The token is always the empty
     * string ("")
     */
    public static class Tokenizer implements PlaceTokenizer<DefaultPlace> {

        @Override
        public DefaultPlace getPlace(final String token) {
            return new DefaultPlace();
        }

        @Override
        public String getToken(final DefaultPlace place) {
            return "";
        }

    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof DefaultPlace;
    }

    @Override
    public int hashCode() {
        return 42;
    }
}
