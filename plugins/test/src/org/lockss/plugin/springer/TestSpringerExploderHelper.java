package org.lockss.plugin.springer;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestSpringerExploderHelper extends LockssTestCase {
  private static final String basePath =
    "JOU=12345/";
  private static final String basePathPub =
    "PUB=foo/JOU=12345/";
  private static final String pathStem =
    "VOL=23456/ISU=5/ART=2004_34567/";
  private static final String pdfPath = 
    "BodyRef/PDF/12345_2004_Article_34567.pdf";
  private static final String xmlPath = 
    "12345_2004_Article_34567.xml";
  private static final String metaPath =
    "12345_2004_Article_34567.xml.meta";
  private static final String shortPath = 
    "JOU=12345/VOL=23456/ISU=5";
  private static final String badPaths[] = {
    "FOO=foo/JOU=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "Pub=foo/JOU=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "PUB=foo/FOO=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "FOO=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "JOU=12345/BOL=23456/ISU=5/ART=2004_34567/",
    "JOU=12345/VOL=23456/JSU=5/ART=2004_34567/",
    "JOU=12345/VOL=23456/ISU=5/QRT=2004_34567/",
  };
  private static final String jpgPath = 
    "12345_2004_Article_34567.jpg";
  private static final String urlStem = "http://springer.clockss.org/";

  private ArchiveEntry processCorrectEntry(String bPath,
					   String path, int len) {
    ArchiveEntry ae = new ArchiveEntry(bPath+ pathStem + path,
				      len, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertEquals(urlStem + basePath, ae.getBaseUrl());
    assertEquals(pathStem + path, ae.getRestOfUrl());
    return ae;
  }

  public void testProcessCorrectPdfEntry() throws Exception {
    ArchiveEntry ae = processCorrectEntry(basePath, pdfPath, 7654);
    assertEquals("application/pdf", ae.getHeaderFields().get("Content-Type"));
    assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
    // XXX test addText
    // XXX test auProps
    ae = processCorrectEntry(basePathPub, pdfPath, 7654);
    assertEquals("application/pdf", ae.getHeaderFields().get("Content-Type"));
    assertEquals("7654", ae.getHeaderFields().get("Content-Length"));
    // XXX test addText
    // XXX test auProps
  }

  public void testProcessCorrectXmlEntry() throws Exception {
    ArchiveEntry ae = processCorrectEntry(basePath, xmlPath, 76543);
    assertEquals("application/xml", ae.getHeaderFields().get("Content-Type"));
    assertEquals("76543", ae.getHeaderFields().get("Content-Length"));
    ae = processCorrectEntry(basePathPub, xmlPath, 76543);
    assertEquals("application/xml", ae.getHeaderFields().get("Content-Type"));
    assertEquals("76543", ae.getHeaderFields().get("Content-Length"));
  }

  public void testProcessCorrectMetaEntry() throws Exception {
    ArchiveEntry ae = processCorrectEntry(basePath, metaPath, 765432);
    assertEquals("application/xml", ae.getHeaderFields().get("Content-Type"));
    assertEquals("765432", ae.getHeaderFields().get("Content-Length"));
    ae = processCorrectEntry(basePathPub, metaPath, 765432);
    assertEquals("application/xml", ae.getHeaderFields().get("Content-Type"));
    assertEquals("765432", ae.getHeaderFields().get("Content-Length"));
  }

  public void testProcessShortName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(shortPath, 7, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertNull(ae.getBaseUrl());
    assertNull(ae.getRestOfUrl());
    assertNull(ae.getHeaderFields());
  }

  public void testProcessBadPaths() throws Exception {
    SpringerExploderHelper seh = new SpringerExploderHelper();

    for (int i = 0; i < badPaths.length; i++) {
      ArchiveEntry ae = new ArchiveEntry(badPaths[i], 2345, 0, null, null);
      seh.process(ae);
      log.debug("Bad path " + badPaths[i]);
      assertNull(ae.getBaseUrl());
      assertNull(ae.getRestOfUrl());
      assertNull(ae.getHeaderFields());
    }
  }

  public void testProcessJpgName() throws Exception {
    ArchiveEntry ae = processCorrectEntry(basePath, jpgPath, 7);
    assertEquals("image/jpeg", ae.getHeaderFields().get("Content-Type"));
    assertEquals("7", ae.getHeaderFields().get("Content-Length"));
    ae = processCorrectEntry(basePathPub, jpgPath, 7);
    assertEquals("image/jpeg", ae.getHeaderFields().get("Content-Type"));
    assertEquals("7", ae.getHeaderFields().get("Content-Length"));
  }

 }
