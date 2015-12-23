package synet.controller.utils;

import java.io.File;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SysUtils
{
	private final static String TAG = "SysUtils";
	private static final String C_STR_SEPERATOR_WIN = "\\";
	private static final String C_STR_SEPERATOR_NIX = "/";
	public static final int C_INT_BASE = 16;

	private static final String C_STR_DATETIMEFORMAT = "yyMMddHHmmssS";

	private static DateTimeFormatter m_dateTimeFormatter = DateTimeFormat.forPattern(C_STR_DATETIMEFORMAT);

	/**
	 * @return true if this system is windows
	 */
	public static boolean isWindows()
	{
		return System.getProperty("os.name").contains("Windows");
	}

	/**
	 * @return true if this system is mac
	 */
	public static boolean isMac()
	{
		return System.getProperty("os.name").contains("Mac");
	}

	/**
	 * @return true if this system is linux
	 */
	public static boolean isLinux()
	{
		return System.getProperty("os.name").contains("Linux");
	}

	/**
	 * @return the DateTime Formatter for the whole system
	 */
	public static DateTimeFormatter getDateTimeFormatter()
	{
		return m_dateTimeFormatter;
	}

	/**
	 * @return the file separator string for the host OS
	 */
	public static String getSeparatorString()
	{
		if (isLinux() || isMac())
		{
			return C_STR_SEPERATOR_NIX;
		}
		else if (isWindows())
		{
			return C_STR_SEPERATOR_WIN;
		}
		else
		{
			Logger.e(TAG, "Unsupported OS for seperator string");
			return "";
		}
	}

	/**
	 * Iterate through the path and create if needed
	 * 
	 * @param p_path
	 * @return true if the path exists
	 */
	public static boolean checkAndCreatePath(String p_path)
	{
		String separator = getSeparatorString();
		StringBuilder builder = new StringBuilder();

		if (p_path != null)
		{
			String[] levels = p_path.split(Pattern.quote(separator));

			for(String level : levels)
			{
				builder.append(level);
				builder.append(separator);


				File file = new File(builder.toString());
				// If it doesn't exist, create it
				if (!file.exists())
				{
					Logger.i(TAG, String.format("directory %s does not exist, creating it", builder.toString()));
					boolean bDir = file.mkdir();	
					if (bDir)
					{
						Logger.i(TAG, "directory created successfully"); 
					}
					else
					{
						Logger.e(TAG, "could not create directory"); 
						return false;
					}
				}
			}
		}
		else
		{
			Logger.e(TAG, "cannot check null directory");
			return false;
		}
		Logger.i(TAG, "checking directory successful: " + p_path);
		return true;
	}
}
