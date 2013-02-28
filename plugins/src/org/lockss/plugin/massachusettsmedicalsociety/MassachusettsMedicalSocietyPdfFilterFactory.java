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

import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * <p>
 * To be replaced by {@link MassachesettsMedicalSocietyNewPdfFilterFactory}
 * which still needs to implement a way to remove the per-page watermarking (see
 * {@link NormalizeXObjects}).
 * </p>
 * 
 * @deprecated Work on {@link MassachesettsMedicalSocietyNewPdfFilterFactory}
 *             instead.
 */
@Deprecated
public class MassachusettsMedicalSocietyPdfFilterFactory implements FilterFactory {
	private static Logger log = Logger.getLogger("MassachusettsMedicalSocietyPdfFilterFactory");
	
	public InputStream createFilteredInputStream(ArchivalUnit au,
									             InputStream in,
									             String encoding)
									            		 throws PluginException {
	    try {
	      log.debug2("PDF filter factory for: " + au.getName());
	      OutputDocumentTransform documentTransform = new MassachusettsMedicalSocietyPdfTransform(au);
	      return PdfUtil.applyFromInputStream(documentTransform, in);
	    }
	    catch (Exception exc) {
	      log.error("Exception in PDF transform; unfiltered", exc);
	      return in;
	    }
	}
  
}
