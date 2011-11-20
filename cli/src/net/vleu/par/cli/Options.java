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
package net.vleu.par.cli;

import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public final class Options {
    @Parameters(commandDescription = "Send a SMS")
    public static class SmsCommand {
        public static final String NAME = "sms";

        @Parameter(description = "message", required = true)
        public List<String> body;

        @Parameter(names = "--phone", description = "Phone number")
        public String phoneNumber;
    }

    @Parameters(commandDescription = "Send a status-bar notification")
    public static class StatusBarNotificationCommand {
        public static final String NAME = "notification";

        @Parameter(names = "--text", description = "Notification's text")
        public String text;

        @Parameter(names = "--title", description = "Notification's title")
        public String title;
    }

    public final static String PROGRAM_NAME = "par";

    @Parameter(
            names = { "--password" },
            required = true,
            description = "Your Google password, be sure to read the note about security at the end")
    public String googlePassword;

    @Parameter(names = { "--user" }, required = true,
            description = "Your Google ID")
    public String googleUserName;

    @Parameter(names = { "--help", "-h" },
            description = "Show help and returns")
    public boolean help = false;

    public final SmsCommand smsCommand = new SmsCommand();
    public final StatusBarNotificationCommand statusBarNotificationCommand =
            new StatusBarNotificationCommand();

    /**
     * Creates a new {@link JCommander} and registers itself into it
     * 
     * @return The created {@link JCommander}
     */
    public JCommander makeJcommander() {
        final JCommander jc = new JCommander(this);
        jc.setProgramName(PROGRAM_NAME);
        jc.addCommand(SmsCommand.NAME, this.smsCommand);
        jc.addCommand(StatusBarNotificationCommand.NAME,
                this.statusBarNotificationCommand);
        return jc;
    }

}
