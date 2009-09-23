/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository.jcr;

import javax.jcr.*;

import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * This class exists only to supply common, useful methods
 * and common constants.
 */
public abstract class JcrRepositoryBase {
  // Constants  
  protected final static String k_propIsTreeSizeFilled = "IsTreeSizeFilled";
  protected final static String k_propSizeMax = "SizeMax";
  protected final static String k_propStemFile = "StemFile";
  protected final static String k_propTreeContentSize = "TreeContentSize";
  protected final static String k_propUrl = "URL";

  // Static variables.  These variables must work when multiple 
  // threads are happening.
  private static Logger logger = Logger.getLogger("JcrRepositoryBase");
  protected static StringObjectTransformer sm_sotTransformer = 
    new StringObjectTransformer();
  
  // Member variables
  protected IdentityManager m_idman;
  protected Node m_node;
  protected Session m_session;
  protected long m_sizeWarcMax;
  protected String m_stemFile;  // The filename, without the 5-digit suffix.
  protected String m_url;


  public JcrRepositoryBase(Session session, Node node, String stemFile,
      long sizeWarcMax, String url, IdentityManager idman)
      throws LockssRepositoryException {
    try {
      testIfNull(session, "session");
      testIfNull(node, "node");
      testIfNull(stemFile, "stemFile");
      testIfNull(url, "url");
      testIfNull(idman, "idman");
      if (sizeWarcMax < 1) {
        throw new LockssRepositoryException(
            "sizeMax must be strictly positive.");
      }
      m_stemFile = stemFile;
      m_sizeWarcMax = sizeWarcMax;
      m_url = url;
      m_idman = idman;
      
      constructorShared(session, node);
      
      m_node.setProperty(k_propSizeMax, sizeWarcMax);
      m_node.setProperty(k_propStemFile, stemFile);
      m_node.setProperty(k_propUrl, m_url);
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("constructor: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }
  
  protected JcrRepositoryBase(Session session, Node node,
      IdentityManager idman) throws LockssRepositoryException {
    Property propStemFile;
    Property propSizeMax;
    Property propURL;
    try {
      testIfNull(session, "session");
      testIfNull(node, "node");
      testIfNull(idman, "idman");
      m_idman = idman;
      constructorShared(session, node);
      if (m_node.hasProperty(k_propStemFile)) {
        propStemFile = m_node.getProperty(k_propStemFile);
        m_stemFile = propStemFile.getString();
      } else {
        logger.error("The stem file was not found on this node; the node probably didn't already exist.  You probably should have used the extended (6-parameter) constructor instead.");
        throw new LockssRepositoryException(
            "The stem file was not found on this node.");
      }
      if (m_node.hasProperty(k_propSizeMax)) {
        propSizeMax = m_node.getProperty(k_propSizeMax);
        m_sizeWarcMax = propSizeMax.getLong();
      } else {
        logger
            .error("The maximum size for a file was not found on this node.");
        throw new LockssRepositoryException(
            "The maximum size was not found on this node.");
      }
      if (m_node.hasProperty(k_propUrl)) {
        propURL = m_node.getProperty(k_propUrl);
        m_url = propURL.getString();
      } else {
        logger.error("The URL was not found on this node.");
        throw new NoUrlException("The URL was not found on this node.");
      }
    } catch (RepositoryException e) {
      logger.error("Repository Exception in constructor(2): "
          + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }

  /**
   * @throws LockssRepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  // This method holds code shared between the two constructors
  // for RepositoryNodeImpl.
  protected void constructorShared(Session session, Node node) 
      throws LockssRepositoryException {
    testIfNull(session, "session");
    testIfNull(node, "node");
    
    try {
      m_node = node;
      m_node.addMixin("mix:referenceable");
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
    
    m_session = session;
  }
  
  /**
   * @param strVersion
   * @return
   * @throws LockssRepositoryException
   */
  protected Node createNode(String strVersion) 
      throws LockssRepositoryException {
    Node nodeNew = null;
    
    try {
      // Delete any previous nodes that existed here.
      while (m_node.hasNode(strVersion)) {
        logger.debug("createNode: There already existed a node with name " 
                + strVersion);
        nodeNew = m_node.getNode(strVersion);
        nodeNew.remove();
        
        m_node.save();
      }
      
      // Create the node.
      nodeNew = m_node.addNode(strVersion);
      m_node.save();

      return nodeNew;
    } catch (RepositoryException e) {
      logger.error("createNode:" + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }


  /**
   * This method resets the tree size this node and every node above it.
   * 
   * It's used by both repository nodes and repository files.
   */
  protected void invalidateTreeSize()
      throws LockssRepositoryException {
    Node nodeInvalidate;
    
    nodeInvalidate = m_node;
    
    try {
      for (;;)  {  // FOREVER
        nodeInvalidate.setProperty(k_propIsTreeSizeFilled, false);
        nodeInvalidate.setProperty(k_propTreeContentSize, 0);
        
        // The forever loop stops when the following statement throws
        // an 'ItemNotFoundException'.
        nodeInvalidate = nodeInvalidate.getParent();
      }
    } catch (ItemNotFoundException e) {
      // Expected and perfectly normal.  Do nothing.
    } catch (RepositoryException e) {
      logger.error("invalidateTreeSize", e);
      throw new LockssRepositoryException(e);
    }
  }


  /**
   * Because the code
   *
   *     if (session == null) {
   *       throw new NullPointerException("session is null.");
   *     }
   * 
   * is far too long.  :->
   */
  protected void testIfNull(Object obj, String strName) {
    if (obj == null) {
      throw new NullPointerException(strName + " is null.");
    }
  }
}
