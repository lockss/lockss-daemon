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

import java.io.*;
import java.sql.*;

import javax.jcr.*;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.*;
import org.apache.jackrabbit.core.data.*;
import org.lockss.protocol.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryFile;

/**
 * @author edwardsb
 *
 * This class tests the memory use of the repository.  It is meant to
 * be run with a profiler (like jprofiler).  If the repository is correctly
 * written, it should not constantly add required memory; garbage collection
 * should keep this class running well.
 */

public class FileVersionMemoryTest {
  // Constants...
  private static final int k_maxNode = 1000000;
  private static final int k_maxVersion = 5;
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final String k_stemFile = "TestRepository/Content";
  private static int k_sizeMaxBuffer = 10000;
  private static final String k_strDirectory = "TestRepository/";
  private static final String k_urlDefault = "http://www.makemoneyfast.co/really_were_not_a_scam_site_honest.html";
  private static final String k_username = "username";

  // Variables
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private Session m_session;
  
  public void run() throws Exception {
    byte[] arbyContent;
    DataStore ds;
    IdentityManager idman;
    int i;
    InputStream istrContent;
    Node nodeFVMT;
    Node nodeTest;
    int numNode;
    int numVersion;
    RepositoryConfig repconfig;
    RepositoryFile rfTest;
    RepositoryFileVersion rfvTest;
    String strNode;
    
    arbyContent = new byte[25];
    for (i=0; i<25; i++) {
      arbyContent[i] = (byte) (65 + i);  // "ABC..XY"
    }
    
    idman = new MockIdentityManager();
    
    // Initiation.  Taken from TestRepositoryFileImpl.java.
    repconfig = RepositoryConfig.create(k_strDirectory + k_nameXml,
        k_strDirectory);
    m_repos = RepositoryImpl.create(repconfig);
    m_session = m_repos.login(new SimpleCredentials(k_username, k_password
        .toCharArray()));
    m_nodeRoot = m_session.getRootNode();    

    nodeFVMT = m_nodeRoot.addNode("FileVersionMemoryTest");
    m_nodeRoot.save();

    // Test.
    // Notice that there is exactly ONE RepositoryFile and exactly
    // ONE RepositoryFileVersion at any time.
    for (numNode = 0; numNode < k_maxNode; numNode++) {
      strNode = Integer.toString(numNode);
      nodeTest = nodeFVMT.addNode(strNode);
      m_nodeRoot.save();
      
      rfTest = new RepositoryFileImpl(m_session, nodeTest, 
          k_stemFile, k_sizeMaxBuffer, k_urlDefault, idman);
      
      for (numVersion = 0; numVersion < k_maxVersion; numVersion++) {
        istrContent = new ByteArrayInputStream(arbyContent);

        rfvTest = rfTest.createNewVersion();
        rfvTest.setInputStream(istrContent);
        rfvTest.commit();
        
        istrContent.close();
        
        istrContent = null; // Remove its one instance.
        rfvTest = null;  // Remove its one instance.
      }
      
      rfTest = null;  // Remove its one instance.
    }
    
    // Shutdown.  Taken from TestRepositoryFileImpl.java
    if (m_repos != null) {
      ds = m_repos.getDataStore();
      if (ds != null) {
        try {
          ds.clearInUse();
          ds.close();
        } catch (DataStoreException e) {
          e.printStackTrace();
        }
      }

      m_repos.shutdown();
      m_repos = null;
    }

    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {
      // From the documentation:

      // A successful shutdown always results in an SQLException to indicate
      // that Cloudscape [Derby]
      // has shut down and that there is no other exception.
    }

    System.gc();

  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    FileVersionMemoryTest fvmt;
    
    fvmt = new FileVersionMemoryTest();
    fvmt.run();
  }
}
