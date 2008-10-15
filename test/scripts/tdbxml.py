#!/usr/bin/python

# $Id: tdbxml.py,v 1.1 2008-10-15 00:59:53 thib_gc Exp $
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

from tdbconst import *

def __preamble(tdb, options):
    if options.style == STYLE_XML_ENTRIES: return
    print '''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE lockss-config [
<!ELEMENT lockss-config (if|property)+>
<!ELEMENT property (property|list|value|if)*>
<!ELEMENT list (value)+>
<!ELEMENT value (#PCDATA)>
<!ELEMENT test EMPTY>
<!ELEMENT and (and|or|not|test)*>
<!ELEMENT or (and|or|not|test)*>
<!ELEMENT not (and|or|not|test)*>
<!ELEMENT if (and|or|not|then|else|test|property)*>
<!ELEMENT then (if|property)*>
<!ELEMENT else (if|property)*>
<!ATTLIST property name CDATA #REQUIRED>
<!ATTLIST property value CDATA #IMPLIED>
<!ATTLIST test hostname CDATA #IMPLIED>
<!ATTLIST test group CDATA #IMPLIED>
<!ATTLIST test daemonVersionMin CDATA #IMPLIED>
<!ATTLIST test daemonVersionMax CDATA #IMPLIED>
<!ATTLIST test daemonVersion CDATA #IMPLIED>
<!ATTLIST test platformVersionMin CDATA #IMPLIED>
<!ATTLIST test platformVersionMax CDATA #IMPLIED>
<!ATTLIST test platformVersion CDATA #IMPLIED>
<!ATTLIST test platformName CDATA #IMPLIED>
<!ATTLIST if hostname CDATA #IMPLIED>
<!ATTLIST if group CDATA #IMPLIED>
<!ATTLIST if daemonVersionMin CDATA #IMPLIED>
<!ATTLIST if daemonVersionMax CDATA #IMPLIED>
<!ATTLIST if daemonVersion CDATA #IMPLIED>
<!ATTLIST if platformVersionMin CDATA #IMPLIED>
<!ATTLIST if platformVersionMax CDATA #IMPLIED>
<!ATTLIST if platformVersion CDATA #IMPLIED>
<!ATTLIST if platformName CDATA #IMPLIED>
<!ATTLIST list append CDATA #IMPLIED>
]>
'''

def __escape(str):
    from xml.sax import saxutils
    return saxutils.escape(str)

def __introduction(tdb, options):
    if options.style == STYLE_XML_ENTRIES: return
    print '''<lockss-config>
'''

def __process(tdb, options):
    pass

def __conclusion(tdb, options):
    if options.style == STYLE_XML_ENTRIES: return
    print '</lockss-config>'

def __postamble(tdb, options):
    pass

def tdb_to_xml(tdb, options):
    __preamble(tdb, options)
    __introduction(tdb, options)
    __process(tdb, options)
    __conclusion(tdb, options)
    __postamble(tdb, options)
