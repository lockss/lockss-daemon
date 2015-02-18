/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/**
 * Zip file utilities
 */
public class ZipUtil {
  static Logger log = Logger.getLogger("ZipUtil");

  /** Magic number of ZIP file.  (Why isn't this public in ZipConstants?) */
  public static final long ZIP_MAGIC = 0x504b0304L;	// "PK\003\004"

  /**
   * Return true if the file looks like a zip file.  Checks only the magic
   * number, not the whole file for validity.
   * @param file a possible zip file
   * @throws IOException
   */
  public static boolean isZipFile(File file) throws IOException {
    BufferedInputStream in =
      new BufferedInputStream(new FileInputStream(file));
    try {
      return isZipFile(in);
    } finally {
      IOUtil.safeClose(in);
    }
  }

  /**
   * Return true if the stream looks like the contents of a zip file.
   * Checks only the magic number, not the whole file for validity.  The
   * stream will be marked and reset to its current position.
   * @param in a stream open on possible zip file content
   * @throws IOException
   */
  public static boolean isZipFile(BufferedInputStream in) throws IOException {
    byte buf[] = new byte[4];
    in.mark(4);
    StreamUtil.readBytes(in, buf, buf.length);
    long v = ByteArray.decodeLong(buf);
    in.reset();
    return (v == ZIP_MAGIC);
  }

  /**
   * Expand the zip file to the specified directory.  Does not allow any
   * files to be created outside of specified dir.
   * @param zip zip file
   * @param toDir dir under which to expand zip contents
   * @throws ZipException if the zip file is invalid
   * @throws IOException
   */
  public static void unzip(File zip, File toDir)
      throws ZipException, IOException {
    InputStream in = new BufferedInputStream(new FileInputStream(zip));
    try {
      unzip(in, toDir);
    } finally {
      IOUtil.safeClose(in);
    }
  }

  /**
   * Interpret the stream as the contents of a zip file and Expand it to
   * the specified directory.  Does not allow any files to be created
   * outside of specified dir.
   * @param in InputStream open on zip-like content
   * @param toDir dir under which to expand zip contents
   * @throws ZipException if the zip file is invalid
   * @throws IOException
   */
  public static void unzip(InputStream in, File toDir)
      throws ZipException, IOException {
    if (!toDir.exists()) {
      toDir.mkdirs();
    }
    if (!toDir.exists()) {
      throw new IOException("Invalid target directory");
    }
    ZipInputStream zip = null;
    try {
      zip = new ZipInputStream(in);
      ZipEntry entry;

      while ((entry = zip.getNextEntry()) != null) {
	if (entry.isDirectory()) {
	  continue;
	}
	String relpath = entry.getName();
	if (relpath.startsWith("/")) {
	  throw new IOException("Absolute paths in zip not allowed:" + relpath);
	}
	File file = new File(toDir, relpath);
	if (!file.getCanonicalPath().startsWith(toDir.getCanonicalPath())) {
	  throw new IOException("Illegal path traversal");
	}
	File parent = file.getParentFile();
	if (parent != null) {
	  if (!parent.exists()) {
	    parent.mkdirs();
	  }
	}
        OutputStream out = new FileOutputStream(file);
	long n = StreamUtil.copy(zip, out);
	IOUtil.safeClose(out);
	if (log.isDebug3()) log.debug3("Write " + n + " bytes to " + file);
      }
    } finally {
      IOUtil.safeClose(zip);
    }
  }

  public static void addFileToZip(ZipOutputStream z,
				  File file) throws IOException {
    addFileToZip(z, file, null);
  }

  public static void addFileToZip(ZipOutputStream z,
				  File file,
				  String entryName) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(file));
      if (entryName == null) {
	entryName = file.getName();
      }
      addFileToZip(z, in, entryName);
    } finally {
      IOUtil.safeClose(in);
    }
  }

  public static void addFileToZip(ZipOutputStream z,
				  InputStream in,
				  String entryName) throws IOException {
    try {
      z.putNextEntry(new ZipEntry(entryName));
      StreamUtil.copy(in, z);
    } finally {
      z.closeEntry();
    }
  }

  public static void addDirToZip(ZipOutputStream z, String dirname,
				 String addPrefix) {
    addDirToZip(z, new File(dirname), addPrefix);
  }

  public static void addDirToZip(ZipOutputStream z,
				 File dir,
				 String addPrefix) {
    addDirToZip(z, dir, addPrefix, dir.getPath());
  }

  public static void addDirToZip(ZipOutputStream z,
				 String filename,
				 String addPrefix,
				 String removePrefix) {
    addDirToZip(z, new File(filename), removePrefix);
  }

  public static void addDirToZip(ZipOutputStream z,
				 File file,
				 String addPrefix,
				 String removePrefix) {
    try {
      if (file.isDirectory()) {
	for (String name : file.list()) {
	  addDirToZip(z, new File(file, name), addPrefix, removePrefix);
	}
      } else {
	// regular file
	String entName = file.getPath();
	if (removePrefix != null && file.getPath().startsWith(removePrefix)) {
	  entName =  entName.substring(removePrefix.length() + 1);
	}
	if (!StringUtil.isNullString(addPrefix)) {
	  entName =  new File(addPrefix, entName).getPath();
	}
	ZipEntry anEntry = new ZipEntry(entName);
	addFileToZip(z, file, entName);
      }
    } catch(Exception e) {
      //handle exception 
    }
  }
}
