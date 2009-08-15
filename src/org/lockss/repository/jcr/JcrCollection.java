/*
 * $Id: JcrCollection.java,v 1.1.2.1 2009-08-15 00:51:25 edwardsb1 Exp $
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

import org.lockss.repository.v2.CollectionOfAuRepositories;
import org.lockss.util.*;

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

  /*
   * Constructor...
   */
  public JcrCollection() {
    /* Constructor */
    /* Does nothing for now... */
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
  public Map<String, Object> listAuRepositories(File dirSource) {
    // This routine looks for all directories under a given directory with the
    // k_FILENAME_DATASTORE in them.  The object returned is just the string
    // for the directory...
    return recurseDirectories(dirSource, new HashMap<String, Object>());
  }

  // A depth-first search for all directories with k_FILENAME_DATASTORE.
  private Map<String, Object> recurseDirectories(File dirCurrent, Map<String, Object> mastrobj) {
    File[] arfileChildren;
    File fileDatastore;
    
    fileDatastore = new File(dirCurrent, k_FILENAME_DATASTORE);
    if (fileDatastore.exists()) {
      mastrobj.put(dirCurrent.getName(), dirCurrent.toString());
    }
    
    // Poorly-chosen function name.  It lists files and directories.
    arfileChildren = dirCurrent.listFiles();

    // *grumble*  The 'for' statement doesn't work correctly with a null variable.
    if (arfileChildren != null) {
      for (File fileChild : arfileChildren) {
        recurseDirectories(fileChild, mastrobj);
      }
    }
    return mastrobj;
  }
}
