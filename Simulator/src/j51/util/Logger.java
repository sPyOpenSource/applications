/*
 * $Id: Logger.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.util;

import java.util.logging.*;
import java.io.*;


/**
 * J51 logger manager.
 * 
 * Logger get configuration form logging.properties.
 * 
 * Log level :
 * 
 * <p>trace or finest provide the most detailed information about execution.
 * <p>debug or finer provide detailed information.
 * <p>notice or fine provide more information.
 * <p>info provide normal information.
 * <p>config provide configuration information.
 * <p>warning provide information about recoverable error.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Logger extends java.util.logging.Logger
{
	static LogManager logManager = null;

	static public Logger getLogger(Class clazz)
	{
		return getLogger(clazz.getName());

	}

	static public Logger getLogger(String name)
	{
		if (logManager == null)
		{
			try
			{
				logManager = LogManager.getLogManager();
				logManager.reset();
				FileInputStream is = new FileInputStream("logging.properties");
				logManager.readConfiguration(is);
				is.close();
			} catch (IOException | SecurityException ex) {
				System.out.println(ex);
			}

		}


		java.util.logging.Logger l = logManager.getLogger(name);

		if (l == null || !(l instanceof Logger))
		{
			l = new Logger(name);
			logManager.addLogger(l);
		}

		return (Logger)l;
	}


	private void log(Level level,Exception ex)
	{
		if (isLoggable(level))
		{
			StringBuilder sb = new StringBuilder();
			log(level,ex.toString());
			try
			{
				PrintStream ps = new PrintStream(new FileOutputStream("exception.txt"));
				ex.printStackTrace(ps);
				ps.close();
			} catch (FileNotFoundException ignore) {
			}

		}
	}

	public void fine(Exception ex)
	{
		log(Level.FINE,ex);
	}

	public void info(Exception ex)
	{
		log(Level.INFO,ex);
	}

	public void warning(Exception ex)
	{
		log(Level.WARNING,ex);
	}

	public void fatal(String s)
	{
		warning(s);
		System.exit(1);
	}

	public void fatal(Exception ex)
	{
		log(Level.SEVERE,ex);
		System.exit(1);
	}

	private Logger(String name)
	{
		super(name,null);
	}
}
