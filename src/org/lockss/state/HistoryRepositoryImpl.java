/*
 * $Id: HistoryRepositoryImpl.java,v 1.2 2002-12-31 00:14:02 aalto Exp $
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


package org.lockss.state;

import java.io.*;
import java.util.*;
import org.lockss.util.Logger;
import org.lockss.util.ListUtil;
import org.lockss.repository.*;
import org.lockss.daemon.CachedUrlSet;
import org.lockss.daemon.Configuration;
import java.net.MalformedURLException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.lockss.util.FileLocationUtil;


/**
 * HistoryRepository is an inner layer of the NodeManager which handles the actual
 * storage of NodeStates.
 */
public class HistoryRepositoryImpl implements HistoryRepository {
  /**
   * Configuration parameter name for Lockss history location.
   */
  public static final String PARAM_HISTORY_LOCATION =
    Configuration.PREFIX + "history.location";

  /**
   * Configuration parameter name for Lockss history mapping file location.
   */
  public static final String PARAM_MAPPING_FILE_LOCATION =
    Configuration.PREFIX + "history.mapping.file";
  /**
   * Name of top directory in which the histories are stored.
   */
  public static final String HISTORY_ROOT_NAME = "cache";

  private static final String HISTORY_FILE_NAME = "history.xml";

  private static String rootDir;
  private Mapping mapping = null;
  private static Logger logger = Logger.getLogger("HistoryRepository");

  HistoryRepositoryImpl(String repository_location) {
    rootDir = repository_location;
  }

  public void storePollHistories(NodeState nodeState) {
    CachedUrlSet cus = nodeState.getCachedUrlSet();
    try {
      File nodeDir = new File(getNodeLocation(cus));
      if (!nodeDir.exists()) {
        nodeDir.mkdirs();
      }
      File nodeFile = new File(nodeDir, HISTORY_FILE_NAME);
      NodeHistoryBean nhb = new NodeHistoryBean();
      nhb.historyBeans = ((NodeStateImpl)nodeState).getPollHistoryBeanList();
      Marshaller marshaller = new Marshaller(new FileWriter(nodeFile));
      marshaller.setMapping(getMapping());
      marshaller.marshal(nhb);
    } catch (Exception e) {
      logger.error("Couldn't store poll history: ", e);
      throw new LockssRepository.RepositoryStateException("Couldn't store history.");
    }
  }

  public void loadPollHistories(NodeState nodeState) {
    CachedUrlSet cus = nodeState.getCachedUrlSet();
    String nodeDir = null;
    try {
      File nodeFile = new File(getNodeLocation(cus) + File.separator +
                               HISTORY_FILE_NAME);
      Unmarshaller unmarshaller = new Unmarshaller(NodeHistoryBean.class);
      unmarshaller.setMapping(getMapping());
      NodeHistoryBean nhb = (NodeHistoryBean)unmarshaller.unmarshal(new FileReader(nodeFile));
      ((NodeStateImpl)nodeState).setPollHistoryBeanList(new ArrayList(nhb.historyBeans));
    } catch (Exception e) {
      logger.error("Couldn't load poll history: ", e);
      throw new LockssRepository.RepositoryStateException("Couldn't load history.");
    }
  }

  public static HistoryRepository getHistoryRepository() {
    if (rootDir==null) {
      rootDir = Configuration.getParam(PARAM_HISTORY_LOCATION);
      if (rootDir==null) {
        logger.error("Couldn't get "+PARAM_HISTORY_LOCATION+" from Configuration");
        throw new LockssRepository.RepositoryStateException("Couldn't load param.");
      }
    }
    return new HistoryRepositoryImpl(rootDir);
  }

  protected String getNodeLocation(CachedUrlSet cus)
      throws MalformedURLException {
    StringBuffer buffer = new StringBuffer(rootDir);
    if (!rootDir.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(HISTORY_ROOT_NAME);
    buffer.append(File.separator);
    String auLoc = FileLocationUtil.mapAuToFileLocation(buffer.toString(),
        cus.getArchivalUnit());
    String urlStr = (String)cus.getSpec().getPrefixList().get(0);
    return FileLocationUtil.mapUrlToFileLocation(auLoc, urlStr);
  }

  protected Mapping getMapping() throws Exception {
    if (mapping==null) {
      String mappingFile = Configuration.getParam(PARAM_MAPPING_FILE_LOCATION);
      if (mappingFile==null) {
        logger.error("Couldn't get "+PARAM_MAPPING_FILE_LOCATION+" from Configuration");
        throw new LockssRepository.RepositoryStateException("Couldn't load param.");
      }
      mapping = new Mapping();
      mapping.loadMapping(mappingFile);
    }
    return mapping;

  }
}