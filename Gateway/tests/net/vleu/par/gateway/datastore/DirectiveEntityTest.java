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

import static org.junit.Assert.assertEquals;
import net.vleu.par.models.Directive;
import net.vleu.par.models.UserId;
import net.vleu.par.models.UserIdTest;
import net.vleu.par.models.Directive.InvalidDirectiveSerialisation;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Commands.StatusBarNotificationData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Tests for {@link DirectiveEntity}
 */
public class DirectiveEntityTest {

    public static final Directive DUMMY_DIRECTIVE = buildDummyDirective();

    private static final String NOTIFICATION_TEXT = "Notification text";
    private static final String NOTIFICATION_TITLE = "Notification title";

    private static Directive buildDummyDirective() {
        final DirectiveData.Builder proto = DirectiveData.newBuilder();
        final StatusBarNotificationData.Builder notificationBuilder =
                StatusBarNotificationData.newBuilder();
        notificationBuilder.setTitle(NOTIFICATION_TITLE);
        notificationBuilder.setText(NOTIFICATION_TEXT);
        proto.addStatusbarNotification(notificationBuilder);
        return new Directive(proto.build());
    }

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setStoreDelayMs(0));

    @Before
    public void setUpLocalServiceTest() {
        this.helper.setUp();
    }

    @After
    public void tearDownLocalServiceTest() {
        this.helper.tearDown();
    }

    /**
     * Test method for {@link DirectiveEntity#directiveFromEntity(Entity)}.
     */
    @Test
    public void testDirectiveFromEntity() {
        final Entity entity =
                DirectiveEntity.entityFromDirective(UserIdTest.DUMMY_USER_ID,
                        DeviceEntityTest.DUMMY_DEVICE_ID, DUMMY_DIRECTIVE);
        assertEquals(UserEntity.keyForId(UserIdTest.DUMMY_USER_ID), entity.getParent()
                .getParent());
        assertEquals(DeviceEntity.keyForIds(UserIdTest.DUMMY_USER_ID,
                DeviceEntityTest.DUMMY_DEVICE_ID), entity.getParent());
    }

    /**
     * Test method for
     * {@link DirectiveEntity#entityFromDirective(UserId, Directive)} and
     * {@link DirectiveEntity#directiveFromEntity(Entity)}.
     * 
     * @throws InvalidDirectiveSerialisation
     *             Test failed
     */
    @Test
    public void testEntityToDirectiveWayAndBack()
            throws InvalidDirectiveSerialisation {
        final Entity entity =
                DirectiveEntity.entityFromDirective(UserIdTest.DUMMY_USER_ID,
                        DeviceEntityTest.DUMMY_DEVICE_ID, DUMMY_DIRECTIVE);
        final Directive unserialized =
                DirectiveEntity.directiveFromEntity(entity);
        assertEquals(DUMMY_DIRECTIVE, unserialized);
    }

}
