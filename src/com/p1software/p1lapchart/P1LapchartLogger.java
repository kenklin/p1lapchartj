package com.p1software.p1lapchart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.SelectResult;

public class P1LapchartLogger {
  private static String FMT = "{'remoteaddr':'%s', 'id':'%s', 'status':'%s'}";
  private Logger logger = null;
  private AmazonSimpleDB sdb = null;
  private SimpleDBCounters counters = null;

  public P1LapchartLogger() {
	String packageName = this.getClass().getPackage().getName();
    logger = LogManager.getFormatterLogger(packageName);
  
    try {
      sdb = new AmazonSimpleDBClient();	// Needs AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
      counters = new SimpleDBCounters(sdb, packageName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void audit(String remoteAddr, String id, String status) {
    logger.info(FMT, remoteAddr, id, status);

    try {
      long k = counters.incrCounter(id);
System.out.println("k(" + id + ") = " + k);
if (k % 2 == 0) {
	long unflushed = counters.flush();
	System.out.println("unflushed = " + unflushed);
	
	SelectResult results = counters.getGlobalCounters();
	 System.out.println(results);
}
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void error(String msg, Object o) {
	logger.error(msg, o);
  }
}
