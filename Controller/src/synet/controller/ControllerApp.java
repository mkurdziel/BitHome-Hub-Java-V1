/**
 * 
 */
package synet.controller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class ControllerApp {

    private static final String C_OPT_COMMPORT_CHAR = "c";
    private static final String C_OPT_CONSOLEPORT_CHAR = "p";
    private static final String C_OPT_BASEDIR_CHAR = "b";
    private static final String C_OPT_LOGFILE_CHAR = "l";
    private static final String C_OPT_daemon_CHAR = "d";
    private static final String C_OPT_daemon = "daemon";
    private static final String C_OPT_HELP_CHAR = "h";
    private static final String C_OPT_HELP = "help";
    private static final String C_STR_APPNAME = "ControllerApp";
    private static final String TAG = C_STR_APPNAME;

    private static Controller m_controller = null;

    /**
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        // Grab the shutdown hooks so we can exit gracefully
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());

        // create the command line parser
        CommandLineParser parser = new PosixParser();

        // create the Options
        Options options = new Options();
        options.addOption( C_OPT_HELP_CHAR, C_OPT_HELP, false, "print this message" );
        options.addOption( C_OPT_daemon_CHAR, C_OPT_daemon, false, "run the controller as a daemon" );
        //		options.addOption( OptionBuilder.withArgName("directory")
        //				.hasArg()
        //				.withDescription("use given base directory")
        //				.create(C_OPT_BASEDIR_CHAR));
        //		options.addOption( OptionBuilder.withArgName("file")
        //				.hasArg()
        //				.withDescription("use given file for log")
        //				.create(C_OPT_LOGFILE_CHAR));
        options.addOption( OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("use given port for console server")
                .create(C_OPT_CONSOLEPORT_CHAR));
        options.addOption( OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("use given serial port for xbee serial device")
                .create(C_OPT_COMMPORT_CHAR));

        //		options.addOption( OptionBuilder.withArgName( "file" )
        //        					.hasArg()
        //        					.withDescription(  "use given file for log" )
        //        					.create( "logfile" ));

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );

            // Handle the help option
            if (line.hasOption(C_OPT_HELP))
            {
                printUsage(options);
            }

            m_controller = new Controller( line.hasOption(C_OPT_daemon_CHAR),
                    line.getOptionValue(C_OPT_COMMPORT_CHAR),
                    line.getOptionValue(C_OPT_CONSOLEPORT_CHAR));
            m_controller.run();

        Logger.v(TAG, "main exit");

        }
        catch( ParseException exp ) {
            System.out.println( "Unexpected exception:" + exp.getMessage() );
            printUsage(options);
        }
    }

    /**
     * Print the usage information
     * @param options 
     */
    private static void printUsage(Options options)
    {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( C_STR_APPNAME, options );
        System.exit(-1);
    }

    /**
     * Thread to handle the graceful shutdown
     */
    public static class ShutdownHookThread extends Thread 
    {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            Logger.i(TAG, "Shutting down");
            super.setName("ShutdownHookThread");
            if (m_controller != null)
            {
                m_controller.shutDown();
            }
            else
            {
                Logger.w(TAG, "Null controller object");
            }
        }
    }
}
