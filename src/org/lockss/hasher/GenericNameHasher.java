/*
 * $Id: GenericNameHasher.java,v 1.1 2002-10-31 01:48:59 troberts Exp $
 */

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

package org.lockss.hasher;
import java.security.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * General class to handle name hashing
 */
public class GenericNameHasher implements CachedUrlSetHasher{
  public static final char DELIMITER= '\n';
  public static final String DELIMITER_STRING= DELIMITER+"";

  MessageDigest digest;
  CachedUrlSet cus;
  Iterator nameIterator;
  byte[] curName = null;
  int curIdx = -1;
  boolean isFinished = false;
  boolean firstElement = true; //are we on the first name, so we can skip the delimiter

  /**
   * Create a GenericNameHasher to take names from cus and feed them into digest
   * @param cus {@link CachedUrlSet} to hash
   * @param digest {@link java.security.MessageDigest} to feed bytes into
   */
  public GenericNameHasher(CachedUrlSet cus, MessageDigest digest){
    this.digest = digest;
    this.cus = cus;
    nameIterator = cus.flatSetIterator();
  }

  /**
   * @param numBytes number of bytes to has in this step
   * @return number of bytes actually hashed; only less than numBytes if we run out of names
   * to hash
   */
  public int hashStep(int numBytes){
    if (digest == null || cus == null || nameIterator == null){
      isFinished = true;
      return 0;
    }
    int bytesLeftToHash = numBytes;

    while (bytesLeftToHash > 0){
      if (curName == null || curName.length <= curIdx){
	if (!nameIterator.hasNext()){
	  this.isFinished = true;
	  return numBytes - bytesLeftToHash;
	}
	if (!firstElement){
	  digest.update(DELIMITER_STRING.getBytes());
	  bytesLeftToHash--;
	}
	else{
	  firstElement = false;
	}
	String curNameStr = getCUSName((CachedUrlSet)nameIterator.next());
	curName = (curNameStr).getBytes();
	curIdx = 0;
      }

      //hash up to numBytesToReturn bytes
      int remNameBytes = curName.length - curIdx;
      int len = 
	bytesLeftToHash < (remNameBytes) ? bytesLeftToHash : remNameBytes;

      digest.update(curName, curIdx, len);
      curIdx += len;
      bytesLeftToHash -= len;
    } 
    return numBytes - bytesLeftToHash;
  }
    

  /**
   * @return true if all the names have been hashed
   */
  public boolean finished(){
    return isFinished;
  }

  protected static String getCUSName(CachedUrlSet cus){
    if (cus == null){
      return null;
    }
    CachedUrlSetSpec cuss = cus.getSpec();
    List names = cuss.getPrefixList();
    
    //XXX done this way until we figure out how to map a CUS to a name
    return (String)names.get(0);
  }
}


