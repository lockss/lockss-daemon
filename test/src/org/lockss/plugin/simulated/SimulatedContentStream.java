/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.plugin.simulated;

import java.io.*;
import java.util.*;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;

import java.lang.reflect.*;

/**
 * <p>Title: SimulatedContentStream </p>
 * <p>Description: This class allows operations to be performed on data being
 * read in from a content file before they are written to the cache.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

class SimulatedContentStream extends BufferedInputStream {

  SimulatedContentStream(InputStream in, boolean isDamaged) {
    super(null);
    if (!isDamaged)
      this.in = new BufferedInputStream(in);
    else {
      byte[] content = getStreamContents(in);
      modifyContent(content);
      this.in = new ByteArrayInputStream(content);
    }
  }

  protected byte[] getStreamContents(InputStream in) {
    int b;
    UnsynchronizedByteArrayOutputStream str = new UnsynchronizedByteArrayOutputStream();
    try {
      while ((b=in.read())!=-1) {
        str.write(b);
      }
    } catch (IOException e) {
    }
    return str.toByteArray();
  }

  protected void modifyContent(byte[] content) {
    /* To "damage" content, change every fifth byte to zero */
    for (int i=0; i<Array.getLength(content); i+=5) {
      content[i] = (byte)0;
    }
  }
}