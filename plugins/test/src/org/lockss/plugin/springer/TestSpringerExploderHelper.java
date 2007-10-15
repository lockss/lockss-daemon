package org.lockss.plugin.springer;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestSpringerExploderHelper extends LockssTestCase {
  private static final String basePath =
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
    "PUB=foo/JOU=12345/VOL=23456/ISU=5";
  private static final String badPaths[] = {
    "FOO=foo/JOU=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "Pub=foo/JOU=12345/VOL=23456/ISU=5/ART=2004_34567/",
    "PUB=foo/FOO=12345/VOL=23456/ISU=5/ART=2004_34567/",
  };
  private static final String jpgPath = 
    "12345_2004_Article_34567.jpg";
  private static final String urlStem = "http://www.springer.com/CLOCKSS/";

  public void testProcessCorrectPdfEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath+ pathStem + pdfPath,
				      7654, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + pdfPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "application/pdf");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7654");
    // XXX test addText
    // XXX test auProps
  }

  public void testProcessCorrectXmlEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath+ pathStem + xmlPath,
				      76543, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + xmlPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "text/xml");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "76543");
  }

  public void testProcessCorrectMetaEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath+ pathStem + metaPath,
				      765432, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertEquals(urlStem + basePath, ae.getBaseUrl());
    assertEquals(pathStem + metaPath, ae.getRestOfUrl());
    assertEquals("text/xml", ae.getHeaderFields().get("Content-Type"));
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
      assertNull(ae.getBaseUrl());
      assertNull(ae.getRestOfUrl());
      assertNull(ae.getHeaderFields());
    }
  }

  public void testProcessJpgName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath + pathStem + jpgPath,
				       7, 0, null, null);
    SpringerExploderHelper seh = new SpringerExploderHelper();

    seh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + jpgPath);
    assertNull(ae.getHeaderFields().get("Content-Type"));
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7");
  }

 }
