package org.lockss.plugin.resiliencealliance;

import org.lockss.daemon.AuParamType;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuParamFunctor;
import org.lockss.test.LockssTestCase;

public class TestResilienceAllianceAuParamFunctor extends LockssTestCase {
  AuParamFunctor fn;

  public TestResilienceAllianceAuParamFunctor() {
  }

  public void setUp() throws Exception {
    super.setUp();
    this.fn = new ResilienceAllianceAuParamFunctor();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testApply() throws PluginException {
    assertEquals("https://foo.bar.com/path/foo", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://foo.bar.com/path/foo", AuParamType.String));
    assertEquals("https://www.bar.com/path/foo", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://bar.com/path/foo", AuParamType.String));
    assertEquals("https://www.bar.com/path/foo", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://www.bar.com/path/foo", AuParamType.String));
    assertEquals("https://www.ace-eco.org/", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://www.ace-eco.org/", AuParamType.String));
    assertEquals("https://www.ace-eco.org/", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://ace-eco.org/", AuParamType.String));
    assertEquals("https://www.ecologyandsociety.org/", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://www.ecologyandsociety.org/", AuParamType.String));
    assertEquals("https://www.ecologyandsociety.org/", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://ecologyandsociety.org/", AuParamType.String));
    assertEquals("https://journal.afonet.org/", this.fn.apply((AuParamFunctor.FunctorData) null, "add_www", "https://journal.afonet.org/", AuParamType.String));



  }
}
