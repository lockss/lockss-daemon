/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.clockss;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.state.AuState;

/**
 * The CLOCKSS permission checker
 */

public class ClockssPermission {
  
  /**
   * The CLOCKSS Permission Statement
   */
  public static final String CLOCKSS_PERMISSION_STRING =
      "CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit";

  /**
   * The CLOCKSS Permission Statement with Open Access Qualification
   */
  public static final String CLOCKSS_OPEN_ACCESS_PERMISSION_STRING =
      "CLOCKSS system has permission to ingest, preserve, and serve this open access Archival Unit";

  List<PermissionChecker> permissionList;

  public ClockssPermission() {
    ArrayList<PermissionChecker> lst = new ArrayList<PermissionChecker>();
    StringPermissionChecker spc;

    spc = new StringPermissionChecker(CLOCKSS_PERMISSION_STRING,
                                      new StringPermissionChecker.StringFilterRule());
    spc.doSetAccessType(AuState.AccessType.Subscription);
    lst.add(spc);

    spc = new StringPermissionChecker(CLOCKSS_OPEN_ACCESS_PERMISSION_STRING,
                                      new StringPermissionChecker.StringFilterRule());
    spc.doSetAccessType(AuState.AccessType.OpenAccess);
    lst.add(spc);

    lst.add(new CreativeCommonsPermissionChecker());
    lst.add(new CreativeCommonsRdfPermissionChecker());

    lst.trimToSize();
    permissionList = Collections.unmodifiableList(lst);
  }

  public List<PermissionChecker> getCheckers() {
    return permissionList;
  }
  
}
