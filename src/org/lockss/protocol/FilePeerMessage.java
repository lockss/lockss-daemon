/*
 * $Id: FilePeerMessage.java,v 1.4 2008-11-02 21:13:48 tlipkis Exp $
 */

/*

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

package org.lockss.protocol;

import java.io.*;

import org.lockss.util.*;

/** Implementation of PeerMessage that stores its data in a file
 */
class FilePeerMessage extends PeerMessage {
  static Logger log = Logger.getLogger("FilePM");

  private File dataFile = null;
  private File dir;
  private OutputStream outStream;
  private long fileSize;

  /** Create a FilePeerMessage, with data to be stored in a file in dir.
   * @param dir directory in which to store data file..
   */
  public FilePeerMessage(File dir) {
    super();
    this.dir = dir;
  }

  /** Return an InputStream on the payload.
   * @throws IllegalStateException if message data not stored yet
   */
  public synchronized InputStream getInputStream()
      throws IllegalStateException, FileNotFoundException {
    checkHasData();
    return new FileInputStream(dataFile);
  }

  /** Return an OutputStream to which to write the payload.  May only be
   * called once.
   * @throws IllegalStateException if called a second time
   */
  public synchronized OutputStream getOutputStream()
      throws IllegalStateException, IOException {
    if (hasData() || isOutputOpen) {
      throw new IllegalStateException("PeerMessage already open for output");
    }
    File file = File.createTempFile("msg", ".data", dir);
    isOutputOpen = true;
    dir = null;
    outStream = new MsgOutputStream(file);
    return outStream;
  }

  public synchronized void delete() {
    if (isOutputOpen) {
      IOUtil.safeClose(outStream);
    }
    if (dataFile != null) {
      dataFile.delete();
      dataFile = null;
    }
  }

  public File getDataFile() {
    return dataFile;
  }

  public boolean hasData() {
    return dataFile != null;
  }

  /** Return the size of the data
   * @throw IllegalStateException if message data not stored yet
   */
  public long getDataSize() {
    checkHasData();
    return fileSize;
  }

  public String toString() {
    return this.toString("File");
  }

  private class MsgOutputStream extends FileOutputStream {
    private File file = null;

    public MsgOutputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
    }

    public synchronized void close() throws IOException {
      if (file != null) {
	super.close();
	fileSize = file.length();
	dataFile = file;
	file = null;
	isOutputOpen = false;
	outStream = null;
      }
    }
  }
}
