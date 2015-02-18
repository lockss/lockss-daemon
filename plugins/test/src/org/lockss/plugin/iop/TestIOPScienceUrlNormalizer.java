/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iop;

import org.lockss.plugin.iop.IOPScienceUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestIOPScienceUrlNormalizer extends LockssTestCase {
  
  private IOPScienceUrlNormalizer norm;

  public void setUp() throws Exception {
    super.setUp();
    norm = new IOPScienceUrlNormalizer();
  }

  public void testIOPScienceNormalizer() throws Exception {
    
    assertEquals("http://iopscience.iop.org/1758-5090/1/4/045005",
                 norm.normalizeUrl("http://iopscience.iop.org/1758-5090/1/4/045005?rel=xyz&relno=1", null));
    assertEquals("http://iopscience.iop.org/1758-5090/1/4/045005",
                 norm.normalizeUrl("http://iopscience.iop.org/1758-5090/1/4/045005?rel=xyz", null));
    assertEquals("http://iopscience.iop.org/1758-5090/1/4/045005",
                 norm.normalizeUrl("http://iopscience.iop.org/1758-5090/1/4/045005?rel=", null));
    assertEquals("http://iopscience.iop.org/1758-5090/1/4/045005?classid=123",
                 norm.normalizeUrl("http://iopscience.iop.org/1758-5090/1/4/045005?classid=123", null));
    
    assertEquals("http://iopscience.iop.org/1742-6596/325/1",
                 norm.normalizeUrl("http://iopscience.iop.org/1742-6596/325/1;jsessionid=00608266206E53E70416BA4D3C38E0AB.c2", null));
  }
  
}
