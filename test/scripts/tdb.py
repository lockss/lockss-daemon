#!/usr/bin/python

# $Id: tdb.py,v 1.3 2008-08-08 22:11:48 thib_gc Exp $
#
# Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

class TdbObject(object):

    def __init__(self):
        self._dictionary = {}

    def get(self, key):
        return self._dictionary.get(key)

    def geti(self, indexed_key):
        key, index = self._key(indexed_key)
        if index:
            if key not in self._dictionary: return None
            else: return self._dictionary[key].get(index)
        else:
            if key not in self._dictionary: return {}
            else: return self._dictionary[key].copy()

    def set(self, key, value):
        self._dictionary[key] = value

    def seti(self, indexed_key, value):
        key, index = self._key(indexed_key)
        if index:
            if key not in self._dictionary: self._dictionary[key] = {}
            self._dictionary[key][index] = value.copy()
        else:
            self._dictionary[key] = value.copy()

    def _key(self, str):
        match = re.match(r'([^\[]+)(?:\[(\w+)\])?$', str)
        if match: return (match.group(1), match.group(2))
        else: raise KeyError, 'invalid key: %s' % (str,)

    def __repr__(self): return repr(self._dictionary)

class ChainedTdbObject(TdbObject):

    def __init__(self, next=None):
        TdbObject.__init__(self)
        self._next = next

    def get(self, key):
        elem = super(ChainedTdbObject,self).get(key)
        if elem or self._next is None: return elem
        return self._next.get(key)

    def geti(self, indexed_key):
        key, index = self._key(indexed_key)
        elem = super(ChainedTdbObject,self).geti(key)
        pelem = {}
        if self._next: pelem = self._next.geti(key)
        pelem.update(elem)
        if index: return pelem.get(index)
        else: return pelem

class Publisher(TdbObject):

    NAME = 'name'

    def name(self): return self.get(Title.NAME)
#    def set_name(self, name): self.set(Title.NAME, name)

class Title(TdbObject):

    NAME = 'name'
    PUBLISHER = 'publisher'

    def name(self): return self.get(Title.NAME)
#    def set_name(self, name): self.set(Title.NAME, name)
    def publisher(self): return self.get(Title.PUBLISHER)
    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

class AU(ChainedTdbObject):

    NAME = 'name'
    TITLE = 'title'

    def __init__(self, next=None):
        ChainedTdbObject.__init__(self, next)

    def name(self): return self.get(AU.NAME)
#    def set_name(self, name): self.set(AU.NAME, name)
    def title(self): return self.get(AU.TITLE)
    def set_title(self, title): self.set(AU.TITLE, title)

class Tdb(object):

    def __init__(self):
        self.__publishers = []
        self.__titles = []
        self.__aus = []

    def add_publisher(self, publisher): self.__publishers.append(publisher)
    def add_title(self, title): self.__titles.append(title)
    def add_au(self, au): self.__aus.append(au)

    def publishers(self): return self.__publishers[:]
    def titles(self): return self.__titles[:]
    def aus(self): return self.__aus[:]

    def internal_print(self):
        print repr(self.publishers())
        print self.titles()
        print self.aus()

#if __name__ == '__main__':
#    from tdbparse import TdbScanner, TdbParser
#    from sys import stdin
#    tdb = Tdb()
#    scanner = TdbScanner(stdin)
#    parser = TdbParser(scanner)
#    tdb = parser.parse()
#    tdb.internal_print()
