package org.lockss.plugin.elsevier;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestElsevierExploderHelper extends LockssTestCase {
  private static final String[] basePath = {
    "01234567/",
    "1234567X/",
    "234567S1/",
  };
  private static final String[] pathStem = {
    "23456789/",
    "3456789X/",
  };
  private static final String pdfPath = 
    "main.pdf";
  private static final String xmlPath = 
    "main.xml";
  private static final String shortPath = 
    basePath[0] + basePath[0];
  private static final String jpgPath = 
    "fx1.jpg";
  private static final String urlStem = "http://elsevier.clockss.org/";
  private static final String[] ignorePath = {
    "V0008I05/CHECKMD5.FIL",
    "CHECKMD5.FIL",
    "checkmd5.fil",
    "V0008I05/checkmd5.fil"
  };

  public void testProcessCorrectPdfEntry() throws Exception {
    for (int i = 0; i < basePath.length; i++) {
      for (int j = 0; j < pathStem.length; j++) {
	ArchiveEntry ae = new ArchiveEntry(basePath[i] + pathStem[j] + pdfPath,
					   7654, 0, null, null);
	ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	eeh.process(ae);
	assertEquals(urlStem + basePath[i], ae.getBaseUrl());
	assertEquals(pathStem[j] + pdfPath, ae.getRestOfUrl());
	assertEquals("application/pdf", ae.getHeaderFields().get("Content-Type"));
	assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
	// XXX - check addText
	// XXX - check auProps
      }
    }
  }

  public void testProcessCorrectXmlEntry() throws Exception {
    for (int i = 0; i < basePath.length; i++) {
      for (int j = 0; j < pathStem.length; j++) {
	ArchiveEntry ae = new ArchiveEntry(basePath[i] + pathStem[j] + xmlPath,
					   76543, 0, null, null);
	ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	eeh.process(ae);
	assertEquals(ae.getBaseUrl(), urlStem + basePath[i]);
	assertEquals(ae.getRestOfUrl(), pathStem[j] + xmlPath);
	assertEquals(ae.getHeaderFields().get("Content-Type"), "application/xml");
	assertEquals(ae.getHeaderFields().get("Content-Length"), "76543");
      }
    }
  }

  public void testProcessShortName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(shortPath, 7, 0, null, null);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertNull(ae.getBaseUrl());
    assertNull(ae.getRestOfUrl());
    assertNull(ae.getHeaderFields());
  }

  public void testProcessIgnoredName() throws Exception {
    for (int i = 0; i < ignorePath.length; i++) {
      ArchiveEntry ae = new ArchiveEntry(ignorePath[i], 7, 0, null, null);
      ElsevierExploderHelper eeh = new ElsevierExploderHelper();
      
      eeh.process(ae);
      assertEquals(urlStem, ae.getBaseUrl());
      assertNull(ae.getRestOfUrl());
      assertNull(ae.getHeaderFields());
    }
  }

  public void testProcessJpgName() throws Exception {
    for (int i = 0; i < basePath.length; i++) {
      for (int j = 0; j < pathStem.length; j++) {
	ArchiveEntry ae = new ArchiveEntry(basePath[i] + pathStem[j] + jpgPath,
					   7, 0, null, null);
	ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	eeh.process(ae);
	assertEquals(ae.getBaseUrl(), urlStem + basePath[i]);
	assertEquals(ae.getRestOfUrl(), pathStem[j] + jpgPath);
	assertEquals(ae.getHeaderFields().get("Content-Type"), "image/jpeg");
	assertEquals(ae.getHeaderFields().get("Content-Length"), "7");
      }
    }
  }

  public void testProcessPdfEntryWithDotSlash() throws Exception {
    for (int i = 0; i < basePath.length; i++) {
      for (int j = 0; j < pathStem.length; j++) {
	ArchiveEntry ae = new ArchiveEntry("./" + basePath[i] + pathStem[j] +
					   pdfPath, 7654, 0, null, null);
	ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	eeh.process(ae);
	assertEquals(urlStem + basePath[i], ae.getBaseUrl());
	assertEquals(pathStem[j] + pdfPath, ae.getRestOfUrl());
	assertEquals("application/pdf", ae.getHeaderFields().get("Content-Type"));
	assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
	// XXX - check addText
	// XXX - check auProps
      }
    }
  }

}
