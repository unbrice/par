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

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.ClientLoginToken;

/**
 * This entity represents the server's configuration.
 * 
 * It can be bootstraped from config files. However, as it may evolve at
 * runtime, it needs to be stored in a database. The {@link ServletHelper} does
 * that.
 * 
 * @see ServletHelper#readServerConfiguration()
 * @see ServletHelper#persistServerConfiguration(ServerConfiguration)
 */
@ThreadSafe
public final class ServerConfiguration {
    private ClientLoginToken c2dmAuthToken;

    /**
     * 
     * @param c2dmAuthToken
     *            Must not be null, will be returned by
     *            {@link #getC2dmAuthToken()}
     * 
     * @see ServletHelper#readServerConfiguration()
     */
    public ServerConfiguration(final ClientLoginToken c2dmAuthToken) {
        setC2dmCAuthToken(c2dmAuthToken);
    }

    /**
     * Getter for {@link #c2dmAuthToken}
     * 
     * @return The value set by {@link #setC2dmCAuthToken(String)}
     */
    public ClientLoginToken getC2dmAuthToken() {
        return this.c2dmAuthToken;
    }

    /**
     * Setter for {@link #c2dmAuthToken}
     * 
     * @param c2dmAuthToken
     *            Must not be null, will be returned by
     *            {@link #getC2dmAuthToken()}
     */
    public void setC2dmCAuthToken(final ClientLoginToken c2dmAuthToken) {
        if (null == c2dmAuthToken)
            throw new NullPointerException("c2dmAuthToken is null");
        this.c2dmAuthToken = c2dmAuthToken;
    }
}
