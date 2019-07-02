/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.app;

import java.util.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * LOCKSS REST service descriptor.  Primarily used as an identifier for
 * naming and to locate a ServiceBinding.  Object identity is not
 * important.
 *
 * <br>This class copied into legacy daemon only to facilitate code sharing
 * with laaws
 */
public class ServiceDescr implements Comparable<ServiceDescr> {

  // Static mapping of abbreviation -> ServiceDescr
  static Map<String,ServiceDescr> abbrevMap = new HashMap<>();

  private final String name;
  private final String abbrev;

  public static final ServiceDescr SVC_CONFIG =
    register(new ServiceDescr("Config Service", "cfg"));
  public static final ServiceDescr SVC_MDX =
    register(new ServiceDescr("Metadata Extraction Service", "mdx"));
  public static final ServiceDescr SVC_MDQ =
    register(new ServiceDescr("Metadata Query Service", "mdq"));
  public static final ServiceDescr SVC_POLLER =
    register(new ServiceDescr("Poller Service", "poller"));
  public static final ServiceDescr SVC_CRAWLER =
    register(new ServiceDescr("Crawler Service", "crawler"));
  public static final ServiceDescr SVC_REPO =
    register(new ServiceDescr("Repository Service", "repo"));

  public ServiceDescr(String name, String abbrev) {
    if (name == null) {
      throw new IllegalArgumentException("ServiceDescr name must not be null");
    }
    if (abbrev == null) {
      throw new IllegalArgumentException("ServiceDescr abbrev must not be null");
    }
    this.name = name;
    this.abbrev = abbrev;
  }

  public String getName() {
    return name;
  }

  public String getAbbrev() {
    return abbrev;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServiceDescr) {
      ServiceDescr sd = (ServiceDescr)obj;
      return ObjectUtils.equals(name, sd.getName())
	&& ObjectUtils.equals(abbrev, sd.getAbbrev());
    }
    return false;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(name);
    hcb.append(abbrev);
    return hcb.toHashCode();
  }

  @Override
  public int compareTo(ServiceDescr o) {
    return name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return "[SD: " + name + ", " + abbrev + "]";
  }

  /** Register a ServiceDescr under its abbreviation.  This is a static
   * registry of known services, doesn't reflect any running state, so can
   * be (is) implemented with a static Map.
   */
  public static ServiceDescr register(ServiceDescr sd) {
    ServiceDescr oldreg = abbrevMap.get(sd.getAbbrev());
    if (oldreg != null && !oldreg.equals(sd)) {
      throw new IllegalStateException("A ServiceDescr is already registered for that abbreviation: " + oldreg);
    }
    abbrevMap.put(sd.getAbbrev(), sd);
    return sd;
  }

  /** Return the ServiceDescr registered for the abbreviation, or null */
  public static ServiceDescr fromAbbrev(String abbrev) {
    return abbrevMap.get(abbrev);
  }
}
