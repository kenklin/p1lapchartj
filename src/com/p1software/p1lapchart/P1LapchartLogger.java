package com.p1software.p1lapchart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class P1LapchartLogger {
  private static String FMT = "{'remoteaddr':'%s', 'id':'%s', 'status':'%s'}";
  private Logger logger = null;

  public P1LapchartLogger() {
    logger = LogManager.getFormatterLogger(this.getClass().getPackage().getName());
  }

  public void audit(String remoteAddr, String id, String status) {
    logger.info(FMT, remoteAddr, id, status);
  }
  
  public void error(String msg, Object o) {
	logger.error(msg, o);
  }
}
