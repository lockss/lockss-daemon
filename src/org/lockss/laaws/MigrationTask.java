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

package org.lockss.laaws;


import org.lockss.plugin.*;
import org.lockss.util.*;

public class MigrationTask {

  private static Logger log = Logger.getLogger("MigrationTask");

  public enum TaskType { COPY_CU_VERSIONS, COPY_AU_STATE }

  V2AuMover auMover;
  CachedUrl cu;
  ArchivalUnit au;
  TaskType type;

  private MigrationTask(V2AuMover mover, TaskType type) {
    this.auMover = mover;
    this.type = type;
  }

  public static MigrationTask copyCuVersions(V2AuMover mover,
                                             ArchivalUnit au,
                                             CachedUrl cu) {
    return new MigrationTask(mover, TaskType.COPY_CU_VERSIONS)
      .setCu(cu)
      .setAu(cu.getArchivalUnit());
  }

  public static MigrationTask copyAuState(V2AuMover mover,
                                          ArchivalUnit au) {
    return new MigrationTask(mover, TaskType.COPY_AU_STATE)
      .setAu(au);
  }

  private MigrationTask setAu(ArchivalUnit au) {
    this.cu = cu;
    return this;
  }

  private MigrationTask setCu(CachedUrl cu) {
    this.cu = cu;
    return this;
  }

  public V2AuMover getAuMover() {
    return auMover;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public CachedUrl getCu() {
    return cu;
  }

}
