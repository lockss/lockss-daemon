/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;
import java.io.*;
import java.util.*;
import org.lockss.daemon.CachedUrlSetSpec;

/**
 * DirectoryEntry is used to describe an internal node in the cache.
 */
public class DirectoryEntryImpl extends RepositoryEntryImpl implements DirectoryEntry {
  public DirectoryEntryImpl(String url, String rootLocation) {
    super(url, rootLocation);
  }

  public boolean isLeaf() {
    return false;
  }

  public Iterator listEntries(CachedUrlSetSpec filter) {
    File entryDir = new File(mapUrlToCacheLocation());
    if (!entryDir.exists()) {
      entryDir.mkdirs();
    }
    File[] children = entryDir.listFiles(new CussFileFilter(filter));
    Vector childV = new Vector();
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if (!child.isDirectory()) continue;
      File childLeaf = new File(child, LeafEntryImpl.LEAF_FILE_NAME);
      boolean isLeaf = childLeaf.exists();
      String childUrl = this.url + File.separator + child.getName();
      if (isLeaf) {
        childV.addElement(new LeafEntryImpl(childUrl, rootLocation));
      } else {
        childV.addElement(new DirectoryEntryImpl(childUrl, rootLocation));
      }
    }
    //XXX sort files

    return childV.iterator();
  }

  private String mapUrlToCacheLocation() {
    return rootLocation + LockssRepositoryImpl.mapUrlToCacheLocation(url);
  }

  private class CussFileFilter implements FileFilter {
    private CachedUrlSetSpec spec;

    public CussFileFilter(CachedUrlSetSpec spec) {
      this.spec = spec;
    }

    public boolean accept(File pathname) {
      if (spec==null) return true;
      else return spec.matches(pathname.getAbsolutePath());
    }
  }
}
