package org.lockss.util;

import java.io.*;

import org.apache.commons.collections.primitives.RandomAccessLongList;
import org.apache.commons.io.IOUtils;

public class FileBackedLongList extends RandomAccessLongList {

  protected static final int BUFFER = 1024 * 1024;
  
  protected static final int BYTES = 8;

  protected int size;
  
  protected File file;
  
  protected boolean deleteFile;
  
  protected RandomAccessFile raf;

  public FileBackedLongList() {
    this(createTempFile());
    this.deleteFile = true;
  }
  
  public FileBackedLongList(String file) {
    this(new File(file));
  }
  
  public FileBackedLongList(File file) throws IOError {
    try {
      this.size = 0;
      this.file = file;
      this.deleteFile = false; // reset by some constructors
      this.raf = new RandomAccessFile(file, "rw");
      this.raf.setLength(0L); // truncates and seeks to 0L in one operation
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }

  @Override
  public boolean add(long element) throws IOError {
    try {
      seekOffset(raf.length());
      raf.writeLong(element);
      ++size;
      return true;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }

  @Override
  public void add(int index, long element) throws IOError {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      long target = index * BYTES;
      long off = raf.length();
      if (off > target) {
        byte[] buf = new byte[BUFFER];
        while (off > target) {
          int len = (off - target > BUFFER) ? BUFFER : (int)(off - target);
          off -= len;
          seekOffset(off);
          raf.read(buf, 0, len);
          seekOffset(off + BYTES);
          raf.write(buf, 0, len);
        }
      }
      seekIndex(index);
      raf.writeLong(element);
      ++size;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public long get(int index) throws IOError {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      seekIndex(index);
      return raf.readLong();
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }

  @Override
  public long removeElementAt(int index) throws IOError {
    try {
      long ret = get(index); // does the bounds check
      long target = raf.length() - BYTES;
      long off = index * BYTES;
      if (off < target) {
        byte[] buf = new byte[BUFFER];
        while (off < target) {
          int len = (target - off > BUFFER) ? BUFFER : (int)(target - off);
          seekOffset(off + BYTES);
          raf.read(buf, 0, len);
          seekOffset(off);
          raf.write(buf, 0, len);
          off += len;
        }
      }
      raf.setLength(target);
      --size;
      return ret;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }  
    
  public void release() {
    IOUtils.closeQuietly(raf);
    if (deleteFile) {
      file.delete();
    }
    size = -1;
  }
  
  @Override
  public long set(int index, long element) {
    try {
      long ret = get(index); // does bounds check but reads past offset
      seekIndex(index);
      raf.writeLong(element);
      return ret;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public int size() {
    return size;
  }

  @Override
  protected void finalize() throws Throwable {
    release();
  }
  
  protected void seekIndex(int index) throws IOError {
    seekOffset(index * BYTES);
  }
  
  protected void seekOffset(long offset) throws IOError {
    try {
      if (raf.getFilePointer() != offset) {
        raf.seek(offset);
      }
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }

  protected static File createTempFile() throws IOError {
    try {
      File ret = File.createTempFile(FileBackedLongList.class.getSimpleName(), ".bin");
      ret.deleteOnExit();
      return ret;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
}
