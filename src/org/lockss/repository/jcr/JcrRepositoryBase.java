/**

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
 * This class exists only to supply common, useful methods
 * and common constants.
 *
 * **** IMPORTANT NOTE ****
 * Because this class is the base class for RepositoryNodeImpl, RepositoryFileImpl,
 * and RepositoryFileVersionImpl, every instance contains an instance of these
 * variables.  This takes a fair bit of memory.  When you need to free up more memory,
 * you should eliminate the member variables of this class, and move up the chain to 
 * find the ancestor with the value for the variables in this class.
 * 
 * @author edwardsb
 *
 */
public abstract class JcrRepositoryBase {
  // Constants  
  protected final static String k_propIsTreeSizeFilled = "IsTreeSizeFilled";
  protected final static String k_propSizeMax = "SizeMax";
  protected final static String k_propStemFile = "StemFile";
  protected final static String k_propTreeContentSize = "TreeContentSize";

  // Static variables.  These variables must work when multiple 
  // threads are happening.
  private static Logger logger = Logger.getLogger("JcrRepositoryBase");
  protected static StringObjectTransformer sm_sotTransformer = 
    new StringObjectTransformer();
  
  // Member variables
  protected Node m_node;
  protected Session m_session;          
  protected String m_stemFile;  // The filename, without the 5-digit suffix.

  /**
   * This constructor creates a new JcrRepositoryBase.
   * 
   * @param session            The current JCR session
   * @param node               The JCR node that contains the database portion
   * @param stemFile           The location to put .WARC files
   * @param url                The associated URL
   * @throws LockssRepositoryException
   */
  public JcrRepositoryBase(Session session, Node node, String stemFile)
      throws LockssRepositoryException {
    try {
      testIfNull(session, "session");
      testIfNull(node, "node");
      testIfNull(stemFile, "stemFile");

      m_stemFile = stemFile;
      
      constructorShared(session, node);
      
      m_node.setProperty(k_propStemFile, stemFile);
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("Repository Exception in constructor(1): " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }
  
  /**
   * This constructor assumes that the JcrRepositoryBase is already in the database.
   * 
   * @param session       The current JCR session
   * @param node          The JCR node that contains the database portion of the data
   * @throws LockssRepositoryException
   */
  protected JcrRepositoryBase(Session session, Node node) throws LockssRepositoryException {
    Property propStemFile;
    Property propSizeMax;
    Property propURL;
    try {
      testIfNull(session, "session");
      testIfNull(node, "node");

      constructorShared(session, node);
      if (m_node.hasProperty(k_propStemFile)) {
        propStemFile = m_node.getProperty(k_propStemFile);
        m_stemFile = propStemFile.getString();
      } else {
        logger.error("The stem file was not found on this node; the node probably didn't already exist.  You probably should have used the extended (6-parameter) constructor instead.");
        throw new LockssRepositoryException(
            "The stem file was not found on this node.");
      }

    } catch (RepositoryException e) {
      logger.error("Repository Exception in constructor(2): "
          + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }

  
  /**
   * This method holds code shared between the two constructors
   * for RepositoryNodeImpl.
   *  
   * @param session    The current JCR session
   * @param node       The JCR node that contains the database portion of the data
   * @throws LockssRepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
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
   * Create a node in the database with a version name.
   * 
   * @param strVersion  The version name.
   * @return The shiny, new, happy Node!  Isn't it cute?
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
   * 
   * @param obj      An object to be testing for equality to null.
   * @param strName  The name of the above object.
   */
  protected void testIfNull(Object obj, String strName) {
    if (obj == null) {
      throw new NullPointerException(strName + " is null.");
    }
  }
}
