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
package net.vleu.par.gwt.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;

/**
 * This is a workaround for
 * http://code.google.com/p/google-web-toolkit/issues/detail?id=6653 Use it the
 * way you would use {@link AbstractActivity}
 */
public abstract class Bug6653AbstractActivity extends AbstractActivity {

    /**
     * Casts from {@link com.google.gwt.event.shared.EventBus} to
     * {@link EventBus} and calls {@link #start(AcceptsOneWidget, EventBus)}
     */
    @Override
    @Deprecated
    public void start(final AcceptsOneWidget panel,
            final com.google.gwt.event.shared.EventBus eventBus) {
        start(panel, (EventBus) eventBus);
    }

    /**
     * @see Activity#start(AcceptsOneWidget,
     *      com.google.gwt.event.shared.EventBus)
     */
    public abstract void start(AcceptsOneWidget panel, EventBus eventBus);

}
