#!/usr/bin/python

# $Id: tdb.py,v 1.9 2009-01-25 01:34:35 thib_gc Exp $
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

class Map(object):
    
    def __init__(self, dic={}):
        '''Constructor.'''
        object.__init__(self)
        self._dictionary = dic.copy()

    def get(self, key):
        if type(key) is not tuple: key = ( key, )
        return self._dictionary.get(key) or self._prefix_get(key)
    
    def set(self, key, value):
        if type(key) is not tuple: key = ( key, )
        self._dictionary[key] = value
    
    def _prefix_get(self, key):
        result = dict()
        for k in self._dictionary.keys():
            if k[0:len(key)] == key:
                result[k[len(key):]] = self._dictionary[k]
        if len(result) == 0: return None
        return result
    
class ChainedMap(Map):
    
    def __init__(self, next=None):
        Map.__init__(self)
        self._next = next

    def get(self, key):        
        myval = super(ChainedMap,self).get(key)
        if self._next is None: return myval
        nextval = self._next.get(key)
        if nextval and type(nextval) is dict:
            if myval: nextval.update(myval)
            return nextval
        else: return myval or nextval

class Publisher(Map):

    NAME = 'name'

    def __init__(self):
        '''Constructor.'''
        Map.__init__(self)

    def name(self): return self.get(Publisher.NAME)

class Title(Map):

    NAME = 'name'
    PUBLISHER = 'publisher'

    def __init__(self):
        '''Constructor.'''
        Map.__init__(self)

    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

    def name(self): return self.get(Title.NAME)
    def publisher(self): return self.get(Title.PUBLISHER)

class AU(ChainedMap):

    ATTR = 'attr'
    ISSN = 'issn'
    NAME = 'name'
    PARAM = 'param'
    PLUGIN = 'plugin'
    RIGHTS = 'rights'

    STATUS = 'status'
    STATUS_DOES_NOT_EXIST = 'does_not_exist'
    STATUS_DO_NOT_PROCESS = 'do_not_process'
    STATUS_EXISTS         = 'exists'
    STATUS_MANIFEST       = 'manifest'
    STATUS_TESTING        = 'testing'
    STATUS_NOT_READY      = 'not_ready'
    STATUS_READY          = 'ready'
    STATUS_PRE_RELEASING  = 'pre_releasing'
    STATUS_PRE_RELEASED   = 'pre_released'
    STATUS_RELEASING      = 'releasing'
    STATUS_RELEASED       = 'released'
    STATUS_DOWN           = 'down'
    STATUS_SUPERSEDED     = 'superseded'
    STATUS_RETRACTED      = 'retracted'
    STATUSES = (STATUS_DOES_NOT_EXIST,
                STATUS_DO_NOT_PROCESS,
                STATUS_EXISTS,
                STATUS_MANIFEST,
                STATUS_TESTING,
                STATUS_NOT_READY,
                STATUS_READY,
                STATUS_PRE_RELEASING,
                STATUS_PRE_RELEASED,
                STATUS_RELEASING,
                STATUS_RELEASED,
                STATUS_DOWN,
                STATUS_SUPERSEDED,
                STATUS_RETRACTED)
    
    TITLE = 'title'

    def __init__(self, next=None):
        '''Constructor.'''
        ChainedMap.__init__(self, next)

    def set_title(self, title): self.set(AU.TITLE, title)

    def attr(self, attr): return self.get(( AU.ATTR, attr ))
    def attrs(self): return self.get(AU.ATTR) or dict()
    def issn(self): return self.get(AU.ISSN)
    def name(self): return self.get(AU.NAME)
    def param(self, param): return self.get(( AU.PARAM, param ))
    def params(self): return self.get(AU.PARAM) or dict()
    def plugin(self): return self.get(AU.PLUGIN)
    def rights(self): return self.get(AU.RIGHTS)
    def status(self): return self.get(AU.STATUS)
    def title(self): return self.get(AU.TITLE)

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
