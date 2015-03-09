/* Id: $ */
/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.plugin.atypon.BaseAtyponRisFilterFactory;

public class TestMassachusettsMedicalSocietyRisFilterFactory extends LockssTestCase {
        private static Logger log = Logger.getLogger("TestMassachusettsMedicalSocietyRisFilterFactory");
        //private MassachusettsMedicalSocietyRisFilterFactory fact;
        private BaseAtyponRisFilterFactory fact;
        private MockArchivalUnit mau;
        public static String risData;
          static {
                  StringBuilder sb = new StringBuilder();
                  sb.append("TY  - JOUR");
                  sb.append("\nJO  - N Engl J Med");
                  sb.append("\nM3  - doi: 10.1056/NEJM197901183000301");
                  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJM197901183000301");
                  sb.append("\nY2  - 2012/02/29");
                  sb.append("\nER  -\n");
                  risData = sb.toString();
          }
        public static String risDataFiltered;
          static {
                  StringBuilder sb = new StringBuilder();
                  sb.append("TY  - JOUR");
                  sb.append("\nJO  - N Engl J Med");
                  sb.append("\nM3  - doi: 10.1056/NEJM197901183000301");
                  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJM197901183000301");
                  sb.append("\n");
                  risDataFiltered = sb.toString();
          }
          public static String moreRisData;
          static {
                  StringBuilder sb = new StringBuilder();
                  sb.append("TY  - JOUR");
                  sb.append("\nT1  - Cortical Arousal in Children with Severe Enuresis");
                  sb.append("\nAU  - Yeung, Chung K.");
                  sb.append("\nAU  - Diao, Mei");
                  sb.append("\nAU  - Sreedhar, Biji");
                  sb.append("\nY1  - 2008/05/29");
                  sb.append("\nPY  - 2008");
                  sb.append("\nDA  - 2008/05/29");
                  sb.append("\nN1  - doi: 10.1056/NEJMc0706528");
                  sb.append("\nDO  - 10.1056/NEJMc0706528");
                  sb.append("\nT2  - New England Journal of Medicine");
                  sb.append("\nJF  - New England Journal of Medicine");
                  sb.append("\nJO  - N Engl J Med");
                  sb.append("\nSP  - 2414");
                  sb.append("\nEP  - 2415");
                  sb.append("\nVL  - 358");
                  sb.append("\nIS  - 22");
                  sb.append("\nPB  - Massachusetts Medical Society");
                  sb.append("\nSN  - 0028-4793");
                  sb.append("\nM3  - doi: 10.1056/NEJMc0706528");
                  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJMc0706528");
                  sb.append("\nY2  - 2015/03/05");
                  sb.append("\nER  - ");
                  moreRisData = sb.toString();
          }
        public static String moreRisDataFiltered;
          static {
                  StringBuilder sb = new StringBuilder();
                  sb.append("TY  - JOUR");
                  sb.append("\nT1  - Cortical Arousal in Children with Severe Enuresis");
                  sb.append("\nAU  - Yeung, Chung K.");
                  sb.append("\nAU  - Diao, Mei");
                  sb.append("\nAU  - Sreedhar, Biji");
                  sb.append("\nY1  - 2008/05/29");
                  sb.append("\nPY  - 2008");
                  sb.append("\nDA  - 2008/05/29");
                  sb.append("\nN1  - doi: 10.1056/NEJMc0706528");
                  sb.append("\nDO  - 10.1056/NEJMc0706528");
                  sb.append("\nT2  - New England Journal of Medicine");
                  sb.append("\nJF  - New England Journal of Medicine");
                  sb.append("\nJO  - N Engl J Med");
                  sb.append("\nSP  - 2414");
                  sb.append("\nEP  - 2415");
                  sb.append("\nVL  - 358");
                  sb.append("\nIS  - 22");
                  sb.append("\nPB  - Massachusetts Medical Society");
                  sb.append("\nSN  - 0028-4793");
                  sb.append("\nM3  - doi: 10.1056/NEJMc0706528");
                  sb.append("\nUR  - http://dx.doi.org/10.1056/NEJMc0706528");
                  sb.append("\nER  - ");
                  moreRisDataFiltered = sb.toString();
          }
        public void setUp() throws Exception {
                super.setUp();
                fact = new BaseAtyponRisFilterFactory();
                mau = new MockArchivalUnit();
        }
        
        // filtering creation date (Y2)
        public void testFilterCreationDate() throws Exception {
                InputStream actIn = fact.createFilteredInputStream(mau,
                    new StringInputStream(risData), Constants.DEFAULT_ENCODING);
                String test = StringUtil.fromInputStream(actIn);
                //log.info("filtered input: "+test);
                //log.info("should match: "+risDataFiltered);
                assertEquals(risDataFiltered, test);
        }
        // 
        public void testRisFilter() throws Exception {
                InputStream actIn = fact.createFilteredInputStream(mau,
                    new StringInputStream(moreRisData), Constants.DEFAULT_ENCODING);
                String test = StringUtil.fromInputStream(actIn);
                //log.info("filtered input: "+test);
                //log.info("should match: "+moreRisDataFiltered);
                assertEquals(moreRisDataFiltered, test);

        }

}