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

/**
 * Non-instatiable class representing HTTP constants.
 */
final class HTTPCodes {
    /** HTTP error code 400 */
    public static final int HTTP_BAD_REQUEST_STATUS = 400;
    /** HTTP error code 403 */
    public static final int HTTP_FORBIDDEN_STATUS = 403;
    /** HTTP error code 410 */
    public static final int HTTP_GONE_STATUS = 410;

    private HTTPCodes() {

    }
}