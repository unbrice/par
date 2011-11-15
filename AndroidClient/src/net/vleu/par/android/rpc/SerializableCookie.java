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
/*
 * Based on Android Asynchronous Http Client
 * Copyright (c) 2011 James Smith <james@loopj.com>
 * http://loopj.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vleu.par.android.rpc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
 * A wrapper class around {@link Cookie} and/or {@link BasicClientCookie}
 * designed for use in {@link PersistentCookieStore}.
 */
class SerializableCookie implements Serializable {
    private static final long serialVersionUID = 6374381828722046732L;

    private transient BasicClientCookie clientCookie;
    private transient final Cookie cookie;

    public SerializableCookie(final Cookie cookie) {
        this.cookie = cookie;
    }

    public Cookie getCookie() {
        Cookie bestCookie = this.cookie;
        if (this.clientCookie != null)
            bestCookie = this.clientCookie;
        return bestCookie;
    }

    private void readObject(final ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        final String name = (String) in.readObject();
        final String value = (String) in.readObject();
        this.clientCookie = new BasicClientCookie(name, value);
        this.clientCookie.setComment((String) in.readObject());
        this.clientCookie.setDomain((String) in.readObject());
        this.clientCookie.setExpiryDate((Date) in.readObject());
        this.clientCookie.setPath((String) in.readObject());
        this.clientCookie.setVersion(in.readInt());
        this.clientCookie.setSecure(in.readBoolean());
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeObject(this.cookie.getName());
        out.writeObject(this.cookie.getValue());
        out.writeObject(this.cookie.getComment());
        out.writeObject(this.cookie.getDomain());
        out.writeObject(this.cookie.getExpiryDate());
        out.writeObject(this.cookie.getPath());
        out.writeInt(this.cookie.getVersion());
        out.writeBoolean(this.cookie.isSecure());
    }
}
