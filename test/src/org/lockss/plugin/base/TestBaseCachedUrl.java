/*
 * $Id: TestBaseCachedUrl.java,v 1.1 2003-09-13 00:46:42 troberts Exp $
 */

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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBaseCachedUrl extends LockssTestCase {
  private static final String PARAM_SHOULD_FILTER_HASH_STREAM =
    Configuration.PREFIX+".baseCachedUrl.filterHashStream";

  public void testFilterParamDefault() {
     MyCachedUrl cu = new MyCachedUrl();
     cu.openForHashing();
     assertFalse(cu.gotUnfilteredStream());
   }

   public void testFilterParamFilterOn() throws IOException {
     String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=true";
     ConfigurationUtil.setCurrentConfigFromString(config);
     MyCachedUrl cu = new MyCachedUrl();
     cu.openForHashing();
     assertFalse(cu.gotUnfilteredStream());
   }

   public void testFilterParamFilterOff() throws IOException {
     String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=false";
     ConfigurationUtil.setCurrentConfigFromString(config);
     MyCachedUrl cu = new MyCachedUrl();
     cu.openForHashing();
     assertTrue(cu.gotUnfilteredStream());
   }

  private class MyAu extends NullPlugin.ArchivalUnit {
    public FilterRule getFilterRule(String mimeType) {
      return new FilterRule () {
	public InputStream createFilteredInputStream(Reader reader) {
	  return new ReaderInputStream(reader);
	}
      };
    }
  } 

  private class MyCachedUrl extends BaseCachedUrl {
    private boolean gotUnfilteredStream = false;
    private Properties props = new Properties();


    public MyCachedUrl() {
      super(null, null);
      props.setProperty("content-type", "text/html");
    }

    public ArchivalUnit getArchivalUnit() {
      return new MyAu();
    }

    public InputStream openForReading() {
      gotUnfilteredStream = true;
      return null;
    }

    public boolean gotUnfilteredStream() {
      return gotUnfilteredStream;
    }

    public boolean hasContent() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public Reader getReader() {
      return new StringReader("Test");
    }

    public Properties getProperties() {
      return props;
    }

    public void setProperties(Properties props) {
      this.props = props;
    }

    public byte[] getUnfilteredContentSize() {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestBaseCachedUrl.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
