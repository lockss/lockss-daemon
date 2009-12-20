package org.lockss.util;

import org.lockss.test.LockssTestCase;

import java.net.URLDecoder;


/**
 * Created by IntelliJ IDEA.
 * User: dsferopo
 * Date: Dec 17, 2009
 * Time: 3:40:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestMetadataUtil extends LockssTestCase {

  private String validISSNS [] = {
          "1144-875X",
          "1543-8120",
          "1508-1109",
          "1733-5329",
          "0740-2783",
          "0097-4463",
          "1402-2001",
          "1523-0430",
          "1938-4246",
          "0006-3363"
  };

  private String invalidISSNS [] = {
          "1144-175X",
          "1144-8753",
          "1543-8122",
          "1541-8120",
          "1508-1409",
          "2740-2783",
          "1938-42463",
          "140-42001",
          "15236-430",
          "1402-200",
          "1938/4246",
          "1402",
          "1402-",
          "-4246"
  };

  private String validDOIS [] = {
          "10.1095/biolreprod.106.054056",
          "10.2992/007.078.0301",
          "10.1206/606.1",
          "10.1640/0002-8444-99.2.61",
          "10.1663/0006-8101(2007)73[267:TPOSRI]2.0.CO;2",
          "10.1663/0006-8101(2007)73[267%3ATPOSRI]2.0.CO%3B2"
  };

  private String invalidDOIS [] = {
          "12.1095/biolreprod.106.054056",
          "10.2992-007.078.0301",
          "10.1206",
          "/0002-8444-99.2.61",
          "-0002-8444-99.2.61",
          "10.1640/0002-8444/99.2.61",
  };

  public void testISSN() {
    for(int i=0; i<validISSNS.length;i++){
      assertTrue(MetadataUtil.isISSN(validISSNS[i]));
    }

    for(int j=0; j<invalidISSNS.length;j++){
      assertFalse(MetadataUtil.isISSN(invalidISSNS[j]));
    }
  }

  public void testDOI() {
    for(int i=0; i<validDOIS.length;i++){
      assertTrue(MetadataUtil.isDOI(URLDecoder.decode(validDOIS[i])));
    }

    for(int j=0; j<invalidDOIS.length;j++){
      assertFalse(MetadataUtil.isDOI(invalidDOIS[j]));
    }
  }

  public static void main(String[] args) {
    TestMetadataUtil t = new TestMetadataUtil();
    t.testISSN();
    t.testDOI();
  }
}
