package org.lockss.plugin.elsevier;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestElsevierExploderHelper extends LockssTestCase {
  private static final String basePath =
    "01234567/";
  private static final String pathStem =
    "23456789/34567890/";
  private static final String pdfPath = 
    "main.pdf";
  private static final String xmlPath = 
    "main.xml";
  private static final String shortPath = 
    basePath + basePath;
  private static final String jpgPath = 
    "fx1.jpg";
  private static final String urlStem = "http://www.elsevier.com/CLOCKSS/";

  public void testProcessCorrectPdfEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath+ pathStem + pdfPath,
				      7654, 0, null, null);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + pdfPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "application/pdf");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7654");
  }

  public void testProcessCorrectXmlEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath+ pathStem + xmlPath,
				      76543, 0, null, null);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + xmlPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "text/xml");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "76543");
  }

  public void testProcessShortName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(shortPath, 7, 0, null, null);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertNull(ae.getBaseUrl());
    assertNull(ae.getRestOfUrl());
    assertNull(ae.getHeaderFields());
  }

  public void testProcessJpgName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath + pathStem + jpgPath,
				       7, 0, null, null);
    ElsevierExploderHelper eeh = new ElsevierExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + jpgPath);
    assertEquals("image/jpeg", ae.getHeaderFields().get("Content-Type"));
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7");
  }

 }
