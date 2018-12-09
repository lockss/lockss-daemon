package org.lockss.util;

import java.io.*;
import java.util.*;

import org.apache.commons.collections.primitives.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class FileBackedList<E> extends AbstractList<E> {

  protected File file;
  
  protected boolean deleteFile;
  
  protected RandomAccessFile raf;
  
  protected LongList offsets;
  
  protected ArrayLongList arrayLongList;
  
  protected FileBackedLongList fileBackedLongList;
  
  public FileBackedList() throws IOError {
    this(Collections.<E>emptyIterator(),
         createTempFile());
    this.deleteFile = true;
  }

  public FileBackedList(String filePath) throws IOError {
    this(Collections.<E>emptyIterator(),
         new File(filePath));
  }
  
  public FileBackedList(File file) throws IOError {
    this(Collections.<E>emptyIterator(),
         file);
  }
  
  public FileBackedList(Iterator<E> iterator) throws IOError {
    this(iterator,
         createTempFile());
    this.deleteFile = true;
  }
  
  public FileBackedList(Collection<E> coll) throws IOError {
    this(coll.iterator(),
         createTempFile());
    this.deleteFile = true;
  }

  public FileBackedList(Iterator<E> iterator,
                        String filePath)
      throws IOError {
    this(iterator,
         new File(filePath));
  }
  
  public FileBackedList(Collection<E> coll,
                        String filePath)
      throws IOError {
    this(coll.iterator(),
         new File(filePath));
  }
  
  public FileBackedList(Collection<E> coll,
                        File file) 
      throws IOError {
    this(coll.iterator(),
         file);
  }
  
  public FileBackedList(Iterator<E> iterator,
                        File file) 
      throws IOError {
    try {
      this.file = file;
      this.deleteFile = false; // reset by some constructors
      this.arrayLongList = new ArrayLongList() {
        @Override
        public void clear() {
          // Commons Primitives 1.0 doesn't define this
          // Parent class horribly inefficient
          // Slightly less inefficient kludge
          for (int i = size() - 1 ; i >= 0 ; --i) {
            removeElementAt(i);
          }
        }
      };
      this.fileBackedLongList = null;
      this.offsets = arrayLongList;
      this.raf = new RandomAccessFile(file, "rw");
      this.raf.setLength(0L); // truncates and seeks to 0L in one operation
      populate(iterator);
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public void add(int index, E element) throws IOError {
    if (index < 0 || index > size()) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      offsets.add(index, append(element));
      if (size() > 1000000 && fileBackedLongList == null) {
        // Starting to get too large for main memory; go to disk also
        fileBackedLongList = new FileBackedLongList(file.getPath() + ".longs");
        for (LongIterator iter = arrayLongList.iterator() ; iter.hasNext() ; ) {
          fileBackedLongList.add(iter.next());
        }
        arrayLongList.clear();
        arrayLongList.trimToSize();
        arrayLongList = null;
        offsets = fileBackedLongList;
      }
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  public void release() {
    IOUtils.closeQuietly(raf);
    raf = null;
    if (deleteFile) {
      file.delete();
    }
    file = null;
    offsets = null;
    if (arrayLongList != null) {
      arrayLongList.clear();
      arrayLongList.trimToSize();
      arrayLongList = null;
    }
    if (fileBackedLongList != null) {
      fileBackedLongList.release();
      fileBackedLongList = null;
    }
  }
  
  @Override
  public E get(int index) throws IOError {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      long off = offsets.get(index);
      if (raf.getFilePointer() != off) {
        raf.seek(off);
      }
      int len = raf.readInt();
      byte[] bytes = new byte[len];
      raf.read(bytes);
      return (E)fromBytes(bytes);
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    release();
  }
  
  @Override
  public E remove(int index) {
    E ret = get(index); // does the bounds check
    offsets.removeElementAt(index);
    return ret;
  }
  
  @Override
  public E set(int index, E element) throws IOError {
    E ret = get(index); // does the bounds check
    try {
      offsets.set(index, append(element));
      return ret;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public int size() {
    return offsets.size();
  }

  protected long append(E element) throws IOException {
    long off = raf.length();
    if (raf.getFilePointer() != off) {
      raf.seek(off);
    }
    byte[] bytes = toBytes(element);
    raf.writeInt(bytes.length);
    raf.write(bytes);
    return off;
  }
  
  protected void populate(Iterator<E> iterator) throws IOException {
    while (iterator.hasNext()) {
      offsets.add(append(iterator.next()));
    }
  }
  
  protected static File createTempFile() throws IOError {
    try {
      File tempFile = File.createTempFile(FileBackedList.class.getSimpleName(), ".bin");
      tempFile.deleteOnExit();
      return tempFile;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  protected static byte[] toBytes(Object obj) throws IOError {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      return baos.toByteArray();
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  protected static Object fromBytes(byte[] bytes) throws IOError {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch (ClassNotFoundException | IOException exc) {
      throw new IOError(exc);
    }
  }
  
}
