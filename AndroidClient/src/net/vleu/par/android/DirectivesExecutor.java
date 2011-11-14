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
package net.vleu.par.android;

import java.util.concurrent.atomic.AtomicInteger;

import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.Directive.ThrowingVisitor;
import net.vleu.par.gateway.models.Directive.Visitor;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Commands.HapticNotificationData;
import net.vleu.par.protocolbuffer.Commands.StatusBarNotificationData;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

/**
 *
 */
public class DirectivesExecutor implements Visitor {

    private static final AtomicInteger notificationId = new AtomicInteger();
    private final Context context;
    private final NotificationManager notificationManager;
    private final Vibrator vibrator;

    public DirectivesExecutor(final Context context) {
        this.context = context;
        this.vibrator =
                (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.notificationManager =
                (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void execute(final DirectiveData data) {
        Directive.accept(data, this);
    }

    /**
     * Do not call this directly, it will be called by
     * {@link Directive#accept(DirectiveData, ThrowingVisitor) for
     * #execute(DirectiveData)
     */
    @Override
    public void visit(final HapticNotificationData data) {
        long[] vibrateSequence;
        if (data.getVibrationSequenceCount() == 0)
            vibrateSequence = Directive.DEFAULT_VIBRATION_SEQUENCE;
        else {
            vibrateSequence = new long[data.getVibrationSequenceCount()];
            for (int n = 0; n < vibrateSequence.length; n++)
                vibrateSequence[n] = data.getVibrationSequence(n);
        }
        this.vibrator.vibrate(vibrateSequence, -1);
    }

    /**
     * Do not call this directly, it will be called by
     * {@link Directive#accept(DirectiveData, ThrowingVisitor) for
     * #execute(DirectiveData)
     */
    @Override
    public void visit(final StatusBarNotificationData data) {
        final Notification notification = new Notification();
        notification.tickerText = data.getTitle();
        notification.icon = R.drawable.ic_launcher;
        notification.when = System.currentTimeMillis();
        final Intent notificationIntent =
                new Intent(this.context, DirectivesExecutor.class);
        final PendingIntent contentIntent =
                PendingIntent.getActivity(this.context, 0, notificationIntent,
                        0);
        notification.setLatestEventInfo(this.context, data.getTitle(),
                data.getText(), contentIntent);
        this.notificationManager.notify(notificationId.incrementAndGet(),
                notification);
    }

}
