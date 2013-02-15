#!/usr/bin/env python

# $Id: tdbparse.py,v 1.20 2013-02-15 19:54:39 thib_gc Exp $

__copyright__ = '''\
Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
'''

__version__ = '''0.4.8'''

from optparse import OptionGroup, OptionParser
import re
import sys
from tdb import *
import tdbq


###
### TdbparseSyntaxError
###

class TdbparseSyntaxError(Exception):
    def __init__(self, msg, filename, lineno, offset):
        super(Exception, self).__init__()
        self.msg = msg
        self.filename = filename
        self.lineno = lineno
        self.offset = offset

    def __str__(self):
        return "\"%s\", line %d.%d: %s" % (self.filename, self.lineno, self.offset, self.msg)


###
### TdbparseConstants
###

class TdbparseConstants:
    '''Constants associated with the tdbparse module.'''

    OPTION_TDBPARSE_ECHO_LINES = 'tdbparse-echo-lines'
    OPTION_TDBPARSE_ECHO_LINES_HELP = 'echo each line read by tdbparse to stderr'
    
    OPTION_TDBPARSE_ECHO_TOKENS = 'tdbparse-echo-tokens'
    OPTION_TDBPARSE_ECHO_TOKENS_HELP = 'echo each token built by tdbparse to stderr'
    

###
### TdbparseLiteral
###

class TdbparseLiteral:
    '''Literals encountered in TDB source text.'''

    PUBLISHER = 'publisher'
    TITLE = 'title'
    AU = 'au'
    IMPLICIT = 'implicit'
    SEMICOLON = ';'
    EQUAL = '='
    CURLY_OPEN = '{'
    CURLY_CLOSE = '}'
    ANGLE_OPEN = '<'
    ANGLE_CLOSE = '>'
    SQUARE_OPEN = '['
    SQUARE_CLOSE = ']'
    QUOTE_DOUBLE = '"'

###
### TdbparseToken
###

class TdbparseToken:
    '''Tokens encountered while parsing TDB source text.'''

    NONE = 1
    PUBLISHER =  2
    TITLE =  3
    AU =  4
    IMPLICIT =  5
    SEMICOLON = 6
    EQUAL = 7
    CURLY_OPEN =  8
    CURLY_CLOSE =  9
    ANGLE_OPEN =  10
    ANGLE_CLOSE =  11
    SQUARE_OPEN = 12
    SQUARE_CLOSE = 13
    STRING = 14
    IDENTIFIER = 15
    END_OF_FILE = 16
    
    def __init__(self, typ, line, col, val=None):
        '''Builds a new token of the given type, at the given line and
        column, initially with a value of None.
        
        typ: A token type. See the TdbparseToken class.
        line: A strictly positive line number.
        col: A strictly positive column number.
        cal: A value for the token. Default: None.'''
        object.__init__(self)
        self.__type = typ
        self.__line = line
        self.__col = col
        self.__value = val
    
    def type(self):
        '''Returns this token's type.'''
        return self.__type
    
    def set_type(self, typ):
        '''Sets this token's type.'''
        self.__type = typ
    
    def line(self):
        '''Return this token's line number.'''
        return self.__line
    
    def set_line(self, line):
        '''Sets this token's line number.'''
        self.__line = line
    
    def col(self):
        '''Return this token's column number.'''
        return self.__col
    
    def set_col(self, col):
        '''Sets this token's column number.'''
        self.__col = col
    
    def value(self):
        '''Returns this token's associated value (or None).'''
        return self.__value

    def set_value(self, value):
        '''Sets the associated value of this token to the given value.'''
        self.__value = value

    def clone(self):
        '''Returns a clone of this token.'''
        return TdbparseToken(self.type(), self.line(), self.col(), self.valeu())

    def translate(self):
        '''Translates this token's type into a human-readable string.'''
        return {TdbparseToken.NONE: '<none>',
                TdbparseToken.PUBLISHER: TdbparseLiteral.PUBLISHER,
                TdbparseToken.TITLE: TdbparseLiteral.TITLE,
                TdbparseToken.AU: TdbparseLiteral.AU,
                TdbparseToken.IMPLICIT: TdbparseLiteral.IMPLICIT,
                TdbparseToken.SEMICOLON: TdbparseLiteral.SEMICOLON,
                TdbparseToken.EQUAL: TdbparseLiteral.EQUAL,
                TdbparseToken.CURLY_OPEN: TdbparseLiteral.CURLY_OPEN,
                TdbparseToken.CURLY_CLOSE: TdbparseLiteral.CURLY_CLOSE,
                TdbparseToken.ANGLE_OPEN: TdbparseLiteral.ANGLE_OPEN,
                TdbparseToken.ANGLE_CLOSE: TdbparseLiteral.ANGLE_CLOSE,
                TdbparseToken.SQUARE_OPEN: TdbparseLiteral.SQUARE_OPEN,
                TdbparseToken.SQUARE_CLOSE: TdbparseLiteral.SQUARE_CLOSE,
                TdbparseToken.STRING: 'a string',
                TdbparseToken.IDENTIFIER: 'an identifier',
                TdbparseToken.END_OF_FILE: 'end of file'}.get(self.type()) or '<unknown: %d>' % (self.type(),)
                
    def __str__(self): return '<TdbparseToken line %d column %d: %s%s>' % (self.line(), self.col(), self.translate(), '' if self.value() is None else ' [%s]' % (self.value(),))

###
### TdbScanner
###

class TdbScanner(object):
    '''Implements a lexical analyzer for the TDB language.

    Initialized with an open file object. Each call to
    next() consumes one token from the input file.

    The constructor accepts keyword arguments which are interpreted
    as options. Option keys cannot begin with an underscore.'''

    RE_WHITE1 = re.compile(r'\s+')
    RE_WHITE0EOL = re.compile(r'\s*$')
    RE_KEYWORD = re.compile(r'(publisher|title|au|implicit)(\s|<|$)')
    RE_IDENTIFIER = re.compile(r'[a-zA-Z0-9_./]+')

    def __init__(self, file, options):
        '''Constructor.

        Keyword arguments are stored as options.'''
        object.__init__(self)
        self.__file = file
        self.__options = options
        self.__stringFlag = None
        self.__cur = ''
        self.__line = 0
        self.__col = 0
        self.__tok = TdbparseToken(TdbparseToken.NONE, 0, 0)
        self.__other_tok = TdbparseToken(TdbparseToken.NONE, 0, 0)

    def next(self):
        '''Consumes and returns one token from the input file.

        Returns a token of type TdbparseToken.END_OF_FILE the first
        time end of file is reached, then raises a runtime error.'''
        # Already at end of file
        if self.__tok.type() == TdbparseToken.END_OF_FILE: raise RuntimeError, 'already at end of file'
        # File is closed
        if self.__file.closed: raise RuntimeError, 'file already closed'
        # Maybe advance to next line or lines
        while TdbScanner.RE_WHITE0EOL.match(self.__cur):
            # Empty unquoted string
            if self.__stringFlag == TdbparseToken.EQUAL:
                self.__stringFlag = None
                self.__token(TdbparseToken.STRING)
                self.__tok.set_value('')
                return self.__tok
            self.__cur = self.__file.readline()
            self.__line = self.__line + 1
            self.__col = 1
            # End of file
            if self.__cur == '':
                self.__token(TdbparseToken.END_OF_FILE)
                return self.__tok
            # Remove line reader's trailing newline; idiom to avoid removing other meaningful whitespace
            if self.__cur[-1] == '\n': self.__cur = self.__cur[0:-1]
            # Optional debug output
            if self.__options is not None and self.__options.tdbparse_echo_lines: sys.stderr.write(self.__cur + '\n')
            match = TdbScanner.RE_WHITE1.match(self.__cur)
            if match: self.__move(match.end())
        # Skip initial whitespace
        ch = self.__cur[0]
        if ch == ' ' or ch == '\t':
            # Don't bother with regex for single whitespace (common)
            ch = self.__cur[1]
            if ch == ' ' or ch == '\t':
                match = TdbScanner.RE_WHITE1.match(self.__cur)
                if match: self.__move(match.end())
                ch = self.__cur[0]
            else: self.__move(1)
        # Strings
        if self.__stringFlag == TdbparseToken.EQUAL or self.__stringFlag == TdbparseToken.SEMICOLON:
            # Empty unquoted string
            if ch == TdbparseLiteral.SEMICOLON or ch == TdbparseLiteral.ANGLE_CLOSE:
                if self.__stringFlag == TdbparseToken.EQUAL:
                    self.__stringFlag = None
                else:
                    self.__stringFlag = TdbparseToken.AU
                self.__token(TdbparseToken.STRING)
                self.__tok.set_value('')
                return self.__tok
            # Quoted string
            if ch == TdbparseLiteral.QUOTE_DOUBLE: return self.__qstring()
            # Unquoted string
            return self.__ustring()
        # Single-character tokens
        if ch == TdbparseLiteral.SEMICOLON:
            if self.__stringFlag == TdbparseToken.AU:
                self.__stringFlag = TdbparseToken.SEMICOLON
            return self.__single(TdbparseToken.SEMICOLON)
        if ch == TdbparseLiteral.ANGLE_OPEN:
            if self.__tok.type() == TdbparseToken.AU:
                self.__stringFlag = TdbparseToken.SEMICOLON
            return self.__single(TdbparseToken.ANGLE_OPEN)
        if ch == TdbparseLiteral.ANGLE_CLOSE:
            self.__stringFlag = None
            return self.__single(TdbparseToken.ANGLE_CLOSE)
        if ch == TdbparseLiteral.EQUAL:
            self.__stringFlag = TdbparseToken.EQUAL
            return self.__single(TdbparseToken.EQUAL)
        if ch == TdbparseLiteral.SQUARE_OPEN: return self.__single(TdbparseToken.SQUARE_OPEN)
        if ch == TdbparseLiteral.SQUARE_CLOSE: return self.__single(TdbparseToken.SQUARE_CLOSE)
        if ch == TdbparseLiteral.CURLY_OPEN: return self.__single(TdbparseToken.CURLY_OPEN)
        if ch == TdbparseLiteral.CURLY_CLOSE: return self.__single(TdbparseToken.CURLY_CLOSE)
        # Keywords
        cur = self.__cur
        match = TdbScanner.RE_KEYWORD.match(cur)
        if match: return self.__keyword(match.group(1))
        # Identifiers
        match = TdbScanner.RE_IDENTIFIER.match(cur)
        if match:
            self.__token(TdbparseToken.IDENTIFIER)
            self.__tok.set_value(match.group(0))
            self.__move(match.end())
            return self.__tok
        # Syntax error
        self.__token(TdbparseToken.NONE)
        raise TdbparseSyntaxError("Unexpected syntax",
            self.file_name(), self.__tok.line(), self.__tok.col())

    def __move(self, n):
        '''Consumes n characters from the current line.'''
        self.__cur = self.__cur[n:]
        self.__col = self.__col + n

    def __keyword(self, keyword):
        '''Consumes one reserved keyword.

        The valid keywords are publisher, title, au and implicit.'''
        if keyword == TdbparseLiteral.AU:
            self.__token(TdbparseToken.AU)
            self.__stringFlag = TdbparseToken.AU
        elif keyword == TdbparseLiteral.TITLE: self.__token(TdbparseToken.TITLE)
        elif keyword == TdbparseLiteral.IMPLICIT: self.__token(TdbparseToken.IMPLICIT)
        elif keyword == TdbparseLiteral.PUBLISHER: self.__token(TdbparseToken.PUBLISHER)
        else:
            raise RuntimeError, 'internal error at line %d column %d: expected %s, %s, %s or %s but got %s' % (self.__tok.line(), self.__tok.col(), TdbparseLiteral.PUBLISHER, TdbparseLiteral.TITLE, TdbparseLiteral.AU, TdbparseLiteral.IMPLICIT, keyword)
        self.__move(len(keyword))
        return self.__tok

    def __single(self, token):
        '''Consumes a single-character token.

        The valid tokens are opening and closing curly, angle and
        square brackets, semicolons and equal signs.'''
        self.__token(token)
        self.__move(1)
        return self.__tok

    def __qstring(self):
        '''Consumes a quoted string, starting at the initial double
        quote and continuing to a matching double quote on the same
        line.

        Double quotes and backslashes must be escaped.'''
        # __bstring has been altered to count characters and escape the
        # string conditionally. The same should be done to __qstring,
        # but it's called very infrequently.
        self.__token(TdbparseToken.STRING)
        val = []
        maxindex = len(self.__cur)
        index = 1 # Consume the opening quote
        if index >= maxindex:
            raise TdbparseSyntaxError('run-on quoted string', self.file_name(), self.__tok.line(), self.__tok.col())
        ch = self.__cur[index]
        while ch != TdbparseLiteral.QUOTE_DOUBLE:
            if ch == '\\':
                index = index + 1 # Consume the backslash
                if index >= maxindex:
                    raise TdbparseSyntaxError('end-of-line after backslash', self.file_name(), self.__tok.line(), self.__tok.col()+index)
                ch = self.__cur[index]
                if ch not in '"\\':
                    raise TdbparseSyntaxError('invalid quoted string escape: %s' % ch, self.file_name(), self.__line, self.__col-1+index)
            val.append(ch)
            index = index + 1 # Consume the character
            if index >= maxindex:
                raise TdbparseSyntaxError('run-on quoted string', self.file_name(), self.__tok.line(), self.__tok.col()+index)
            ch = self.__cur[index]
        index = index + 1 # Consume the closing quote
        self.__move(index)
        self.__tok.set_value(''.join(val))
        if self.__stringFlag == TdbparseToken.EQUAL:
            self.__stringFlag = None
        elif self.__stringFlag == TdbparseToken.SEMICOLON:
            self.__stringFlag = TdbparseToken.AU
        return self.__tok

    def __ustring(self):
        '''Consumes an unquoted string, starting at the initial non-
        whitespace character and continuing to the end of the line,
        a semicolon, or a closing angle bracket.

        Semicolons, closing angle brackets, backslashes, leading and
        trailing spaces, and leading quote marks must be escaped.'''
        self.__token(TdbparseToken.STRING)
        cur = self.__cur
        index = 0
        maxindex = len(cur)
        escapes = []
        while index < maxindex:
          ch = cur[index]
          if ch == TdbparseLiteral.SEMICOLON or ch == TdbparseLiteral.ANGLE_CLOSE: break
          if ch == '\\':
            escapes.append(index)
            index = index + 1 # Count the backslash
            if index >= maxindex: raise TdbparseSyntaxError('run-on unquoted string', self.file_name(), self.__tok.line(), self.__tok.col()+index)
            ch = cur[index]
            if not (ch == TdbparseLiteral.SEMICOLON or ch == TdbparseLiteral.ANGLE_CLOSE \
                or ch == ' ' or ch == '\\' or (ch == '"' and index == 1)): raise TdbparseSyntaxError('invalid unquoted string escape: %s' % ch, self.file_name(), self.__line, self.__col-1+index)
          index = index + 1 # Count the character
        val = cur[0:index].rstrip()
        self.__move(index) # Consume the characters
        if len(escapes) > 0:
          val_seq = [c for c in val]
          if val_seq[-1] == '\\': val_seq.append(' ')
          for i in reversed(escapes): del val_seq[i]
          val = ''.join(val_seq)
        self.__tok.set_value(val)
        if self.__stringFlag == TdbparseToken.EQUAL:
            self.__stringFlag = None
        elif self.__stringFlag == TdbparseToken.SEMICOLON:
            self.__stringFlag = TdbparseToken.AU
        return self.__tok

    def __token(self, typ):
        '''Sets the current token to one with the given type and gives it
        the current line and column number.
        
        typ: A token type. See TdbparseToken.'''
        tok = self.__other_tok
        tok.set_type(typ)
        tok.set_line(self.__line)
        tok.set_col(self.__col)
        self.__tok, self.__other_tok = tok, self.__tok
        
    def file_name(self):
        if str(self.__file.__class__) == "StringIO.StringIO":
            return "<string>"
        return self.__file.name


###
### TdbParser
###

class TdbParser(object):
    '''Implements a syntactic analyzer for the TDB language.

    Initialized with a TdbScanner object. The parse() method is
    public.

    All other methods and variables are private.'''

    def __init__(self, scanner, options):
        self.__scanner = scanner
        self.__file_name = scanner.file_name()
        self.__options = options
        self.__initialize_data()
        self.__initialize_parser()

    def parse(self):
        '''title_database :
            publisher_container_or_publisher_block*
            TdbparseToken.END_OF_FILE
        ;'''
        if self.__token0.type() == TdbparseToken.END_OF_FILE:
            raise RuntimeError, 'already done parsing'
        while self.__token0.type() == TdbparseToken.CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TdbparseToken.END_OF_FILE)
        return self.__tdb

    def __publisher_container_or_publisher_block(self):
        '''publisher_container_or_publisher_block :
            publisher_container
        |
            publisher_block
        ;'''
        if self.__token1.type() == TdbparseToken.PUBLISHER:
            self.__publisher_block()
        else:
            self.__publisher_container()

    def __publisher_container(self):
        '''publisher_container :
            TdbparseToken.CURLY_OPEN
            assignment*
            publisher_container_or_publisher_block*
            TdbparseToken.CURLY_CLOSE
        ;'''
        self.__expect(TdbparseToken.CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]:
            self.__assignment()
        while self.__token0.type() == TdbparseToken.CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TdbparseToken.CURLY_CLOSE)
        self.__current_au.pop()

    def __publisher_block(self):
        '''publisher_block :
            TdbparseToken.CURLY_OPEN
            publisher
            assignment*
            title_container_or_title_block*
            TdbparseToken.CURLY_CLOSE
        ;'''
        self.__expect(TdbparseToken.CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        self.__publisher()
        while self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]:
            self.__assignment()
        while self.__token0.type() == TdbparseToken.CURLY_OPEN:
            self.__title_container_or_title_block()
        self.__expect(TdbparseToken.CURLY_CLOSE)
        self.__current_au.pop()

    def __title_container_or_title_block(self):
        '''title_container_or_title_block :
            title_container
        |
            title_block
        ;'''
        if self.__token1.type() == TdbparseToken.TITLE:
            self.__title_block()
        else:
            self.__title_container()

    def __title_container(self):
        '''title_container :
            TdbparseToken.CURLY_OPEN
            assignment*
            title_container_or_title_block*
            TdbparseToken.CURLY_CLOSE
        ;'''
        self.__expect(TdbparseToken.CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]:
            self.__assignment()
        while self.__token0.type() == TdbparseToken.CURLY_OPEN:
            self.__title_container_or_title_block()
        self.__expect(TdbparseToken.CURLY_CLOSE)
        self.__current_au.pop()

    def __title_block(self):
        '''title_block :
            TdbparseToken.CURLY_OPEN
            title
            assignment*
            au_container_or_au*
            TdbparseToken.CURLY_CLOSE
        ;'''
        self.__expect(TdbparseToken.CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        self.__title()
        while self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]:
            self.__assignment()
        while self.__token0.type() in [TdbparseToken.AU, TdbparseToken.CURLY_OPEN]:
            self.__au_container_or_au()
        self.__expect(TdbparseToken.CURLY_CLOSE)
        self.__current_au.pop()

    def __au_container_or_au(self):
        '''au_container_or_au :
            au_container
        |
            au
        ;'''
        if self.__token0.type() == TdbparseToken.AU:
            self.__au()
        elif self.__token0.type() == TdbparseToken.CURLY_OPEN:
            self.__au_container()
        else:
            assert False # This code is unreachable in the current grammar.
            raise TdbparseSyntaxError(
                'expected %s or %s but got %s' % 
                (TdbparseLiteral.AU, TdbparseLiteral.CURLY_OPEN,
                 self.__token0.translate()), 
                self.__file_name, self.__token0.line(),
                self.__token0.col())

    def __au_container(self):
        '''au_container :
            TdbparseToken.CURLY_OPEN
            assignment*
            au_container_or_au*
            TdbparseToken.CURLY_CLOSE
        ;'''
        self.__expect(TdbparseToken.CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]:
            self.__assignment()
        while self.__token0.type() in [TdbparseToken.AU, TdbparseToken.CURLY_OPEN]:
            self.__au_container_or_au()
        self.__expect(TdbparseToken.CURLY_CLOSE)
        self.__current_au.pop()

    def __assignment(self):
        '''assignment :
            simple_assignment
        |
            implicit
        ;'''
        assert self.__token0.type() in [TdbparseToken.IDENTIFIER, TdbparseToken.IMPLICIT]
        if self.__token0.type() == TdbparseToken.IDENTIFIER:
            self.__simple_assignment()
            key, val = self.__stack.pop()
            self.__current_au[-1].set(key, val)
        elif self.__token0.type() == TdbparseToken.IMPLICIT:
            self.__implicit()
        else:
            assert False # This code is unreachable in the current grammar.
            raise TdbparseSyntaxError(
                'expected %s or %s but got %s' % 
                (TdbparseLiteral.IDENTIFIER, TdbparseLiteral.IMPLICIT,
                 self.__token0.translate()), 
                self.__file_name, self.__token0.line(),
                self.__token0.col())

    def __identifier(self):
        '''identifier :
            TdbparseToken.IDENTIFIER
            ( TdbparseToken.SQUARE_OPEN TdbparseToken.IDENTIFIER TdbparseToken.SQUARE_CLOSE )?
        ;'''
        tokval = self.__token0.value() # Remember potential identifier
        self.__expect(TdbparseToken.IDENTIFIER)
        base = tokval
        if self.__accept(TdbparseToken.SQUARE_OPEN):
            tokval = self.__token0.value() # Remember potential identifier
            self.__expect(TdbparseToken.IDENTIFIER)
            self.__expect(TdbparseToken.SQUARE_CLOSE)
            self.__stack.append((base, tokval))
        else: self.__stack.append((base,))

    def __list_of_identifiers(self):
        '''list_of_identifiers :
            TdbparseToken.IDENTIFIER
            ( TdbparseToken.SEMICOLON TdbparseToken.IDENTIFIER )*
        ;'''
        lis = []
        self.__identifier()
        lis.append(self.__stack.pop())
        while self.__accept(TdbparseToken.SEMICOLON):
            self.__identifier()
            lis.append(self.__stack.pop())
        self.__stack.append(lis)

    def __simple_assignment(self):
        '''simple_assignment :
            identifier
            TdbparseToken.EQUAL
            TdbparseToken.STRING
        ;'''
        self.__identifier()
        self.__expect(TdbparseToken.EQUAL)
        tokval = self.__token0.value() # Remember potential string
        self.__expect(TdbparseToken.STRING)
        self.__stack.append((self.__stack.pop(), tokval))

    def __list_of_simple_assignments(self):
        '''list_of_simple_assignments :
            simple_assignment
            ( TdbparseToken.SEMICOLON simple_assignment )*
        ;'''
        lis = []
        self.__simple_assignment()
        lis.append(self.__stack.pop())
        while self.__accept(TdbparseToken.SEMICOLON):
            self.__simple_assignment()
            lis.append(self.__stack.pop())
        self.__stack.append(lis)

    def __list_of_strings(self):
        '''list_of_strings :
            TdbparseToken.STRING
            ( TdbparseToken.SEMICOLON TdbparseToken.STRING )*
        ;'''
        lis = []
        tokval = self.__token0.value() # Remember potential string
        self.__expect(TdbparseToken.STRING)
        lis.append(tokval)
        while self.__accept(TdbparseToken.SEMICOLON):
            tokval = self.__token0.value() # Remember potential string
            self.__expect(TdbparseToken.STRING)
            lis.append(tokval)
        self.__stack.append(lis)

    def __implicit(self):
        '''implicit :
            TdbparseToken.IMPLICIT
            TdbparseToken.ANGLE_OPEN
            list_of_identifiers
            TdbparseToken.ANGLE_CLOSE
        ;'''
        self.__expect(TdbparseToken.IMPLICIT)
        self.__expect(TdbparseToken.ANGLE_OPEN)
        self.__list_of_identifiers()
        self.__expect(TdbparseToken.ANGLE_CLOSE)
        self.__current_au[-1].set('$implicit', self.__stack.pop())


    def __au(self):
        '''au :
            TdbparseToken.AU
            TdbparseToken.ANGLE_OPEN
            list_of_strings
            TdbparseToken.ANGLE_CLOSE
        ;'''
        tokline, tokcol = self.__token0.line(), self.__token0.col() # Remember potential au token
        self.__expect(TdbparseToken.AU)
        self.__expect(TdbparseToken.ANGLE_OPEN)
        self.__list_of_strings()
        self.__expect(TdbparseToken.ANGLE_CLOSE)
        au = AU(self.__current_au[-1])
        au.set_title(self.__current_title)
        impl = self.__current_au[-1].get('$implicit')
        vals = self.__stack.pop()
        if len(impl) != len(vals):
            raise TdbparseSyntaxError('expected %d implicit assignments but got %d' % (len(impl), len(vals)), self.__file_name, tokline, tokcol)
        for key, val in zip(impl, vals):
            au.set(key, val)
        self.__tdb.add_au(au)

    def __title(self):
        '''title :
            TdbparseToken.TITLE
            TdbparseToken.ANGLE_OPEN
            list_of_simple_assignments
            TdbparseToken.ANGLE_CLOSE
        ;'''
        self.__expect(TdbparseToken.TITLE)
        self.__expect(TdbparseToken.ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TdbparseToken.ANGLE_CLOSE)
        self.__current_title = Title()
        self.__current_title.set_publisher(self.__current_publisher)
        for key, val in self.__stack.pop():
            self.__current_title.set(key, val)
        self.__tdb.add_title(self.__current_title)

    def __publisher(self):
        '''publisher :
            TdbparseToken.PUBLISHER
            TdbparseToken.ANGLE_OPEN
            list_of_simple_assignments
            TdbparseToken.ANGLE_CLOSE
        ;'''
        self.__expect(TdbparseToken.PUBLISHER)
        self.__expect(TdbparseToken.ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TdbparseToken.ANGLE_CLOSE)
        self.__current_publisher = Publisher()
        for key, val in self.__stack.pop():
            self.__current_publisher.set(key, val)
        self.__tdb.add_publisher(self.__current_publisher)

    def __initialize_parser(self):
        '''Initializes the parser.

        Generic enough for k-lookahead.'''
        self.__token0 = TdbparseToken(TdbparseToken.NONE, 0, 0)
        self.__token1 = TdbparseToken(TdbparseToken.NONE, 0, 0)
        tok = self.__scanner.next()
        if self.__options.tdbparse_echo_tokens: sys.stderr.write(str(tok) + '\n')
        self.__token0 = tok
        if tok.type() == TdbparseToken.END_OF_FILE:
            self.__scanner = None
            return
        tok = self.__scanner.next()
        if self.__options.tdbparse_echo_tokens: sys.stderr.write(str(tok) + '\n')
        self.__token1 = tok
        if tok.type() == TdbparseToken.END_OF_FILE:
            self.__scanner = None
            return

    def __advance(self):
        '''Advances to the next token.'''
        if self.__scanner is None: tok = TdbparseToken(TdbparseToken.NONE, 0, 0)
        else:
            tok = self.__scanner.next()
            if self.__options.tdbparse_echo_tokens: sys.stderr.write(str(tok) + '\n')
            if tok.type() == TdbparseToken.END_OF_FILE: self.__scanner = None
        self.__token0, self.__token1 = self.__token1, tok

    def __accept(self, toktyp):
        '''If the given token is next, consumes it and returns True,
        otherwise does not consume a token and returns False.'''
        if self.__token0.type() == toktyp:
            self.__advance()
            return True
        return False

    def __expect(self, toktyp):
        '''If the given token is next, consumes it, otherwise raises
        TdbparseSyntaxError.'''
        if not self.__accept(toktyp):
            raise TdbparseSyntaxError(
                'expected %s but got %s' % (TdbparseToken(toktyp, 0, 0).translate(), self.__token0.translate()), 
                self.__file_name, self.__token0.line(),
                self.__token0.col())

    def __initialize_data(self):
        self.__tdb = Tdb()
        self.__current_publisher = None
        self.__current_title = None
        self.__current_au = [ None ]
        self.__stack = []

def tdbparse(file, options):
    tdb = TdbParser(TdbScanner(file, options), options).parse()
    return tdbq.tdbq_reprocess(tdb, options)

def __option_parser__(parser=None):
    if parser is None: parser = OptionParser(version=__version__)
    parser = tdbq.__option_parser__(parser)
    tdbparse_group = OptionGroup(parser, 'tdbparse module (%s)' % (__version__,))
    tdbparse_group.add_option('--' + TdbparseConstants.OPTION_TDBPARSE_ECHO_LINES,
                              action='store_true',
                              help=TdbparseConstants.OPTION_TDBPARSE_ECHO_LINES_HELP)
    tdbparse_group.add_option('--' + TdbparseConstants.OPTION_TDBPARSE_ECHO_TOKENS,
                              action='store_true',
                              help=TdbparseConstants.OPTION_TDBPARSE_ECHO_TOKENS_HELP)
    parser.add_option_group(tdbparse_group)
    return parser

def __reprocess_options__(parser, options):
    tdbq.__reprocess_options__(parser, options)

###
### Test code
###

from StringIO import StringIO
import unittest
 
###
### TestTdbScanner
###

class TestTdbScanner(unittest.TestCase):
    def options(self):
        parser = __option_parser__()
        (options, args) = parser.parse_args(values=parser.get_default_values())
        return options

    def testBasic(self):
        scanner = TdbScanner(StringIO('''\
            publisher
            title
            au
            implicit
            < a = b ; c = d >
            {
            }
            [
            ]'''), self.options())
        for typ in [TdbparseToken.PUBLISHER,
                    TdbparseToken.TITLE,
                    TdbparseToken.AU,
                    TdbparseToken.IMPLICIT,
                    TdbparseToken.ANGLE_OPEN, TdbparseToken.IDENTIFIER, TdbparseToken.EQUAL, TdbparseToken.STRING, TdbparseToken.SEMICOLON, TdbparseToken.IDENTIFIER, TdbparseToken.EQUAL, TdbparseToken.STRING, TdbparseToken.ANGLE_CLOSE,
                    TdbparseToken.CURLY_OPEN,
                    TdbparseToken.CURLY_CLOSE,
                    TdbparseToken.SQUARE_OPEN,
                    TdbparseToken.SQUARE_CLOSE,
                    TdbparseToken.END_OF_FILE]:
            self.assertEquals(typ, scanner.next().type())
        self.assertRaises(RuntimeError, scanner.next)
    
    def testEmptyInput(self):
        for str in ['',
                    '       ',
                    '\n\n\n',
                    '  \n  \n  \n  ']:
            scanner = TdbScanner(StringIO(str), self.options())
            self.assertEquals(TdbparseToken.END_OF_FILE, scanner.next().type())
            self.assertRaises(RuntimeError, scanner.next)

    def testBadStrings(self):
        for st, skip in [('"', 0),
                         ('"foo', 0),
                         ('"foo\\', 0),
                         ('"foo\\x"', 0),
                         ('foo = bar\\', 2),
                         ('foo = bar\\x', 2),
                         ('foo = "', 2),
                         ('foo = "\\', 2),
                         ('foo = "\\k', 2),
                         ('foo = "\\"', 2),
                         ]:
            scanner = TdbScanner(StringIO(st), self.options())
            for i in range(skip): scanner.next()
            self.assertRaises(TdbparseSyntaxError, scanner.next)

    def testStrings(self):
#a = "nothing special"
#b = "embedded\\\"quote"
#c = "embedded\\\\backslash"
#d = ""
        scanner = TdbScanner(StringIO('''\
e = nothing special either
f =    surrounding whitespace      
g =    \\ one leading space
h =    \\    one leading space side effect   
i =    one trailing space\\    
j = embedded\\ space
k = embedded\\;semicolon
l = embedded\\>angle
m = \\"leading quote mark
n = embedded"quote"marks'''), self.options())
        tok = scanner.next()
        for val in [#'nothing special',
                    #'embedded"quote',
                    #'embedded\\backslash',
                    #'',
                    'nothing special either',
                    'surrounding whitespace',
                    ' one leading space',
                    '    one leading space side effect',
                    'one trailing space ',
                    'embedded space',
                    'embedded;semicolon',
                    'embedded>angle',
                    '"leading quote mark',
                    'embedded"quote"marks']:
            while tok.type() != TdbparseToken.STRING: tok = scanner.next()
            self.assertEquals(val, tok.value())
            tok = scanner.next()

###
### TestTdbParser
###

class TestTdbParser(unittest.TestCase):
    def options(self):
        parser = __option_parser__()
        (options, args) = parser.parse_args(values=parser.get_default_values())
        return options

    def testBasic(self):
        parser = TdbParser(TdbScanner(StringIO('''\
{

  publisher <
    name = The Publisher
  >

  {

    title <
      name = The Title
    >

    implicit < a ; b ; c >
    au < vala ; valb ; valc >

  }

}
'''), self.options()), self.options())
        tdb = parser.parse()
        self.assertEquals(1, len(tdb.aus()))
        au0 = tdb.aus()[0]
        for k, v in [('a', 'vala'), ('b', 'valb'), ('c', 'valc')]:
            self.assertEquals(v, au0.get(k))
        self.assertEquals(1, len(tdb.titles()))
        title0 = tdb.titles()[0]
        self.assertEquals('The Title', title0.name())
        self.assertEquals(1, len(tdb.publishers()))
        publisher0 = tdb.publishers()[0]
        self.assertEquals('The Publisher', publisher0.name())

    def testSyntaxErrors(self):
        for src, mes in [('''\
<
''', TdbparseSyntaxError('expected end of file but got <', '<string>', 1, 1)),
                         ('''\
{
  "foo" "and" "so" "forth"
''', TdbparseSyntaxError('Unexpected syntax', '<string>', 2, 3)),
                         ('''\
{
  publisher [
''', TdbparseSyntaxError('expected < but got [', '<string>', 2, 13)),
                         ('''\
{
  {
}
''', TdbparseSyntaxError('expected } but got end of file', '<string>', 4, 1)),
                         ('''\
{
  title <
    name = The Title
  >
}
''', TdbparseSyntaxError('expected } but got title', '<string>', 2, 3)),
                         ('''\
{
  foo = "bar"
  publisher <
    name = The Publisher
  >
}
''', TdbparseSyntaxError('expected } but got publisher', '<string>', 3, 3)),
                         ('''\
{
  publisher <
    name = The Publisher
  >
  au < foo ; bar ; baz >
}''', TdbparseSyntaxError('expected } but got au', '<string>', 5, 3)),
                         ('''\
{
  publisher <
    name = The Publisher
  >
  {
    title <
      name = The Title
    >
    implicit < a ; b ; c >
    au < vala ; valb >
  }
}
''', TdbparseSyntaxError('expected 3 implicit assignments but got 2', '<string>', 10, 5)),
                         ('''\
{
  publisher <
    name = The Publisher
  >
  {
    title <
      name = The Title
    >
    implicit < a ; b ; c >
    au < vala ; valb ; valc ; vald >
  }
}
''', TdbparseSyntaxError('expected 3 implicit assignments but got 4', '<string>', 10, 5))]:
            try:
              scanner = TdbScanner(StringIO(src), self.options())
              parser = TdbParser(scanner, self.options())
              parser.parse()
            except TdbparseSyntaxError, exc:
                self.assertEquals(str(mes), str(exc))

if __name__ == '__main__': unittest.main()

