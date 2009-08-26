#!/usr/bin/python

# $Id: tdbparse.py,v 1.15 2009-08-26 09:32:28 thib_gc Exp $
#
# Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.

import re
from tdb import *

TOKEN_NONE              =  1
TOKEN_KEYWORD_PUBLISHER =  2
TOKEN_KEYWORD_TITLE     =  3
TOKEN_KEYWORD_AU        =  4
TOKEN_KEYWORD_IMPLICIT  =  5
TOKEN_CURLY_OPEN        =  6
TOKEN_CURLY_CLOSE       =  7
TOKEN_ANGLE_OPEN        =  8
TOKEN_ANGLE_CLOSE       =  9
TOKEN_SQUARE_OPEN       = 10
TOKEN_SQUARE_CLOSE      = 11
TOKEN_SEMICOLON         = 12
TOKEN_EQUAL             = 13
TOKEN_STRING            = 14
TOKEN_IDENTIFIER        = 15
TOKEN_EOF               = 16

TOKENS_WITH_VALUES = [ TOKEN_STRING, TOKEN_IDENTIFIER ]

def _translate_token(tok):
    return {
        TOKEN_NONE: 'NONE',
        TOKEN_KEYWORD_PUBLISHER: 'publisher',
        TOKEN_KEYWORD_TITLE: 'title',
        TOKEN_KEYWORD_AU: 'au',
        TOKEN_KEYWORD_IMPLICIT: 'implicit',
        TOKEN_CURLY_OPEN: '{',
        TOKEN_CURLY_CLOSE: '}',
        TOKEN_ANGLE_OPEN: '<',
        TOKEN_ANGLE_CLOSE: '>',
        TOKEN_SQUARE_OPEN: '[',
        TOKEN_SQUARE_CLOSE: ']',
        TOKEN_SEMICOLON: ';',
        TOKEN_EQUAL: '=',
        TOKEN_STRING: 'string',
        TOKEN_IDENTIFIER: 'identifier',
        TOKEN_EOF: 'end of file'
    }.get(tok)

class TdbScanner(object):
    '''Implements a lexical analyzer for the TDB language.

    Initialized with an open file object. Each call to
    get_next_token() consumes one token from the input file,
    optionally leaving behind an associated value which can be
    retrieved with get_value().

    All other methods and variables are private.

    The constructor accepts keyword arguments which are interpreted
    as options. Option keys cannot begin with an underscore.'''

    def __init__(self, file, **kwargs):
        '''Constructor.

        Keyword arguments are stored as options.'''
        self.__file = file
        self.__options = kwargs.copy()
        self.__options['_expect_string'] = None
        self.__line = ''
        self.__token = TOKEN_NONE
        self.__value = None

    def get_next_token(self):
        '''Consumes and returns one token from the input file.

        Returns TOKEN_EOF the first time end of file is reached, then
        raises a runtime error.'''
        # Already at end of file
        if self.__token == TOKEN_EOF: raise RuntimeError, 'already at end of file'
        # File is closed
        if self.__file.closed: raise RuntimeError, 'file already closed'
        # Maybe advance to next line or lines
        while re.match(r'\s*$', self.__line):
            # Empty bare string
            if self.__options['_expect_string'] == TOKEN_EQUAL:
                self.__options['_expect_string'] = None
                self.__value = ''
                self.__token = TOKEN_STRING
                return self.__token
            self.__line = self.__file.readline()
            # End of file
            if self.__line == '':
                self.__token = TOKEN_EOF
                return self.__token
            self.__line = self.__line.lstrip()
        # Skip initial whitespace
        match = re.match(r'\s+', self.__line)
        if match: self.__advance(match.end())
        # Strings
        if self.__options['_expect_string'] in [TOKEN_EQUAL, TOKEN_SEMICOLON]:
            # Empty bare string
            if re.match(r'[;>]', self.__line):
                if self.__options['_expect_string'] == TOKEN_EQUAL:
                    self.__options['_expect_string'] = None
                else:
                    self.__options['_expect_string'] = TOKEN_KEYWORD_AU
                self.__value = ''
                self.__token = TOKEN_STRING
                return self.__token
            # Quoted string
            if self.__line.startswith('"'): return self.__qstring()
            # Bare string
            return self.__bstring()
        # Keywords
        match = re.match(r'(publisher|title|au|implicit)(\s|[<>])', self.__line)
        if match: return self.__keyword(match)
        # Single-character tokens
        if self.__line.startswith('{'): return self.__single(TOKEN_CURLY_OPEN)
        if self.__line.startswith('}'): return self.__single(TOKEN_CURLY_CLOSE)
        if self.__line.startswith('<'):
            if self.__token == TOKEN_KEYWORD_AU:
                self.__options['_expect_string'] = TOKEN_SEMICOLON
            return self.__single(TOKEN_ANGLE_OPEN)
        if self.__line.startswith('>'):
            self.__options['_expect_string'] = None
            return self.__single(TOKEN_ANGLE_CLOSE)
        if self.__line.startswith('['): return self.__single(TOKEN_SQUARE_OPEN)
        if self.__line.startswith(']'): return self.__single(TOKEN_SQUARE_CLOSE)
        if self.__line.startswith(';'):
            if self.__options['_expect_string'] == TOKEN_KEYWORD_AU:
                self.__options['_expect_string'] = TOKEN_SEMICOLON
            return self.__single(TOKEN_SEMICOLON)
        if self.__line.startswith('='):
            self.__options['_expect_string'] = TOKEN_EQUAL
            return self.__single(TOKEN_EQUAL)
        # Identifiers
        match = re.match(r'[a-zA-Z0-9_./]+', self.__line)
        if match:
            self.__value = match.group()
            self.__advance(match.end())
            self.__token = TOKEN_IDENTIFIER
            return self.__token
        # Syntax error
        self.__token = TOKEN_NONE
        raise RuntimeError, 'syntax error near: %s' % ( self.__line, )

    def get_value(self):
        '''Retrieves the value associated with the last token returned
        by get_next_token().

        Only makes sense for integer, string and identifier tokens.'''
        return self.__value

    def __advance(self, n):
        '''Consumes n characters from the current line.'''
        self.__line = self.__line[n:]

    def __keyword(self, match):
        '''Consumes one reserved keyword (expected to be group 1 of
        the match).

        The valid keywords are publisher, title, au and implicit.'''
        keyword = match.group(1)
        self.__advance(len(keyword))
        if keyword == 'publisher': self.__token = TOKEN_KEYWORD_PUBLISHER
        elif keyword == 'title': self.__token = TOKEN_KEYWORD_TITLE
        elif keyword == 'au':
            self.__token = TOKEN_KEYWORD_AU
            self.__options['_expect_string'] = TOKEN_KEYWORD_AU
        elif keyword == 'implicit': self.__token = TOKEN_KEYWORD_IMPLICIT
        else: raise RuntimeError, 'internal error: %s' % ( keyword, )
        return self.__token

    def __single(self, token):
        '''Consumes a single-character token.

        The valid tokens are opening and closing curly, angle and
        square brackets, semicolons and equal signs.'''
        self.__advance(1)
        self.__token = token
        return self.__token

    def __qstring(self):
        '''Consumes a quoted string, starting at the initial double
        quote and continuing to a matching double quote on the same
        line.

        Double quotes and backslashes must be escaped.'''
        result = []
        in_escape = False
        consumed = 1 # Include the leading quote
        for char in self.__line[1:]:
            consumed = consumed + 1
            if in_escape:
                if char in '"\\':
                    result.append(char)
                    in_escape = False
                else:
                    self.__token = TOKEN_NONE
                    raise RuntimeError, 'invalid quoted string escape: \\%s' % ( char, )
            else:
                if char == '"': break
                elif char == '\\': in_escape = True
                else: result.append(char)
        else:
            # End of line without encountering matching quote
            self.__token = TOKEN_NONE
            raise RuntimeError, 'run-on quoted string'
        self.__advance(consumed)
        self.__value = ''.join(result)
        self.__token = TOKEN_STRING
        if self.__options['_expect_string'] == TOKEN_EQUAL:
            self.__options['_expect_string'] = None
        elif self.__options['_expect_string'] == TOKEN_SEMICOLON:
            self.__options['_expect_string'] = TOKEN_KEYWORD_AU
        return self.__token

    def __bstring(self):
        '''Consumes a quoted string, starting at the initial non-
        whitespace character and continuing to the end of the line,
        a semicolon, or a closing angle bracket.

        Semicolons, closing angle brackets, backslashes, and leading
        and trailing spaces must be escaped.'''
        result = []
        in_escape = False
        consumed = 0
        for char in self.__line[:]:
            consumed = consumed + 1
            if in_escape:
                if char in ';>\\ ':
                    result.append(char)
                    in_escape = False
                else:
                    self.__token = TOKEN_NONE
                    raise RuntimeError, 'invalid bare string escape: \\%s' % ( char, )
            else:
                if char in ';>':
                    consumed = consumed - 1 # Don't consume terminator
                    break
                elif char == '\\': in_escape = True
                else: result.append(char)
        self.__advance(consumed)
        self.__value = ''.join(result).strip()
        self.__token = TOKEN_STRING
        if self.__options['_expect_string'] == TOKEN_EQUAL:
            self.__options['_expect_string'] = None
        elif self.__options['_expect_string'] == TOKEN_SEMICOLON:
            self.__options['_expect_string'] = TOKEN_KEYWORD_AU
        return self.__token

class TdbParser(object):
    '''Implements a syntactic analyzer for the TDB language.

    Initialized with a TdbScanner object. The parse() method is
    public.

    All other methods and variables are private.'''

    LOOKAHEAD = 1

    def __init__(self, scanner, **kwargs):
        self.__scanner = scanner
        self.__options = kwargs.copy()
        self.__initialize_data()
        self.__initialize_parser()

    def parse(self):
        '''title_database :
            publisher_container_or_publisher_block*
            TOKEN_EOF
        ;'''
        if self.__token[0] == TOKEN_EOF:
            raise RuntimeError, 'already done parsing'
        self.__advance()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TOKEN_EOF)
        return self.__tdb

    def __publisher_container_or_publisher_block(self):
        '''publisher_container_or_publisher_block :
            publisher_container
        |
            publisher_block
        ;'''
        if self.__token[1] == TOKEN_KEYWORD_PUBLISHER:
            self.__publisher_block()
        else:
            self.__publisher_container()

    def __publisher_container(self):
        '''publisher_container :
            TOKEN_CURLY_OPEN
            assignment*
            publisher_container_or_publisher_block*
            TOKEN_CURLY_CLOSE
        ;'''
        self.__expect(TOKEN_CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_IMPLICIT]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TOKEN_CURLY_CLOSE)
        self.__current_au.pop()

    def __publisher_block(self):
        '''publisher_block :
            TOKEN_CURLY_OPEN
            publisher
            assignment*
            title_container_or_title_block*
            TOKEN_CURLY_CLOSE
        ;'''
        self.__expect(TOKEN_CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        self.__publisher()
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_IMPLICIT]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__title_container_or_title_block()
        self.__expect(TOKEN_CURLY_CLOSE)
        self.__current_au.pop()

    def __title_container_or_title_block(self):
        '''title_container_or_title_block :
            title_container
        |
            title_block
        ;'''
        if self.__token[1] == TOKEN_KEYWORD_TITLE:
            self.__title_block()
        else:
            self.__title_container()

    def __title_container(self):
        '''title_container :
            TOKEN_CURLY_OPEN
            assignment*
            title_container_or_title_block*
            TOKEN_CURLY_CLOSE
        ;'''
        self.__expect(TOKEN_CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_IMPLICIT]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__title_container_or_title_block()
        self.__expect(TOKEN_CURLY_CLOSE)
        self.__current_au.pop()

    def __title_block(self):
        '''title_block :
            TOKEN_CURLY_OPEN
            title
            assignment*
            au_container_or_au*
            TOKEN_CURLY_CLOSE
        ;'''
        self.__expect(TOKEN_CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        self.__title()
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_IMPLICIT]:
            self.__assignment()
        while self.__token[0] in [TOKEN_KEYWORD_AU, TOKEN_CURLY_OPEN]:
            self.__au_container_or_au()
        self.__expect(TOKEN_CURLY_CLOSE)
        self.__current_au.pop()

    def __au_container_or_au(self):
        '''au_container_or_au :
            au_container
        |
            au
        ;'''
        if self.__token[0] == TOKEN_KEYWORD_AU:
            self.__au()
        elif self.__token[0] == TOKEN_CURLY_OPEN:
            self.__current_au.append(AU(self.__current_au[-1]))
            self.__au_container()
        else:
            raise RuntimeError, 'expected %s or %s but got %s' % (_translate_token(TOKEN_KEYWORD_AU),
                                                                  _translate_token(TOKEN_CURLY_OPEN),
                                                                  _translate_token(self.__token[0]))

    def __au_container(self):
        '''au_container :
            TOKEN_CURLY_OPEN
            assignment*
            au_container_or_au*
            TOKEN_CURLY_CLOSE
        ;'''
        self.__expect(TOKEN_CURLY_OPEN)
        self.__current_au.append(AU(self.__current_au[-1]))
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_IMPLICIT]:
            self.__assignment()
        while self.__token[0] in [TOKEN_KEYWORD_AU, TOKEN_CURLY_OPEN]:
            self.__au_container_or_au()
        self.__expect(TOKEN_CURLY_CLOSE)
        self.__current_au.pop()

    def __assignment(self):
        '''assignment :
            simple_assignment
        |
            implicit
        ;'''
        if self.__token[0] == TOKEN_IDENTIFIER:
            self.__simple_assignment()
            key, val = self.__stack.pop()
            self.__current_au[-1].set(key, val)
        elif self.__token[0] == TOKEN_KEYWORD_IMPLICIT:
            self.__implicit()
        else:
            raise RuntimeError, 'expected %s or %s but got %s' % (_translate_token(TOKEN_IDENTIFIER),
                                                                  _translate_token(TOKEN_KEYWORD_IMPLICIT),
                                                                  _translate_token(self.__token[0]))

    def __identifier(self):
        '''identifier :
            TOKEN_IDENTIFIER
            ( TOKEN_SQUARE_OPEN TOKEN_IDENTIFIER TOKEN_SQUARE_CLOSE )?
        ;'''
        self.__expect(TOKEN_IDENTIFIER)
        base = self.__value.pop(0)
        if self.__accept(TOKEN_SQUARE_OPEN):
            self.__expect(TOKEN_IDENTIFIER)
            self.__expect(TOKEN_SQUARE_CLOSE)
            self.__stack.append(( base, self.__value.pop(0) ))
        else: self.__stack.append(( base, ))

    def __list_of_identifiers(self):
        '''list_of_identifiers :
            TOKEN_IDENTIFIER
            ( TOKEN_SEMICOLON TOKEN_IDENTIFIER )*
        ;'''
        lis = []
        self.__identifier()
        lis.append(self.__stack.pop())
        while self.__accept(TOKEN_SEMICOLON):
            self.__identifier()
            lis.append(self.__stack.pop())
        self.__stack.append(lis)

    def __simple_assignment(self):
        '''simple_assignment :
            identifier
            TOKEN_EQUAL
            TOKEN_STRING
        ;'''
        self.__identifier()
        self.__expect(TOKEN_EQUAL)
        self.__expect(TOKEN_STRING)
        self.__stack.append( (self.__stack.pop(), self.__value.pop(0)) )

    def __list_of_simple_assignments(self):
        '''list_of_simple_assignments :
            simple_assignment
            ( TOKEN_SEMICOLON simple_assignment )*
        ;'''
        lis = []
        self.__simple_assignment()
        lis.append(self.__stack.pop())
        while self.__accept(TOKEN_SEMICOLON):
            self.__simple_assignment()
            lis.append(self.__stack.pop())
        self.__stack.append(lis)

    def __list_of_strings(self):
        '''list_of_strings :
            TOKEN_STRING
            ( TOKEN_SEMICOLON TOKEN_STRING )*
        ;'''
        lis = []
        self.__expect(TOKEN_STRING)
        lis.append(self.__value.pop(0))
        while self.__accept(TOKEN_SEMICOLON):
            self.__expect(TOKEN_STRING)
            lis.append(self.__value.pop(0))
        self.__stack.append(lis)

    def __implicit(self):
        '''implicit :
            TOKEN_KEYWORD_IMPLICIT
            TOKEN_ANGLE_OPEN
            list_of_identifiers
            TOKEN_ANGLE_CLOSE
        ;'''
        self.__expect(TOKEN_KEYWORD_IMPLICIT)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_identifiers()
        self.__expect(TOKEN_ANGLE_CLOSE)
        self.__current_au[-1].set('$implicit', self.__stack.pop())


    def __au(self):
        '''au :
            TOKEN_KEYWORD_AU
            TOKEN_ANGLE_OPEN
            list_of_strings
            TOKEN_ANGLE_CLOSE
        ;'''
        self.__expect(TOKEN_KEYWORD_AU)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_strings()
        self.__expect(TOKEN_ANGLE_CLOSE)
        au = AU(self.__current_au[-1])
        au.set_title(self.__current_title)
        for key, val in zip(self.__current_au[-1].get('$implicit'), self.__stack.pop()):
            au.set(key, val)
        self.__tdb.add_au(au)

    def __title(self):
        '''title :
            TOKEN_KEYWORD_TITLE
            TOKEN_ANGLE_OPEN
            list_of_simple_assignments
            TOKEN_ANGLE_CLOSE
        ;'''
        self.__expect(TOKEN_KEYWORD_TITLE)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TOKEN_ANGLE_CLOSE)
        self.__current_title = Title()
        self.__current_title.set_publisher(self.__current_publisher)
        for key, val in self.__stack.pop():
            self.__current_title.set(key, val)
        self.__tdb.add_title(self.__current_title)

    def __publisher(self):
        '''publisher :
            TOKEN_KEYWORD_PUBLISHER
            TOKEN_ANGLE_OPEN
            list_of_simple_assignments
            TOKEN_ANGLE_CLOSE
        ;'''
        self.__expect(TOKEN_KEYWORD_PUBLISHER)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TOKEN_ANGLE_CLOSE)
        self.__current_publisher = Publisher()
        for key, val in self.__stack.pop():
            self.__current_publisher.set(key, val)
        self.__tdb.add_publisher(self.__current_publisher)

    def __initialize_parser(self):
        '''Initializes the parser.

        Generic enough for k-lookahead.'''
        self.__token = [ TOKEN_NONE ]
        self.__value = []
        for i in range(TdbParser.LOOKAHEAD):
            if self.__scanner is None:
                tok = TOKEN_NONE
            else:
                tok = self.__scanner.get_next_token()
                if tok == TOKEN_EOF: self.__scanner = None
            self.__token.append(tok)
            if tok in TOKENS_WITH_VALUES: self.__value.append(self.__scanner.get_value())

    def __advance(self):
        '''Advances to the next token.

        Generic enough for k-lookahead.'''
        if self.__scanner is None: tok = TOKEN_NONE
        else:
            tok = self.__scanner.get_next_token()
            if tok == TOKEN_EOF: self.__scanner = None
        self.__token.pop(0)
        self.__token.append(tok)
        if tok in TOKENS_WITH_VALUES: self.__value.append(self.__scanner.get_value())

    def __accept(self, token):
        '''If the given token is next, consumes it and returns True,
        otherwise does not consume a token and returns False.'''
        if self.__token[0] == token:
            self.__advance()
            return True
        return False

    def __expect(self, token):
        '''If the given token is next, consumes it, otherwise raises
        a runtime error.'''
        if not self.__accept(token):
            raise RuntimeError, 'expected %s but got %s' % (_translate_token(token),
                                                            _translate_token(self.__token[0]))

    def __initialize_data(self):
        self.__tdb = Tdb()
        self.__current_publisher = None
        self.__current_title = None
        self.__current_au = [ None ]
        self.__stack = []
