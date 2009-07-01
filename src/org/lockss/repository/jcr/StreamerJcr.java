/**
 * 
 */
package org.lockss.repository.jcr;

import java.io.*;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.lockss.util.*;

/**
 * @author edwardsb
 *
 */

// Helper classes

class StreamerJcr implements org.lockss.util.Streamer {
  private final static int k_thresholdDeferredStream = 10240;

  private static Logger logger = Logger.getLogger("RepositoryFileImpl");

  private String m_prop;
  private Node m_node;
  
  public StreamerJcr(String prop, Node node) {
    m_prop = prop;
    m_node = node;
  }

  public InputStream getInputStream() {
    try {
      return m_node.getProperty(m_prop).getStream();
    } catch (PathNotFoundException e1) {
      logger.error("JcrStreamerJcr.getInputStream: ", e1);
      return null;
    } catch (ValueFormatException e2) {
      logger.error("JcrStreamerJcr.getInputStream: ", e2);
      return null;        
    } catch (RepositoryException e3) {
      logger.error("JcrStreamerJcr.getInputStream: ", e3);
      return null;
    }
  }

  public OutputStream getOutputStream() {
    return new JcrOutputStream(k_thresholdDeferredStream);
  }
  
  // Inner class within an inner class!
  
  // Note that the caller MUST call close() on a JcrOutputStream.
  class JcrOutputStream extends DeferredTempFileOutputStream {
    public JcrOutputStream(int threshold) {
      super(threshold);
    }

    public void close() throws IOException {
      // The documentation says that closing a ByteArrayOutputStream has 
      // no effect.  I don't want to rely on that behavior.
      super.close();
      
      try {
        if (isInMemory()) {
          ByteArrayInputStream baisData;
          
          // The translation between bytes and string doesn't work well when 
          // the bytes are negative.
          baisData = new ByteArrayInputStream(getData());
          m_node.setProperty(m_prop, baisData);
        } else {
          m_node.setProperty(m_prop, new FileInputStream(getFile()));
          getFile().delete();
        }
      } catch (ValueFormatException e) {
        logger.error("JcrOutputStream.close(): ", e);
        throw new IOException(e.getMessage());
      } catch (VersionException e) {
        logger.error("JcrOutputStream.close(): ", e);
        throw new IOException(e.getMessage());
      } catch (LockException e) {
        logger.error("JcrOutputStream.close(): ", e);
        throw new IOException(e.getMessage());
      } catch (ConstraintViolationException e) {
        logger.error("JcrOutputStream.close(): ", e);
        throw new IOException(e.getMessage());
      } catch (RepositoryException e) {
        logger.error("JcrOutputStream.close(): ", e);
        throw new IOException(e.getMessage());
      }
    }
  }

}

