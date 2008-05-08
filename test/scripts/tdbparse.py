#!/usr/bin/python

import re

#
# Constants
#

TOKEN_NONE              =  1
TOKEN_KEYWORD_PUBLISHER =  2
TOKEN_KEYWORD_TITLE     =  3
TOKEN_KEYWORD_AU        =  4
TOKEN_KEYWORD_COLUMNS   =  5
TOKEN_CURLY_OPEN        =  6
TOKEN_CURLY_CLOSE       =  7
TOKEN_ANGLE_OPEN        =  8
TOKEN_ANGLE_CLOSE       =  9
TOKEN_SQUARE_OPEN       = 10
TOKEN_SQUARE_CLOSE      = 11
TOKEN_SEMICOLON         = 12
TOKEN_EQUAL             = 13
TOKEN_STRING            = 14
TOKEN_INTEGER           = 15
TOKEN_IDENTIFIER        = 16
TOKEN_EOF               = 17

class TdbScanner(object):
    '''Implements a lexical analyzer for the TDB language.

    Initialized with an open file object. Each call to
    get_next_token() consumes one token from the input file,
    optionally leaving behind an associated value which can be
    retrieved with get_value().

    All other methods and variables are private.'''

    def __init__(self, file, **kwargs):
        self.__file = file
        self.__options = kwargs.copy()
        self.__line = ''
        self.__token = TOKEN_NONE
        self.__value = None

    def get_next_token(self):
        '''Consumes and returns one token from the input file.

        Returns TOKEN_EOF the first time end of file is reached, then
        raises a runtime error.'''
        # Already at end of file
        if self.__token == TOKEN_EOF:
            raise RuntimeError, 'already at end of file'
        # File is closed
        if self.__file.closed:
            raise RuntimeError, 'file already closed'
        # Maybe advance to next line or lines
        while re.match(r'\s*$', self.__line):
            # Empty bare string
            if self.__token == TOKEN_EQUAL:
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
        if self.__token == TOKEN_EQUAL:
            # Empty bare string
            if re.match(r'[;>]', self.__line):
                self.__value = ''
                self.__token = TOKEN_STRING
                return self.__token
            # Quoted string
            if re.match(r'"', self.__line): return self.__qstring()
            # Bare string
            return self.__bstring()
        # Keywords
        match = re.match(r'(publisher|title|au|columns)(\s|[<>])', self.__line)
        if match: return self.__keyword(match)
        # Single-character tokens
        if re.match(r'\{', self.__line): return self.__single(TOKEN_CURLY_OPEN)
        if re.match(r'\}', self.__line): return self.__single(TOKEN_CURLY_CLOSE)
        if re.match(r'<', self.__line): return self.__single(TOKEN_ANGLE_OPEN)
        if re.match(r'>', self.__line): return self.__single(TOKEN_ANGLE_CLOSE)
        if re.match(r'\[', self.__line): return self.__single(TOKEN_SQUARE_OPEN)
        if re.match(r'\]', self.__line): return self.__single(TOKEN_SQUARE_CLOSE)
        if re.match(r';', self.__line): return self.__single(TOKEN_SEMICOLON)
        if re.match(r'=', self.__line): return self.__single(TOKEN_EQUAL)
        # Integers
        match = re.match(r'\d+', self.__line)
        if match:
            self.__value = int(match.group())
            self.__advance(match.end())
            self.__token = TOKEN_INTEGER
            return self.__token
        # Identifiers
        match = re.match(r'\w+', self.__line)
        if match:
            self.__value = match.group()
            self.__advance(match.end())
            self.__token = TOKEN_IDENTIFIER
            return self.__token
        # Syntax error
        self.__token = TOKEN_NONE
        raise RuntimeError, 'syntax error near: %s' % (self.__line,)

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

        The valid keywords are publisher, title, au and columns.'''
        keyword = match.group(1)
        self.__advance(len(keyword))
        if keyword == 'publisher': self.__token = TOKEN_KEYWORD_PUBLISHER
        elif keyword == 'title': self.__token = TOKEN_KEYWORD_TITLE
        elif keyword == 'au': self.__token = TOKEN_KEYWORD_AU
        elif keyword == 'columns': self.__token = TOKEN_KEYWORD_COLUMNS
        else: raise RuntimeError, 'internal error: %s', (keyword,)
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
                    raise RuntimeError, 'invalid quoted string escape: \\%s' % (char,)
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
                    raise RuntimeError, 'invalid bare string escape: \\%s' % (char,)
            else:
                if char in ';>':
                    consumed = consumed - 1 # Don't consume terminator
                    break
                elif char == '\\': in_escape = True
                else: result.append(char)
        self.__advance(consumed)
        self.__value = ''.join(result).strip()
        self.__token = TOKEN_STRING
        return self.__token
