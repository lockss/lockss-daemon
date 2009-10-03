/*
 * $Id: CollectionOfAuRepositoriesImpl.java,v 1.1.2.4 2009-10-03 01:49:13 edwardsb1 Exp $
 */
/*
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

import java.io.*;
import java.util.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.repository.v2.*;
import org.lockss.util.*;
import org.lockss.util.PlatformUtil.*;

/**
 * Maintains all AU repositories under a given directory.  
 * I assume that all drives have their own directory.
 * 
 * @author Brent E. Edwards
 *
 */
public class CollectionOfAuRepositoriesImpl implements CollectionOfAuRepositories {

  // Constants
  static private final String k_DIRECTORY_DATASTORE = "/org/lockss/repository/jcr/DatastoreFiles/";
  static public final String k_FILENAME_DATASTORE = "LargeDatastore.xml";

  // Static variables
  private static Logger logger = Logger.getLogger("CollectionOfAuRepositoriesImpl");

  // Member variables
  private File m_dirSource;

  /**
   * Construct the class.
   * 
   * @param dirSource                    a File, under which all AuRepositories are.
   * @throws LockssRepositoryException   if <code>dirSource</code> is not a directory.
   */
  public CollectionOfAuRepositoriesImpl(File dirSource) throws LockssRepositoryException {
    if (!dirSource.exists() || !dirSource.isDirectory()) {
      logger.error("The source must be a directory.");      
      throw new LockssRepositoryException("The CollectionOfAuRepositoriesImpl must be created with a directory.");
    }
    
    m_dirSource = dirSource;
  }
  
   /**
    * Given a directory, add the files necessary to store files there.
    * 
    * @param dirSource        Where to generate the files for an AU Repository.
    * @throws IOException     If <code>dirSource</code> either was not a directory, or it could not be created. 
    * @see org.lockss.repository.v2.CollectionOfAuRepositories#generateAuRepository(java.io.File)
    */
  public void generateAuRepository(File dirSource) throws IOException {
    InputStream istrDatastore;
    OutputStream ostrDatastore;
    
    // Given a directory, this method will create the necessary files to generate the database...
    
    if (!dirSource.exists()) {
      if (!dirSource.mkdirs()) {
        logger.error("Could not construct " + dirSource);
        throw new IOException("Could not construct " + dirSource);
      }
    }

    if (!dirSource.isDirectory()) {
      logger.error(dirSource + " is not a directory.");
      throw new IOException(dirSource + " is not a directory.");
    }
    
    // Okay.  It's a directory.  Copy the necessary file there.
    istrDatastore = getClass().getResourceAsStream(k_DIRECTORY_DATASTORE + k_FILENAME_DATASTORE);
    ostrDatastore = new FileOutputStream(new File(dirSource, k_FILENAME_DATASTORE));
    StreamUtil.copy(istrDatastore, ostrDatastore);
    
    // Does anything else need to be done for the Au Repository?
  }

  
  /**
   *  This routine looks for all directories under a given directory with the
   *  k_FILENAME_DATASTORE in them.
   *  
   *  ** NOTE: BUG: The list returned now returns the directory name, NOT the
   *  name that was originally given.
   *  
   * @return 
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#listAuRepositories(java.io.File)
   */
  public Map<String, File> listAuRepositories() {
    return recurseDirectories(m_dirSource, new HashMap<String, File>());
  }


  /**
   * A depth-first search among all directories under a given directory.
   * This method obviously does not correctly handle non-DAG directory structures.
   * 
   * *** BUG: This routine should return the name originally given to an AU.
   * 
   * @param dirCurrent  The directory being searched
   * @param mastrfile   The current list of directories that could hold the files for a repository. 
   * @return A map from a string to the file the string represents. 
   */
  private Map<String, File> recurseDirectories(File dirCurrent, Map<String, File> mastrfile) {
    File[] arfileChildren;
    File fileDatastore;
    
    fileDatastore = new File(dirCurrent, k_FILENAME_DATASTORE);
    if (fileDatastore.exists()) {
      mastrfile.put(dirCurrent.getName(), fileDatastore);
    }
    
    // This function lists both files and directories, not just files.
    arfileChildren = dirCurrent.listFiles();

    // *grumble*  The 'for' statement doesn't work correctly with a null variable.
    if (arfileChildren != null) {
      for (File fileChild : arfileChildren) {
        recurseDirectories(fileChild, mastrfile);
      }
    }
    return mastrfile;
  }

  
  /**
   * This method opens an AU in a given directory.
   * 
   * @param au The archival unit to open
   * @param dirLocation Where the AU holds its data
   * @throws FileNotFoundException 
   * 
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#openAuRepository(java.io.File)
   */
  public LockssAuRepository openAuRepository(ArchivalUnit au, File dirLocation)
      throws LockssRepositoryException, FileNotFoundException {
    LockssAuRepository lar;
    
    lar = new LockssAuRepositoryImpl(au);
    return lar;
  }

  
  /**
   * The disk space used and available for the directory of this class.
   * 
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#getDF()
   */
  public DF getDF() throws UnsupportedException {
    PlatformUtil pu = PlatformUtil.getInstance();
    
    return pu.getDF(m_dirSource.getAbsolutePath());
  }
}
