/**
 * 
 */
package org.lockss.util;

import java.io.*;
import java.util.Random;

import org.lockss.util.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestStreamerFile extends TestCase {

  private final static int k_sizeText = 255;
  
  private Random m_rand;
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    m_rand = new Random();
    super.setUp();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.util.StreamerFile#getInputStream()}.
   * @throws IOException 
   */
  public final void testGetInputStream() throws IOException {
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

    jstr = new StreamerFile(fileTemp);
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

  /**
   * Test method for {@link org.lockss.util.StreamerFile#writeFromInputStream(java.io.InputStream)}.
   * @throws IOException 
   */
  public final void testWriteFromInputStream() throws IOException {
    byte[] arbyText;
    ByteArrayInputStream baistr;
    File fileTemp;
    InputStream istrFile; 
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
    fileTemp = FileUtil.createTempFile("testWriteFromInputStream", "tmp");
    
    jstr = new StreamerFile(fileTemp);
    ostrJstr = jstr.getOutputStream();
    ostrJstr.write(arbyText);
    ostrJstr.close();
    
    // Get it from our file.
    // Notice that we don't get it from the Streamer.
    baistr = new ByteArrayInputStream(arbyText);
    istrFile = new FileInputStream(fileTemp);
    assertTrue(StreamUtil.compare(baistr, istrFile));
    
    fileTemp.delete();
  }

}
