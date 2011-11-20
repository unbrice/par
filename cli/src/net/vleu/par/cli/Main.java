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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 *
 */
public class Main {

    public static void main(final String[] args) {
        final Main main = new Main();
        final boolean validOptions;
        if (shouldDisplayUsage(args)) {
            main.showUsage();
            return;
        }
        validOptions = main.parse(args);
        if (validOptions)
            main.run();
        else
            System.exit(1);
    }

    /**
     * If it returns true we'll short-circuit argument parsing
     * 
     * @param args
     *            As per {@link #main(String[])}
     * @return True if usage should be displayed, else false
     */
    private static boolean shouldDisplayUsage(final String[] args) {
        if (args.length == 0)
            return true;
        for (final String arg : args)
            if (arg.equals("-h") || arg.equals("--help"))
                return true;
        return false;
    }

    private final JCommander jCommander;

    private final Options options;

    private Main() {
        this.options = new Options();
        this.jCommander = this.options.makeJcommander();
    }

    /**
     * @param args
     *            Arguments to parse, as per {@link #main(String[])}
     */
    private boolean parse(final String[] args) {
        try {
            this.jCommander.parse(args);
        }
        catch (final ParameterException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Runs the command specified in the arguments
     */
    private void run() {
        if (this.options.help) {
            showUsage();
            return;
        }
    }

    private void showUsage() {
        this.jCommander.usage();
        System.err.println("  Security:");
        System.err.println("    To prevent your password from appearing in the process list");
        System.err.println("    you can store the authentication options in a file and pass");
        System.err.println("    the @$FILE_NAME so that these options are read from the file.");
        
        System.err.println("  Examples:");
        System.err
        .println("    par --user '$USER@gmail.com' --password '$PASSWORD' notification --text 'Hello !'");
        System.err
        .println("");
        System.err
        .println("    echo --user '$USER@gmail.com' --password '$PASSWORD' > google_password.txt");
        System.err
        .println("    par @google_password.txt notification --text 'Hello'");
        System.err
        .println("    par @google_password.txt notification --text 'world !'");
    }

}
