package org.lockss.plugin.wiley;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestWileyExploderHelper extends LockssTestCase {
  private static final String basePath =
    "01234567/";
  private static final String year = "2010/";
  private static final String pathStem =
    "123/ABC12345/";
  private static final String pdfPath = 
    "12345_ftp.pdf";
  private static final String sgmPath = 
    "12345_ftp.sgm";
  private static final String shortPath = 
    basePath + basePath;
  private static final String jpgPath = 
    "image_m/mfig123.jpg";
  private static final String urlStem = "http://wiley.clockss.org/";

  public void testProcessCorrectPdfEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath + year + pathStem + pdfPath,
				      7654, 0, null, null);
    WileyExploderHelper eeh = new WileyExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + pdfPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "application/pdf");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7654");
    // XXX - check addText
    // XXX - check AuProps
  }

  public void testProcessCorrectSgmEntry() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath + year + pathStem + sgmPath,
				      76543, 0, null, null);
    WileyExploderHelper eeh = new WileyExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + sgmPath);
    assertEquals(ae.getHeaderFields().get("Content-Type"), "application/sgml");
    assertEquals(ae.getHeaderFields().get("Content-Length"), "76543");
  }

  public void testProcessShortName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(shortPath, 7, 0, null, null);
    WileyExploderHelper eeh = new WileyExploderHelper();

    eeh.process(ae);
    assertNull(ae.getBaseUrl());
    assertNull(ae.getRestOfUrl());
    assertNull(ae.getHeaderFields());
  }

  public void testProcessJpgName() throws Exception {
    ArchiveEntry ae = new ArchiveEntry(basePath + year  + pathStem + jpgPath,
				       7, 0, null, null);
    WileyExploderHelper eeh = new WileyExploderHelper();

    eeh.process(ae);
    assertEquals(ae.getBaseUrl(), urlStem + basePath);
    assertEquals(ae.getRestOfUrl(), pathStem + jpgPath);
    assertEquals("image/jpeg", ae.getHeaderFields().get("Content-Type"));
    assertEquals(ae.getHeaderFields().get("Content-Length"), "7");
  }

}
