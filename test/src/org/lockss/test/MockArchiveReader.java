/*
 * $Id$
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.math.*;
import java.util.*;
import org.archive.io.*;

import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * A mock version of <code>org.archive.io.ArchiveReader</code> used for testing
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class MockArchiveReader extends ArchiveReader {
  Logger log = Logger.getLogger("MockArchiveReader");

  private UrlCacher myUrlCacher;
  private InputStream myInputStream;

  private List validateList = null;
  private boolean strict = false;
  private boolean digest = false;
  private boolean valid = true;
  private boolean compressed = true;
  private String version = "0.0.0";
  private String readerIdentifier = "MockArchiveReader";

  //  A HashMap with key the offset and value the ArchiveRecord
  private Map records = new HashMap();
  //  A HashMap with key the offset and value the next offset
  private Map offsets = new HashMap();
  private long nextPutOffset = 0;
  private long nextGetOffset = 0;

  /**
   * Construct a mock archive reader backed by an ARC file
   * @param uc The UrlCacher for the ARC file
   * @param arcStream The InputStream for the ARC file
   */
  public MockArchiveReader(UrlCacher uc, InputStream arcStream) {
    myUrlCacher = uc;
    myInputStream = arcStream;
  }

  /**
   * Construct a mock archive reader full of synthetic content
   */
  public MockArchiveReader(UrlCacher uc) {
    this(uc, null);
  }

  public void addArchiveRecord(String url, String content,
			       Map headers) throws IOException {
    long futureOffset = nextPutOffset + content.length();
    Long key = new Long(nextPutOffset);
    ArchiveRecord rec = new MyMockArchiveRecord(url, content, headers,
						myInputStream);
    records.put(key, rec);
    offsets.put(key, new Long(futureOffset));
    nextPutOffset = futureOffset;
  }

  public void close() {
    records = null;
    offsets = null;
    nextPutOffset = 0;
    nextGetOffset = 0;
    myUrlCacher = null;
    myInputStream = null;
  }

  protected ArchiveRecord createArchiveRecord(InputStream is, long offset) {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void dump(boolean compress) {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public ArchiveRecord get(long offset) {
    ArchiveRecord ret = null;
    Long currentOffset = new Long(offset);
    Long newOffset = (Long)offsets.get(currentOffset);
    if (newOffset != null) {
      nextGetOffset = newOffset.longValue();
      ret = (ArchiveRecord)records.get(currentOffset);
    }
    return ret;
  }

  public ArchiveRecord get() {
    return get(nextGetOffset);
  }

  public ArchiveRecord getCurrentRecord() {
    return get();
  }

  public ArchiveReader getDeleteFileOnCloseReader(File f) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getDotFileExtension() {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getFileExtension() {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getFileName() {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getReaderIdentifier() {
    return readerIdentifier;
  }

  public String getStrippedFileName() {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public static String getStrippedFileName(String name,
					   String dotFileExtension) {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getVersion() {
    return version;
  }

  protected void gotoEOR(ArchiveRecord r) {
    // XXX
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean isCompressed() {
    return compressed;
  }

  public boolean isDigest() {
    return digest;
  }

  public boolean isStrict() {
    return strict;
  }

  public boolean isValid() {
    return valid;
  }

  public Iterator iterator() {
    return records.values().iterator();
  }

  public void logStdErr(java.util.logging.Level level,
			String message) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setDigest(boolean d) {
    digest = d;
  }

  public void setStrict(boolean d) {
    strict = d;
  }

  public List validate() {
    return validateList;
  }

  public List validate(int noRecord) {
    return validateList;
  }

  private class MyMockArchiveRecord extends ArchiveRecord {
    private String arc = null;
    private MyMockARCRecordMetaData metaData = null;
    private long offset = 0;
    private String content = null;

    MyMockArchiveRecord(String url, String ct, Map headerFields,
			InputStream arcStream) throws IOException {
      super(arcStream);
      arc = "foobar";
      content = ct;
      metaData = new MyMockARCRecordMetaData(arc, headerFields, url);
    }

    public int available() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void close() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void dump() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void dump(java.io.OutputStream os) {
      throw new UnsupportedOperationException("Not Implemented");
    }

    protected  String getDigest4Cdx(ArchiveRecordHeader h) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public String getDigestStr() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public ArchiveRecordHeader getHeader() {
      throw new UnsupportedOperationException("Not Implemented");
      // return metaData;
    }
           
    protected  java.io.InputStream getIn() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected java.lang.String getIp4Cdx(ArchiveRecordHeader h) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected java.lang.String getMimetype4Cdx(ArchiveRecordHeader h) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected long getPosition() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected java.lang.String getStatusCode4Cdx(ArchiveRecordHeader h) {	
      throw new UnsupportedOperationException("Not Implemented");
    }

    protected void incrementPosition() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    protected void incrementPosition(long incr) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected boolean isEor() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public boolean isStrict() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public boolean markSupported() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    protected java.lang.String outputCdx(String strippedFileName) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public int read() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public int read(byte[] b, int offset, int length) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected void setEor(boolean eor) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected void setHeader(ArchiveRecordHeader header) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public void setStrict(boolean strict) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    private void skip() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public long skip(long n) {
      throw new UnsupportedOperationException("Not Implemented");
    }

  }

  private class MyMockARCRecordMetaData implements ArchiveRecordHeader {
    private String arc = null;
    private Map headerFields = null;
    private long offset = 0;
    private String url = null;

    MyMockARCRecordMetaData(String arcName, Map metaData, String u) {
      arc = arcName;
      headerFields = metaData;
      url = u;
    }

    public String getArc() {
      return arc;
    }

    public File getArcFile() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public int getContentBegin() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public String getDate() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public String getDigest() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public Set getHeaderFieldKeys() {
      return headerFields.keySet();
    }
           
    public Map getHeaderFields() {
      return headerFields;
    }
           
    public Object getHeaderValue(java.lang.String key) {
      return headerFields.get(key);
    }
           
    public String getIp() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public long getLength() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public String getMimetype() {
      return (String) headerFields.get("Content-type");
    }
           
    public long getOffset() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public String getReaderIdentifier() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public String getRecordIdentifier() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public String getStatusCode() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public String getUrl() {
      return url;
    }
           
    public String getVersion() {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public void setDigest(String d) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    public void setStatusCode(String statusCode) {
      throw new UnsupportedOperationException("Not Implemented");
    }
           
    protected  void testRequiredField(java.util.Map fields, String requiredField) {
      throw new UnsupportedOperationException("Not Implemented");
    }
    public String toString() {
      throw new UnsupportedOperationException("Not Implemented");
    }
  }
}
