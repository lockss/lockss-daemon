#!/usr/bin/python

import re

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
        match = re.match(r'(publisher|title|au|columns)(\s|[<>])', self.__line)
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
        elif keyword == 'au':
            self.__token = TOKEN_KEYWORD_AU
            self.__options['_expect_string'] = TOKEN_KEYWORD_AU
        elif keyword == 'columns': self.__token = TOKEN_KEYWORD_COLUMNS
        else: raise RuntimeError, 'internal error: %s' % (keyword,)
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
        self.__initialize()

    def parse(self):
        if self.__token[0] == TOKEN_EOF:
            raise RuntimeError, 'already done parsing'
        self.__advance()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TOKEN_EOF)

    def __publisher_container_or_publisher_block(self):
        if self.__token[1] == TOKEN_KEYWORD_PUBLISHER:
            self.__publisher_block()
        else:
            self.__publisher_container()

    def __publisher_container(self):
        self.__expect(TOKEN_CURLY_OPEN)
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_COLUMNS]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__publisher_container_or_publisher_block()
        self.__expect(TOKEN_CURLY_CLOSE)

    def __publisher_block(self):
        self.__expect(TOKEN_CURLY_OPEN)
        self.__publisher()
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_COLUMNS]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__title_container_or_title_block()

    def __title_container_or_title_block(self):
        if self.__token[1] == TOKEN_KEYWORD_TITLE:
            self.__title_block()
        else:
            self.__title_container()

    def __title_container(self):
        self.__expect(TOKEN_CURLY_OPEN)
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_COLUMNS]:
            self.__assignment()
        while self.__token[0] == TOKEN_CURLY_OPEN:
            self.__title_container_or_title_block()
        self.__expect(TOKEN_CURLY_CLOSE)

    def __title_block(self):
        self.__expect(TOKEN_CURLY_OPEN)
        self.__title()
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_COLUMNS]:
            self.__assignment()
        while self.__token[0] in [TOKEN_KEYWORD_AU, TOKEN_CURLY_OPEN]:
            self.__au_container_or_au()

    def __au_container_or_au(self):
        if self.__token[0] == TOKEN_KEYWORD_AU:
            self.__au()
        elif self.__token[0] == TOKEN_CURLY_OPEN:
            self.__au_container()
        else:
            raise RuntimeError, 'expected opening curly brace or au, got %s' % (self.__token[0],)

    def __au_container(self):
        self.__expect(TOKEN_CURLY_OPEN)
        while self.__token[0] in [TOKEN_IDENTIFIER, TOKEN_KEYWORD_COLUMNS]:
            self.__assignment()
        while self.__token[0] in [TOKEN_KEYWORD_AU, TOKEN_CURLY_OPEN]:
            self.__au_container_or_au()
        self.__expect(TOKEN_CURLY_CLOSE)

    def __assignment(self):
        if self.__token[0] == TOKEN_IDENTIFIER:
            self.__simple_assignment()
        elif self.__token[0] == TOKEN_KEYWORD_COLUMNS:
            self.__columns()
        else:
            raise RuntimeError, 'expected identifier or columns, got %s' % (self.__token[0],)

    def __identifier(self):
        self.__expect(TOKEN_IDENTIFIER)
        if self.__accept(TOKEN_SQUARE_OPEN):
            self.__expect(TOKEN_INTEGER)
            self.__expect(TOKEN_SQUARE_CLOSE)

    def __list_of_identifiers(self):
        self.__expect(TOKEN_IDENTIFIER)
        while self.__accept(TOKEN_SEMICOLON):
            self.__expect(TOKEN_IDENTIFIER)

    def __simple_assignment(self):
        self.__identifier()
        self.__expect(TOKEN_EQUAL)
        self.__expect(TOKEN_STRING)

    def __list_of_simple_assignments(self):
        self.__simple_assignment()
        while self.__accept(TOKEN_SEMICOLON):
            self.__simple_assignment()

    def __list_of_strings(self):
        self.__expect(TOKEN_STRING)
        while self.__accept(TOKEN_SEMICOLON):
            self.__expect(TOKEN_STRING)

    def __columns(self):
        self.__expect(TOKEN_KEYWORD_COLUMNS)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_identifiers()
        self.__expect(TOKEN_ANGLE_CLOSE)

    def __au(self):
        self.__expect(TOKEN_KEYWORD_AU)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_strings()
        self.__expect(TOKEN_ANGLE_CLOSE)

    def __title(self):
        self.__expect(TOKEN_KEYWORD_TITLE)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TOKEN_ANGLE_CLOSE)

    def __publisher(self):
        self.__expect(TOKEN_KEYWORD_PUBLISHER)
        self.__expect(TOKEN_ANGLE_OPEN)
        self.__list_of_simple_assignments()
        self.__expect(TOKEN_ANGLE_CLOSE)

    def __initialize(self):
        '''Initializes the parser.

        Generic enough for k-lookahead.'''
        self.__token = [TOKEN_NONE]
        for i in range(TdbParser.LOOKAHEAD):
            if self.__scanner is None: tok = TOKEN_NONE
            else:
                tok = self.__scanner.get_next_token()
                if tok == TOKEN_EOF: self.__scanner = None
            self.__token.append(tok)


    def __advance(self):
        '''Advances to the next token.

        Generic enough for k-lookahead.'''
        if self.__scanner is None: tok = TOKEN_NONE
        else:
            tok = self.__scanner.get_next_token()
            if tok == TOKEN_EOF: self.__scanner = None
        self.__token.pop(0)
        self.__token.append(tok)

    def __accept(self, token):
        if self.__token[0] == token:
            self.__advance()
            return True
        return False

    def __expect(self, token):
        if (self.__accept(token)): return True
        raise RuntimeError, 'expected <%s>, got <%s>' % (token, self.__token[0])

if __name__ == '__main__':
    from sys import stdin
    scanner = TdbScanner(stdin)
    parser = TdbParser(scanner)
    parser.parse()
