/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;
import java.io.*;
import java.util.*;

/**
 * LeafEntry is a leaf-specific subclass of RepositoryEntry.
 */
public interface LeafEntry extends RepositoryEntry {
  /**
   * Whether or not the entry exists in the cacher.
   * @return true if the entry is cached
   */
  public boolean exists();

  /**
   * Prepares the entry to write to a new version.  Should be called before storing
   * any data.
   */
  public void makeNewVersion();

  /**
   * Closes the current version to any future writing.  Should be called when done
   * storing data.
   */
  public void closeCurrentVersion();

  /**
   * Returns the current version.  This is the open version when writing,
   * and the one accessed by the <code>getInputStream()</code> and
   * <code>getProperties()</code>.
   * @return the current version
   */
  public int getCurrentVersion();

  /**
   * Return an <code>InputStream</code> object which accesses the
   * content in the cache.
   * @return an <code>InputStream</code> object from which the contents of
   *         the cache can be read.
   */
  public InputStream getInputStream();

  /**
   * Return a <code>Properties</code> object containing the headers of
   * the object in the cache.
   * @return a <code>Properties</code> object containing the headers of
   *         the original object being cached.
   */
  public Properties getProperties();

  /**
   * Return an <code>OutputStream</code> object which writes to a new version
   * in the cache.  <code>makeNewVersion()</code> must be called first.
   * @return an <code>OutputStream</code> object to which the new contents can be
   * written.
   * @throws LeafEntry.NoNewVersionException if <code>makeNewVersion()</code> hasn't been called.
   * @see makeNewVersion()
   */
  public OutputStream getNewOutputStream() throws NoNewVersionException;

  /**
   * Stores the properties for a new version of the cache.  <code>makeNewVersion()</code>
   * must be called first.
   * @param newProps a <code>Properties</code> object containing the headers of
   *         the new version being cached.
   * @throws LeafEntry.NoNewVersionException if <code>makeNewVersion()</code> hasn't been called.
   * @see makeNewVersion()
   */
  public void setNewProperties(Properties newProps) throws NoNewVersionException;

  /**
   * <code>NoNewVersionException</code> is the <code>Exception</code> thrown
   * when <code>getNewOutputStream()</code> or <code>setNewProperties()</code>
   * is called before <code>makeNewVersion()</code> is called.
   */
  public class NoNewVersionException extends Exception {
    public NoNewVersionException(String message) {
      super(message);
    }
  }

}
