/*
 * $Id: HistoryRepositoryImpl.java,v 1.46 2004-02-11 23:50:14 eaalto Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.daemon.Configuration;
import org.lockss.protocol.*;

/**
 * HistoryRepository is an inner layer of the NodeManager which handles the actual
 * storage of NodeStates.
 */
public class HistoryRepositoryImpl
  extends BaseLockssManager implements HistoryRepository {

  /**
   * Configuration parameter name for Lockss history location.
   */
  public static final String PARAM_HISTORY_LOCATION =
    Configuration.PREFIX + "history.location";

  /**
   * Name of top directory in which the histories are stored.
   */
  public static final String HISTORY_ROOT_NAME = "cache";

  // resource file.  Fully qualify for XmlMarshaller
  static final String MAPPING_FILE_NAME =
      "/org/lockss/state/pollmapping.xml";
  // these share space with content, so must be prefaced by '#'
  static final String HISTORY_FILE_NAME = "#history.xml";
  static final String NODE_FILE_NAME = "#nodestate.xml";
  static final String AU_FILE_NAME = "#au_state.xml";
  static final String DAMAGED_NODES_FILE_NAME = "#damaged_nodes.xml";
  static final String IDENTITY_AGREEMENT_FILE_NAME = "#id_agreement.xml";
  // this contains a '#' so that it's not defeatable by strings which
  // match the prefix in a url (like '../tmp/')
  private static final String TEST_PREFIX = "/#tmp";

  // The map files being used
  static final String[] MAPPING_FILES = {
      MAPPING_FILE_NAME,
      ExternalizableMap.MAPPING_FILE_NAME,
      IdentityManager.MAPPING_FILE_NAME
  };

  private ArchivalUnit storedAu;

  private String rootLocation;
  private static Logger logger = Logger.getLogger("HistoryRepository");

  HistoryRepositoryImpl(ArchivalUnit au, String rootPath) {
    storedAu = au;
    rootLocation = rootPath;
    if (!rootLocation.endsWith(File.separator)) {
      // this shouldn't happen
      rootLocation += File.separator;
    }
  }

  public void startService() {
    super.startService();
    if (rootLocation==null) {
      String msg = PARAM_HISTORY_LOCATION + " not configured";
      logger.error(msg);
      throw new LockssDaemonException(msg);
    }
    checkFileChange();
  }

  public void stopService() {
    // we want to checkpoint here
    super.stopService();
  }

  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
  }

  public void setAuConfig(Configuration auConfig) {
  }

  public void storeNodeState(NodeState nodeState) {
    CachedUrlSet cus = nodeState.getCachedUrlSet();
    try {
      logger.debug3("Storing state for CUS '" + cus.getUrl() + "'");
      store(getNodeLocation(cus), NODE_FILE_NAME, new NodeStateBean(nodeState));
    } catch (Exception e) {
      logger.error("Couldn't store node state: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't store node state.");
    }
  }

  public NodeState loadNodeState(CachedUrlSet cus) {
    try {
      String fileName = getNodeLocation(cus) + File.separator + NODE_FILE_NAME;
      logger.debug3("Loading state for CUS '" + cus.getUrl() + "'");
      NodeStateBean nsb = (NodeStateBean)load(fileName, NodeStateBean.class);
      if (nsb==null) {
        logger.debug3("No node state file for node '"+cus.getUrl()+"'");
        return new NodeStateImpl(cus, -1,
                                 new CrawlState(-1, CrawlState.FINISHED, 0),
                                 new ArrayList(), this);
      }
      return new NodeStateImpl(cus, nsb, this);
    } catch (XmlMarshaller.MarshallingException me) {
      logger.error("Marshalling exception on node state for '" +
                   cus.getUrl() + "': " + me.getMessage());
      // continue
      return new NodeStateImpl(cus, -1,
                               new CrawlState(-1, CrawlState.FINISHED, 0),
                               new ArrayList(), this);
    } catch (Exception e) {
      logger.error("Couldn't load node state: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load node state.");
    }
  }


  public void storePollHistories(NodeState nodeState) {
    CachedUrlSet cus = nodeState.getCachedUrlSet();
    try {
      logger.debug3("Storing histories for CUS '"+cus.getUrl()+"'");
      NodeHistoryBean nhb = new NodeHistoryBean();
      nhb.historyBeans = ((NodeStateImpl)nodeState).getPollHistoryBeanList();
      store(getNodeLocation(cus), HISTORY_FILE_NAME, nhb);
    } catch (Exception e) {
      logger.error("Couldn't store poll history: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't store history.");
    }
  }

  public void loadPollHistories(NodeState nodeState) {
    CachedUrlSet cus = nodeState.getCachedUrlSet();
    File nodeFile = null;
    try {
      String fileName = getNodeLocation(cus)+File.separator+HISTORY_FILE_NAME;
      nodeFile = new File(fileName);
      logger.debug3("Loading histories for CUS '"+cus.getUrl()+"'");
      NodeHistoryBean nhb = (NodeHistoryBean)load(fileName,
          NodeHistoryBean.class);
      if (nhb==null) {
        logger.debug3("No histories to load.");
        ((NodeStateImpl)nodeState).setPollHistoryBeanList(new ArrayList());
        return;
      }
      if (nhb.historyBeans==null) {
        logger.debug3("Empty history list loaded.");
        nhb.historyBeans = new ArrayList();
      }
      ((NodeStateImpl)nodeState).setPollHistoryBeanList(
          new ArrayList(nhb.historyBeans));
    } catch (XmlMarshaller.MarshallingException me) {
      logger.error("Parsing exception.  Moving file to '.old'");
      nodeFile.renameTo(new File(nodeFile.getAbsolutePath()+".old"));
      ((NodeStateImpl)nodeState).setPollHistoryBeanList(new ArrayList());
    } catch (Exception e) {
      logger.error("Couldn't load poll history: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load history.");
    }
  }

  public void storeIdentityAgreements(List idList) {
    try {
      logger.debug3("Storing identity agreements for AU '" +
          storedAu.getName() + "'");
      store(rootLocation, IDENTITY_AGREEMENT_FILE_NAME,
          new IdentityAgreementList(idList));
    } catch (Exception e) {
      logger.error("Couldn't store identity agreements: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't store identity agreements.");
    }
  }

  public List loadIdentityAgreements() {
    try {
      logger.debug3("Loading identity agreements for AU '" +
          storedAu.getName() + "'");
      IdentityAgreementList idList =
          (IdentityAgreementList)load(
          rootLocation + IDENTITY_AGREEMENT_FILE_NAME,
          IdentityAgreementList.class);
      if (idList==null) {
        logger.debug2("No identities file for AU '"+storedAu.getName()+"'");
        return new ArrayList();
      }
      return idList.getList();
    } catch (XmlMarshaller.MarshallingException me) {
      logger.error("Marshalling exception for identity agreements: " +
          me.getMessage());
      // continue
      return new ArrayList();
    } catch (Exception e) {
      logger.error("Couldn't load identity agreements: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load identity agreements.");
    }
  }


  public void storeAuState(AuState auState) {
    try {
      logger.debug3("Storing state for AU '" +
                    auState.getArchivalUnit().getName() + "'");
      store(rootLocation, AU_FILE_NAME, new AuStateBean(auState));
    } catch (Exception e) {
      logger.error("Couldn't store au state: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't store au state.");
    }
  }

  public AuState loadAuState() {
    try {
      logger.debug3("Loading state for AU '" + storedAu.getName() + "'");
      AuStateBean asb = (AuStateBean)load(rootLocation + AU_FILE_NAME,
          AuStateBean.class);
      if (asb==null) {
        logger.debug2("No au state file for AU '"+storedAu.getName()+"'");
        return new AuState(storedAu, -1, -1, -1, null, this);
      }
      // does not load in an old treewalk time, so that one will be run
      // immediately
      return new AuState(storedAu, asb.getLastCrawlTime(),
                         asb.getLastTopLevelPollTime(),
                         -1, asb.getCrawlUrls(), this);
    } catch (XmlMarshaller.MarshallingException me) {
      logger.error("Marshalling exception for au state '"+
          storedAu.getName() + "': " + me.getMessage());
      // continue
      return new AuState(storedAu, -1, -1, -1, null, this);
    } catch (Exception e) {
      logger.error("Couldn't load au state: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load au state.");
    }
  }

  public void storeDamagedNodeSet(DamagedNodeSet nodeSet) {
    try {
      logger.debug3("Storing damaged nodes for AU '" +
          nodeSet.theAu.getName() + "'");
      store(rootLocation, DAMAGED_NODES_FILE_NAME, nodeSet);
    } catch (Exception e) {
      logger.error("Couldn't store damaged nodes: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't store damaged nodes.");
    }
  }

  public DamagedNodeSet loadDamagedNodeSet() {
    try {
      logger.debug3("Loading state for AU '" + storedAu.getName() + "'");
      DamagedNodeSet damNodes = (DamagedNodeSet)load(
          rootLocation + DAMAGED_NODES_FILE_NAME, DamagedNodeSet.class);
      if (damNodes==null) {
        logger.debug2("No damaged node file for AU '"+storedAu.getName()+"'");
        return new DamagedNodeSet(storedAu, this);
      }
      damNodes.theAu = storedAu;
      damNodes.repository = this;
      return damNodes;
    } catch (XmlMarshaller.MarshallingException me) {
      logger.error("Marshalling exception for damaged nodes for '"+
                   storedAu.getName()+"': " + me.getMessage());
      // continue
      return new DamagedNodeSet(storedAu, this);
    } catch (Exception e) {
      logger.error("Couldn't load damaged nodes: ", e);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load damaged nodes.");
    }
  }

  void store(String root, String fileName, Object storeObj)
      throws Exception {
    XmlMarshaller marshaller = new XmlMarshaller();
    marshaller.store(root, fileName, storeObj,
        marshaller.getMapping(MAPPING_FILES));
  }

  Object load(String fileName, Class loadClass) throws Exception {
    XmlMarshaller marshaller = new XmlMarshaller();
    return marshaller.load(fileName, loadClass,
        marshaller.getMapping(MAPPING_FILES));
  }

  protected String getNodeLocation(CachedUrlSet cus)
      throws MalformedURLException {
    String urlStr = (String)cus.getUrl();
    if (AuUrl.isAuUrl(urlStr)) {
      return rootLocation;
    } else {
      try {
        URL testUrl = new URL(urlStr);
        String path = testUrl.getPath();
        if (path.indexOf("/.")>=0) {
          if (FileUtil.isLegalPath(path)) {
            // canonicalize to remove urls including '..' and '.'
            path = TEST_PREFIX + path;
            File testFile = new File(path);
            String canonPath = testFile.getCanonicalPath();
            String sysDepPrefix = FileUtil.sysDepPath(TEST_PREFIX);
            int pathIndex = canonPath.indexOf(sysDepPrefix) +
                sysDepPrefix.length();
            urlStr = testUrl.getProtocol() + "://" +
                testUrl.getHost().toLowerCase() +
                FileUtil.sysIndepPath(canonPath.substring(pathIndex));
          } else {
            logger.error("Illegal URL detected: "+urlStr);
            throw new MalformedURLException("Illegal URL detected.");
          }
        }
      } catch (IOException ie) {
        logger.error("Error testing URL: "+ie);
        throw new MalformedURLException ("Error testing URL.");
      }
      return LockssRepositoryImpl.mapUrlToFileLocation(rootLocation, urlStr);
    }
  }

  // These functions are for repository name changes and the like.
  // They should be used when necessary, then commented out until the next
  // implementation is required.

  void checkFileChange() {
    if ((theDaemon==null) || (theDaemon.getPluginManager()==null)) {
      // abort if null, for test code
      return;
    }
    File topDir = new File(rootLocation);
    File topDirState = new File(topDir, AU_FILE_NAME);
    // if the new version doesn't exist, post-order recurse
    if (!topDirState.exists()) {
      logger.info("Older file versions being used; updating to current names");
      try {
        File auCusDir = new File(getNodeLocation(storedAu.getAuCachedUrlSet()));
        if (auCusDir.isDirectory()) {
          recurseFileChange(auCusDir);
        }
      } catch (MalformedURLException mue) {
        logger.error("Error updating from old state filenames: "+mue);
        return;
      }
    }
    // finish by fixing top level values
    File oldDamageFile = new File(topDir, "damaged_nodes.xml");
    if (oldDamageFile.exists()) {
      oldDamageFile.renameTo(new File(topDir, DAMAGED_NODES_FILE_NAME));
    }
    File oldAuState = new File(topDir, "au_state.xml");
    if (oldAuState.exists()) {
      oldAuState.renameTo(topDirState);
    }
    logger.debug("Finished updating.");
  }

  void recurseFileChange(File nodeDir) throws MalformedURLException {
    File[] children = nodeDir.listFiles();
    for (int ii=0; ii<children.length; ii++) {
      // post-order recursion
      if (children[ii].isDirectory()) {
        recurseFileChange(children[ii]);
      }
    }

    // finish by fixing own values
    File oldNodeState = new File(nodeDir, "nodestate.xml");
    if (oldNodeState.exists()) {
      oldNodeState.renameTo(new File(nodeDir, NODE_FILE_NAME));
    }
    File oldHistoryFile = new File(nodeDir, "history.xml");
    if (oldHistoryFile.exists()) {
      oldHistoryFile.renameTo(new File(nodeDir, HISTORY_FILE_NAME));
    }
  }

  /**
   * Factory method to create new HistoryRepository instances.
   * @param au the {@link ArchivalUnit}
   * @return the new HistoryRepository instance
   */
  public static HistoryRepository createNewHistoryRepository(ArchivalUnit au) {
    // XXX needs to handle multiple disks/repository locations

    String historyLocation = Configuration.getParam(PARAM_HISTORY_LOCATION);
    if (historyLocation == null) {
      logger.error("Couldn't get " + PARAM_HISTORY_LOCATION +
          " from Configuration");
      throw new LockssRepository.RepositoryStateException(
          "Couldn't load param.");
    }
    historyLocation = extendCacheLocation(historyLocation);

    return new HistoryRepositoryImpl(au,
        LockssRepositoryImpl.mapAuToFileLocation(historyLocation, au));
  }

  static String extendCacheLocation(String cacheDir) {
    StringBuffer buffer = new StringBuffer(cacheDir);
    if (!cacheDir.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(HISTORY_ROOT_NAME);
    buffer.append(File.separator);
    return buffer.toString();
  }

  public static class Factory implements LockssAuManager.Factory {
  public LockssAuManager createAuManager(ArchivalUnit au) {
    return createNewHistoryRepository(au);
  }
}

}
