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

package org.lockss.daemon;

import java.util.*;

import org.lockss.state.*;

/** The acceptable permission statements, one of which LOCKSS needs to see
 * before it will collect content
 */

public class LockssPermission {
  
  /**
   * The Standard LOCKSS Permission Statement
   */
  public static final String LOCKSS_PERMISSION_STRING =
      "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";

  /**
   * The Standard LOCKSS Permission Statement with Open Access Qualification
   */
  public static final String LOCKSS_OPEN_ACCESS_PERMISSION_STRING =
      "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit";

  public static final String LOCKSS_OJS_PERMISSION_STRING =
      "This journal utilizes the LOCKSS system to create a distributed archiving system among participating libraries and permits those libraries to create permanent archives of the journal for purposes of preservation and restoration";

  List<PermissionChecker> permissionList;

  public LockssPermission() {
    ArrayList<PermissionChecker> lst = new ArrayList<PermissionChecker>();
    StringPermissionChecker spc;

    spc = new StringPermissionChecker(LOCKSS_PERMISSION_STRING,
                                      new StringPermissionChecker.StringFilterRule());
    spc.doSetAccessType(AuState.AccessType.Subscription);
    lst.add(spc);

    spc = new StringPermissionChecker(LOCKSS_OPEN_ACCESS_PERMISSION_STRING,
                                      new StringPermissionChecker.StringFilterRule());
    spc.doSetAccessType(AuState.AccessType.OpenAccess);
    lst.add(spc);

    spc = new StringPermissionChecker(LOCKSS_OJS_PERMISSION_STRING,
                                      new StringPermissionChecker.StringFilterRule());
    spc.doSetAccessType(AuState.AccessType.Subscription);
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
