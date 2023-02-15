/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.base;

import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

public class TestBaseAuParamFunctor extends LockssTestCase {

  AuParamFunctor fn;

  public void setUp() throws Exception {
    super.setUp();
    fn = new BaseAuParamFunctor();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testApply() throws PluginException {
    assertEquals("WWW.foo.bar",
		 fn.apply(null, "url_host",
			  "http://WWW.foo.bar/path/foo", AuParamType.String));
    assertEquals("/path/foo",
		 fn.apply(null, "url_path",
			  "http://WWW.foo.bar/path/foo", AuParamType.String));
    assertEquals(23,
		 fn.apply(null, "short_year",
			  2023, AuParamType.Year));

    assertEquals("http://foo.bar/",
		 fn.apply(null, "del_www",
			  "http://WWW.foo.bar/", AuParamType.String));
    assertEquals("http://www.FOO.BAR/",
		 fn.apply(null, "add_www",
			  "http://FOO.BAR/", AuParamType.String));

    assertEquals("https://FOO.BAR/",
		 fn.apply(null, "to_https",
			  "http://FOO.BAR/", AuParamType.String));
    assertEquals("http://FOO.BAR/",
		 fn.apply(null, "to_http",
			  "https://FOO.BAR/", AuParamType.String));

    assertEquals("http%3A%2F%2Ffoo.bar%2Fpath%3Fa%3Dv%26a2%3Dv2",
		 fn.apply(null, "url_encode",
			  "http://foo.bar/path?a=v&a2=v2",
			  AuParamType.String));
    assertEquals("http://foo.bar/path?a=v&a2=v2",
		 fn.apply(null, "url_decode",
			  "http%3A%2F%2Ffoo.bar%2Fpath%3Fa%3Dv%26a2%3Dv2",
			  AuParamType.String));

    assertEquals("aaa",
                 fn.apply(null, "range_min", "aaa-zzz", AuParamType.String));
    assertEquals("zzz",
                 fn.apply(null, "range_max", "aaa-zzz", AuParamType.String));

    assertEquals(Long.valueOf(123L),
                 fn.apply(null, "num_range_min", "123-456", AuParamType.Long));
    assertEquals(Long.valueOf(456L),
                 fn.apply(null, "num_range_max", "123-456", AuParamType.Long));
  }

  AuParamType fntype(String name) throws PluginException {
    return fn.type(null, name);
  }

  /**
   * Also tests the exact number and names of all functors
   */
  public void testType() throws PluginException {
    assertEquals(12, BaseAuParamFunctor.fnTypes.size());
    assertEquals(null, fntype("no_fn"));
    assertEquals(AuParamType.String, fntype("url_host"));
    assertEquals(AuParamType.String, fntype("url_path"));
    assertEquals(AuParamType.String, fntype("add_www"));
    assertEquals(AuParamType.String, fntype("del_www"));
    assertEquals(AuParamType.String, fntype("to_http"));
    assertEquals(AuParamType.String, fntype("to_https"));
    assertEquals(AuParamType.String, fntype("url_encode"));
    assertEquals(AuParamType.String, fntype("url_decode"));
    assertEquals(AuParamType.String, fntype("range_min"));
    assertEquals(AuParamType.String, fntype("range_max"));
    assertEquals(AuParamType.Long, fntype("num_range_min"));
    assertEquals(AuParamType.Long, fntype("num_range_max"));
  }
}
