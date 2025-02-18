/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

lexer grammar TdbLexer;

@lexer::header {

/* ===================================================================

WARNING - WARNING - WARNING - WARNING - WARNING - WARNING - WARNING

If you are modifying a Java file with this warning in it, you are
making changes in the wrong place. This Java file was generated by
ANTLR from a grammar file. You should be editing the grammar file
instead.

=================================================================== */

}

@lexer::members {

  private static void syntaxError(String src, int line, int col, String fmt, Object... args) {
    String msg = String.format("%s:%d:%d: %s", src, line, col, String.format(fmt, args));
    throw new RuntimeException(msg);
  }

  enum SMode { NONE, AU, ANGLE, EQUALS }
  SMode sMode = SMode.NONE;
  SMode getSMode() { return sMode; }
  void setSMode(SMode sMode) { this.sMode = sMode; }
  boolean isNone() { return getSMode() == SMode.NONE; }
  void modeNone() { setSMode(SMode.NONE); }
  boolean isAu() { return getSMode() == SMode.AU; }
  void modeAu() { setSMode(SMode.AU); }
  boolean isAngle() { return getSMode() == SMode.ANGLE; }
  void modeAngle() { setSMode(SMode.ANGLE); }
  boolean isEquals() { return getSMode() == SMode.EQUALS; }
  void modeEquals() { setSMode(SMode.EQUALS); }

  enum SType { NONE, QUOTED, BARE, EMPTY }
  SType sType = SType.NONE;
  SType getSType() { return sType; }
  void setSType(SType sType) { this.sType = sType; }
  void typeQuoted() { setSType(SType.QUOTED); }
  boolean isQuoted() { return getSType() == SType.QUOTED; }
  void typeBare() { setSType(SType.BARE); }
  boolean isBare() { return getSType() == SType.BARE; }
  void typeEmpty() { setSType(SType.EMPTY); }
  boolean isEmpty() { return getSType() == SType.EMPTY; }

  boolean emitEpsilon = false;
  java.util.LinkedList<Token> myTokens = new java.util.LinkedList<Token>();

  boolean expectString() {
    return isAngle() || isEquals();
  }

  void adjustStringMode() {
    if (isAngle()) {
      modeAu();
    }
    else if (isEquals()) {
      modeNone();
    }
  }

  boolean isQString() {
    CharStream cs = getInputStream();
    for (int i = 1 ; /*nothing*/ ; ++i) {
      switch (cs.LA(i)) {
        case ' ': case '\t': break;
        case '"': typeQuoted(); return true;
        default: return false;
      }
    }
  }

  boolean isBString() {
    if (isQuoted()) {
      return false;
    }
    CharStream cs = getInputStream();
    for (int i = 1 ; /*nothing*/ ; ++i) {
      switch (cs.LA(i)) {
        case ' ': case '\t': break;
        case '\\':
          switch (cs.LA(i + 1)) {
            case '\\': case ';': case '>': case '#': case '"': case ' ': typeBare(); return true;
            default: return false;
          }
        case ';': case '>': case '#': case '\r': case '\n': case EOF: return false;
        default: typeBare(); return true;
      }
    }
  }

  boolean isEmptyBString() {
    if (isBare() || isQuoted()) {
      return false;
    }
    CharStream cs = getInputStream();
    for (int i = 1 ; /*nothing*/ ; ++i) {
      switch (cs.LA(i)) {
        case ' ': case '\t': break;
        case ';': case '>': case '#': case '\r': case '\n': case EOF: typeEmpty(); return true;
        default: return false;
      }
    }
  }

  void processString() {
    switch (getSType()) {
      case QUOTED: processQString(); adjustStringMode(); break;
      case BARE: processBString(); adjustStringMode(); break;
      case EMPTY: processEmptyBString(); break;
      default: throw new RuntimeException("Error: expected string but got invalid string type");
    }
    setSType(SType.NONE);
  }
  void processQString() {
    String ret = getText().trim();
    ret = ret.substring(1, ret.length() - 1);
    int ind = ret.indexOf('\\');
    if (ind >= 0) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0 ; i < ret.length() ; ++i) {
        char ch = ret.charAt(i);
        switch (ch) {
          case '\\':
            char ch2 = ret.charAt(i + 1);
            switch (ch2) {
              case '\\': case '"':
                sb.append(ch2);
                break;
              default:
                syntaxError(getSourceName(), getLine(), getCharPositionInLine(),
                            "Bad quoted string escape: %s", getCharErrorDisplay(ch2));
            }
            ++i;
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      ret = sb.toString();
    }
    setText(ret);
  }

  void processBString() {
    String ret = getText().trim();
    if (ret.charAt(ret.length() - 1) == '\\') {
      ret = ret + " "; // re-inject trailing escaped space
    }
    int ind = ret.indexOf('\\');
    if (ind >= 0) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0 ; i < ret.length() ; ++i) {
        char ch = ret.charAt(i);
        switch (ch) {
          case '\\':
            char ch2 = ret.charAt(i + 1);
            switch (ch2) {
              case '\\': case ';': case '>': case '#': case ' ':
                sb.append(ch2);
                break;
              case '"':
                if (i == 0) {
                  sb.append(ch2);
                  break;
                }
                // else drop down to error below (only valid at beginning of string)
              default:
                syntaxError(getSourceName(), getLine(), getCharPositionInLine(),
                            "Bad bare string escape: %s", getCharErrorDisplay(ch2));
            }
            ++i;
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      ret = sb.toString();
    }
    setText(ret);
  }

  void processEmptyBString() {
    adjustStringMode();
    String ret = getText();
    emitEpsilon = true;
    char ch = ret.charAt(ret.length() - 1);
    switch (ch) {
      case ';': setType(SEMICOLON); actionSemicolon(); break;
      case '>': setType(ANGLE_CLOSE); actionAngleClose(); break;
      case '#': setType(COMMENT); pushMode(COMMENT_MODE); skip(); break;
      case '\r': case '\n': setType(WHITESPACE); skip(); break;
      default: syntaxError(getSourceName(), getLine(), getCharPositionInLine(),
                           "Expected end of string but got %s", getCharErrorDisplay(ch));
    }
  }

  @Override
  public void emit(Token token) {
    if (emitEpsilon) {
      emitEpsilon = false;
      Token epsilon = getTokenFactory().create(_tokenFactorySourcePair,        // source pair
                                               STRING,                         // type
                                               "",                             // text
                                               token.getChannel(),             // channel
                                               token.getStartIndex(),          // start index
                                               token.getStartIndex(),          // stop index
                                               token.getLine(),                // line
                                               token.getCharPositionInLine()); // column
      myTokens.add(epsilon);
    }
    myTokens.addLast(token);
    setToken(token);
  }

  @Override
  public Token nextToken() {
    super.nextToken();
    if (myTokens.isEmpty()) {
      return emitEOF();
    }
    return myTokens.removeFirst();
  }

  void actionSemicolon() {
    if (isAu()) {
      modeAngle();
    }
  }

  void actionAngleOpen() {
    if (isAu()) {
      modeAngle();
    }
  }

  void actionAngleClose() {
    if (isAu()) {
      modeNone();
    }
  }

}

// Order matters; strings first to prevent other tokens from matching

STRING       : {expectString() && isQString()}? [ \t]* '"' ( '\\' [\\"] | ~[\\"] )* '"' {processString();}
             | {expectString() && isBString()}? [ \t]* ('\\' [" \\;>#] | ~[ \t"\\;>#\r\n] ) ('\\' [ \\;>#] | ~[\\;>#\r\n] )* {processString();}
             | {expectString() && isEmptyBString()}? [ \t]* [;>#\r\n] {processString();}
             ;

PUBLISHER    : 'publisher' ;
TITLE        : 'title' ;
AU           : 'au' {modeAu();} ;
IMPLICIT     : 'implicit' ;
CURLY_OPEN   : '{' ;
CURLY_CLOSE  : '}' ;
ANGLE_OPEN   : '<' {actionAngleOpen();} ;
ANGLE_CLOSE  : '>' {actionAngleClose();} ;
SEMICOLON    : ';' {actionSemicolon();} ;
EQUALS       : '=' {modeEquals();} ;

IDENTIFIER   : [a-zA-Z] [a-zA-Z0-9_.]* ( '[' [a-zA-Z0-9] [a-zA-Z0-9_./]* ']' )? ;

WHITESPACE   : {!expectString()}? [ \t\r\n]+ -> skip ;

COMMENT      : '#' -> more, pushMode(COMMENT_MODE) ;

/*
 * mode COMMENT_MODE
 */

mode COMMENT_MODE;

COMMENT_SKIP : ~[\r\n]+ -> more ;
COMMENT_END  : [\r\n] -> type(COMMENT), skip, popMode ;
