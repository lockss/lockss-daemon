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

public class BaseOperandsAndOperator<OperandType, OperatorType> {

  protected List<OperandType> operands;
  
  protected OperatorType operator;
  
  public BaseOperandsAndOperator() {
    this(null, null);
  }
  
  public BaseOperandsAndOperator(List<OperandType> operands) {
    this(operands, null);
  }
  
  public BaseOperandsAndOperator(List<OperandType> operands, OperatorType operator) {
    if (operands == null) {
      this.operands = new ArrayList<>(3);
    }
    else {
      this.operands = new ArrayList<>(operands.size());
      this.operands.addAll(operands);
    }
    this.operator = operator;
  }
  
  public BaseOperandsAndOperator(OperatorType operator) {
    this(null, operator);
  }
  
  public List<OperandType> getOperands() {
    return operands;
  }
  
  public OperatorType getOperator() {
    return operator;
  }
  
  public void setOperator(OperatorType operator) {
    this.operator = operator;
  }
  
}