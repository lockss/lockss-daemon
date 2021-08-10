package org.lockss.plugin.highwire;

import org.lockss.test.LockssTestCase;
import org.lockss.util.SetUtil;

import java.util.Set;

public class TestHighWireJCoreUrlConsumerFactory extends LockssTestCase {
  Set<String> destinationUrls = SetUtil.set(
    "https://www.jrheum.org/content/jrheum/46/11/E011.full.pdf",
    "https://www.jrheum.org/content/jrheum/46/1/112.full.pdf",
    "https://www.ghspjournal.org/content/7/4"
  );

  Set<String> originatingUrls = SetUtil.set(
    "https://www.jrheum.org/content/46/11/E011.full.pdf",
    "https://www.jrheum.org/content/46/1/112.full.pdf",
    "https://www.ghspjournal.org/content/7/4.toc"
  );

  public void testOrigPattern() throws Exception {
    for (String url : originatingUrls) {
      assertMatchesRE(HighWireJCoreUrlConsumerFactory.origPat, url);
      assertNotMatchesRE(HighWireJCoreUrlConsumerFactory.destPat, url);
    }
  }

  public void testDestPattern() throws Exception {
    for (String url : destinationUrls) {
      assertNotMatchesRE(HighWireJCoreUrlConsumerFactory.origPat, url);
      assertMatchesRE(HighWireJCoreUrlConsumerFactory.destPat, url);
    }
  }

}
