/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.massachusettsmedicalsociety;

import java.io.*;
import java.util.ArrayList;

import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StreamUtil;
import org.lockss.util.StringUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.massachusettsmedicalsociety.RisFilterInputStream;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.PdfDocument;

public class TestRisFilterInputStream extends LockssTestCase {
	private static Logger log = Logger.getLogger("TestRisFilterInputStream");
	private RisFilterInputStream filtered;
	private MockArchivalUnit mau;
	public static String risData;
	static {
	  StringBuilder sb = new StringBuilder();
	  sb.append("TY  - JOUR");
	  sb.append("\nJO  - N Engl J Med");
	  sb.append("\nM3  - doi: 10.1056/NEJM197901183000301");
	  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJM197901183000301");
	  sb.append("\nY2  - 2012/02/29");
	  sb.append("\nER  -");
	  risData = sb.toString();
	}
	public static String risDataFilteredSingleValue;
	static {
	  StringBuilder sb = new StringBuilder();
	  sb.append("TY  - JOUR");
	  sb.append("\nJO  - N Engl J Med");
	  sb.append("\nM3  - doi: 10.1056/NEJM197901183000301");
	  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJM197901183000301");
	  sb.append("\nER  -");
	  risDataFilteredSingleValue = sb.toString();
	}
	public static String risDataFilteredMultiValue;
	  static {
	  StringBuilder sb = new StringBuilder();
	  sb.append("TY  - JOUR");
	  sb.append("\nJO  - N Engl J Med");
	  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJM197901183000301");
	  sb.append("\nY2  - 2012/02/29");
	  risDataFilteredMultiValue = sb.toString();
	  }
	  
	public void setUp() throws Exception {
		super.setUp();
		mau = new MockArchivalUnit();
	}
	
	public void testFilterSingleValue() throws Exception {
		filtered = new RisFilterInputStream((new ReaderInputStream(new StringReader(risData),Constants.DEFAULT_ENCODING)),
											Constants.DEFAULT_ENCODING,
											"Y2");
		
		assertEquals(risDataFilteredSingleValue, StringUtil.fromInputStream(filtered));
	}
	
	public void testFilterMultiValue() throws Exception {
		ArrayList<String> filterList = new ArrayList<String>();
		filterList.add("M3");
		filterList.add("ER");
		filtered = new RisFilterInputStream((new ReaderInputStream(new StringReader(risData),Constants.DEFAULT_ENCODING)),
											Constants.DEFAULT_ENCODING,
											filterList);

		assertEquals(risDataFilteredMultiValue, StringUtil.fromInputStream(filtered));
	}
	
	
	
}