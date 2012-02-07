#!/usr/bin/env python

# $Id: tdb.py,v 1.13 2012-02-07 00:32:49 thib_gc Exp $

# Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '''0.3.4'''

import re

class MapError(Exception):
    pass

class _Map(object):
    '''A map from tuples to values.  In addition, when a tuple
    provided to 'get' is a prefix of a set of tuples previously
    provided to 'set', the returned value is a dictionary representing
    all of the mapped pairs previously provided. The prefix is
    stripped off.  There is no delete operation.
    '''
    def __init__(self, dic={}):
        '''Constructor.'''
        object.__init__(self)
        self._dictionary = dic.copy()

    def get(self, key):
        if key == ():
            raise MapError('Null tuple not allowed as key.')
        if type(key) is not tuple: key = ( key, )
        return self._get(key, self._dictionary)
    
    def _get(self, key, rec):
        if len(key) == 0: return rec
        if type(rec) is not dict:
            raise MapError('Trying to get deeper level than was set.')
        newrec = rec.get(key[0])
        if newrec: return self._get(key[1:], newrec)
        return None
    
    def set(self, key, val):
        if key == ():
            raise MapError('Null tuple not allowed as key.')
        if val == None:
            raise MapError('None value not allowed as key.')
        if type(key) is not tuple: key = ( key, )
        # reserve dict values for internal use
        if type(val) is dict:
            raise MapError('dictionary not allowed as value.')
        self._set(key, val, self._dictionary)
    
    def _set(self, key, val, rec):
        assert len(key) > 0
        k0 = key[0]
        if len(key) == 1:
            if type(rec) is not dict:
                raise MapError('Trying to set a deeper level than before.')
            if type(rec.get(k0)) is dict:
                raise MapError('Trying to set a shallower level than before.')
            if k0 in rec:
                raise MapError("Attempt to redefine %s" % (k0))
            rec[k0] = val
            return
        if k0 not in rec:
            rec[k0] = dict()
        newrec = rec.get(k0)
        self._set(key[1:], val, newrec)
    
class _ChainedMap(_Map):
    '''A map from tuples to values, including nested scopes.
    '''
    def __init__(self, next=None):
        _Map.__init__(self)
        self._next = next

    def get(self, key):
        myval = super(_ChainedMap,self).get(key)
        if self._next is None: return myval
        nextval = self._next.get(key)
        # myval in the recursive calls will never be None, so it is
        # checked here explicitly.
        if myval is None: return nextval
        
        return self._combine_thing(myval, nextval)
        
    def _combine_dicts(self, myval, result):
        '''Combine two trees of dictionaries, destructively altering
        'result'.  Any path present in either is present in 'result'.
        myval is not altered.
        '''
        assert type(myval) is dict
        assert type(result) is dict

        for (k, v) in myval.items():
            result[k] = self._combine_thing(v, result.get(k))
        return result
    
    def _combine_thing(self, myval, nextval):
        '''Combines either two dictionaries or two values.  myval may
        not be None, but nextval may be.  If both are dictionaries,
        the returned value combines the trees at all levels.  If both
        are values, then myval is returned.  Raises MapError if one is
        a dictionary and the other isn't.
        '''
        # The above should be detected at "set", but isn't; see comment at set
        assert myval != None
        if type(myval) is dict:
            if nextval == None:
                return self._combine_dicts(myval, dict())
            if type(nextval) is not dict:
                raise MapError('Conflict on depth of array at different scopes')
            return self._combine_dicts(myval, nextval.copy())
        else:
            if type(nextval) is dict:
                raise MapError('Conflict on depth of array at different scopes')
            return myval
    
    def set(self, key, val):
        if self._next and type(self._next.get(key)) is dict:
            raise MapError('Conflict on depth of array at different scopes')
        # bug: lacking backpointers, this can't check for children having
        # a dictionary for this value. That case will raise MapError when
        # the child's get is called.
        return super(_ChainedMap, self).set(key, val)

class Publisher(_Map):
    '''A tdb Publisher object.'''
    NAME = 'name'
    
    def __init__(self):
        '''Constructor.'''
        super(Publisher, self).__init__()
    
    def name(self): return self.get(Publisher.NAME)

class Title(_Map):
    '''A tdb Title object.'''
    NAME = 'name'
    TYPE = 'type'
    EISBN = 'eisbn'
    EISSN = 'eissn'
    ISBN = 'isbn'
    ISSN = 'issn'
    ISSNL = 'issnl'
    PUBLISHER = 'publisher'
    TYPE = 'type'
    class Type:
        BOOK = 'book'
        JOURNAL = 'journal'
        DEFAULT = JOURNAL

    def __init__(self):
        '''Constructor.'''
        super(Title, self).__init__()
    
    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

    def name(self): return self.get(Title.NAME)
    def type(self): return self.get(Title.TYPE)
    def eisbn(self): return self.get(Title.EISBN)
    def eissn(self): return self.get(Title.EISSN)
    def isbn(self): return self.get(Title.ISBN)
    def issn(self): return self.get(Title.ISSN)
    def issnl(self): return self.get(Title.ISSNL)
    def publisher(self): return self.get(Title.PUBLISHER)
    def type(self): return self.get(Title.TYPE) or Title.Type.DEFAULT

class AU(_ChainedMap):
    '''Adds convenience getters to a _ChainedMap.'''
    class Status:
        DOES_NOT_EXIST = 'doesNotExist'
        DO_NOT_PROCESS = 'doNotProcess'
        EXISTS = 'exists'
        EXPECTED = 'expected'
        MANIFEST = 'manifest'
        WANTED = 'wanted'
        CRAWLING = 'crawling'
        TESTING = 'testing'
        NOT_READY = 'notReady'
        READY = 'ready'
        PRE_RELEASING = 'preReleasing'
        PRE_RELEASED = 'preReleased'
        RELEASING = 'releasing'
        RELEASED = 'released'
        DOWN = 'down'
        SUPERSEDED = 'superseded'
        ZAPPED = 'zapped'

    ATTR = 'attr'
    EDITION = 'edition'
    EISBN = 'eisbn'
    ISBN = 'isbn'
    NAME = 'name'
    NONDEFPARAM = 'nondefparam'
    PARAM = 'param'
    PLUGIN = 'plugin'
    PLUGIN_PREFIX = 'pluginPrefix'
    PLUGIN_SUFFIX = 'pluginSuffix'
    PROXY = 'proxy'
    RIGHTS = 'rights'
    STATUS = 'status'
    TITLE = 'title'
    YEAR = 'year'
    VOLUME = 'volume'
    
    def __init__(self, next=None):
        '''Constructor.'''
        super(AU, self).__init__(next)
    
    def set_title(self, title): self.set(AU.TITLE, title)

    def attr(self, attr): return self.get( (AU.ATTR, attr) )
    def attrs(self): return self.get(AU.ATTR) or {}
    def auid(self): return AU.computeAuid(self.plugin(), self.params())
    def edition(self): return self.get(AU.EDITION)
    def eisbn(self): return self.get(AU.EISBN)
    def isbn(self): return self.get(AU.ISBN)
    def name(self): return self.get(AU.NAME)
    def nondefparam(self, nondefparam):
        val = self.get( (AU.NONDEFPARAM, nondefparam) )
        if val is None or len(val) == 0: return None
        else: return val
    def nondefparams(self): return self.get(AU.NONDEFPARAM) or {}
    def param(self, param): return self.get( (AU.PARAM, param) )
    def params(self): return self.get(AU.PARAM) or {}
    def plugin(self): return self.get(AU.PLUGIN) or self.get(AU.PLUGIN_PREFIX) + self.get(AU.PLUGIN_SUFFIX)
    def proxy(self):
        val = self.get(AU.PROXY)
        if val is None or len(val) == 0: return None
        else: return val
    def rights(self): return self.get(AU.RIGHTS)
    def status(self): return self.get(AU.STATUS)
    def title(self): return self.get(AU.TITLE)
    def year(self): return self.get(AU.YEAR)
    def volumeName(self): return self.get(AU.VOLUME)

    @staticmethod
    def __auidencode(c):
        if re.match(r'^[-_*A-Za-z0-9]$', c): return c
        if c == ' ': return '+'
        return '%' + ('0' if ord(c) < 16 else '') + hex(ord(c))[2:].upper()

    @staticmethod
    def computeAuid(plugin, params):
        keys = params.keys()
        keys.sort() # in Java: by iterating over a TreeSet
        #a = '&'.join('~'.join([''.join(self.__auidencode()) for s in []]))
        return plugin.replace('.', '|') + '&' + '&'.join(['~'.join([''.join(map(AU.__auidencode, [c for c in s])) for s in [k, params[k]]]) for k in keys])

class Tdb(object):

    def __init__(self):
        '''Constructor.'''
        self.__publishers = []
        self.__titles = []
        self.__aus = []

    def add_publisher(self, publisher): self.__publishers.append(publisher)
    def add_title(self, title): self.__titles.append(title)
    def add_au(self, au): self.__aus.append(au)

    def publishers(self): return self.__publishers[:]
    def titles(self): return self.__titles[:]
    def aus(self): return self.__aus[:]
