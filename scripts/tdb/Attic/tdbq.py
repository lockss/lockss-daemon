#!/usr/bin/python

# $Id: tdbq.py,v 1.3 2010-08-04 22:31:03 thib_gc Exp $

# Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '''0.3.1'''

from optparse import OptionGroup, OptionParser
import re
import sys
from tdb import AU, Tdb

class TdbqConstants:
    '''Constants associated with the tdbq module.'''
    
    OPTION_PRODUCTION_STATUSES = 'productionStatuses'
    OPTION_PRODUCTION_STATUSES_SHORT = 'P'
    OPTION_PRODUCTION_STATUSES_HELP = 'only keep AUs in production statuses'
    
    OPTION_QUERY = 'query'
    OPTION_QUERY_SHORT = 'q'
    OPTION_QUERY_HELP = 'principal query expression'
    
    OPTION_TDBQ_ECHO_QUERY = 'tdbq-echo-query'
    OPTION_TDBQ_ECHO_QUERY_HELP = 'echo the query string to stderr'
    
    OPTION_TDBQ_ECHO_TOKENS = 'tdbq-echo-tokens'
    OPTION_TDBQ_ECHO_TOKENS_HELP = 'echo each token built by tdbq to stderr'
    
    OPTION_TESTING_STATUSES = 'testingStatuses'
    OPTION_TESTING_STATUSES_SHORT = 'T'
    OPTION_TESTING_STATUSES_HELP = 'only keep AUs in testing statuses'
        
class TdbqLiteral:
    '''Literals encountered in TDB query strings.'''
    
    RE_WHITESPACE = r'\s+'
    RE_IDENTIFIER = r'[a-zA-Z0-9_./]+(\[[a-zA-Z0-9_./]+\])*'
    AND = 'and'
    OR = 'or'
    IS = 'is'
    NOT = 'not'
    SET = 'set'
    EQUAL = '='
    NOT_EQUAL = '!='
    MATCHES = '~'
    DOES_NOT_MATCH = '!~'
    PAREN_OPEN = '('
    PAREN_CLOSE = ')'
    QUOTE_DOUBLE = '"'
    QUOTE_SINGLE = '\''

class TdbqToken(object):
    '''Tokens encountered while parsing TDB source files.'''
    
    NONE = 1
    AND = 2
    OR = 3
    IS = 4
    NOT = 5
    SET = 6
    EQUAL = 7
    NOT_EQUAL = 8
    MATCHES = 9
    DOES_NOT_MATCH = 10
    PAREN_OPEN = 11
    PAREN_CLOSE = 12
    STRING = 13
    IDENTIFIER = 14
    END_OF_STRING = 15
    
    def __init__(self, typ, index):
        '''Builds a new token of the given type, at the given index,
        initially with a value of None.
        
        typ: A token type. See the TdbqToken class.
        index: A non-negative index.'''
        object.__init__(self)
        self.__type = typ
        self.__index = index
        self.__value = None
    
    def set_value(self, value):
        '''Sets the associated value of this token to the given value.
        
        value: This token's value.'''
        self.__value = value

    def type(self):
        '''Returns this token's type.'''
        return self.__type
    
    def index(self):
        '''Return this token's index.'''
        return self.__index
    
    def value(self):
        '''Returns this token's associated value (or None).'''
        return self.__value
    
    def translate(self):
        '''Translates this token's type into a human-readable string.'''
        return {TdbqToken.NONE: '<none>',
                TdbqToken.AND: TdbqLiteral.AND,
                TdbqToken.OR: TdbqLiteral.OR,
                TdbqToken.IS: TdbqLiteral.IS,
                TdbqToken.NOT: TdbqLiteral.NOT,
                TdbqToken.SET: TdbqLiteral.SET,
                TdbqToken.EQUAL: TdbqLiteral.EQUAL,
                TdbqToken.NOT_EQUAL: TdbqLiteral.NOT_EQUAL,
                TdbqToken.MATCHES: TdbqLiteral.MATCHES,
                TdbqToken.DOES_NOT_MATCH: TdbqLiteral.DOES_NOT_MATCH,
                TdbqToken.PAREN_OPEN: TdbqLiteral.PAREN_OPEN,
                TdbqToken.PAREN_CLOSE: TdbqLiteral.PAREN_CLOSE,
                TdbqToken.STRING: 'a string',
                TdbqToken.IDENTIFIER: 'an identifier',
                TdbqToken.END_OF_STRING: 'end of string'}.get(self.type()) or '<unknown: %d>' % (self.type(),)
    
    def __str__(self): return '<TdbqToken index %d: %s%s>' % (self.index(), self.translate(), '' if self.value() is None else ' [%s]' % (self.value(),))

class Tdbq(object):
    def __init__(self): object.__init__(self)
    def keep_au(self, au): raise RuntimeError, 'abstract class'

class TdbqPredicate(Tdbq):
    def __init__(self, func):
        Tdbq.__init__(self)
        self.__func = func
    def keep_au(self, au): return self.__func(au)
    
class TdbqOr(TdbqPredicate):
    def __init__(self, op1, op2): TdbqPredicate.__init__(self, lambda au: op1.keep_au(au) or op2.keep_au(au))
        
class TdbqAnd(TdbqPredicate):
    def __init__(self, op1, op2): TdbqPredicate.__init__(self, lambda au: op1.keep_au(au) and op2.keep_au(au))

class TdbqNot(TdbqPredicate):
    def __init__(self, op): TdbqPredicate.__init__(self, lambda au: not op.keep_au(au))

class TdbqScanner(object):
    '''A lexical analyzer for the TDB query language.
    
    Initialized with a string. Each call to next() consumes one token
    from the input string. The class TdbqToken is used for the return
    values. All other methods are private.
    
    All fields are private. They include the original input string,
    the current input string, the current index into the input string,
    and the current token.'''
    
    def __init__(self, str, options):
        '''Builds a lexical analyzer for a TDB query string.
        
        str: A TDB query string.'''
        self.__orig = str
        self.__str = str
        self.__options = options
        self.__ind = 0
        self.__token(TdbqToken.NONE)
        if self.__options.tdbq_echo_query: sys.stderr.write(self.orig() + '\n')
    
    def next(self):
        '''Consumes and returns one token from the input string.
        
        Returns a token of type TdbqToken.END_OF_STRING the first time
        the end of the query string is reached, and raises a runtime
        exception thereafter.'''
        # Already at end of string
        if self.__cur.type() == TdbqToken.END_OF_STRING:
            raise RuntimeError, 'already at end of string'
        # Skip whitespace
        match = re.match(TdbqLiteral.RE_WHITESPACE, self.__str)
        if match:
            self.__move(match.end())
        # End of input
        if self.__str == '':
            self.__token(TdbqToken.END_OF_STRING)
            return self.__cur
        # Literal
        if re.match(re.compile('|'.join([TdbqLiteral.EQUAL, TdbqLiteral.NOT_EQUAL, TdbqLiteral.MATCHES, TdbqLiteral.DOES_NOT_MATCH, '\\' + TdbqLiteral.PAREN_OPEN, '\\' + TdbqLiteral.PAREN_CLOSE])), self.__str):
            return self.__literal()
        # Keyword
        if re.match(re.compile('(' + '|'.join([TdbqLiteral.AND, TdbqLiteral.OR, TdbqLiteral.IS, TdbqLiteral.NOT, TdbqLiteral.SET]) + ') '), self.__str):
            return self.__keyword()
        # String
        if self.__str.startswith(TdbqLiteral.QUOTE_DOUBLE): return self.__dquote()
        if self.__str.startswith(TdbqLiteral.QUOTE_SINGLE): return self.__squote()
        # Identifier
        match = re.match(TdbqLiteral.RE_IDENTIFIER, self.__str)
        if match:
            self.__token(TdbqToken.IDENTIFIER)
            self.__move(match.end())
            self.__cur.set_value(match.group())
            return self.__cur
        # Syntax error
        self.__token(TdbqToken.NONE)
        raise RuntimeError, 'syntax error at index %d near: %s' % (self.__ind, self.__str)
            
    def __dquote(self):
        '''Consumes a double-quoted string, beginning with
        TdbqLiteral.QUOTE_DOUBLE and continuing to the matching
        TdbqLiteral.QUOTE_DOUBLE (backslashes and embedded
        TdbqLiteral.QUOTE_DOUBLE must be escaped with a backslash),
        and returns a token of type TdbqToken.STRING.'''
        self.__token(TdbqToken.STRING)
        val = []
        self.__move(1) # Skip the opening quote
        if self.__str == '': raise RuntimeError, 'run-on string at index %d' % (self.__cur.index())
        ch = self.__str[0]
        self.__move(1)
        while ch != TdbqLiteral.QUOTE_DOUBLE:
            if ch == '\\':
                ch = self.__str[0]
                self.__move(1)
                if ch not in '"\\':
                    self.__token(TdbqToken.NONE)
                    raise RuntimeError, 'invalid string escape at index %d: \\%s' % (ch, self.__ind-1)
            val.append(ch)
            ch = self.__str[0]
            self.__move(1)
        self.__cur.set_value(''.join(val))
        return self.__cur
                
    def __squote(self):
        '''Consumes a single-quoted string, beginning with
        TdbqLiteral.QUOTE_SINGLE and continuing to the matching
        TdbqLiteral.QUOTE_SINGLE (backslashes and embedded
        TdbqLiteral.QUOTE_SINGLE must be escaped with a backslash),
        and returns a token of type TdbqToken.STRING.'''
        self.__token(TdbqToken.STRING)
        val = []
        self.__move(1)
        ch = self.__str[0]
        self.__move(1)
        while ch != TdbqLiteral.QUOTE_SINGLE:
            if ch == '\\':
                ch = self.__str[0]
                self.__move(1)
                if ch not in '\'\\':
                    self.__token(TdbqToken.NONE)
                    raise RuntimeError, 'invalid string escape at index %d: \\%s' % (ch, self.__ind-1)
            val.append(ch)
            ch = self.__str[0]
            self.__move(1)
        self.__cur.set_value(''.join(val))
        return self.__cur

    def __literal(self):
        '''Consumes one of TdbqLiteral.EQUAL, TdbqLiteral.NOT_EQUAL,
        TdbqLiteral.MATCHES, TdbqLiteral.DOES_NOT_MATCH,
        TdbqLiteral.PAREN_OPEN or TdbqLiteral.PAREN_CLOSE, and returns
        a token, respectively of type TdbqToken.EQUAL,
        TdbqToken.NOT_EQUAL, TdbqToken.MATCHES,
        TdbqToken.DOES_NOT_MATCH, TdbqToken.PAREN_OPEN or
        TdbqToken.PAREN_CLOSE.'''
        lst = [(TdbqLiteral.EQUAL, TdbqToken.EQUAL),
               (TdbqLiteral.NOT_EQUAL, TdbqToken.NOT_EQUAL),
               (TdbqLiteral.MATCHES, TdbqToken.MATCHES),
               (TdbqLiteral.DOES_NOT_MATCH, TdbqToken.DOES_NOT_MATCH),
               (TdbqLiteral.PAREN_OPEN, TdbqToken.PAREN_OPEN),
               (TdbqLiteral.PAREN_CLOSE, TdbqToken.PAREN_CLOSE)]
        for lit, tok in lst:
            if self.__str.startswith(lit):
                self.__token(tok)
                self.__move(len(lit))
                return self.__cur
        else: raise RuntimeError, 'internal error: bad literal at index %d near: %s' % (self.__ind, self.__str)
        

    def __keyword(self):
        '''Consumes one of TdbqLiteral.AND, TdbqLiteral.OR,
        TdbqLiteral.IS, TdbqLiteral.NOT or TdbqLiteral.SET, and
        returns a token, respectively of type TdbqToken.AND,
        TdbqToken.OR, TdbqToken.IS, TdbqToken.NOT or TdbqToken.SET.'''
        lst = [(TdbqLiteral.AND, TdbqToken.AND),
               (TdbqLiteral.OR, TdbqToken.OR),
               (TdbqLiteral.IS, TdbqToken.IS),
               (TdbqLiteral.NOT, TdbqToken.NOT),
               (TdbqLiteral.SET, TdbqToken.SET)]
        for lit, tok in lst:
            if self.__str.startswith(lit + ' '):
                self.__token(tok)
                self.__move(len(lit) + 1)
                return self.__cur
        else: raise RuntimeError, 'internal error: bad literal at index %d near: %s' % (self.__ind, self.__str)
            
    def __move(self, n):
        '''Advances the input string by the given number of
        characters.
        
        n: The number of characters to consume from the input
        string.'''
        self.__ind = self.__ind + n
        self.__str = self.__str[n:]

    def __token(self, typ):
        '''Creates a token of the given type at the current index, and
        sets the current token to it.
        
        typ: A token type. See TdbqToken.'''
        self.__cur = TdbqToken(typ, self.__ind)
        
    def orig(self):
        '''Return the query string this scanner was originally built
        for.'''
        return self.__orig

class TdbqParser(object):
    '''An LL(0) parser for the TDB query language.
    
    Initialized with a lexical scanner of type TdbqScanner. The call
    to parse() consumes the entirety of the scanner's input and
    returns a filter of type Tdbq.'''
    
    def __init__(self, scanner, options):
        object.__init__(self)
        self.__scanner = scanner
        self.__options = options
        self.__ind = 0
        self.__input = []
        self.__stack = []
    
    def parse(self):
        '''tdbq :
            or_expression
            TdbqToken.END_OF_STRING
        ;
        '''
        tok = self.__scanner.next()
        if self.__options.tdbq_echo_tokens: sys.stderr.write(str(tok) + '\n')
        while tok.type() != TdbqToken.END_OF_STRING:
            self.__input.append(tok)
            tok = self.__scanner.next()
            if self.__options.tdbq_echo_tokens: sys.stderr.write(str(tok) + '\n')
        self.__input.append(tok) # TdbqToken.END_OF_STRING
        self.__or_expression()
        self.__expect(TdbqToken.END_OF_STRING, self.__input[self.__ind].index())
        return self.__stack.pop()
        
    def __or_expression(self):
        '''or_expression :
            or_expression
            TdbqToken.OR
            and_expression
        |
            and_expression
        ;'''
        if self.__input[self.__ind].type() in [TdbqToken.PAREN_OPEN, TdbqToken.IDENTIFIER]:
            self.__and_expression()
        if self.__accept(TdbqToken.OR):
            self.__or_expression()
            op2 = self.__stack.pop()
            op1 = self.__stack.pop()
            self.__stack.append(TdbqOr(op1, op2))
    
    def __and_expression(self):
        '''and_expression :
            and_expression
            TdbqToken.AND
            expression
        |
            expression
        ;'''
        if self.__input[self.__ind].type() in [TdbqToken.PAREN_OPEN, TdbqToken.IDENTIFIER]:
            self.__expression()
        if self.__accept(TdbqToken.AND):
            self.__and_expression()
            op2 = self.__stack.pop()
            op1 = self.__stack.pop()
            self.__stack.append(TdbqAnd(op1, op2))
            
    def __expression(self):
        '''expression :
            TdbqToken.PAREN_OPEN
            or_expression
            TdbqToken.PAREN_CLOSE
        |
            TdbqToken.IDENTIFIER
            TdbqToken.IS
            TdbqToken.NOT?
            ( TdbqToken.SET | TdbqToken.STRING )
        |
            TdbqToken.IDENTIFIER
            ( TdbqToken.EQUAL | TdbqToken.NOT_EQUAL | TdbqToken.MATCHES | TdbqToken.DOES_NOT_MATCH )
            TdbqToken.STRING
        ;'''
        if self.__accept(TdbqToken.PAREN_OPEN):
            self.__or_expression()
            self.__expect(TdbqToken.PAREN_CLOSE, self.__input[self.__ind].index())
            return
        
        # Identifier
        ident = self.__input[self.__ind]
        self.__expect(TdbqToken.IDENTIFIER, ident.index())
        func_ident = str_to_lambda_au(ident.value())
        if func_ident is None:
            raise RuntimeError, 'bad identifier at index %d: %s' % (ident.index(), ident.value())
        
        # Operator, or "is" and optional "not"
        oper = self.__input[self.__ind]
        opertyp = oper.type()
        if self.__accept(TdbqToken.IS):
            if self.__accept(TdbqToken.NOT): opertyp = TdbqToken.NOT_EQUAL
            else: opertyp = TdbqToken.EQUAL
        elif opertyp in [TdbqToken.EQUAL, TdbqToken.NOT_EQUAL, TdbqToken.MATCHES, TdbqToken.DOES_NOT_MATCH]:
            self.__expect(opertyp, oper.index())
        else:
            raise RuntimeError, 'expected "%s", "%s", "%s", "%s",or "%s" at index %d but got: %s' % (TdbqLiteral.IS, TdbqLiteral.EQUAL, TdbqLiteral.NOT_EQUAL, TdbqLiteral.MATCHES, TdbqLiteral.DOES_NOT_MATCH, oper.index(), oper.translate())

        # "set" or string
        value = self.__input[self.__ind]
        if self.__accept(TdbqToken.SET):
            if oper.type() != TdbqToken.IS:
                raise RuntimeError, '"%s" not immediately following "%s" or "%s %s" at index %d' % (TdbqLiteral.SET, TdbqLiteral.IS, TdbqLiteral.IS, TdbqLiteral.NOT, value.index())
            if opertyp == TdbqToken.EQUAL: func_value = lambda val: val is not None
            elif opertyp == TdbqToken.NOT_EQUAL: func_value = lambda val: val is None
            else: 
                raise RuntimeError, 'internal error: bad operator with "%s" at index %d: %s' % (TdbqLiteral.SET, value.index(), opertyp)
        elif self.__accept(TdbqToken.STRING):
            for op, fn in [(TdbqToken.EQUAL, lambda val: val == value.value()),
                           (TdbqToken.NOT_EQUAL, lambda val: val != value.value()),
                           (TdbqToken.MATCHES, lambda val: re.search(re.compile(value.value()), val)),
                           (TdbqToken.DOES_NOT_MATCH, lambda val: re.search(re.compile(value.value()), val) is None)]:
                if op == opertyp:
                    func_value = fn
                    break
            else:
                raise RuntimeError, 'internal error: bad operator at index %d: %s' % (oper.index(), opertyp)
        else:
            raise RuntimeError, 'expected "%s" or a string at index %d but got: %s' % (TdbqLiteral.SET, value.index(), value.translate())

        # Putting it all together
        self.__stack.append(TdbqPredicate(lambda au: func_value(func_ident(au))))
    
    def __accept(self, toktyp):
        if self.__input[self.__ind].type() == toktyp:
            self.__move()
            return True
        return False
    
    def __expect(self, toktyp, ind):
        if not self.__accept(toktyp): raise RuntimeError, 'unexpected syntax at index %d: %s' % (self.__input[self.__ind].index(), self.__scanner.orig()[self.__input[self.__ind].index():])
            
    def __move(self): self.__ind = self.__ind + 1

def __option_parser__(parser=None):
    if parser is None: parser = OptionParser(version=__version__)
    tdbq_group = OptionGroup(parser, 'tdbq module (%s)' % (__version__,))
    tdbq_group.add_option('-' + TdbqConstants.OPTION_PRODUCTION_STATUSES_SHORT,
                          '--' + TdbqConstants.OPTION_PRODUCTION_STATUSES,
                          dest=TdbqConstants.OPTION_PRODUCTION_STATUSES,
                          action='store_true',
                          help=TdbqConstants.OPTION_PRODUCTION_STATUSES_HELP)
    tdbq_group.add_option('-' + TdbqConstants.OPTION_QUERY_SHORT,
                          '--' + TdbqConstants.OPTION_QUERY,
                          dest=TdbqConstants.OPTION_QUERY,
                          help=TdbqConstants.OPTION_QUERY_HELP)
    tdbq_group.add_option('--' + TdbqConstants.OPTION_TDBQ_ECHO_QUERY,
                          action='store_true',
                          help=TdbqConstants.OPTION_TDBQ_ECHO_QUERY_HELP)
    tdbq_group.add_option('--' + TdbqConstants.OPTION_TDBQ_ECHO_TOKENS,
                          action='store_true',
                          help=TdbqConstants.OPTION_TDBQ_ECHO_TOKENS_HELP)
    tdbq_group.add_option('-' + TdbqConstants.OPTION_TESTING_STATUSES_SHORT,
                          '--' + TdbqConstants.OPTION_TESTING_STATUSES,
                          dest=TdbqConstants.OPTION_TESTING_STATUSES,
                          action='store_true',
                          help=TdbqConstants.OPTION_TESTING_STATUSES_HELP)
    parser.add_option_group(tdbq_group)
    return parser

def __reprocess_options__(parser, options):
    if options.testingStatuses and options.productionStatuses:
        parser.error('-%s/--%s and -%s/--%s are mutually exclusive' % (TdbqConstants.OPTION_TESTING_STATUSES_SHORT, TdbqConstants.OPTION_TESTING_STATUSES, TdbqConstants.OPTION_PRODUCTION_STATUSES_SHORT, TdbqConstants.OPTION_PRODUCTION_STATUSES))

def tdbq_reprocess(tdb, options):
    '''Reprocesses a Tdb instance according to the query that may be
    included in the options.
    
    Returns the same Tdb instance if there is no query.'''
    if not (options.testingStatuses or options.productionStatuses or options.query): return tdb
    query = None
    if options.query: query = TdbqParser(TdbqScanner(options.query, options), options).parse()
    statuses = None
    if options.productionStatuses:
        statuses = TdbqPredicate(lambda au: au.status() in [AU.Status.RELEASED,
                                                            AU.Status.DOWN,
                                                            AU.Status.SUPERSEDED,
                                                            AU.Status.RETRACTED])
    elif options.testingStatuses:
        statuses = TdbqPredicate(lambda au: au.status() in [AU.Status.EXISTS,
                                                            AU.Status.MANIFEST,
                                                            AU.Status.WANTED,
                                                            AU.Status.TESTING,
                                                            AU.Status.NOT_READY,
                                                            AU.Status.TESTED,
                                                            AU.Status.RETESTING,
                                                            AU.Status.READY,
                                                            AU.Status.PRE_RELEASING,
                                                            AU.Status.PRE_RELEASED,
                                                            AU.Status.RELEASING,
                                                            AU.Status.RELEASED,
                                                            AU.Status.DOWN,
                                                            AU.Status.SUPERSEDED,
                                                            AU.Status.RETRACTED])
    if query and statuses: prog = TdbqAnd(query, statuses)
    elif query: prog = query
    else: prog = statuses
    newtdb = Tdb()
    for au in tdb.aus():
        if prog.keep_au(au):
            newtdb.add_au(au)
            if au.title() not in newtdb.titles(): newtdb.add_title(au.title())
            if au.title().publisher() not in newtdb.publishers(): newtdb.add_publisher(au.title().publisher())
    return newtdb

def str_to_lambda_au(str):
    '''Translates a keyword into a lambda function that accepts an AU
    and returns the corresponding AU trait.
    
    Returns None if the keyword is not recognized.'''
    for id, fn in [('status', lambda au: au.status()),
                   ('year', lambda au: au.year()),
                   ('name', lambda au: au.name()),
                   ('plugin', lambda au: au.plugin()),
                   ('rights', lambda au: au.rights()),
                   ('auid', lambda au: au.auid()),
                   ('title', lambda au: au.title().name()),
                   ('issn', lambda au: au.title().issn()),
                   ('eissn', lambda au: au.title().eissn()),
                   ('publisher', lambda au: au.title().publisher().name())]:
        if id == str: return fn
    return None
