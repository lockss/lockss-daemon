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

package org.lockss.plugin.massachusettsmedicalsociety;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.extractor.RisMetadataExtractor;
import org.lockss.util.Logger;

/*
 * Uses the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: www.nejm.org/action/downloadCitation?format=ris&doi=10.1056%2FNEJM197901183000301&include=cit&direct=checked
 * The RIS file should look something like this:
 * 
 * TY  - JOUR
 * T1  - Viral Hepatitis, Type B
 * AU  - Krugman, Saul
 * AU  - Overby, Lacy R.
 * AU  - Mushahwar, Isa K.
 * AU  - Ling, Chung-Mei
 * AU  - Frösner, Gert G.
 * AU  - Deinhardt, Friedrich
 * Y1  - 1979/01/18
 * PY  - 1979
 * DA  - 1979/01/18
 * N1  - doi: 10.1056/NEJM197901183000301
 * DO  - 10.1056/NEJM197901183000301
 * T2  - New England Journal of Medicine
 * JF  - New England Journal of Medicine
 * JO  - N Engl J Med
 * SP  - 101
 * EP  - 106
 * VL  - 300
 * IS  - 3
 * PB  - Massachusetts Medical Society
 * SN  - 0028-4793
 * M3  - doi: 10.1056/NEJM197901183000301
 * UR  - http://dx.doi.org/10.1056/NEJM197901183000301
 * Y2  - 2012/02/29
 * ER  - 
 */
public class MassachussetsMedicalSocietyRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BaseArchivalUnit");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new RisMetadataExtractor();
  }

}
