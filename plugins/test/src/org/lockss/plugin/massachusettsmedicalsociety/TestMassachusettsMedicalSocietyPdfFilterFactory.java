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

import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StreamUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.util.PdfDocument;

public class TestMassachusettsMedicalSocietyPdfFilterFactory extends LockssTestCase {
	private static Logger log = Logger.getLogger("TestMassachusettsMedicalSocietyPdfFilterFactory");
	private static String TEST_FOLDER = "plugins/test/src/org/lockss/plugin/massachusettsmedicalsociety";
	private MassachusettsMedicalSocietyPdfFilterFactory fact;
	private MockArchivalUnit mau;
	public void setUp() throws Exception {
		super.setUp();
		fact = new MassachusettsMedicalSocietyPdfFilterFactory();
		mau = new MockArchivalUnit();
	}
	
	public void testFilterPdf() throws Exception {
		File pdfFile = new File(TEST_FOLDER + "/nejmfiltertestin.pdf");
		InputStream pdfIn = new FileInputStream(pdfFile);
		InputStream filteredIn = fact.createFilteredInputStream(mau, pdfIn, Constants.DEFAULT_ENCODING);
		
		//In order to get our expected and actual outputs to match up we have to save the
		//actual stream to a temp file and read it in as a file input stream.
		PdfDocument actPdf = new PdfDocument(filteredIn);
		File actTemp = File.createTempFile("actout", null);
		FileOutputStream actOut = new FileOutputStream(actTemp);
		actPdf.save(actOut);
		FileInputStream actIn = new FileInputStream(actTemp);
		
		File expFile = new File(TEST_FOLDER + "/nejmfiltertestout.pdf");
		FileInputStream expIn = new FileInputStream(expFile);
		
		assertTrue(StreamUtil.compare(expIn, actIn));
		IOUtil.safeClose(expIn);
		IOUtil.safeClose(actIn);
		IOUtil.safeClose(pdfIn);
		IOUtil.safeClose(filteredIn);
		actTemp.deleteOnExit();
	}
	
}