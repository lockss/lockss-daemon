/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.util;

import java.io.*;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel.MapMode;

import org.lockss.test.LockssTestCase;

public class TestCountingRandomAccessFile extends LockssTestCase {

  public static class Grandparent {
    public void foo() {
      System.out.println("begin grandparent");
      System.out.println("end grandparent");
    }
  }
  public static class Parent extends Grandparent {
    public void foo() {
      System.out.println("begin parent");
      super.foo();
      System.out.println("end parent");
    }
  }
  public static class Child extends Parent {
    public void foo() {
      System.out.println("begin child");
      
      System.out.println("end child");
    }
  }
  
  public void testFoo() {
    Grandparent g = new Grandparent();
    g.foo();
  }
  
  public void doRandomAccessFile(RandomAccessFile raf) throws IOException {
    int size = 1_000_000;
    for (int i = 0 ; i < size ; ++i) {
      raf.writeLong(i);
    }
    for (int i = 0 ; i < size ; ++i) {
      int j = (i * 101 + 997) % size;
      if (j == size - 1) {
        continue;
      }
      raf.seek(j * 8);
      assertEquals(j, raf.readLong());
      if (raf.getFilePointer() != j + 8) {
        raf.seek((j * 8) + 8);
      }
      assertEquals(j + 1, raf.readLong());
    }
    for (int i = 0 ; i < size ; i += 997) {
      raf.seek(i * 8);
      raf.writeLong(-i);
    }
    for (int i = 0 ; i < size ; i += 997) {
      raf.seek(i * 8);
      assertEquals(-i, raf.readLong());
    }
  }
  
  public void testRandomAccessFile() throws Exception {
    File f = CountingRandomAccessFile.createTempFile();
    RandomAccessFile raf = new RandomAccessFile(f, "rw");
    doRandomAccessFile(raf);
    f.delete();
  }
  
  public void testReadWriteRandomAccessFile() throws Exception {
    CountingRandomAccessFile rwraf = new CountingRandomAccessFile();
    doRandomAccessFile(rwraf);
    rwraf.close();
  }
  
  public void testLongBufferRaf() throws Exception {
    File f = CountingRandomAccessFile.createTempFile();
    RandomAccessFile raf = new RandomAccessFile(f, "rw");
    doLongBuffer(raf);
    raf.close();
    f.delete();
  }
  
  public void testLongBufferRwraf() throws Exception {
    CountingRandomAccessFile rwraf = new CountingRandomAccessFile();
    doLongBuffer(rwraf);
    rwraf.close();
  }
  
  public void doLongBuffer(RandomAccessFile raf) throws Exception {
    int size = 1_000_000;
    LongBuffer buf = raf.getChannel().map(MapMode.READ_WRITE, 0, size * 8).asLongBuffer();
    for (int i = 0 ; i < size ; ++i) {
      buf.put(i);
      assertEquals((i+1)*8, raf.length());
    }
    for (int i = 0 ; i < size ; ++i) {
      int j = (i * 101 + 997) % size;
      if (j == size - 1) {
        continue;
      }
      assertEquals(j, buf.get(j));
      assertEquals(j + 1, buf.get(j + 1));
    }
    for (int i = 0 ; i < size ; i += 997) {
      buf.put(i, -i);
    }
    for (int i = 0 ; i < size ; i += 997) {
      assertEquals(-i, buf.get(i));
    }
  }
  
}
