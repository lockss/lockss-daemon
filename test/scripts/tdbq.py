#!/usr/bin/python

# $Id: tdbq.py,v 1.1 2010-04-02 11:15:16 thib_gc Exp $
#
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

from optparse import OptionError, OptionGroup
import re
from tdb import AU, Tdb

class TdbqConstants:

    VERSION = '0.1.0'
    OPTION_LONG = '--'
    OPTION_SHORT = '-'
    OPTION_QUERY = 'query'
    OPTION_QUERY_SHORT = 'q'
    OPTION_QUERY_HELP = 'principal query expression'
    OPTION_PRODUCTION_STATUSES = 'productionStatuses'
    OPTION_PRODUCTION_STATUSES_SHORT = 'P'
    OPTION_PRODUCTION_STATUSES_HELP = 'only keep AUs in production statuses'
    OPTION_TESTING_STATUSES = 'testingStatuses'
    OPTION_TESTING_STATUSES_SHORT = 'T'
    OPTION_TESTING_STATUSES_HELP = 'only keep AUs in testing statuses'
        
class TdbqLiteral:
    
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
    
    NONE = 0
    AND = 1
    OR = 2
    IS = 3
    NOT = 4
    SET = 5
    EQUAL = 6
    NOT_EQUAL = 7
    MATCHES = 8
    DOES_NOT_MATCH = 9
    PAREN_OPEN = 10
    PAREN_CLOSE = 11
    STRING = 12
    IDENTIFIER = 13
    END_OF_STRING = 14
    
    def __init__(self, token, index):
        object.__init__(self)
        self.__token = token
        self.__index = index
        self.__value = None
    
    def set_value(self, value): self.__value = value
    def token(self): return self.__token
    def index(self): return self.__index
    def value(self): return self.__value

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

class TdbqScanner:
    
    def __init__(self, str):
        self.__orig = str
        self.__str = str
        self.__ind = 0
        self.__token(TdbqToken.NONE)
    
    def next(self):
        # Already at end of string
        if self.__cur.token() == TdbqToken.END_OF_STRING:
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
        self.__token(TdbqToken.STRING)
        val = []
        self.__move(1)
        ch = self.__str[0]
        self.__move(1)
        while ch != TdbqLiteral.QUOTE_DOUBLE:
            if ch == '\\':
                ch = self.__str[0]
                self.__move(1)
                if ch not in '"\\':
                    self.__token(TdbqToken.NONE)
                    raise RuntimeError, 'invalid quoted string escape at index %d: \\%s' % (ch, self.__ind-1)
            val.append(ch)
            ch = self.__str[0]
            self.__move(1)
        self.__cur.set_value(''.join(val))
        return self.__cur
                
    def __squote(self):
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
                    raise RuntimeError, 'invalid quoted string escape at index %d: \\%s' % (ch, self.__ind-1)
            val.append(ch)
            ch = self.__str[0]
            self.__move(1)
        self.__cur.set_value(''.join(val))
        return self.__cur

    def __literal(self):
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
        self.__ind = self.__ind + n
        self.__str = self.__str[n:]

    def __token(self, tok):
        self.__cur = TdbqToken(tok, self.__ind)

class TdbqParser:
    
    def __init__(self, scanner):
        self.__scanner = scanner
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
        while tok.token() != TdbqToken.END_OF_STRING:
            self.__input.append(tok)
            tok = self.__scanner.next()
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
        if self.__input[self.__ind].token() in [TdbqToken.PAREN_OPEN, TdbqToken.IDENTIFIER]:
            self.__and_expression()
        if self.__accept(TdbqToken.OR):
            self.__or_expression()
            op2 = self.__stack.pop()
            op1 = self.__stack.pop()
            self.__stack.append(TdbqPredicate(lambda au: op1.keep_au(au) or op2.keep_au(au)))
    
    def __and_expression(self):
        '''and_expression :
            and_expression
            TdbqToken.AND
            expression
        |
            expression
        ;'''
        if self.__input[self.__ind].token() in [TdbqToken.PAREN_OPEN, TdbqToken.IDENTIFIER]:
            self.__expression()
        if self.__accept(TdbqToken.AND):
            self.__and_expression()
            op2 = self.__stack.pop()
            op1 = self.__stack.pop()
            self.__stack.append(TdbqPredicate(lambda au: op1.keep_au(au) and op2.keep_au(au)))
            
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
        opertok = oper.token()
        if self.__accept(TdbqToken.IS):
            if self.__accept(TdbqToken.NOT): opertok = TdbqToken.NOT_EQUAL
            else: opertok = TdbqToken.EQUAL
        elif opertok in [TdbqToken.EQUAL, TdbqToken.NOT_EQUAL, TdbqToken.MATCHES, TdbqToken.DOES_NOT_MATCH]:
            self.__expect(opertok, oper.index())
        else:
            raise RuntimeError, 'expected "%s" %s %s %s or %s at index %d but got: %s' % (TdbqLiteral.IS, TdbqLiteral.EQUAL, TdbqLiteral.NOT_EQUAL, TdbqLiteral.MATCHES, TdbqLiteral.DOES_NOT_MATCH, oper.index(), oper.token())

        # "set" or string
        value = self.__input[self.__ind]
        if self.__accept(TdbqToken.SET):
            if oper.token() != TdbqToken.IS:
                raise RuntimeError, '"%s" not immediately following "%s" or "%s %s" at index %d' % (TdbqLiteral.SET, TdbqLiteral.IS, TdbqLiteral.IS, TdbqLiteral.NOT, value.index())
            if opertok == TdbqToken.EQUAL: func_value = lambda val: val is not None
            elif opertok == TdbqToken.NOT_EQUAL: func_value = lambda val: val is None
            else: 
                raise RuntimeError, 'internal error: bad operator with "%s" at index %d: %s' % (TdbqLiteral.SET, value.index(), opertok)
        elif self.__accept(TdbqToken.STRING):
            for op, fn in [(TdbqToken.EQUAL, lambda val: val == value.value()),
                           (TdbqToken.NOT_EQUAL, lambda val: val != value.value()),
                           (TdbqToken.MATCHES, lambda val: re.search(re.compile(value.value()), val)),
                           (TdbqToken.DOES_NOT_MATCH, lambda val: re.search(re.compile(value.value()), val) is None)]:
                if op == opertok:
                    func_value = fn
                    break
            else:
                raise RuntimeError, 'internal error: bad operator at index %d: %s' % (oper.index(), opertok)
        else:
            raise RuntimeError, 'expected "%s" or string at index %d but got: %s' % (TdbqLiteral.SET, value.index(), value.token())

        # Putting it all together
        self.__stack.append(TdbqPredicate(lambda au: func_value(func_ident(au))))
    
    def __accept(self, tok):
        if self.__input[self.__ind].token() == tok:
            self.__move()
            return True
        return False
    
    def __expect(self, tok, ind):
        if not self.__accept(tok): raise RuntimeError, 'expected %s but got %s at index %d' % (tok, self.__input[self.__ind].token(), ind)
            
    def __move(self): self.__ind = self.__ind + 1

def __option_parser__(parser):
    tdbq_group = OptionGroup(parser, 'tdbq module (%s)' % (TdbqConstants.VERSION,))
    tdbq_group.add_option(TdbqConstants.OPTION_SHORT + TdbqConstants.OPTION_QUERY_SHORT,
                          TdbqConstants.OPTION_LONG + TdbqConstants.OPTION_QUERY,
                          dest=TdbqConstants.OPTION_QUERY,
                          help=TdbqConstants.OPTION_QUERY_HELP)
    tdbq_group.add_option(TdbqConstants.OPTION_SHORT + TdbqConstants.OPTION_PRODUCTION_STATUSES_SHORT,
                          TdbqConstants.OPTION_LONG + TdbqConstants.OPTION_PRODUCTION_STATUSES,
                          dest=TdbqConstants.OPTION_PRODUCTION_STATUSES,
                          action='store_true',
                          help=TdbqConstants.OPTION_PRODUCTION_STATUSES_HELP)
    tdbq_group.add_option(TdbqConstants.OPTION_SHORT + TdbqConstants.OPTION_TESTING_STATUSES_SHORT,
                          TdbqConstants.OPTION_LONG + TdbqConstants.OPTION_TESTING_STATUSES,
                          dest=TdbqConstants.OPTION_TESTING_STATUSES,
                          action='store_true',
                          help=TdbqConstants.OPTION_TESTING_STATUSES_HELP)
    parser.add_option_group(tdbq_group)

def __reprocess_options__(parser, options):
    if options.testingStatuses and options.productionStatuses:
        parser.error('%(short)s%(testshort)s/%(long)s%(test)s and %(short)s%(prodshort)s/%(long)s%(prod)s are mutually exclusive' % {'long': TdbqConstants.OPTION_LONG,
                                                                                                                                     'short': TdbqConstants.OPTION_SHORT,
                                                                                                                                     'test': TdbqConstants.OPTION_TESTING_STATUSES,
                                                                                                                                     'testshort': TdbqConstants.OPTION_TESTING_STATUSES_SHORT,
                                                                                                                                     'prod': TdbqConstants.OPTION_PRODUCTION_STATUSES,
                                                                                                                                     'prodshort': TdbqConstants.OPTION_PRODUCTION_STATUSES_SHORT})

def __reprocess_tdb__(tdb, options):
    if not (options.testingStatuses or options.productionStatuses or options.query): return tdb
    query = None
    if options.query: query = TdbqParser(TdbqScanner(options.query)).parse()
    statuses = None
    if options.productionStatuses:
        statuses = TdbqPredicate(lambda au: au.status() in [AU.STATUS_RELEASED,
                                                            AU.STATUS_DOWN,
                                                            AU.STATUS_SUPERSEDED,
                                                            AU.STATUS_RETRACTED])
    elif options.testingStatuses:
        statuses = TdbqPredicate(lambda au: au.status() in [AU.STATUS_EXISTS,
                                                            AU.STATUS_MANIFEST,
                                                            AU.STATUS_WANTED,
                                                            AU.STATUS_TESTING,
                                                            AU.STATUS_NOT_READY,
                                                            AU.STATUS_TESTED,
                                                            AU.STATUS_RETESTING,
                                                            AU.STATUS_READY,
                                                            AU.STATUS_PRE_RELEASING,
                                                            AU.STATUS_PRE_RELEASED,
                                                            AU.STATUS_RELEASING,
                                                            AU.STATUS_RELEASED,
                                                            AU.STATUS_DOWN,
                                                            AU.STATUS_SUPERSEDED,
                                                            AU.STATUS_RETRACTED])
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
    for id, fn in [('status', lambda au: au.status()),
                   ('year', lambda au: au.year()),
                   ('name', lambda au: au.name()),
                   ('plugin', lambda au: au.plugin()),
                   ('rights', lambda au: au.rights()),
                   ('title', lambda au: au.title().name()),
                   ('issn', lambda au: au.title().issn()),
                   ('eissn', lambda au: au.title().eissn()),
                   ('publisher', lambda au: au.title().publisher().name())]:
        if id == str: return fn
    return None
