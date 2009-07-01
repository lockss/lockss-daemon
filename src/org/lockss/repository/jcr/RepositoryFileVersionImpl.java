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
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.jcr.*;

import org.archive.io.warc.*;
import org.archive.util.*;
import org.archive.util.anvl.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * WARNING: This method uses org.archive.io.warc. The documentation for this class says:
 * 
 * Experimental WARC Writer and Readers. Code and specification subject to
 * change with no guarantees of backward compatibility: i.e. newer readers may
 * not be able to parse WARCs written with older writers.
 */
public class RepositoryFileVersionImpl extends JcrRepositoryBase 
implements RepositoryFileVersion {
  
  // Constants ----
  // A location (offset within a file) isn't set.
  private final static long k_locUnset = -1;
  private final static String k_propContentFile = "ContentFile";
  private final static String k_propDeleted = "Deleted";
  private final static String k_propFileContentParameterized = 
    "FileContentParameterized";
  private final static String k_propFileIndex = "FileIndex";  
  private final static String k_propLocContent = "ContentPosition";
  private final static String k_propParent = "Parent";
  private final static String k_propProperties = "Properties";
  private final static String k_propSizeCurrent = "SizeCurrent";
  private final static String k_propSizeDeferredStream = "SizeDeferredStream";
  private final static String k_propSizeEditing = "SizeEditing";
  private final static String k_propVersionNumber = "VersionNumber";

  // Static member variables ----
  // The WARCWriter requires an atomic integer.
  private final static AtomicInteger sm_serialNumber = new AtomicInteger();
  protected static Logger logger = Logger.getLogger("RepositoryFileVersionImpl");

  // Member variables -----
  private ANVLRecord m_anvlRecord;
  protected DeferredTempFileOutputStream m_deffileTempContent;
  private File m_fileContentParameterized;  // With the five-digit suffix.
  protected boolean m_isLocked;  // If false: we can write to this version.
  private long m_lFileIndex;
  private long m_posWarc; // position within the WARC file.
  protected RepositoryFileImpl m_rfiParent;
  protected long m_sizeCurrent;    
  // Number bytes before the deferred stream becomes a file.
  protected int m_sizeDeferredStream = 10240;    
  protected long m_sizeEditing;

  /**
   * Note that "node" should NOT be the node from the parent RepositoryFile.
   * Each version should have its own node.
   * 
   * @param session
   * @param node
   * @param stemFile -- the filename, without the five-digit extension.
   * @param sizeMax
   * @param url
   * @param RepositoryFileImpl
   * 
   * @throws LockssRepositoryException
   * @throws FileNotFoundException
   */
  protected RepositoryFileVersionImpl(Session session, Node node, String stemFile,
      long sizeMax, String url, RepositoryFileImpl rfiParent, 
      int sizeDeferredStream, IdentityManager idman)
      throws LockssRepositoryException, FileNotFoundException {
    super(session, node, stemFile, sizeMax, url, idman);
    
    testIfNull(rfiParent, "rfiParent");
        
    m_fileContentParameterized = null;  // Nothing is stored, so null.
    m_lFileIndex = 0;
    m_posWarc = k_locUnset;
    m_sizeCurrent = 0;
    m_sizeEditing = 0;
    m_isLocked = false;
    m_rfiParent = rfiParent;
    m_sizeDeferredStream = sizeDeferredStream;
    
    constructorShared2(session, node);
    
    try {
      m_node.addMixin("mix:referenceable");
      
      m_node.setProperty(k_propFileIndex, m_lFileIndex);
      m_node.setProperty(k_propParent, m_rfiParent.m_node);
      m_node.setProperty(k_propSizeDeferredStream, m_sizeDeferredStream);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }
  
  
  protected RepositoryFileVersionImpl(Session session, Node node, RepositoryFileImpl rfiParent,
          IdentityManager idman) 
      throws LockssRepositoryException {
    super(session, node, idman);
    
    Property propStemFile;
    Property propFileContentParameterized;
    Property propFileHandler;
    Property propLocContent;
    Property propSizeCurrent;
    Property propSizeDeferredStream;
    Property propSizeEditing;
    Property propSizeMax;
    Property propUrl;

    testIfNull(rfiParent, "parent");

    constructorShared2(session, node);

    // EITHER the fileContent (ie: the unparameterized name) or the
    // fileContentParameterized (ie: the file where things are stored)
    // must be set.
    
    // These sections must be in the order fileContent, 
    // fileContentParameterized.  The value of m_isCommitted is 
    // correctly overwritten as true whenever fileContentParameterized
    // is set.
    
    try {
      propStemFile = getProperty(k_propStemFile, false);
      if (propStemFile != null) {
        m_stemFile = propStemFile.getString();
      }
      
      m_isLocked = false;
      propFileContentParameterized = getProperty(k_propFileContentParameterized, 
          false);
      if (propFileContentParameterized != null) {
        m_fileContentParameterized = new File(propFileContentParameterized.
            getString());
        m_isLocked = true;
      }
      
      if (m_stemFile == null && m_fileContentParameterized == null) {
        throw new LockssRepositoryException("Neither the parameterized nor " +
            "unparameterized filename was found in the repository.");
      }
      
      // The file index (number as part of file name), if it exists, should be set.
      propFileHandler = getProperty(k_propFileIndex, false);
      if (propFileHandler != null) {
        m_lFileIndex = propFileHandler.getLong();
      } else {
        // Technically, this is a throwable error.  
        // In reality, it's not important; we'll automatically find the
        // right spot to add new data.
        logger.info("file index not found.  Using default file index of 0.");
        m_lFileIndex = 0;
      }

      propLocContent = getProperty(k_propLocContent, false);
      if (propLocContent != null) {
        m_posWarc = propLocContent.getLong();
      } else {
        logger.info("location of content not found.  Using default, unset value.");
        m_posWarc = k_locUnset;
      }

      propSizeCurrent = getProperty(k_propSizeCurrent, false);
      if (propSizeCurrent != null) {
        m_sizeCurrent = propSizeCurrent.getLong();
      } else {
        logger.info("Current size not found.  Using default value of 0.");
        m_sizeCurrent = 0;
      }
      
      propSizeEditing = getProperty(k_propSizeEditing, false);
      if (propSizeEditing != null) {
        m_sizeEditing = propSizeEditing.getLong();
      } else {
        logger.info("Editing size not found.  Using default value of 0.");
        m_sizeEditing = 0;
      }

      propSizeMax = getProperty(k_propSizeMax, true);
      m_sizeWarcMax = propSizeMax.getLong();

      propUrl = getProperty(k_propUrl, true);
      m_url = propUrl.getString();

      m_rfiParent = rfiParent;
      
      propSizeDeferredStream = getProperty(k_propSizeDeferredStream, true);
      m_sizeDeferredStream = (int) propSizeDeferredStream.getLong();
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }
  
  // This method holds all code shared between the two constructors.
  private void constructorShared2(Session session, Node node) 
      throws LockssRepositoryException {
    m_deffileTempContent = null;
    m_node = node;
    
    // The WARC writer cannot write a record with no
    // ANVL record and no content.  However, tests
    // occasionally put in no content.  Therefore, we
    // must have a default anvlRecord.
    
    m_anvlRecord = new ANVLRecord();
    m_anvlRecord.addLabelValue("Implemented by", "LOCKSS");
    
    invalidateTreeSize();
  }

  
  /**
   * @see org.lockss.repository.v2.RepositoryFileVersion#commit()
   * @throws IOException
   * @throws LockssRepositoryException
   * @throws NoTextException
   */
  public synchronized void commit() 
      throws IOException, LockssRepositoryException, NoTextException {
    /**
     * After "getOutputStream()" or other ways to fill the content has been called
     * and the output stream has been filled, this operation saves the file in the
     * storage. In previous versions, this method was called "seal".
     * 
     * See also: <code>discard</code>.
     */
    File fileTempContent = null;
    InputStream istrTempContent = null;
    FileOutputStream fosWarc = null;
    WARCWriter warcw = null;

    if (!m_isLocked) {
      // If there's temporary content, then store it.
      if (m_deffileTempContent != null) {
        // Appending to the end of the file.
        fosWarc = getPermanentOutputStream(); 

        m_posWarc = m_fileContentParameterized.length();
        m_sizeCurrent = m_deffileTempContent.getByteCount();

        try {
          // **** The final version of this program will need the following
          // information:
          // **** cmprs: Whether to compress the data
          // **** warcinfoData: Any WARC metadata.
          warcw = new WARCWriter(sm_serialNumber, fosWarc, m_fileContentParameterized,
              false, null, null);

          // **** The final version of this program will need the following
          // information:
          // **** contentType: MIME content type
          // **** create14digitdate: The date for the source.
          // **** namedFields: Any metadata that belongs to the WARC

          if (m_deffileTempContent.isInMemory()) {
            istrTempContent = new ByteArrayInputStream(m_deffileTempContent.getData());            
          } else {   // the content is not in memory.
            fileTempContent = m_deffileTempContent.getFile();
            istrTempContent = new FileInputStream(fileTempContent);
          }
          warcw.writeResourceRecord(m_url, ArchiveUtils.get14DigitDate(),
              "application/octet-stream", m_anvlRecord, istrTempContent,
              m_deffileTempContent.getByteCount());
        } finally {
          // WARCWriter is not among the IOUtil.safeClose() types,
          // and it shouldn't be.
          if (warcw != null) {
            warcw.close();
          }

          IOUtil.safeClose(istrTempContent);
        }
      } else {
        logger.error("commit() requires text to be committed.");
        throw new NoTextException("commit() requires text to be committed.");
      }

      m_isLocked = true;

      try {
        m_node.setProperty(k_propContentFile,
            m_fileContentParameterized.getPath());
        m_node.setProperty(k_propLocContent,
            m_posWarc);
        m_node.setProperty(k_propSizeCurrent, m_sizeCurrent);

        m_session.save();
        m_session.refresh(true);
        clearTempContent();
      } catch (RepositoryException e) {
        throw new LockssRepositoryException(e);
      }
    } else { // m_isLocked already set
      throw new LockssRepositoryException("You may not call commit() twice.");
    }
    
    invalidateTreeSize();
  }


  /**
   * Mark a file as deleted. To reactivate, call <code>undelete</code>.
   * 
   * This method does not actually delete the file (ie: it does not call
   * m_nodeVersion.remove().)
   * 
   * This method can be called, even after a version is locked.
   */
  public void delete() 
      throws LockssRepositoryException {
    try {
      m_node.setProperty(k_propDeleted, true);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }

  
  /**
   * After "getOutputStream()" has been called, this method undoes the changes
   * and returns to the last saved version. See also: <code>commit</code>.
   * 
   * Discard just removes the information from the editing node.
   */
  public void discard() 
      throws LockssRepositoryException {
    File fileTempContent;
    
    if (!m_isLocked) {
      // Delete any temporary material.
      if (m_deffileTempContent != null) {
        try {
          m_deffileTempContent.close();
        } catch (IOException e) {
          // Caught, NOT rethrown, since we're just going to delete this file anyway.
          logger.debug2("In discard, got an IO Exception " +
                          "while closing.  (Ignored.) ", e);
        }
  
        if (! m_deffileTempContent.isInMemory()) {
          fileTempContent = m_deffileTempContent.getFile();
          fileTempContent.delete();
        }
        
        m_deffileTempContent = null;
      }
    } else {  // m_isLocked
      throw new LockssRepositoryException("discard() is not allowed, once " +
          "commit() has been called.");
    }
  }

  /**
   * Returns the size of the current version of stored cache.
   *
   * IMPORTANT: The method returns the size regardless of whether 
   * the 'isDeleted' flag has been set.  
   */  
  public long getContentSize() 
      throws LockssRepositoryException {
    return m_sizeCurrent;
  }

  /**
   * Returns the content within the RepositoryFile, as an InputStream.
   * 
   * The calling function must close this stream.
   * 
   * Note: You may not close warcrContent in this method;
   * doing so closes the derived istrContent.
   * 
   * A future programmer should determine whether there are
   * streams left open by this method -- even if the returned
   * istrContent is closed.
   */
  public InputStream getInputStream() 
      throws IOException, LockssRepositoryException {
    InputStream istrContent = null;
    WARCReader warcrContent = null;
    
    if (hasContent()) {
      if (!isDeleted()) {
        // Data is only in the original file.
        if (m_fileContentParameterized != null) {
          warcrContent = WARCReaderFactory.get(m_fileContentParameterized,
              m_posWarc);
          istrContent = warcrContent.get();
        } else { // if m_fileContentFileParameterized == null
          logger.error("getInputStream(): Requested " +
                "content, but the content file was never set.");
        }
      } else { // isDeleted()
        logger.info("getInputStream: called when the " +
                        "input stream was deleted.");
        return null;
      }
    } else { // !hasContent()
      logger.info("getInputStream: called when the " +
                        "input stream has no content.");
      return null;
    }
    return istrContent;
  }
  
  
  /**
   * Returns the node for this repository file version.
   * 
   * This protected method is NOT part of the RepositoryFileVersion interface.
   * It is used by RepositoryFileImpl, in particular 'setPreferredVersion'.
   */
  protected Node getNode() {
    return m_node;
  }
  
  /**
   * Returns the properties within a RepositoryFileVersion.
   * 
   * Important note: Properties comes from java.util.Properties. Properties 
   * is a hash function. 
   * 
   * Property comes from javax.jcr.Property.  The text of the Property 
   * contains the properties.
   * 
   * Important note: properties are independent of the text.  The 'commit'
   * method only changes text; properties are permanent once they're set.
   */
  public Properties getProperties() 
      throws IOException, LockssRepositoryException {
    InputStream istrReturn = null;
    Property propReturn;
    Properties propsReturn = new Properties();

    try {
      if (m_node.hasProperty(k_propProperties)) {
        propReturn = m_node.getProperty(k_propProperties);
        istrReturn = propReturn.getStream();
        propsReturn.load(istrReturn);
      } else { // Doesn't have property k_propProperties
        logger.debug3("getProperties: the requested " +
                          "properties do not exist.  Returning null.");
        propsReturn = null;
      }
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    } finally {
      IOUtil.safeClose(istrReturn);
    }
    

    return propsReturn;
  }
  
  public boolean hasContent() 
      throws LockssRepositoryException {
    if (!isDeleted()) {
      return m_fileContentParameterized != null && 
        m_posWarc != k_locUnset;
    } else { // if isDeleted
      logger.error("hasContent: called when content was " +
                        "deleted.");
      return false;
    }
  }

  /**
   * Determines whether this file is deleted. Deleted files may have old content
   * or children, but will appear in the list of files only when explicitly
   * asked for.
   */
  public boolean isDeleted() 
      throws LockssRepositoryException {
    Property propDeleted;
    
    try {
      if (m_node.hasProperty(k_propDeleted)) {
        propDeleted = m_node.getProperty(k_propDeleted);
        return propDeleted.getBoolean();
      } else {
        logger.debug3("isDeleted: the property was not set.  " +
                        "Returning false.");
        return false;
      }
    } catch (PathNotFoundException e) {
      logger.error("In isDeleted(), the '" + k_propDeleted
          + "' property was not found.  Returning false.");
      return false;
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }
  
  
  /**
   * Important note:
   * 
   * The parameter is the stem for the new file.  We need to move the file assigned to this
   * stem to the new stem.
   */
  public void move(String stemNew) throws LockssRepositoryException {
    InputStream istrContent;
    OutputStream ostrContent;
    
    testIfNull(stemNew, "stemNew");

    try {
      // Retrieve the text in this version
      istrContent = getInputStream();

      // Update the file name.
      m_fileContentParameterized = null;
      m_stemFile = stemNew;
      m_isLocked = false;
      
      m_node.setProperty(k_propStemFile, stemNew);
      
      m_session.save();
      m_session.refresh(true);
      
      // Write the text to the new file.
      setInputStream(istrContent);
      commit();
    } catch (IOException e) {
      logger.error("move: ", e);
      throw new LockssRepositoryException(e);
    } catch (RepositoryException e) {
      logger.error("move: ", e);
      throw new LockssRepositoryException(e);
    }
    
    // TODO: Mark the old text as old, so that it can be deleted later.
  }

  /**
   * The content is stored as an InputStream. This method will transfer the
   * information in the InputStream to the content. 
   * 
   * ** IMPORTANT NOTE:
   * setInputStream takes an input stream as parameter, not an object input
   * stream. What you put into the stream is exactly what you get out of the
   * stream. You cannot write bytes of a String into the input stream, then use
   * readUTF to get the String back.
   */
  public void setInputStream(InputStream istrContent) 
      throws IOException, LockssRepositoryException {
    testIfNull(istrContent, "istrContent");
    
    if (!m_isLocked) {
      m_sizeEditing = 0;
      
      // Copy everything, byte by byte, to the temp location.
      try {
        m_deffileTempContent = new DeferredTempFileOutputStream(m_sizeDeferredStream);
        m_sizeEditing = StreamUtil.copy(istrContent, m_deffileTempContent);
        m_deffileTempContent.flush();
        
        m_node.setProperty(k_propSizeEditing, m_sizeEditing);      
        
        m_session.save();
        m_session.refresh(true);
      } catch (RepositoryException e) {
        throw new LockssRepositoryException(e);
      } finally {
        IOUtil.safeClose(m_deffileTempContent);
      } // try / finally
    } else {  // m_isLocked
      throw new LockssRepositoryException("setInputStream() may not be called after commit().");
    }
  }

  /**
   * Sets the properties within a RepositoryFile.
   */
  public void setProperties(Properties prop)
      throws IOException, LockssRepositoryException {
    ByteArrayInputStream baisProp = null;
    ByteArrayOutputStream baosProp = null;
    byte[] arbyProp;

    testIfNull(prop, "prop");
    
    if (!m_isLocked) {
      try {
        baosProp = new ByteArrayOutputStream();
    
        prop.store(baosProp, "");
        arbyProp = baosProp.toByteArray();
      } finally {
        IOUtil.safeClose(baosProp);
      }
      
      try {
        baisProp = new ByteArrayInputStream(arbyProp);
        
        try {
          m_node.setProperty(k_propProperties, baisProp);
          
          m_session.save();
          m_session.refresh(true);
        } catch (RepositoryException e) {
          throw new LockssRepositoryException(e);
        }
      } finally {
        IOUtil.safeClose(baisProp);
      }
    } else {  // m_isLocked
      throw new LockssRepositoryException("setProperties() may not be called after commit().");
    }
  }

  /**
   * This method marks the file as no longer deleted. This method also
   * reactivates content.
   * 
   * Note that undelete may be called, even when m_isLocked is set.
   */
  public void undelete() 
      throws LockssRepositoryException {
    try {
      m_node.setProperty(k_propDeleted, false);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }
  
  // The following method is useful for object recognition.
  public boolean equals(Object obj) {
    RepositoryFileVersionImpl rfviObj;
    
    if (obj instanceof RepositoryFileVersionImpl) {
      rfviObj = (RepositoryFileVersionImpl) obj;
      
      try {
        return rfviObj.m_node.isSame(m_node);
      } catch (RepositoryException e) {
        logger.error("equals: ", e);
        logger.error("Tossing the exception into the bit bucket; returning false.");
        return false;
      }
    } else {  // obj is not RepositoryFileVersion
      return false;
    }
  }
  
  // The following private methods should not be called by outside members, except for testing.
  
  /**
   * Please note: This method only clears temporary content. It does NOT remove
   * content from the permanent storage.
   * 
   * This method is called by 'commit()'.
   */
  public void clearTempContent() {
    if (m_deffileTempContent != null) { // Remove the temporary storage...    
      try {
        m_deffileTempContent.close();
        
        if (!m_deffileTempContent.isInMemory()) {
          m_deffileTempContent.deleteTempFile();
        }
       
      } catch (IOException e) {
        logger.debug3("In clearContent(), we got an " +
                        "IO Exception.  (Ignored.) ", e);
      } finally {      
        m_deffileTempContent = null;
      }
    }  // No else needed; if the temp content is empty, do nothing.
  }
  
  
  private FileOutputStream getPermanentOutputStream()
      throws FileNotFoundException, LockssRepositoryException {
    FileOutputStream fosWarc;
    String strFilename;

//  Determine whether the current, parameterized file has enough
//  space for our new material.
    while (m_fileContentParameterized == null || 
        (m_fileContentParameterized.length() + m_sizeEditing > 
        m_sizeWarcMax)) {
      m_lFileIndex++;
      strFilename = createPermanentFileName(m_stemFile, m_lFileIndex);      
      m_fileContentParameterized = new File(strFilename);
    }

    fosWarc = new FileOutputStream(m_fileContentParameterized, true);

    try {
      m_node.setProperty(k_propFileContentParameterized, 
          m_fileContentParameterized.getPath());
      m_node.setProperty(k_propFileIndex, m_lFileIndex);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }

    return fosWarc;
  }

  /**
   * 'protected' because it's used by the test program.
   */
  static protected String createPermanentFileName(String strOldFilename, 
          long lFileParameter) {
    StringBuilder sbFilename;
  
    // There might be better ways to create the name; in this case,
    // I'm just appending the name with (at least) 5 digits and .WARC.
    // I expect that the original files will be given without a final 
    // .WARC.
    
    // This code works, even if the m_lFileParameter becomes greater
    // than 99999: The files just get names like "foo100005.warc",
    // with the number greater than 99999.  
    
    sbFilename = new StringBuilder();
    sbFilename.append(strOldFilename);
    sbFilename.append(String.format("%1$05d", lFileParameter));
    sbFilename.append(".warc");
    
    return sbFilename.toString();
  }
  
  /**
   * To save a bit of typing, again and again...
   *  
   * prop:    Which property under m_nodeVersion to return.
   * isError: if true, then throw an error if there's no property.
   *          if false, then return null if there's no property.
   */
  private Property getProperty(String prop, boolean isError) 
      throws LockssRepositoryException {
    try {
      if (m_node.hasProperty(prop)) {
        return m_node.getProperty(prop);
      } else {
        if (isError) {
          logger.error("Property " + prop + " was not found in the version.");
          throw new LockssRepositoryException("Property " + prop + " was not found in the version.");
        } else {
          return null;
        }
      }
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
  }
}
