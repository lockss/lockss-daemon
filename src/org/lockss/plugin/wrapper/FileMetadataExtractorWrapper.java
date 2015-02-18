/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wrapper;
import java.io.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;

/** Error catching wrapper for FileMetadataExtractor */
public class FileMetadataExtractorWrapper
  implements FileMetadataExtractor, PluginCodeWrapper {

  FileMetadataExtractor inst;

  public FileMetadataExtractorWrapper(FileMetadataExtractor inst) {
    this.inst = inst;
  }

  public Object getWrappedObj() {
    return inst;
  }

  public void extract(MetadataTarget target,
		      CachedUrl cu,
		      FileMetadataExtractor.Emitter emitter)
      throws IOException, PluginException {
    try {
      inst.extract(target, cu, emitter);
    } catch (LinkageError e) {
      throw new PluginException.LinkageError(e);
    }
  }

  public String toString() {
    return "[W: " + inst.toString() + "]";
  }

  static class Factory implements WrapperFactory {
    public Object wrap(Object obj) {
      return new FileMetadataExtractorWrapper((FileMetadataExtractor)obj);
    }
  }
}
