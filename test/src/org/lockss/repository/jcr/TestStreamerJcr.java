/**
 * 
 */
package org.lockss.repository.jcr;

import java.io.*;
import java.util.Random;

import javax.jcr.*;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.data.*;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.util.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestStreamerJcr extends TestCase {

  // Constants
  private static final String k_dirXml = "test/src/org/lockss/repository/jcr/TestRepository/";
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final String k_propTest = "TestStreamerJcr";
  private static final int k_sizeMaxBuffer = 10000;
  private final static int k_sizeText = 255;
  private static final String k_username = "username";

  // Static member variables
  private Random m_rand;

  // Member variables
  private MockIdentityManager m_idman;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private Session m_session;

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    RepositoryConfig repconfig;

    repconfig = RepositoryConfig.create(k_dirXml + k_nameXml,
        k_dirXml);
    m_repos = RepositoryImpl.create(repconfig);
    m_session = m_repos.login(new SimpleCredentials(k_username, k_password
        .toCharArray()));
    m_nodeRoot = m_session.getRootNode();
    
    m_rand = new Random();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    DataStore ds;

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
    
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.StreamerJcr#getInputStream()}.
   */
  public final void testGetInputStream() throws Exception {
    byte[] arbyText;
    ByteArrayInputStream baistr;
    File fileTemp;
    InputStream istrJstr;  // What a wonderful name!
    int i;
    Streamer jstr;
    OutputStream ostrJstr;
    
    // Generate random text...
    arbyText = new byte[k_sizeText];
    for (i=0; i<k_sizeText; i++) {
      arbyText[i] = (byte) (64 + m_rand.nextInt(26)); 
    }
    
    // Put it into our file...
    baistr = new ByteArrayInputStream(arbyText);
    fileTemp = FileUtil.createTempFile("testGetInputStream", "tmp");

    jstr = new StreamerJcr(k_propTest, m_nodeRoot);
    ostrJstr = jstr.getOutputStream();
    ostrJstr.write(arbyText);
    ostrJstr.close();
    
    // Get it from our Streamer.
    // Notice that we don't get it from the (known) file.
    istrJstr = jstr.getInputStream();
    baistr = new ByteArrayInputStream(arbyText);
    assertTrue(StreamUtil.compare(baistr, istrJstr)); 
    
    fileTemp.delete();

  }
}
