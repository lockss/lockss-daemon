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

package org.lockss.repository.v2;

import java.io.*;
import java.util.*;

import org.lockss.repository.*;
import org.lockss.repository.v2.*;

/**
 * @author edwardsb
 *
 * This class holds information about one version of one file.
 * (A file may have many versions.)
 */
public interface RepositoryFileVersion {
  /**
   * Returns the value within the RepositoryFile.
   */
  public InputStream getInputStream() throws IOException, LockssRepositoryException;

 
  /**
   * The content is stored as an InputStream. This method will transfer the
   * information in the InputStream to the content.
   */
  public void setInputStream(InputStream istr) throws IOException, LockssRepositoryException;

  
  /**
   * Does the file have stored content?
   */
  public boolean hasContent() throws LockssRepositoryException;

  /**
   * Determines whether this file is deleted. Deleted files may have old content
   * or children, but will appear in the list of files only when explicitly
   * asked for.
   */
  public boolean isDeleted() throws LockssRepositoryException;

  /**
   * Mark a file as deleted. To reactivate, call <code>undelete</code> .
   */
  public void delete() throws LockssRepositoryException;

  /**
   * This method marks the file as no longer deleted. This method also
   * reactivates content.
   */
  public void undelete() throws LockssRepositoryException;

  /**
   * After "getOutputStream()" has been called and the output stream has been
   * filled, this operation saves the file in the storage. In previous versions,
   * this method was called "seal". See also: <code>jettison</code>.
   */
  public void commit() throws IOException, LockssRepositoryException, NoTextException;

  /**
   * After "getOutputStream()" has been called, this method undoes the changes
   * and returns to the last saved version. See also: <code>commit</code>.
   */
  public void discard() throws LockssRepositoryException;

  /**
   * Returns the size of the current version of stored cache.
   * Important note: deleted files are INCLUDED in the content size.
   */
  public long getContentSize() throws LockssRepositoryException;

  /**
   * Returns the properties within a RepositoryFileVersion.
   */
  public Properties getProperties() throws IOException, LockssRepositoryException;

  /**
   * Sets the properties within a RepositoryFileVersion.
   */
  public void setProperties(Properties prop) throws IOException, LockssRepositoryException;
  
  /**
   * Put the underlying file in a different location.
   * 
   * @param strNewLocation
   */
  public void move(String strNewLocation) throws LockssRepositoryException;
    
}
