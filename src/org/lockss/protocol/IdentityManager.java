/*
* $Id: IdentityManager.java,v 1.25 2003-04-05 00:57:15 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.poller.Vote;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Claire Griffin
 * @version 1.0
 */

public class IdentityManager extends BaseLockssManager {
  protected static Logger log = Logger.getLogger("IDMgr");

  static final String PARAM_LOCAL_IP = Configuration.PREFIX + "localIPAddress";

  static final String PREFIX = Configuration.PREFIX + "id.";
  static final String PARAM_MAX_DELTA = PREFIX + "maxReputationDelta";
  static final String PARAM_AGREE_DELTA = PREFIX + "agreeDelta";
  static final String PARAM_DISAGREE_DELTA = PREFIX + "disagreeDelta";
  static final String PARAM_CALL_INTERNAL = PREFIX + "callInternalDelta";
  static final String PARAM_SPOOF_DETECTED = PREFIX + "spoofDetected";
  static final String PARAM_REPLAY_DETECTED = PREFIX + "replayDetected";
  static final String PARAM_ATTACK_DETECTED = PREFIX + "attackDetected";
  static final String PARAM_VOTE_NOTVERIFIED = PREFIX + "voteNotVerified ";
  static final String PARAM_VOTE_VERIFIED = PREFIX + "voteVerified";
  static final String PARAM_VOTE_DISOWNED = PREFIX + "voteDisowned";

  static final String PARAM_IDDB_DIR = PREFIX + "database.dir";

  static final String IDDB_FILENAME = "iddb.xml";
  static final String IDDB_MAP_FILENAME = "idmapping.xml";

  /* Reputation constants */
  public static final int MAX_DELTA = 0;
  public static final int AGREE_VOTE = 1;
  public static final int DISAGREE_VOTE = 2;
  public static final int CALL_INTERNAL = 3;
  public static final int SPOOF_DETECTED = 4;
  public static final int REPLAY_DETECTED = 5;
  public static final int ATTACK_DETECTED = 6;
  public static final int VOTE_NOTVERIFIED = 7;
  public static final int VOTE_VERIFIED = 8;
  public static final int VOTE_DISOWNED = 9;

  static final int INITIAL_REPUTATION = 500;
  static final int REPUTATION_NUMERATOR = 1000;

  static Logger theLog = Logger.getLogger("IdentityManager");
  static Random theRandom = new Random();

  static String localIdentityStr = null;

  int[] reputationDeltas = new int[10];

  LcapIdentity theLocalIdentity = null;
  Mapping mapping = null;

  HashMap theIdentities = new HashMap(); // all known identities

  public IdentityManager() { }

  /**
   * start the identity manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    reloadIdentities();
    log.info("Local identity: " + getLocalIdentity());
    getDaemon().getStatusService().registerStatusAccessor("Identities",
							  new Status());
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    try {
      storeIdentities();
    }
    catch (ProtocolException ex) {
    }
    super.stopService();
  }

  /**
   * public constructor for the creation of an Identity object
   * from an address.
   * @param addr the InetAddress
   * @return a newly constructed Identity
   */
  public LcapIdentity findIdentity(InetAddress addr) {
    LcapIdentity ret;

    if(addr == null)  {
      ret = getLocalIdentity();
    }
    else  {
      ret = getIdentity(LcapIdentity.makeIdKey(addr));
      if(ret == null)  {
        ret = new LcapIdentity(addr);
        theIdentities.put(ret.getIdKey(), ret);
      }
    }

    return ret;
  }


  /**
   * get and return an already created identity
   * @param idKey the key for the identity we want to find
   * @return the LcapIdentity or null
   */
  public LcapIdentity getIdentity(Object idKey)  {
    return (LcapIdentity)theIdentities.get(idKey);
  }

  /**
   * Get the Identity of the local host
   * @return newly constructed <code>Identity<\code>
   */
  public LcapIdentity getLocalIdentity() {
    if (theLocalIdentity == null)  {
      try {
        InetAddress addr = InetAddress.getByName(getLocalHostName());
        theLocalIdentity = new LcapIdentity(addr);
      } catch (UnknownHostException uhe) {
        theLog.error("Could not resolve: "+localIdentityStr, uhe);
      }
    }
    return theLocalIdentity;
  }

  /**
   * Get the local host name
   * @return hostname as a String
   */
  public static String getLocalHostName() {
    if (localIdentityStr == null)  {
      localIdentityStr = Configuration.getParam(PARAM_LOCAL_IP);
    }
    return localIdentityStr;
  }

  /**
   * return true if this Identity is the same as the local host
   * @param id the LcapIdentity
   * @return boolean true if is the local identity, false otherwise
   */
  public boolean isLocalIdentity(LcapIdentity id) {
    if (theLocalIdentity == null)  {
      getLocalIdentity();
    }
    return id.isEqual(theLocalIdentity);
  }

  /**
   * returns true if the InetAddress is the same as the InetAddress for our
   * local host
   * @param addr the address to check
   * @return boolean true if the this is InetAddress is considered local
   */
  public boolean isLocalIdentity(InetAddress addr) {
    if (theLocalIdentity == null)  {
      getLocalIdentity();
    }
    return theLocalIdentity.m_address.equals(addr);
  }

  /**
   * return the max value of an Identity's reputation
   * @return the int value of max reputation
   */
  public int getMaxReputaion() {
    return REPUTATION_NUMERATOR;
  }

  public void changeReputation(LcapIdentity id, int changeKind) {
    int delta = reputationDeltas[changeKind];
    int max_delta = reputationDeltas[MAX_DELTA];
    int reputation = id.getReputation();

    if (id == theLocalIdentity) {
      theLog.debug(id.getIdKey() + " ignoring reputation delta " + delta);
      return;
    }

    delta = (int) (((float) delta) * theRandom.nextFloat());

    if (delta > 0) {
      if (delta > max_delta) {
        delta = max_delta;

      }
      if (delta > (REPUTATION_NUMERATOR - reputation)) {
        delta = (REPUTATION_NUMERATOR - reputation);

      }
    }
    else if (delta < 0) {
      if (delta < (-max_delta)) {
        delta = -max_delta;
      }
      if ((reputation + delta) < 0) {
        delta = -reputation;
      }
    }
    if (delta != 0)
      theLog.debug(id.getIdKey() +" change reputation from " + reputation +
                   " to " + (reputation + delta));
    id.changeReputation(delta);
  }


  void reloadIdentities() {
    try {
      String iddbDir = Configuration.getParam(PARAM_IDDB_DIR);
      if (iddbDir==null) {
        theLog.warning("No value found for config parameter '" +
                       PARAM_IDDB_DIR+"'");
        return;
      }
      String fn = iddbDir + File.separator + IDDB_FILENAME;
      File iddbFile = new File(fn);
      if((iddbFile != null) && iddbFile.canRead()) {
        Unmarshaller unmarshaller = new Unmarshaller(IdentityListBean.class);
        unmarshaller.setMapping(getMapping());
        IdentityListBean idlb = (IdentityListBean)unmarshaller.unmarshal(
            new FileReader(iddbFile));
        setIdentities(idlb.getIdBeans());
      }
      else {
        theLog.warning("Unable to read Identity file:" + fn);

      }
    } catch (Exception e) {
      theLog.warning("Couldn't load identity database: " + e.getMessage());
    }
  }

  void storeIdentities() throws ProtocolException {
    try {
      String fn = Configuration.getParam(PARAM_IDDB_DIR);
      if (fn==null) {
        theLog.warning("No value found for config parameter '" +
                       PARAM_IDDB_DIR+"'");
        return;
      }

      File iddbDir = new File(fn);
      if (!iddbDir.exists()) {
        iddbDir.mkdirs();
      }
      File iddbFile = new File(iddbDir, IDDB_FILENAME);
      if(!iddbFile.exists()) {
        iddbFile.createNewFile();
      }
      if((iddbFile != null) && iddbFile.canWrite()) {
        IdentityListBean idlb = getIdentityListBean();
        Marshaller marshaller = new Marshaller(new FileWriter(iddbFile));
        marshaller.setMapping(getMapping());
        marshaller.marshal(idlb);
      }
      else {
        throw new ProtocolException("Unable to store identity database.");
      }

    } catch (Exception e) {
      theLog.error("Couldn't store identity database: ", e);
      throw new ProtocolException("Unable to store identity database.");
    }
  }


  IdentityListBean getIdentityListBean() {
    synchronized(theIdentities) {
      List beanList = new ArrayList(theIdentities.size());
      Iterator mapIter = theIdentities.values().iterator();
      while(mapIter.hasNext()) {
        LcapIdentity id = (LcapIdentity) mapIter.next();
        IdentityBean bean = new IdentityBean(id.getIdKey(),id.getReputation());
        beanList.add(bean);
      }
      IdentityListBean listBean = new IdentityListBean(beanList);
      return listBean;
    }
  }

  void setIdentities(Collection idList) {
    Iterator beanIter = idList.iterator();
    synchronized(theIdentities) {
      while (beanIter.hasNext()) {
        IdentityBean bean = (IdentityBean)beanIter.next();
        try {
          LcapIdentity id = new LcapIdentity(bean.getKey(), bean.getReputation());
          theIdentities.put(id.getIdKey(), id);
        }
        catch (UnknownHostException ex) {
          theLog.warning("Error reloading identity-Unknown Host: " +
                         bean.getKey());
        }
      }
    }
  }


  Mapping getMapping() {

    if (mapping==null) {
      URL mappingLoc = this.getClass().getResource(IDDB_MAP_FILENAME);
      if (mappingLoc == null) {
        theLog.error("Unable to find resource '"+IDDB_MAP_FILENAME+"'");
        return null;
      }

      Mapping map = new Mapping();
      try {
        map.loadMapping(mappingLoc);
        mapping = map;
      } catch (Exception ex) {
        theLog.error("Loading of mapfile failed:" + mappingLoc);
      }
    }
    return mapping;
  }

  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
    reputationDeltas[MAX_DELTA] =
        config.getInt(PARAM_MAX_DELTA, 100);
    reputationDeltas[AGREE_VOTE] =
        config.getInt(PARAM_AGREE_DELTA, 100);
    reputationDeltas[DISAGREE_VOTE] =
        config.getInt(PARAM_DISAGREE_DELTA, -150);
    reputationDeltas[CALL_INTERNAL] =
        config.getInt(PARAM_CALL_INTERNAL, 100);
    reputationDeltas[SPOOF_DETECTED] =
        config.getInt(PARAM_SPOOF_DETECTED, -30);
    reputationDeltas[REPLAY_DETECTED] =
        config.getInt(PARAM_REPLAY_DETECTED, -20);
    reputationDeltas[ATTACK_DETECTED] =
        config.getInt(PARAM_ATTACK_DETECTED, -500);
    reputationDeltas[VOTE_NOTVERIFIED] =
        config.getInt(PARAM_VOTE_NOTVERIFIED, -30);
    reputationDeltas[VOTE_VERIFIED] =
        config.getInt(PARAM_VOTE_VERIFIED, 40);
    reputationDeltas[VOTE_DISOWNED] =
        config.getInt(PARAM_VOTE_DISOWNED, -400);
  }

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("ip", true));

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("ip", "IP",
				       ColumnDescriptor.TYPE_IP_ADDRESS),
		  new ColumnDescriptor("lastPkt", "Last Pkt",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a packet that originated " +
				       "at IP was received"),
		  new ColumnDescriptor("lastOp", "Last Op",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a non-NoOp packet that " +
				       "originated at IP was received"),
		  new ColumnDescriptor("origTot", "Orig Tot",
				       ColumnDescriptor.TYPE_INT,
				       "Total packets received that " +
				       "originated at IP."),
		  new ColumnDescriptor("origOp", "Orig Op",
				       ColumnDescriptor.TYPE_INT,
				       "Total non-noop packets received that "+
				       "originated at IP."),
		  new ColumnDescriptor("sendOrig", "1 Hop",
				       ColumnDescriptor.TYPE_INT,
				       "Packets arriving from originator " +
				       "in one hop."),
		  new ColumnDescriptor("sendFwd", "Fwd",
				       ColumnDescriptor.TYPE_INT,
				       "Packets forwarded by IP to us."),
		  new ColumnDescriptor("reputation", "Reputation",
				       ColumnDescriptor.TYPE_INT)
		  );


  private class Status implements StatusAccessor {
    public void populateTable(StatusTable table) {
      String key = table.getKey();
      table.setTitle("Cache Identities");
      table.setColumnDescriptors(statusColDescs);
      table.setDefaultSortRules(statusSortRules);
      table.setRows(getRows(key));
//       table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      for (Iterator iter = theIdentities.values().iterator();
	   iter.hasNext();) {
	table.add(makeRow((LcapIdentity)iter.next()));
      }
      return table;
    }

    private Map makeRow(LcapIdentity id) {
      Map row = new HashMap();
      InetAddress ip = id.getAddress();
      Object obj = ip;
      if (isLocalIdentity(ip)) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(ip);
	val.setBold(true);
	obj = val;
      }
      row.put("ip", obj);
      row.put("lastPkt", new Long(id.getLastActiveTime()));
      row.put("lastOp", new Long(id.getLastOpTime()));
      row.put("origTot", new Long(id.getEventCount(LcapIdentity.EVENT_ORIG)));
      row.put("origOp",
	      new Long(id.getEventCount(LcapIdentity.EVENT_ORIG_OP)));
      row.put("sendOrig",
	      new Long(id.getEventCount(LcapIdentity.EVENT_SEND_ORIG)));
      row.put("sendFwd",
	      new Long(id.getEventCount(LcapIdentity.EVENT_SEND_FWD)));
//       row.put("dupl", new Long(id.getEventCount(LcapIdentity.EVENT_DUPL)));
      row.put("reputation", new Long(id.getReputation()));
      return row;
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
//       res.add(new StatusTable.SummaryInfo("Total bytes hashed",
// 					  ColumnDescriptor.TYPE_INT,
// 					  new Integer(0)));
      return res;
    }

  }
}
