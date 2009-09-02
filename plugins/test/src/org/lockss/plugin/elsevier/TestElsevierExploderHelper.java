package org.lockss.plugin.elsevier;

import java.io.*;
import org.lockss.daemon.CrawlSpec;
import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;
import org.lockss.crawler.Exploder;

public class TestElsevierExploderHelper extends LockssTestCase {
  // Must be valid ISSNs
  private static final String[] journalPath = {
    "19368798",
    "1356689X",
  };
  private static final String[] issuePath = {
    "01234567/",
    "1234567X/",
    "234567S1/",
  };
  private static final String[] articlePath = {
    "23456789/",
    "3456789X/",
  };
  private static final String pdfPath = 
    "main.pdf";
  private static final String xmlPath = 
    "main.xml";
  private static final String shortPath = 
    issuePath[0] + issuePath[0];
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
    for (int i = 0; i < journalPath.length; i++) {
      for (int j = 0; j < issuePath.length; j++) {
        for (int k = 0; k < articlePath.length; k++) {
          String archiveName = "http://www.exmp.com/" + journalPath[i] + ".tar";
	  ArchiveEntry ae = new ArchiveEntry(issuePath[j] + articlePath[k] +
					     pdfPath, (long)7654, (long)0,
					     (InputStream) null,
					     (CrawlSpec) null,
					     (Exploder) null,
					     archiveName);
	  ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	  eeh.process(ae);
	  assertEquals(urlStem + journalPath[i] + "/", ae.getBaseUrl());
	  assertEquals(issuePath[j] + articlePath[k] + pdfPath,
		       ae.getRestOfUrl());
	  assertEquals("application/pdf",
		       ae.getHeaderFields().get("Content-Type"));
	  assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
	  // XXX - check addText
	  // XXX - check auProps
	}
      }
    }
  }

  public void testExplodedAuBaseUrlStem() throws Exception {
    String urlStem = "http://somebody.clockss.org/";
    for (int i = 0; i < journalPath.length; i++) {
      for (int j = 0; j < issuePath.length; j++) {
        for (int k = 0; k < articlePath.length; k++) {
          String archiveName = "http://www.exmp.com/" + journalPath[i] + ".tar";
	  ArchiveEntry ae = new ArchiveEntry(issuePath[j] + articlePath[k] +
					     pdfPath, (long)7654, (long)0,
					     (InputStream) null,
					     (CrawlSpec) null,
					     (Exploder) null,
					     archiveName, urlStem);
	  ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	  eeh.process(ae);
	  assertEquals(urlStem + journalPath[i] + "/", ae.getBaseUrl());
	  assertEquals(issuePath[j] + articlePath[k] + pdfPath,
		       ae.getRestOfUrl());
	  assertEquals("application/pdf",
		       ae.getHeaderFields().get("Content-Type"));
	  assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
	  // XXX - check addText
	  // XXX - check auProps
	}
      }
    }
  }

  public void testProcessCorrectXmlEntry() throws Exception {
    for (int i = 0; i < journalPath.length; i++) {
      for (int j = 0; j < issuePath.length; j++) {
        for (int k = 0; k < articlePath.length; k++) {
          String archiveName = "http://www.exmp.com/" + journalPath[i] + ".tar";
	  ArchiveEntry ae = new ArchiveEntry(issuePath[j] + articlePath[k] +
					     xmlPath, (long)76543, (long)0,
					     (InputStream) null,
					     (CrawlSpec) null,
					     (Exploder) null,
					     archiveName);
	  ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	  eeh.process(ae);
	  assertEquals(urlStem + journalPath[i] + "/", ae.getBaseUrl());
	  assertEquals(issuePath[j] + articlePath[k] + xmlPath,
		       ae.getRestOfUrl());
	  assertEquals("application/xml",
		       ae.getHeaderFields().get("Content-Type"));
	  assertEquals("76543", ae.getHeaderFields().get("Content-Length"));
	}
      }
    }
  }

  public void testProcessJpgName() throws Exception {
    for (int i = 0; i < journalPath.length; i++) {
      for (int j = 0; j < issuePath.length; j++) {
        for (int k = 0; k < articlePath.length; k++) {
          String archiveName = "http://www.exmp.com/" + journalPath[i] + ".tar";
	  ArchiveEntry ae = new ArchiveEntry(issuePath[j] + articlePath[k] +
					     jpgPath, (long)7, (long)0,
					     (InputStream) null,
					     (CrawlSpec) null,
					     (Exploder) null,
					     archiveName);
	  ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	  eeh.process(ae);
	  assertEquals(urlStem + journalPath[i] + "/", ae.getBaseUrl());
	  assertEquals(issuePath[j] + articlePath[k] + jpgPath,
		       ae.getRestOfUrl());
	  assertEquals("image/jpeg", ae.getHeaderFields().get("Content-Type"));
	  assertEquals("7", ae.getHeaderFields().get("Content-Length"));
	}
      }
    }
  }

  public void testProcessPdfEntryWithDotSlash() throws Exception {
    for (int i = 0; i < journalPath.length; i++) {
      for (int j = 0; j < issuePath.length; j++) {
        for (int k = 0; k < articlePath.length; k++) {
          String archiveName = "http://www.exmp.com/" + journalPath[i] + ".tar";
	  ArchiveEntry ae = new ArchiveEntry("./" +
					     issuePath[j] + articlePath[k] +
					     pdfPath, (long)123456, (long)0,
					     (InputStream) null,
					     (CrawlSpec) null,
					     (Exploder) null,
					     archiveName);
	  ElsevierExploderHelper eeh = new ElsevierExploderHelper();

	  eeh.process(ae);
	  assertEquals(urlStem + journalPath[i] + "/", ae.getBaseUrl());
	  assertEquals(issuePath[j] + articlePath[k] + pdfPath,
		       ae.getRestOfUrl());
	  assertEquals("application/pdf",
		       ae.getHeaderFields().get("Content-Type"));
	  assertEquals("123456",
		       ae.getHeaderFields().get("Content-Length"));
	  // XXX - check addText
	  // XXX - check auProps
	}
      }
    }
  }

  public void testProcessShortName() throws Exception {
    String archiveName = "http://www.exmp.com/" + journalPath[0] + ".tar";
    ArchiveEntry ae = new ArchiveEntry(shortPath, 7, 0, null, null, null,
				       archiveName);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertNull(ae.getBaseUrl());
    assertNull(ae.getRestOfUrl());
    assertNull(ae.getHeaderFields());
  }

  public void testProcessIgnoredName() throws Exception {
    for (int i = 0; i < ignorePath.length; i++) {
      String archiveName = "http://www.exmp.com/" + journalPath[0] + ".tar";
      ArchiveEntry ae = new ArchiveEntry(ignorePath[i], 7, 0, null, null, null,
					 archiveName);
      ElsevierExploderHelper eeh = new ElsevierExploderHelper();
      
      eeh.process(ae);
      assertEquals(ae.getBaseUrl(), urlStem + journalPath[0] + "/");
      assertNull(ae.getRestOfUrl());
      assertNull(ae.getHeaderFields());
    }
  }

}
