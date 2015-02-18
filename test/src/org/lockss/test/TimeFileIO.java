/*
 * $Id$
 */

/*

Copyright (c) 2005 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
nncopies of the Software, and to permit persons to whom the Software is
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

package org.lockss.test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import org.mortbay.util.B64Code;
import org.lockss.util.*;

/** Measure Java I/O and hash performance, using a variety of I/O methods.
 * Run with no args for instructions.
 *
 * <p>Problems discovered with NIO:<ul>
 *
 * <li>Using Channel.map() to obtain a MappedByteBuffer seems to yield the
 * best performance, but each call to map() allocates a new
 * MappedByteBuffer, and the only way to get rid of them (and unmap the
 * mapping) is to wait for the garbage collector.  This often causes
 * IOException: Cannot allocate memory.  See <a
 * href=http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038>Sun bug
 * 4724038</a> for more details.
 *
 * <li>Reading from a channel into any type of buffer other than a
 * DirectByteBuffer causes Java to first read into a temporary
 * DirectByteBuffer then copy into the destination buffer.  This temporary
 * buffer is obtained with sun.nio.ch.getTemporaryDirectBuffer(), which
 * caches a few DirectByteBuffers in each thread.  However, as of Java
 * 1.4.2, if the cache gets filled with small buffers, later calls to
 * getTemporaryDirectBuffer() for larger buffers will allocate a new buffer
 * each time, until the entries in the cache are GCed, leading to
 * OutOfMemoryError.  Small buffers apparently get allocated under the hood
 * in some circumstances; if doAll() runs all the tests in the same thread
 * on OpenBSD it runs into this problem.
 *
 *</ul>
 */
public class TimeFileIO extends LockssTiming {
  static final int DEFAULT_BUFSIZE = 1024 * 1024;
  static final String DEFAULT_ALGORITHM = "SHA1";

  private TreeTimingReporter reporter;
  private byte bArray[];
  private ByteBuffer bBuffer;
  private MessageDigest[] hashers;

  private String root;
  private String label;
  private String alg = DEFAULT_ALGORITHM;
  private String duration;
  private File rootFile;
  private String bufSizeStr;
  private int bufSize = DEFAULT_BUFSIZE;
  private int hashes = 1;
  private boolean isWalk;
  private boolean isOpen;
  private boolean isRead;
  private boolean isGzip;
  private boolean isHash;
  private boolean isDaemonHash;
  private boolean isNio;
  private char bufferMode = '0';
  private boolean isAll;
  private boolean isPrint;
  private boolean isThread;

  private String msg;
  private String ioMsg = "InputStream";


  /** Allocate buffers, create MessageDigests */
  public void setUp() throws Exception {
    super.setUp();
    if (isRead) {
      bArray = new byte[bufSize];
      if (isNio) {
	switch (bufferMode) {
	case '0':
	  break;
	case 'b':
	  bBuffer = ByteBuffer.allocate(bufSize);
	  break;
	case 'd':
	  bBuffer = ByteBuffer.allocateDirect(bufSize);
	  break;
	case 'w':
	  bBuffer = ByteBuffer.wrap(bArray);
	  if (!bBuffer.hasArray()) {
	    throw new UnsupportedOperationException("Wrapped ByteBuffer doesn't have backing array");
	  }
	  break;
	case 'm':
	  break;
	}
      }
    }

    if (isHash) {
      hashers = new MessageDigest[hashes];
      for (int ix = 0; ix < hashes; ix++) {
	hashers[ix] = MessageDigest.getInstance(alg);
      }
    }
  }

  /** Time previously setup test */
  private void timeIt() throws Exception {
    StringBuffer sb = new StringBuffer();
    if (label != null) {
      sb.append(label);
      sb.append(": ");
    }
    sb.append(msg);
    if (isRead && ioMsg != null) {
      sb.append(" (");
      sb.append(ioMsg);
      if (bBuffer != null && bBuffer.hasArray()) {
	sb.append("*");
      }
      sb.append(")");
    }
    reporter = new TreeTimingReporter(sb.toString());
    time(reporter,
	 new Computation() {
	   public void execute() throws Exception {
	     doTreeWalk(rootFile);
	   }});
  }

  /** Traverse directory tree, possibly opening, reading or hashing
   * files */
  private void doTreeWalk(File rootFile)
      throws FileNotFoundException, IOException {
    reporter.filesProcessed++;
    if (rootFile.isFile()) {
      doLeaf(rootFile);
    } else if (rootFile.isDirectory()) {
      File children[] = rootFile.listFiles();
      if (children == null) {
	return;
      }
      for (int ix = 0; ix < children.length; ix++) {
	doTreeWalk(children[ix]);
      }
    }
  }

  /** Process a regular file */
  private void doLeaf(File file) throws IOException {
    if (isOpen) {
      doOpen(file);
    } else if (isRead) {
      doRead(file);
    }
  }

  /** Open a file */
  private void doOpen(File file) throws IOException {
    if (isNio) {
    } else {
      FileInputStream ins = new FileInputStream(file);
      ins.close();
    }
  }

  /** Read a file */
  private void doRead(File file) throws IOException {
    if (isNio) {
      doReadNio(file);
    } else {
      InputStream is;
      FileInputStream fis = new FileInputStream(file);
      is = new BufferedInputStream(fis);
      if (isGzip) {
	is = new GZIPInputStream(is);
      }
      long n;
      while ((n = is.read(bArray)) > 0) {
	if (isHash) {
	  doHash(bArray, n);
	}
	reporter.bytesProcessed += n;
      }
      fis.close();
    }
  }

  /** Read a file using NIO */
  private void doReadNio(File file) throws IOException {
    long n;
    FileChannel channel;
    RandomAccessFile raf = new RandomAccessFile (file, "r");
    char mode = bufferMode;
    switch (mode) {
    case '0':
      while ((n = raf.read(bArray)) > 0) {
	if (isHash) {
	  doHash(bArray, n);
	}
	reporter.bytesProcessed += n;
      }
      break;
    case 'b':
    case 'd':
    case 'w':
      channel = raf.getChannel();
      while ((n = channel.read(bBuffer)) > 0) {
	if (isHash) {
	  doHash(bBuffer, n);
	}
	reporter.bytesProcessed += n;
	bBuffer.clear();
      }
      break;
    case 'm':
      channel = raf.getChannel();
      long len = channel.size();
      for (long pos = 0; pos < len; ) {
	long size = len - pos;
	if (size > bufSize) size = bufSize;
	MappedByteBuffer map =
	  channel.map(FileChannel.MapMode.READ_ONLY, pos, size);
	map.load();
	pos += size;
	if (isHash) {
	  doHash(map, size);
	}
	reporter.bytesProcessed += size;
      }
      break;
    }
    raf.close();
  }

  /** Hash the byte array n times */
  private void doHash(byte[] bArray, long size) {
    for (int pos = 0; pos < size; pos += Integer.MAX_VALUE) {
      int n;
      if (size - pos < Integer.MAX_VALUE) {
	n = (int)(size - pos);
      } else {
	n = Integer.MAX_VALUE;
      }
      for (int ix = 0; ix < hashes; ix++) {
	hashers[ix].update(bArray, pos, n);
      }
    }
  }

  /** Hash the ByteBuffer n times, using its backing araay if possible else
   * copying to a byte array */
  private void doHash(ByteBuffer buf, long size) {
    buf.rewind();
    if (buf.hasArray()) {
      doHash(buf.array(), size);
    } else {
      int len = bArray.length;
      for (int pos = 0; pos < size; pos += len) {
	int n;
	if (size - pos < len) {
	  n = (int)(size - pos);
	} else {
	  n = len;
	}
	buf.get(bArray, 0, n);
	for (int ix = 0; ix < hashes; ix++) {
	  hashers[ix].update(bArray, 0, n);
	}
      }
    }
  }

  static class TreeTimingReporter extends TimingReporterImpl {
    private String msg;
 //   private String outLabel;
    private long bytesProcessed = 0;
    private int filesProcessed = 0;

    TreeTimingReporter(String msg) {
      this.msg = msg;
    }
    
    public void report() {
      StringBuffer sb = new StringBuffer();
      StringBuffer sb2 = new StringBuffer();
      sb.append(msg);
      sb.append(":  ");
      sb.append(Long.toString(m_sumTime/m_count));
      sb.append(" ms");
      sb2.append(m_sumTime);
      sb2.append(" ms");
      if (bytesProcessed > 0) {
	sb.append(",  ");
	sb.append(rateString(bytesProcessed, m_sumTime));
	sb.append(" B/ms");
	sb2.append(", ");
	sb2.append(bytesProcessed);
	sb2.append(" bytes");
      }
      if (filesProcessed > 0) {
	sb.append(",  ");
	sb.append(rateString(filesProcessed, m_sumTime));
	sb.append(" ");
	sb.append("files/ms");
	sb2.append(", ");
	sb2.append(filesProcessed);
	sb2.append(" files");
      }
      if (m_count != 1) {
	sb.append("\n  (");
	sb.append(m_count);
	sb.append(" iterations, ");
	sb.append(sb2.toString());
	sb.append(")");
      }
      System.out.println(sb.toString());
    }
  }

  private void run(String[] argv) throws Exception {
    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if (arg.startsWith("-f")) {
	  root = argv[++ix];
	} else if (arg.equals("-l")) {
	  label = argv[++ix];
	} else if (arg.equals("-ha")) {
	  alg = argv[++ix];
	} else if (arg.equals("-b")) {
	  bufSizeStr = argv[++ix];
	  bufSize = Integer.parseInt(bufSizeStr);
	} else if (arg.equals("-d")) {
	  duration = argv[++ix];
	  setDuration(Integer.parseInt(duration));
	} else if (arg.startsWith("-a")) {
	  isAll = true;
	  if (arg.equals("-at")) {
	    isThread = true;
	  }
	} else if (arg.equals("-p")) {
	  isPrint = true;
	} else if (arg.equals("-z")) {
	  isGzip = true;
	} else if (arg.equals("-n")) {
	  isNio = true;
	  ioMsg = "nio";
	} else if (arg.equals("-nb")) {
	  isNio = true;
	  bufferMode = 'b';
	  ioMsg = "nio ByteBuffer";
	} else if (arg.equals("-nd")) {
	  isNio = true;
	  bufferMode = 'd';
	  ioMsg = "nio DirectByteBuffer";
	} else if (arg.equals("-nm")) {
	  isNio = true;
	  bufferMode = 'm';
	  ioMsg = "nio MappedBuffer";
	} else if (arg.equals("-nw")) {
	  isNio = true;
	  bufferMode = 'w';
	  ioMsg = "nio Wrapped BArray";
	} else if (arg.equals("-w")) {
	  msg = "Tree walk";
	  isWalk = true;
	} else if (arg.equals("-o")) {
	  isOpen = true;
	  msg = "Open files";
	} else if (arg.equals("-r")) {
	  msg = "Read files";
	  isRead = true;
	} else if (arg.startsWith("-h")) {
	  msg = "Hash files";
	  isHash = isRead = true;
	  if (!arg.equals("-h")) {
	    try {
	      String s = arg.substring(2);
	      hashes = Integer.parseInt(s);
	      msg += "(" + hashes + ")";
	    } catch (Exception e) {
	      usage(e.getMessage());
	    }
	  }
// 	} else if (arg.equals("-hh")) {
// 	  isDaemonHash = isRead = isHash = true;
// 	  msg = "Daemon hash files";
	} else {
	  usage("Unknown arg: " + arg);
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }
    if (root == null) {
	usage("Specify file or root with  -f file");
    }
    rootFile = new File(root);
    if (!rootFile.exists()) {
      usage("No such file or tree: " + rootFile);
    }

    if (!(isAll | isWalk | isRead | isOpen)) {
      usage("Must specify one of -w, -o, -r, -h");
    }
    if (isAll) {
      doAll();
    } else {
      setUp();
      timeIt();
      if (isHash && isPrint) {
	for (int ix = 0; ix < hashes; ix++) {
	  System.out.println("Hash: " + byteString(hashers[ix].digest()));
	}
      }
      tearDown();
    }
  }

  String byteString(byte[] a) {
    return String.valueOf(B64Code.encode(a));
  }

  private void doAll() throws Exception {
    String[] argSets ={
      "-w", "-o",
      "-r", "-r -n", "-r -nb", "-r -nd", "-r -nm",
      "-h", "-h -n", "-h -nb", "-h -nd", "-h -nm",
    };
    for (int ix = 0; ix < argSets.length; ix++) {
      StringBuffer sb = new StringBuffer();
      sb.append("-f ");
      sb.append(root);
      if (label != null) {
	sb.append(" -l ");
	sb.append(label);
      }
      if (duration != null) {
	sb.append(" -d ");
	sb.append(duration);
      }
      if (bufSizeStr != null) {
	sb.append(" -b ");
	sb.append(bufSizeStr);
      }
      if (isPrint) {
	sb.append(" -p");
      }
      sb.append(" ");
      sb.append(argSets[ix]);
      final String args = sb.toString();
      log.debug3("args: " + args);
      if (isThread) {
	Thread t = new Thread(new Runnable() {
	    public void run() {
	      doOne(args);
	    }});
	t.start();
	t.join();
      } else {
	doOne(args);
      }
    }
  }

  private void doOne(String args) {
    TimeFileIO tf = new TimeFileIO();
    List arglist = StringUtil.breakAt(args, " ");
    try {
      tf.run((String[])arglist.toArray(new String[0]));
    } catch (Exception e) {
      log.warning("Exception running: " + args, e);
      System.gc();
    }
  }

  public static void main(String[] argv) throws Exception {
    TimeFileIO tf = new TimeFileIO();
    tf.run(argv);
  }

  private void usage() {
    PrintStream o = System.out;
    o.println("Usage: java " + this.getClass().getName() +
	      " -f file  verb  [iomode]  options ...");
    o.println("   -f file     name of file or root of tree to process");
    o.println("  Verb.  One of:");
    o.println("   -w      walk tree only, don't open files");
    o.println("   -o      open files only, don't read");
    o.println("   -r      read files");
    o.println("   -h      hash files");
    o.println("   -hn     hash files n times in parallel ");
    o.println("   -z      wrap InputStream in GZIPInputStream");
    o.println("   -a      run all tests (mostly)");
    o.println("   -at      run all tests sequentially in separate threads");
    o.println("  I/O Mode.  One of:");
    o.println("   (none)  use Stream I/O (InputStream)");
    o.println("   -n      use NIO Channel");
    o.println("   -nb     use NIO ByteBuffer");
    o.println("   -nd     use NIO DirectByteBuffer");
    o.println("   -nm     use NIO MappedByteBuffer");
    o.println("   -nb     use NIO wrapped ByteBuffer");
    o.println("  Options,  Any of:");
    o.println("   -ha alg     hash algorithm (default "+DEFAULT_ALGORITHM+")");
    o.println("   -b size     buffer size (default 1MB)");
    o.println("   -d msec     test duration (default "+DEFAULT_DURATION + ")");
    o.println("   -l label    label results");
    o.println("   -p          print hash");
    System.exit(2);
  }

  private void usage(String msg) {
    System.out.println(msg);
    usage();
  }

}
