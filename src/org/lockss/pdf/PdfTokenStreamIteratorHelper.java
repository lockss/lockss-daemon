/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.pdf;

import java.util.*;

public abstract class PdfTokenStreamIteratorHelper<TokenType extends PdfToken,
                                                   OperandsAndOperatorType extends PdfOperandsAndOperator<TokenType>,
                                                   OperandsAndOperatorIteratorType extends Iterator<OperandsAndOperatorType>,
                                                   ElementType>
    implements Iterator<ElementType> {

  protected ElementType next;
  
  protected Deque<OperandsAndOperatorIteratorType> iterators;
  
  protected Set<Object> processed;
  
  public PdfTokenStreamIteratorHelper(OperandsAndOperatorIteratorType initial) {
    this.next = null;
    this.processed = new HashSet<>(); // note: "initial" is not represented in the set
    this.iterators = new ArrayDeque<>();
    iterators.add(initial);
  }
  
  @Override
  public boolean hasNext() throws PdfRuntimeException {
    if (iterators.isEmpty()) {
      return false;
    }
    if (next == null) {
      try {
        findNext();
      }
      catch (Exception exc) {
        throw new PdfRuntimeException(exc);
      }
    }
    return next != null;
  }
  
  @Override
  public ElementType next() throws PdfRuntimeException {
    if (hasNext()) {
      ElementType ret = next;
      next = null;
      return ret;
    }
    else {
      throw new NoSuchElementException();
    }
  }
  
  protected void findNext() throws Exception {
    while (!iterators.isEmpty()) {
      OperandsAndOperatorIteratorType iterator = iterators.getFirst();
      while (iterator.hasNext()) {
        OperandsAndOperatorType oao = iterator.next();
        next = findNext(iterator, oao);
        if (next != null) {
          return;
        }
      }
      iterators.removeFirst(); // exited inner while: this iterator is exhausted
    }
  }
  
  protected abstract ElementType findNext(OperandsAndOperatorIteratorType iterator,
                                          OperandsAndOperatorType oao)
      throws Exception;

}
