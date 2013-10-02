package com.p1software.p1lapchart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

/**
 * AWS eventually consistent SimpleDB for implementing long counters whose
 * columns are "name" and "value".
 * 
 *	AmazonSimpleDB sdb = new AmazonSimpleDBClient();	// Needs AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
 *	SimpleDBCounters counters = new SimpleDBCounters(sdb, "mydomain");
 *	. . .
 *	counters.incrCounter("connections", 2);
 *
 * @author Ken
 */
public class SimpleDBCounters extends AmazonSimpleDBClient {
	class Counter {
		public long k = 0;
		public long unflushedDelta = 0;	// delta that hasn't been put to SimpleDB
		public Counter(long k) {
			this.k = k;
			this.unflushedDelta = 0;
		}
	};

	public static final int			MAX_LONG_DIGITS = 19;
	protected static String			VALUE_ITEMNAME = "value";

	protected AmazonSimpleDB		sdb = null;
	protected String				domain = null;
	protected Logger				logger = null;
	protected Map<String, Counter>	counters = null;
	protected Timer					flushTimer = null;
	protected int					unflushedCounters = 0;	// Number of unflushed Counter
	
	public SimpleDBCounters() {
	}
		
	public SimpleDBCounters(AmazonSimpleDB sdb, String domain, int flushInterval) {
		init(sdb, domain, flushInterval);
	}
	
	public void init(AmazonSimpleDB sdb, String domain, int flushInterval) {
		this.sdb = sdb;
		this.domain = domain;   
		createDomain(new CreateDomainRequest(domain));
		counters = new HashMap<String, Counter>();
		setFlushTimer(flushInterval);
	}
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
    class FlushTimerTask extends TimerTask {
		public void run() {
			flush();
		}
    }
	    
	public void setFlushTimer(int interval) {
		info("setFlushTimer(" + interval + ")");
		if (flushTimer != null) {
			flushTimer.cancel();
		}
        flushTimer = new Timer();
        flushTimer.schedule(new FlushTimerTask(), 0, interval);
	}
	
	/**
	 * Increments and returns the in-memory counter's value (not necessarily the
	 * value in SimpleDB, which could have been updated by other threads,
	 * processes, machines, etc.).
	 * 
	 * @param name	Counter name
	 * @param n		Increment value
	 * @return		Counter value after increment
	 * @throws		AmazonServiceException
	 * @throws		AmazonClientException
	 */
	public synchronized long incrCounter(String name, long n)
			throws AmazonServiceException, AmazonClientException
    {
		unflushedCounters++;

		Counter c = counters.get(name);
		if (c != null) {
			c.k += n;
		} else {
			c = new Counter(n);
		}
		c.unflushedDelta += n;
		counters.put(name, c);
		
		return c.k;
	}
	
	/**
	 * Increments and returns the in-memory counter's value (not necessarily the
	 * value in SimpleDB, which could have been updated by other threads,
	 * processes, machines, etc.).
	 * 
	 * @param name	Counter name
	 * @return		Counter value after increment
	 * @throws		AmazonServiceException
	 * @throws		AmazonClientException
	 */
	public synchronized long incrCounter(String name) 
			throws AmazonServiceException, AmazonClientException
	{
		return incrCounter(name, 1);
	}

	/**
	 * Decrements and returns the in-memory counter's value (not necessarily the
	 * value in SimpleDB, which could have been updated by other threads,
	 * processes, machines, etc.).
	 * 
	 * @param name	Counter name
	 * @param n		Decrement value
	 * @return
	 * @throws		AmazonServiceException
	 * @throws		AmazonClientException
	 */
	public synchronized long decrCounter(String name, long n)
			throws AmazonServiceException, AmazonClientException
    {
		unflushedCounters++;
		
		Counter c = counters.get(name);
		if (c != null) {
			c.k -= n;
		} else {
			c = new Counter(-n);
		}
		c.unflushedDelta -= n;
		counters.put(name, c);
		
		return c.k;
	}
	
	/**
	 * Decrements and returns the in-memory counter's value (not necessarily the
	 * value in SimpleDB, which could have been updated by other threads,
	 * processes, machines, etc.).
	 * 
	 * @param name	Counter name
	 * @return		Counter value after decrement
	 * @throws		AmazonServiceException
	 * @throws		AmazonClientException
	 */
	public synchronized long decrCounter(String name)
			throws AmazonServiceException, AmazonClientException
    {
		return decrCounter(name, 1);
    }
	
	/**
	 * Returns the in-memory counter's value (not necessarily the value in 
	 * SimpleDB, which could have been updated by other threads, processes,
	 * machines, etc.).
	 * 
	 * @param name	Counter name
	 * @return		Counter value
	 * @throws		AmazonServiceException
	 * @throws		AmazonClientException
	 */
	public Counter getCounter(String name)
			throws AmazonServiceException, AmazonClientException
	{
		return counters.get(name);
	}
	
	protected void info(String msg)
	{
		if (logger != null) {
			logger.info(msg);
		}		
	}
	
	/**
	 * Flushes a single in-memory counter to the SimpleDB if it has not been
	 * flushed.  Synchronization is the responsibility of the caller. 
	 * 
	 * @param entry	Counter name
	 * @return		true if and only if the entry's SimpleDB is up to date.
	 */
	protected boolean flush(Map.Entry<String, Counter> entry)
	{
		boolean flushed = false;
		Counter c = entry.getValue();

		info("flush(" + entry.getKey() + ") =  " + c.unflushedDelta);
		
		if (c != null && c.unflushedDelta != 0) {
			try {
				// Get SimpleDB item
				GetAttributesRequest getReq = new GetAttributesRequest(domain, VALUE_ITEMNAME);
				GetAttributesResult res = sdb.getAttributes(getReq);
				if (res != null) {
					// Get item's value
					List<Attribute> getAttrs = res.getAttributes();
					if (getAttrs.size() > 0) {
						Attribute getAttr = getAttrs.get(0);
						String valString = getAttr.getValue();
						long valLong = SimpleDBUtils.decodeZeroPaddingLong(valString);
						info("flush getAttributes: id=" + entry.getKey() + ", value=" + valLong);

						// Set up sdb PutAttributesRequest
						String newValString = SimpleDBUtils.encodeZeroPadding(valLong + c.unflushedDelta, MAX_LONG_DIGITS);
						List<ReplaceableAttribute> putAttrs = new ArrayList<ReplaceableAttribute>(1);
						putAttrs.add(new ReplaceableAttribute(VALUE_ITEMNAME, newValString, true));

						// Conditionally update SimpleDB item (might throw ConditionalCheckFailed)
						UpdateCondition cond = new UpdateCondition(VALUE_ITEMNAME, valString, true);
						PutAttributesRequest putReq = new PutAttributesRequest(domain, VALUE_ITEMNAME, putAttrs, cond);
						sdb.putAttributes(putReq);
						info("flush putAttributes 1: id=" + entry.getKey() + ", value=" + newValString);
					} else {
						String newValString = SimpleDBUtils.encodeZeroPadding(c.k, MAX_LONG_DIGITS);
						List<ReplaceableAttribute> putAttrs = new ArrayList<ReplaceableAttribute>();
						putAttrs.add(new ReplaceableAttribute(VALUE_ITEMNAME, newValString, true));

						// Create SimpleDB item
						PutAttributesRequest putReq = new PutAttributesRequest(domain, VALUE_ITEMNAME, putAttrs);
						sdb.putAttributes(putReq);
						info("flush putAttributes 2: id=" + entry.getKey() + ", value=" + newValString);
					}
				} else {
					assert(c.k == c.unflushedDelta);
					String newValString = SimpleDBUtils.encodeZeroPadding(c.k, MAX_LONG_DIGITS);
					List<ReplaceableAttribute> putAttrs = new ArrayList<ReplaceableAttribute>();
					putAttrs.add(new ReplaceableAttribute(VALUE_ITEMNAME, newValString, true));

					// Create SimpleDB item
					PutAttributesRequest putReq = new PutAttributesRequest(domain, VALUE_ITEMNAME, putAttrs);
					sdb.putAttributes(putReq);
					info("flush putAttributes 3: id=" + entry.getKey() + ", value=" + newValString);
				}
			
				flushed = true;
				c.unflushedDelta = 0;	// All deltas have been flushed
			} catch (AmazonServiceException awse) {
				// http://docs.aws.amazon.com/AmazonSimpleDB/latest/DeveloperGuide/APIError.html
				if (awse.getErrorCode().equalsIgnoreCase("ConditionalCheckFailed")) {
					info("ConditionalCheckFailed - skipping this flush for " + entry.getKey());					
				} else {
					awse.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return flushed;
	}
	
	/**
	 * Flushes all the in-memory counters to the SimpleDB.  The counters are not
	 * implicitly cleared.
	 * 
	 * @return		Number of entries that were not successfully flushed
	 */
	public synchronized int flush()
	{
		info("flush(" + unflushedCounters + ")");
		if (unflushedCounters > 0) {
			unflushedCounters = 0;
			for (Map.Entry<String, Counter> entry : counters.entrySet()) {
				if (!flush(entry)) {
					unflushedCounters++;
				}
			}
		}
		return unflushedCounters;
	}
	
	/**
	 * Clears all in-memory counters.  The counters are not implicitly flushed.
	 */
	public synchronized void clear()
	{
		counters.clear();
	}
	
	public synchronized SelectResult getGlobalCounters() {
		String selectExpr = "select * from `" + domain + "`";
        SelectResult selectResult = sdb.select(new SelectRequest(selectExpr));
		return selectResult;
	}
}
