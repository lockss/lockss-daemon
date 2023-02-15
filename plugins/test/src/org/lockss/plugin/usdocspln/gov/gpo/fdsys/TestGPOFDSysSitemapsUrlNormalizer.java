/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
