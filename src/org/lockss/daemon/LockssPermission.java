/*
 * $Id: LockssPermission.java,v 1.7 2008-01-27 06:43:57 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.daemon;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

/** The acceptable permission statements, one of which LOCKSS needs to see
 * before it will collect content
 */

public class LockssPermission {
  public static final String LOCKSS_PERMISSION_STRING =
    "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";

  public static final String LOCKSS_OPEN_ACCESS_PERMISSION_STRING =
    "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit";

  public static final String LOCKSS_OJS_PERMISSION_STRING =
    "This journal utilizes the LOCKSS system to create a distributed archiving system among participating libraries and permits those libraries to create permanent archives of the journal for purposes of preservation and restoration";

  List permissionList;

  public LockssPermission() {
    ArrayList lst = new ArrayList();
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
    lst.add(new CreativeCommonsV3PermissionChecker());
    lst.trimToSize();
    permissionList = Collections.unmodifiableList(lst);
  }

  public List getCheckers() {
    return permissionList;
  }
}
