/*
 * $Id: JcrCollection.java,v 1.1.2.3 2009-09-23 02:03:02 edwardsb1 Exp $
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
 * @author edwardsb
 *
 */
public class JcrCollection implements CollectionOfAuRepositories {

  // Constants
  static private final String k_DIRECTORY_DATASTORE = "/org/lockss/repository/jcr/DatastoreFiles/";
  static public final String k_FILENAME_DATASTORE = "LargeDatastore.xml";

  // Static variables
  private static Logger logger = Logger.getLogger("JcrCollection");

  // Member variables
  private File m_dirSource;
  
  /*
   * Constructor...
   */
  public JcrCollection(File dirSource) throws LockssRepositoryException {
    if (!dirSource.exists() || !dirSource.isDirectory()) {
      logger.error("The source must be a directory.");      
      throw new LockssRepositoryException("The JcrCollection must be created with a directory.");
    }
    
    m_dirSource = dirSource;
  }
  
  /* (non-Javadoc)
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

  
  /* (non-Javadoc)
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#listAuRepositories(java.io.File)
   */
  public Map<String, File> listAuRepositories() {
    // This routine looks for all directories under a given directory with the
    // k_FILENAME_DATASTORE in them.  The object returned is just the string
    // for the directory...
    return recurseDirectories(m_dirSource, new HashMap<String, File>());
  }

  
  // A depth-first search for all directories with k_FILENAME_DATASTORE.
  private Map<String, File> recurseDirectories(File dirCurrent, Map<String, File> mastrfile) {
    File[] arfileChildren;
    File fileDatastore;
    
    fileDatastore = new File(dirCurrent, k_FILENAME_DATASTORE);
    if (fileDatastore.exists()) {
      mastrfile.put(dirCurrent.getName(), fileDatastore);
    }
    
    // Poorly-chosen function name.  It lists files and directories.
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
   * Important note: I assume that the AU is already available, from outside the JcrCollection.
   * 
   * @throws FileNotFoundException 
   * 
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#openAuRepository(java.io.File)
   */
  public LockssAuRepository openAuRepository(ArchivalUnit au, File dirLocation)
      throws LockssRepositoryException, FileNotFoundException {
//    JcrHelperRepository jhrAu;
//    JcrHelperRepositoryFactory jhrf;
    LockssAuRepository lar;
    
//    RepositoryNodeImpl rni;
//    
//    if (!JcrHelperRepositoryFactory.isPreconstructed()) {
//      logger.error("You must call JcrHelperRepositoryFactory.preconstructor before you call openAuRepository.");
//      throw new LockssRepositoryException("JcrHelperRepositoryFactory.preconstructor must be called first.");
//    }
//    
//    jhrf = JcrHelperRepositoryFactory.constructor();
//    jhrAu = jhrf.getHelperRepositoryByDirectory(dirLocation);
//
//    if (jhrAu == null) {
//      // The repository isn't in the JcrHelperRepositoryFactory.
//      // Choose an arbitrary repository.
//      jhrAu = jhrf.chooseHelperRepository();
//      
//      if (jhrAu == null) {
//        logger.error("Cannot open an AU repository if no JcrHelperRepositories exist.");
//        throw new LockssRepositoryException("No JcrHelperRepositories.");
//      }
//    }
    
    lar = new LockssAuRepositoryImpl(au);

    return lar;
  }

  
  /* (non-Javadoc)
   * @see org.lockss.repository.v2.CollectionOfAuRepositories#getDF()
   */
  public DF getDF() throws UnsupportedException {
    PlatformUtil pu = PlatformUtil.getInstance();
    
    return pu.getDF(m_dirSource.getAbsolutePath());
  }
}
