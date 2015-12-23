package synet.controller.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashSet;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import synet.controller.configuration.Configuration;

/**
 * API for sending log output.
 *
 * <p>Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e()
 * methods.
 *
 * <p>The order in terms of verbosity, from least to most is
 * ERROR, WARN, INFO, DEBUG, VERBOSE.  Verbose should never be compiled
 * into an application except during development.  Debug logs are compiled
 * in but stripped at runtime.  Error, warning and info logs are always kept.
 *
 * <p><b>Tip:</b> A good convention is to declare a <code>TAG</code> constant
 * in your class:
 *
 * <pre>private static final String TAG = "MyActivity";</pre>
 *
 * and use that in subsequent calls to the log methods.
 * </p>
 *
 * <p><b>Tip:</b> Don't forget that when you make a call like
 * <pre>Log.v(TAG, "index=" + i);</pre>
 * that when you're building the string to pass into Log.d, the compiler uses a
 * StringBuilder and at least three allocations occur: the StringBuilder
 * itself, the buffer, and the String object.  Realistically, there is also
 * another buffer allocation and copy, and even more pressure on the gc.
 * That means that if your log message is filtered out, you might be doing
 * significant work and incurring significant overhead.
 */
public final class Logger {
	private static final String C_STR_LOGLEVEL = "minLogLevel";
	private static final String C_STR_TAGFILTER = "tagFilter";
	private static final String C_STR_USELOGFILE = "useLogFile";
	private static final String TAG = "Logger";
	private static final String C_STR_SUFFIX = ".log";

	public final static int MIN_LEVEL = 0;

	/**
	 * Priority constant for the println method; use Log.v.
	 */
	public static final int VERBOSE = 2;

	/**
	 * Priority constant for the println method; use Log.d.
	 */
	public static final int DEBUG = 3;

	/**
	 * Priority constant for the println method; use Log.i.
	 */
	public static final int INFO = 4;

	/**
	 * Priority constant for the println method; use Log.w.
	 */
	public static final int WARN = 5;

	/**
	 * Priority constant for the println method; use Log.e.
	 */
	public static final int ERROR = 6;

	/**
	 * Priority constant for the println method.
	 */
	public static final int ASSERT = 7;

	// Private variables
	private static FileWriter m_logFile;
	private static PrintWriter m_printLog;
	private static DateTimeFormatter m_formatter = DateTimeFormat.forPattern("[dd/MM/yyyy:kk:mm:ss.SSS]");

	// Configuration Items
	private static Configuration m_config;
	private static boolean m_useLogFile;
	private static int m_levelfilter = MIN_LEVEL;
	private static boolean m_isTagFilter;
	private static HashSet<String> m_tagHashSet = new HashSet<String>();
	private static PrintStream m_outStream = null;
	private static PrintStream m_errStream = null;

	private Logger() {
	}

	/**
	 * Start the logging system and initialize anything necessary
	 */
	public static void start(String p_logFileName)
	{
		i(TAG, "starting with log file: " + p_logFileName);
		String logFileStr = p_logFileName+DateTime.now().toString(SysUtils.getDateTimeFormatter())+C_STR_SUFFIX;

		if (SysUtils.checkAndCreatePath(p_logFileName))
		{
			try {
				m_logFile = new FileWriter(logFileStr);
				m_printLog = new PrintWriter(m_logFile, true);
				m_useLogFile = true;
				m_isTagFilter = false;
			} catch (IOException e) {
				e(TAG, "could not open log file: " + logFileStr, e);
			}
		}
	}

	/**
	 * Stop the logging system and clean up anything necessary
	 * 
	 */
	public static void stop()
	{
		if (m_useLogFile)
		{
			try {
				m_printLog.close();
				m_logFile.close();
			} catch (IOException e) {
				e(TAG, "could not close log file", e);
			}
		}
		i(TAG, "stopped");
	}

	/**
	 * Set the output streams of the logger. Either to local console or
	 * possibly over a socket of some type
	 * 
	 * @param p_outStream
	 * @param p_errStream
	 */
	public static void setOutputStreams(PrintStream p_outStream, PrintStream p_errStream)
	{
		m_outStream = p_outStream;
		m_errStream = p_errStream;
	}

	/**
	 * Send a {@link #VERBOSE} log message.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public static int v(String tag, String msg) {
		return println(VERBOSE, tag, msg);
	}

	/**
	 * Send a {@link #VERBOSE} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr An exception to log
	 */
	public static int v(String tag, String msg, Throwable tr) {
		return println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
	}

	/**
	 * Send a {@link #DEBUG} log message.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public static int d(String tag, String msg) {
		return println(DEBUG, tag, msg);
	}

	/**
	 * Send a {@link #DEBUG} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr An exception to log
	 */
	public static int d(String tag, String msg, Throwable tr) {
		return println(DEBUG, tag, msg + '\n' + getStackTraceString(tr));
	}

	/**
	 * Send an {@link #INFO} log message.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public static int i(String tag, String msg) {
		return println(INFO, tag, msg);
	}

	/**
	 * Send a {@link #INFO} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr An exception to log
	 */
	public static int i(String tag, String msg, Throwable tr) {
		return println(INFO, tag, msg + '\n' + getStackTraceString(tr));
	}

	/**
	 * Send a {@link #WARN} log message.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public static int w(String tag, String msg) {
		return println(WARN, tag, msg);
	}

	/**
	 * Send a {@link #WARN} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr An exception to log
	 */
	public static int w(String tag, String msg, Throwable tr) {
		return println(WARN, tag, msg + '\n' + getStackTraceString(tr));
	}

	/**
	 * Checks to see whether or not a log for the specified tag is loggable at the specified level.
	 * 
	 *  The default level of any tag is set to INFO. This means that any level above and including
	 *  INFO will be logged. Before you make any calls to a logging method you should check to see
	 *  if your tag should be logged. You can change the default level by setting a system property:
	 *      'setprop log.tag.&lt;YOUR_LOG_TAG> &lt;LEVEL>'
	 *  Where level is either VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS. SUPRESS will 
	 *  turn off all logging for your tag. You can also create a local.prop file that with the
	 *  following in it:
	 *      'log.tag.&lt;YOUR_LOG_TAG>=&lt;LEVEL>'
	 *  and place that in /data/local.prop.
	 *  
	 * @param tag The tag to check.
	 * @param level The level to check.
	 * @return Whether or not that this is allowed to be logged.
	 * @throws IllegalArgumentException is thrown if the tag.length() > 23.
	 */
	public static native boolean isLoggable(String tag, int level);

	/*
	 * Send a {@link #WARN} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param tr An exception to log
	 */
	public static int w(String tag, Throwable tr) {
		return println(WARN, tag, getStackTraceString(tr));
	}

	/**
	 * Send an {@link #ERROR} log message.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public static int e(String tag, String msg) {
		return println(ERROR, tag, msg);
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr An exception to log
	 */
	public static int e(String tag, String msg, Throwable tr) {
		int r = println(ERROR, tag, msg + '\n' + getStackTraceString(tr));
		//RuntimeInit.reportException(tag, tr, false);  // asynchronous
		return r;
	}

	/**
	 * Handy function to get a loggable stack trace from a Throwable
	 * @param tr An exception to log
	 */
	public static String getStackTraceString(Throwable tr) {
		if (tr == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		tr.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * Return a string indicating the level
	 * 
	 * @param p_int
	 * @return
	 */
	public static String levelFrontInt(int p_int)
	{
		switch(p_int)
		{
		case 2:
			return "VERBOSE";
		case 3:
			return "DEBUG";
		case 4:
			return "INFO";
		case 5:
			return "WARN";
		case 6:
			return "ERROR";
		case 7:
			return "ABORT";
		}

		return "UNKNOWN";
	}

	/**
	 * @param p_useLogFile
	 */
	public static void setUseLogFile(boolean p_useLogFile)
	{
		m_useLogFile = p_useLogFile;
		m_config.addProperty(C_STR_USELOGFILE, m_useLogFile);
	}

	/**
	 * Set the tag filter to a comma separated string
	 * 
	 * @param p_strTag
	 */
	public static void setTagFilter(String p_strTag)
	{
		if (p_strTag.compareTo("") == 0)
		{
			setTagFilter((String[])null);
		}
		else
		{
			setTagFilter(p_strTag.split(","));
		}
	}

	/**
	 * Sets a tag filter. Set to null to remove tag filter
	 * @param p_strTag
	 */
	public static void setTagFilter(String[] p_strTag)
	{
		if (p_strTag == null)
		{
			i(TAG, "using NO tag filters");
			m_tagHashSet.clear();
			m_isTagFilter = false;
			m_config.clearProperty(C_STR_TAGFILTER);
		}
		else
		{
			for(String tag : p_strTag)
			{
				i(TAG, "setting tag filter to " + tag);
				m_tagHashSet.add(tag);
			}
			m_isTagFilter = true;

			// Save to the preferences as a string
			StringBuilder sb = new StringBuilder();
			for(String tag : m_tagHashSet)
			{
				if (sb.length() != 0)
				{
					sb.append(",");
				}
				sb.append(tag);
			}
			m_config.addProperty(C_STR_TAGFILTER, sb.toString());
		}
	}

	/**
	 * Set the minimum level filter
	 * 
	 * @param p_minLevel
	 */
	public static void setMinLevel(int p_minLevel)
	{
		m_levelfilter = p_minLevel;
		m_config.addProperty(C_STR_LOGLEVEL, m_levelfilter);
	}

	/**
	 * Low-level logging call.
	 * @param priority The priority/type of this log message
	 * @param tag Used to identify the source of a log message.  It usually identifies
	 *        the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @return The number of bytes written.
	 */
	//public static native int println(int priority, String tag, String msg);
	public static int println(int priority, String tag, String msg)
	{
		if (priority > m_levelfilter)
		{
			if (!m_isTagFilter || (m_isTagFilter && m_tagHashSet.contains(tag)))
			{
				String str = String.format("%s%s-%s:%s", DateTime.now().toString(m_formatter),levelFrontInt(priority), tag, msg);
				if (m_outStream != null)
				{
					switch(priority)
					{
					case 6:
					case 7:
					case 8:
						m_errStream.println(str);
						m_outStream.println();
						break;
					default:
						m_outStream.println(str);
					}
				}

				if (m_useLogFile)
				{
					m_printLog.println(str);
				}
			}
		}
		return 0;
	}

	/**
	 * Set the configuration object for the Logger
	 * 
	 * @param p_config
	 */
	public static void setConfiguration(Configuration p_config)
	{
		m_config = p_config;

		setUseLogFile(m_config.getBoolean(C_STR_USELOGFILE, true));
		setTagFilter(m_config.getString(C_STR_TAGFILTER, ""));
		setMinLevel(m_levelfilter = m_config.getInt(C_STR_LOGLEVEL, MIN_LEVEL));
	}
}
