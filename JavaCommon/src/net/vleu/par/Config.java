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
package net.vleu.par;

/**
 * Configuration settings
 */
public class Config {
    public static final String SERVER_DOMAIN = "vleupar.appspot.com";
    public static final String SERVER_BASE_URL = "https://" + SERVER_DOMAIN;
    public static final String SERVER_BASE_RPC_URL = SERVER_BASE_URL + "/api/0";
    public static final String SERVER_RPC_JSON_SUFFIX = "json";
    public static final String SERVER_RPC_PROTOBUFF_SUFFIX = "pb";
    public static final String SERVER_RPC_URL_JSON = SERVER_BASE_RPC_URL + "/"
        + SERVER_RPC_JSON_SUFFIX;
    public static final String SERVER_RPC_URL_PROTOBUFF = SERVER_BASE_RPC_URL
        + "/" + SERVER_RPC_PROTOBUFF_SUFFIX;
    /** If true, the JSON will be indexed by fields numbers instead of fields names */
    public static final boolean SERVER_RPC_JSON_NUMERIC = true;

    protected Config() {
    }
}
