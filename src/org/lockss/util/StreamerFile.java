/**
 * 
 */
package org.lockss.util;

import java.io.*;

import org.apache.log4j.Logger;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 */
public class StreamerFile implements Streamer {
  /**
   * <p>A logger for this class.</p>
   */
  protected static Logger log = Logger.getLogger("StreamerFile");

  /**
   * The file being streamed.
   */
  private File m_file;
  
  public StreamerFile(File file) {
    m_file = file;
  }
  
  public boolean equals(Object obj) {
    StreamerFile strfile;
    
    if (obj instanceof StreamerFile) {
      strfile = (StreamerFile) obj;
      
      return m_file.equals(strfile.m_file);
    } else {   // obj is not a StreamerFile.
      return false;
    }
  }

  /* (non-Javadoc)
   * @see org.lockss.protocol.JcrStreamer#getInputStream()
   */
  public InputStream getInputStream() {
    InputStream istr;
    
    try {
      istr = new BufferedInputStream(new FileInputStream(m_file));
    } catch (FileNotFoundException e) {
      log.error("getInputStream: ", e);
      return null;
    }
    
    return istr;
  }

  /* (non-Javadoc)
   * @see org.lockss.protocol.JcrStreamer#writeFromInputStream(java.io.InputStream)
   */
  public OutputStream getOutputStream() throws IOException {
    OutputStream ostr;
    
    ostr = new BufferedOutputStream(new FileOutputStream(m_file));
    
    return ostr;
  }

  
  public int hashCode() {
    return m_file.hashCode();
  }
}
