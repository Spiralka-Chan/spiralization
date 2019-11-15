package org.secta.spiral.common;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtils {
  private static volatile Logger instance;

  public static Logger getLog() {
    Logger localInstance = instance;
    if (localInstance == null) {
      synchronized (LogUtils.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = localInstance = Logger.getLogger(LogUtils.class.getName());
          FileHandler fh;
          try {
            fh = new FileHandler("./launcher.log", true);
            instance.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
          } catch (SecurityException | IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return localInstance;
  }
}
