/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.devtools;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class FilterRunner {

  public static void filterDirectory(FilterRule filter, File srcDir,
				     File destDir)
      throws FileNotFoundException, IOException {
    if (filter == null) {
      throw new IllegalArgumentException("Called with null filter");
    } else if (srcDir == null) {
      throw new IllegalArgumentException("Called with null source dir");
    } else if (!srcDir.isDirectory()) {
      throw new IllegalArgumentException("Called with src that isn't a directory: "+srcDir);
    } else if (destDir == null) {
      throw new IllegalArgumentException("Called with null dest directory");
    } else if (destDir.exists() && !destDir.isDirectory()) {
      throw new IllegalArgumentException("Called with dest that isn't a directory: "+destDir);
    }

    if (!destDir.exists()) {
      destDir.mkdir();
    }

    File children[] = srcDir.listFiles();
    for (int ix=0; ix<children.length; ix++) {
      if (children[ix].isFile()) {
	filterSingleFile(filter, children[ix], destDir);
      } else if (children[ix].isDirectory()) {
	String childDir = children[ix].getPath();
	childDir = childDir.substring(childDir.lastIndexOf(File.separator));
	File newDestDir = new File(destDir.getPath()+File.separator+childDir);
	newDestDir.mkdir();
	filterDirectory(filter, children[ix], newDestDir);
      }
    }
  }

  public static void filterSingleFile(FilterRule filter,
				      File src, File dest)
      throws FileNotFoundException, IOException {
    if (filter == null) {
      throw new IllegalArgumentException("Called with null filter");
    } else if (src == null) {
      throw new IllegalArgumentException("Called with null source file");
    } else if (dest == null) {
      throw new IllegalArgumentException("Called with null dest file");
    } else if (!src.isFile()) {
      throw new IllegalArgumentException("Called with src that isn't a file");
    }
    if (dest.isDirectory()) {
      String file = src.getPath();
      file = file.substring(file.lastIndexOf(File.separator));
      dest = new File(dest, file);
    }
//     System.out.println("Filtering "+src+" to "+dest);
    Reader reader = new FileReader(src);
    dest.createNewFile();
    Writer writer = new FileWriter(dest);
    try {
      StreamUtil.copy(filter.createFilteredReader(reader), writer);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
  }

  public static FilterRule filterRuleFromString(String filterStr)
      throws ClassNotFoundException, InstantiationException,
	     IllegalAccessException {
    Class filterRuleClass = Class.forName(filterStr);
    return (FilterRule)filterRuleClass.newInstance();
  }

  public static void filterDirectory(FilterFactory filter, File srcDir,
				     File destDir)
      throws FileNotFoundException, IOException {
    if (filter == null) {
      throw new IllegalArgumentException("Called with null filter");
    } else if (srcDir == null) {
      throw new IllegalArgumentException("Called with null source dir");
    } else if (!srcDir.isDirectory()) {
      throw new IllegalArgumentException("Called with src that isn't a directory: "+srcDir);
    } else if (destDir == null) {
      throw new IllegalArgumentException("Called with null dest directory");
    } else if (destDir.exists() && !destDir.isDirectory()) {
      throw new IllegalArgumentException("Called with dest that isn't a directory: "+destDir);
    }

    if (!destDir.exists()) {
      destDir.mkdir();
    }

    File children[] = srcDir.listFiles();
    for (int ix=0; ix<children.length; ix++) {
      if (children[ix].isFile()) {
	filterSingleFile(filter, children[ix], destDir);
      } else if (children[ix].isDirectory()) {
	String childDir = children[ix].getPath();
	childDir = childDir.substring(childDir.lastIndexOf(File.separator));
	File newDestDir = new File(destDir.getPath()+File.separator+childDir);
	newDestDir.mkdir();
	filterDirectory(filter, children[ix], newDestDir);
      }
    }
  }

  public static void filterSingleFile(FilterFactory filter,
				      File src, File dest)
      throws FileNotFoundException, IOException {
    if (filter == null) {
      throw new IllegalArgumentException("Called with null filter");
    } else if (src == null) {
      throw new IllegalArgumentException("Called with null source file");
    } else if (dest == null) {
      throw new IllegalArgumentException("Called with null dest file");
    } else if (!src.isFile()) {
      throw new IllegalArgumentException("Called with src that isn't a file");
    }
    if (dest.isDirectory()) {
      String file = src.getPath();
      file = file.substring(file.lastIndexOf(File.separator));
      dest = new File(dest, file);
    }
//     System.out.println("Filtering "+src+" to "+dest);
    InputStream in = new BufferedInputStream(new FileInputStream(src));
    dest.createNewFile();
    OutputStream out = new FileOutputStream(dest);
    try {
      StreamUtil.copy(filter.createFilteredInputStream(null, in,
						       Constants.DEFAULT_ENCODING),
		      out);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
  }

  public static FilterFactory filterFactoryFromString(String filterStr)
      throws ClassNotFoundException, InstantiationException,
	     IllegalAccessException {
    Class filterFactoryClass = Class.forName(filterStr);
    return (FilterFactory)filterFactoryClass.newInstance();
  }

  public static void main(String args[]) {
    String src = args[0];
    String dest = args[1];
    String filterStr = args[2];

    try {
      FilterRule filter = filterRuleFromString(filterStr);
      filterDirectory(filter, new File(src), new File(dest));
    } catch (Exception e) {
      try {
	FilterFactory filter = filterFactoryFromString(filterStr);
	filterDirectory(filter, new File(src), new File(dest));
      } catch (Exception e1) {
	e1.printStackTrace();
      }
    }
  }
}
