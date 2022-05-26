package org.lockss.plugin.ojs3;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestOjs3HtmlHashFilterFactory  extends LockssTestCase {
  private Ojs3HtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new Ojs3HtmlHashFilterFactory();
  }


  private static final String vancouver1 =
    "<div class=\"csl-right-inline\">Dal PontGE. Lawyer Professionalism in the 21st Century. VULJ [Internet]. 2018Dec.31 [cited 2022May19];8(1):7\u001318. Available from: https://vulj.vu.edu.au/index.php/vulj/article/view/1132</div>";

  private static final String vancouver2 =
    "<div class=\"csl-right-inline\">Dal PontGE. Lawyer Professionalism in the 21st Century. VULJ [Internet]. 2018Dec.31 [cited 2022May18];8(1):7\u001318. Available from: https://vulj.vu.edu.au/index.php/vulj/article/view/1132</div>";

  private static final String tarubian1 =
      "<div class=\"csl-entry\">Zeller, Bruno, and Richard Lightfoot. \u001CGood Faith: An ICSID Convention Requirement?\u001D. <i>Victoria University Law and Justice Journal</i> 8, no. 1 (December 31, 2018): 19\u001332. Accessed May 19, 2022. https://vulj.vu.edu.au/index.php/vulj/article/view/1139.</div>";

  private static final String tarubian2 =
      "<div class=\"csl-entry\">Zeller, Bruno, and Richard Lightfoot. \u001CGood Faith: An ICSID Convention Requirement?\u001D. <i>Victoria University Law and Justice Journal</i> 8, no. 1 (December 31, 2018): 19\u001332. Accessed May 18, 2022. https://vulj.vu.edu.au/index.php/vulj/article/view/1139.</div>";

  private static final String harvard1 =
      "<div class=\"csl-entry\">Mendoza, R. \u001CRuby\u001D . (2022) \u001CBook Review\u0014The Borders of AIDS: Race, Quarantine &amp; Resistance by Karma R. Ch�vez\u001D, <i>Literacy in Composition Studies</i>, 9(2), pp. 67-72. Available at: https://licsjournal.org/index.php/LiCS/article/view/2193 (Accessed: 21May2022).</div>";

  private static final String harvard2 =
      "<div class=\"csl-entry\">Mendoza, R. \u001CRuby\u001D . (2022) \u001CBook Review\u0014The Borders of AIDS: Race, Quarantine &amp; Resistance by Karma R. Ch�vez\u001D, <i>Literacy in Composition Studies</i>, 9(2), pp. 67-72. Available at: https://licsjournal.org/index.php/LiCS/article/view/2193 (Accessed: 20May2022).</div>";

  private static final String abdnt1 =
      "<div class=\"csl-entry\">DRERUP, C.; EVESLAGE, M.; SUNDERK�TTER, C.; EHRCHEN, J. Diagnostic Value of Laboratory Parameters for Distinguishing Between Herpes Zoster and Bacterial Superficial Skin and Soft Tissue Infections. <b>Acta Dermato-Venereologica</b>, <i>[S. l.]</i>, v. 100, n. 1, p. 1\u00135, 2020. DOI: 10.2340/00015555-3357. Dispon�vel em: https://medicaljournalssweden.se/actadv/article/view/1631. Acesso em: 19 may. 2022.</div>";

  private static final String abdnt2 =
      "<div class=\"csl-entry\">DRERUP, C.; EVESLAGE, M.; SUNDERK�TTER, C.; EHRCHEN, J. Diagnostic Value of Laboratory Parameters for Distinguishing Between Herpes Zoster and Bacterial Superficial Skin and Soft Tissue Infections. <b>Acta Dermato-Venereologica</b>, <i>[S. l.]</i>, v. 100, n. 1, p. 1\u00135, 2020. DOI: 10.2340/00015555-3357. Dispon�vel em: https://medicaljournalssweden.se/actadv/article/view/1631. Acesso em: 18 may. 2022.</div>";


  public void testAbdntFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(abdnt1), getStringfromFilteredInputStream(abdnt2));
  }
  public void testVancouverFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(vancouver1), getStringfromFilteredInputStream(vancouver2));
  }
  public void testHarvardFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(harvard1), getStringfromFilteredInputStream(harvard2));
  }
  public void testTarubianFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(tarubian1), getStringfromFilteredInputStream(tarubian2));
  }

  public String getStringfromFilteredInputStream(String in) throws IOException {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(in),
        Constants.DEFAULT_ENCODING);
    return StringUtil.fromInputStream(actIn);
  }
}
