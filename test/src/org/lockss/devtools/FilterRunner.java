/*
 * $Id: FilterRunner.java,v 1.5 2003-10-07 22:24:24 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.lockss.crawler.*;
import org.lockss.plugin.FilterRule;
import org.lockss.filter.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 */
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
    } else if (!destDir.isDirectory()) {
      throw new IllegalArgumentException("Called with dest that isn't a directory: "+destDir);
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
    System.out.println("Filtering "+src+" to "+dest);
    Reader reader = new FileReader(src);
    OutputStream os = new FileOutputStream(dest);
    StreamUtil.copy(filter.createFilteredInputStream(reader), os);
  }

  public void main(String args[]) {
    
  }

}
