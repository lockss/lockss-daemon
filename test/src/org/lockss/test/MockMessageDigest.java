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

package org.lockss.test;
import java.security.*;
import java.util.*;

/**
 * This is a mock version of <code>java.security.MessageDigest</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockMessageDigest extends MessageDigest implements Cloneable {

  LinkedList<Byte> inputList;


  public MockMessageDigest(){
    super("Mock hash algorithm");
    inputList = new LinkedList<Byte>();
  }

  /**
   * Mock implementation of the standard Message digest function (stores bytes on a queue for
   * later checking via <code>getUpdatedByte<code> or <code>getUpdatedBytes</code>)
   * @param input byte to hash
   * @see MockMessageDigest#getUpdatedByte()
   * @see MockMessageDigest#getUpdatedBytes(byte[])
   */
  public void update(byte input){
    inputList.add(new Byte(input));
  }

  /**
   * Mock implementation of the standard Message digest function (stores bytes on a queue for
   * later checking via <code>getUpdatedByte<code> or <code>getUpdatedBytes</code>)
   * @param input byte array to hash
   * @see MockMessageDigest#getUpdatedByte()
   * @see MockMessageDigest#getUpdatedBytes(byte[])
   */
  public void update(byte[] input){
    for (int i=0; i<input.length; i++){
      inputList.add(new Byte(input[i]));
    }
  }

  /**
   * Mock implementation of the standard Message digest function (stores bytes on a queue for
   * later checking via <code>getUpdatedByte<code> or <code>getUpdatedBytes</code>)
   * @param input byte array to hash
   * @param offset index of first element in array to start hashing
   * @param len number of elements in array to hash
   * @see MockMessageDigest#getUpdatedByte()
   * @see MockMessageDigest#getUpdatedBytes(byte[])
   */
  public void update(byte[] input, int offset, int len){
    for (int i=offset; i<offset+len; i++){
      inputList.add(new Byte(input[i]));
    }
  }



  /**
   * Method to retrieve bytes fed in by <code>update</code>
   * @return next byte in the queue or -1 if none are left
   * @see MockMessageDigest#update(byte)
   * @see MockMessageDigest#update(byte[])
   */
  public byte getUpdatedByte(){
    if (inputList.size() <= 0){
      return -1;
    }
    Byte ret = (Byte)inputList.remove(0);
    return ret.byteValue();
  }

  /**
   * Method to retrieve bytes fed in by <code>update</code>
   * @return number of bytes written to output
   * @see MockMessageDigest#update(byte)
   * @see MockMessageDigest#update(byte[])
   */
  public byte[] getUpdatedBytes(){
    byte[] res = new byte[inputList.size()];
    getUpdatedBytes(res);
    return res;
  }

  /**
   * Method to retrieve bytes fed in by <code>update</code>
   * @param output byte[] to fill with bytes from the queue
   * @return number of bytes written to output
   * @see MockMessageDigest#update(byte)
   * @see MockMessageDigest#update(byte[])
   */
  public int getUpdatedBytes(byte[] output){
    int length = Math.min(inputList.size(), output.length);

    for (int i=0; i<length; i++){
      output[i] = getUpdatedByte();
    }
    return length;
  }

  public String toString() {
    return "[MockMessageDigest: "+inputList+"]";
  }

  /**
   * Return the number of bytes we have left
   */
  public int getNumRemainingBytes() {
    return inputList.size();
  }

  protected void engineUpdate(byte input) {
    update(input);
  }

  protected void engineReset() {
    inputList = new LinkedList<Byte>();
  }

  protected void engineUpdate(byte[] input, int offset, int len) {
    if ((len>0) && (input.length>=offset+len)) {
      for (int ii=0; ii<len; ii++) {
        engineUpdate(input[offset+ii]);
      }
    }
  }

  protected byte[] engineDigest() {
    byte[] bytes = new byte[inputList.size()];
    int ix = 0;
    for (byte v : inputList) {
      bytes[ix++] = v;
    }
    return bytes;
  }

  /**
   * BlockHasher requires cloneable message digests. 
   */
  public Object clone() {
    MockMessageDigest retVal = new MockMessageDigest();
    retVal.inputList = (LinkedList)inputList.clone();
    return retVal;
  }
}
