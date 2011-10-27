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
package net.vleu.par.gateway.datastore;

import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.Directive.InvalidDirectiveSerialisation;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public final class DirectiveEntity {
    public static final String KIND = "Directive";
    public static final String PROTOCOL_BUFFER_PROPERTY = "protobuff";

    public static Directive directiveFromEntity(final Entity entity)
            throws InvalidDirectiveSerialisation {
        assert (entity.getKind() == KIND);
        final Blob asBlob = (Blob) entity.getProperty(PROTOCOL_BUFFER_PROPERTY);
        return Directive.fromProtocolBuffer(asBlob.getBytes());
    }

    public static Entity entityFromDirective(final UserId ownerId,
            final Directive directive) {
        final Blob asBlob = new Blob(directive.asProtocolBufferBytes());
        final Key parentKey = UserEntity.keyForId(ownerId);
        final Entity res = new Entity(KIND, parentKey);
        res.setUnindexedProperty(PROTOCOL_BUFFER_PROPERTY, asBlob);
        return res;
    }

    private DirectiveEntity() {

    }
}
