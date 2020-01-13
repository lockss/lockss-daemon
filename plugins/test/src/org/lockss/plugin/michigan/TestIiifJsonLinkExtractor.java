package org.lockss.plugin.michigan;

import java.util.*;

import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.LockssTestCase;

public class TestIiifJsonLinkExtractor extends LockssTestCase {

  public void testIiifJsonLinkextractor() throws Exception {
    IiifJsonLinkExtractor le = new IiifJsonLinkExtractor();
    final List<String> res = new ArrayList<String>();
    le.extractUrls(null,
                   getClass().getResourceAsStream("info1.json"),
                   "utf-8",
                   "http://www.example.com/image-service/123456789/info.json",
                   new Callback() {
                     @Override
                     public void foundLink(String url) {
                       res.add(url);
                     }
                   });
    assertEquals(Arrays.asList("http://www.example.com/image-service/123456789/full/full/0/default.jpg",
                               "http://www.example.com/image-service/123456789/0,0,256,256/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/0,256,256,256/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/0,512,256,97/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/256,0,256,256/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/256,256,256,256/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/256,512,256,97/256,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/512,0,238,256/,256/0/default.jpg",
                               "http://www.example.com/image-service/123456789/512,256,238,256/,256/0/default.jpg",
                               "http://www.example.com/image-service/123456789/512,512,238,97/238,/0/default.jpg",
                               "http://www.example.com/image-service/123456789/full/full/0/default.png",
                               "http://www.example.com/image-service/123456789/0,0,256,256/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/0,256,256,256/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/0,512,256,97/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/256,0,256,256/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/256,256,256,256/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/256,512,256,97/256,/0/default.png",
                               "http://www.example.com/image-service/123456789/512,0,238,256/,256/0/default.png",
                               "http://www.example.com/image-service/123456789/512,256,238,256/,256/0/default.png",
                               "http://www.example.com/image-service/123456789/512,512,238,97/238,/0/default.png"),
                 res);
  }
  
}
