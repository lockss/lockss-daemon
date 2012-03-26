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
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.PageTransformUtil.ExtractStringsToOutputStream;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.ArchivalUnitDependent;
import org.lockss.plugin.highwire.NewEnglandJournalOfMedicinePdfTransform;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.NormalizeMetadata;
import org.lockss.plugin.highwire.NewEnglandJournalOfMedicinePdfTransform.EraseVariableFooter;
import org.lockss.plugin.highwire.NewEnglandJournalOfMedicinePdfTransform.EraseVariableFooter.ProcessEndTextObject;
import org.lockss.plugin.taylorandfrancis.TaylorAndFrancisPdfFilterFactory.FilteringException;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.PDRectangle;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.pdmodel.interactive.action.type.*;
import org.pdfbox.pdmodel.interactive.annotation.*;
import org.pdfbox.util.PDFStreamEngine.OperatorProcessorFactory;
import org.pdfbox.util.operator.OperatorProcessor;

import uk.org.lockss.plugin.annualreviews.AnnualReviewsPdfTransform;

/*
 * NEJM RIS files have a line that contains the creation date of the file in the form:
 * Y2 - 2012/03/26
 * Reads through RIS files line by line and removes the line with the start tag Y2
 */
public class MassachusettsMedicalSocietyRisFilterFactory implements FilterFactory {
	private static Logger log = Logger.getLogger("MassachusettsMedicalSocietyRisFilterFactory");
	
	public InputStream createFilteredInputStream(ArchivalUnit au,
									             InputStream in,
									             String encoding)
									            		 throws PluginException {
		return new RisFilterInputStream(in, encoding, "Y2");
	}
}
