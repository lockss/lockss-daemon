/*
 * $Id: CollectionOfAuRepositoriesImpl.java,v 1.1.2.7 2009-11-03 23:44:52 edwardsb1 Exp $
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
  static private final String k_FILENAME_AUNAME = "AUName.txt";
  
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
  public CollectionOfAuRepositoriesImpl(File dirSource) throws IOException {
    if (!dirSource.exists() || !dirSource.isDirectory()) {
      logger.error("The source must be a directory.");      
      throw new IOException("The CollectionOfAuRepositoriesImpl must be created with a directory.");
    }
    
    m_dirSource = dirSource;
  }
  
  
  protected File getSource() {
    return m_dirSource;
  }
  
   /**
    * Given a directory, add the files necessary to store files there.
    *
    * @param au                     The archival unit.  Only its name is used in the code.
    * @param dirSource         Where to generate the files for an AU Repository.
    * @throws IOException     If <code>dirSource</code> either was not a directory, or it could not be created. 
    * @see org.lockss.repository.v2.CollectionOfAuRepositories#generateAuRepository(java.io.File)
    */
  public void generateAuRepository(ArchivalUnit au, File dirSource) throws LockssRepositoryException, IOException {
    BufferedWriter buffwName;
    InputStream istrDatastore;
    OutputStream ostrDatastore;
    String strAuID;
    
    if (au == null) {
      throw new LockssRepositoryException("The AU must not be null.");
    }
    
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
  
    // Set the name for the AU Repository.
    // This should be changed to a proper configuration file.
    strAuID = au.getAuId();
    buffwName = new BufferedWriter(new FileWriter(new File(dirSource, k_FILENAME_AUNAME)));
    buffwName.write(strAuID);
    buffwName.close();
  }

  
  /**
   *  This routine looks for all directories under a given directory with the
   *  k_FILENAME_DATASTORE in them.
   *  
   * @return A map from the name of a file to the <code>File</code> associated. 
   */
  public List<String> listAuRepositories() throws IOException{
    return recurseDirectories(m_dirSource, new ArrayList<String>());
  }


  /**
   * A depth-first search among all directories under a given directory.
   * This method obviously does not correctly handle non-DAG directory structures.
   * 
   * @param dirCurrent  The directory being searched
   * @param listrAuId   The current list of directories that could hold the files for a repository. 
   * @return A map from a string to the file the string represents. 
   * @throws FileNotFoundException 
   */
  private List<String> recurseDirectories(File dirCurrent, List<String> listrAuId) throws IOException {
    BufferedReader buffrAuName;
    File[] arfileChildren;
    File fileDatastore;
    String strAuName;
    
    fileDatastore = new File(dirCurrent, k_FILENAME_AUNAME);
    if (fileDatastore.exists()) {
      try {
        buffrAuName = new BufferedReader(new FileReader(fileDatastore));
        strAuName = buffrAuName.readLine();
        listrAuId.add(strAuName);
      } catch (FileNotFoundException e) {
        logger.critical("Somehow, the AU name file passed the 'exists' test AND caused a FileNotFoundException.");
        throw new RuntimeException("The file " + fileDatastore.getPath() + " both exists and cannot be found.");
      }
    }
    
    // This function lists both files and directories, not just files.
    arfileChildren = dirCurrent.listFiles();

    // *grumble*  The 'for' statement doesn't work correctly with a null variable.
    if (arfileChildren != null) {
      for (File fileChild : arfileChildren) {
        recurseDirectories(fileChild, listrAuId);
      }
    }
    return listrAuId;
  }

  
  /**
   * This method opens a Lockss AU Repository from an ArchivalUnit. 
   * 
   * @param au The archival unit to open
   * @throws FileNotFoundException 
   */
  public LockssAuRepository openAuRepository(ArchivalUnit au)
      throws LockssRepositoryException, FileNotFoundException {
    LockssAuRepository lar;
    
    lar = new LockssAuRepositoryImpl(au, this);
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
