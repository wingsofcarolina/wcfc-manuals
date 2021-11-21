package org.wingsofcarolina.manuals;

import java.util.logging.Logger;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.wingsofcarolina.manuals.model.User;

import java.io.IOException;

public class SimpleLogger {
    // Set a small log file size to demonstrate the rolling log files.
	public static final int MB = 1000*1024;
	public static final int FILE_SIZE = 20*MB;
    
	Logger logger;

    public SimpleLogger(String name, ManualsConfiguration config) {
    	 logger = Logger.getLogger(SimpleLogger.class.getName() + "-" + name);

        try {
            // Creating an instance of FileHandler with 5 logging files
            // sequences.
            FileHandler handler = new FileHandler(config.getRoot() + "/" + name + ".%g.log" , FILE_SIZE, 5, true);
            handler.setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] %3$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format,
                            new Date(lr.getMillis()),
                            lr.getLevel().getLocalizedName(),
                            lr.getMessage()
                    );
                }
            });
            logger.addHandler(handler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            logger.warning("Failed to initialize logger handler.");
        }
    }

	public void logUser(User user) {
		logger.info(user.toString());
	}

	public void logAccess(User user, String name) {
		logger.info(user.getName() + " : " + name);
	}
}