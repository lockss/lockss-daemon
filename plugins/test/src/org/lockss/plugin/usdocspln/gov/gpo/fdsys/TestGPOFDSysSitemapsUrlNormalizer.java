/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestGPOFDSysSitemapsUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer normalizer;
  
  protected ArchivalUnit au;
  
  public void setUp() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                    "http://www.example.gov/"));
    au = mau;
    normalizer = new GPOFDSysSitemapsUrlNormalizer();
  }
  
  public void testUnchanged() throws Exception {
    unchanged("http://www.example.com/some/other/site.html");
    unchanged("http://www.example.gov/images/foo.gif");
    unchanged("http://www.example.gov/smap/fdsys/sitemap_2011/2011_XYZ_sitemap.xml");
    unchanged("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html");
    unchanged("http://www.example.gov/fdsys/granule/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov/fdsys/granule/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/foo/XYZ-2011-04-21.foo");
    unchanged("http://www.example.gov/fdsys/search/getfrtoc.action?selectedDate=2011-04-21");
    unchanged("http://www.example.com:80/some/other/site.html");
    unchanged("http://www.example.gov:80/images/foo.gif");
    unchanged("http://www.example.gov:80/smap/fdsys/sitemap_2011/2011_XYZ_sitemap.xml");
    unchanged("http://www.example.gov:80/fdsys/pkg/XYZ-2011-04-21/content-detail.html");
    unchanged("http://www.example.gov:80/fdsys/granule/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov:80/fdsys/pkg/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov:80/fdsys/granule/XYZ-2011-04-21/foo.xml");
    unchanged("http://www.example.gov:80/fdsys/pkg/XYZ-2011-04-21/foo/XYZ-2011-04-21.foo");
    unchanged("http://www.example.gov:80/fdsys/search/getfrtoc.action?selectedDate=2011-04-21");
  }
  
  protected void unchanged(String url) throws Exception {
    assertEquals(url, normalizer.normalizeUrl(url, au));
  }
  
  public void testChanged() throws Exception {
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov/fdsys/search/pagedetails.action?browsePath=Sample+Document&granuleId=XYZ-2011-04-21&packageId=XYZ-2011-04-21&collapse=true&fromBrowse=true");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov/fdsys/search/pagedetails.action?browsePath=Sample+Document&packageId=XYZ-2011-04-21");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov/fdsys/search/pagedetails.action?packageId=XYZ-2011-04-21&collapse=true&fromBrowse=true");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov/fdsys/search/pagedetails.action?packageId=XYZ-2011-04-21");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov:80/fdsys/search/pagedetails.action?browsePath=Sample+Document&granuleId=XYZ-2011-04-21&packageId=XYZ-2011-04-21&collapse=true&fromBrowse=true");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov:80/fdsys/search/pagedetails.action?browsePath=Sample+Document&packageId=XYZ-2011-04-21");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov:80/fdsys/search/pagedetails.action?packageId=XYZ-2011-04-21&collapse=true&fromBrowse=true");
    changed("http://www.example.gov/fdsys/pkg/XYZ-2011-04-21/content-detail.html",
            "http://www.example.gov:80/fdsys/search/pagedetails.action?packageId=XYZ-2011-04-21");
  }
  
  protected void changed(String expected, String actual) throws Exception {
    assertEquals(expected, normalizer.normalizeUrl(actual, au));
  }
  
}
