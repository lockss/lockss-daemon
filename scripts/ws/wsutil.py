#!/usr/bin/env python

'''Utilities common to Web Services tools.'''

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.
'''

__license__ = '''\
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

__version__ = '0.1.0'

from datetime import date, datetime

try: from ZSI.auth import AUTH
except ImportError: sys.exit('Error: the ZSI Python module must be installed')

def datetimems(ms):
  '''Returns a datetime instance from a date and time expressed in milliseconds
  since epoch (or None if the input is None or negative).
  Parameters:
  - ms (numeric): a number of milliseconds since epoch
  '''
  if ms is None or ms < 0: return None
  return datetime.fromtimestamp(ms / 1000)

def datems(ms):
  '''Returns a date instance from a date and time expressed in milliseconds
  since epoch (or None if the input is None or negative).
  Parameters:
  - ms (numeric): a number of milliseconds since epoch
  '''
  if ms is None or ms < 0: return None
  return date.fromtimestamp(ms / 1000)

def durationms(ms):
  '''Returns an approximate text representation of the number of milliseconds
  given. The result is of one of the following forms:
  - 123ms (milliseconds)
  - 12s (seconds)
  - 12m34s (minutes and seconds)
  - 12h34m56s (hours, minutes and seconds)
  - 1d23h45m (days, hours and minutes)
  - 4w3d21h (weeks, days and hours)
  Parameters:
  - ms (numeric): a number of milliseconds
  '''
  s, ms = divmod(ms, 1000)
  if s == 0: return '%dms' % (ms,)
  m, s = divmod(s, 60)
  if m == 0: return '%ds' % (s,)
  h, m = divmod(m, 60)
  if h == 0: return '%dm%ds' % (m, s)
  d, h = divmod(h, 24)
  if d == 0: return '%dh%dm%ds' % (h, m, s)
  w, d = divmod(d, 7)
  if w == 0: return '%dd%dh%dm' % (d, h, m)
  return '%dw%dd%dh' % (w, d, h)

def zsiauth(u, p):
  '''Makes a ZSI authentication object suitable for ZSI-based Web Services
  modules.
  Parameters:
  - u (string): a UI username
  - p (string): a UI password
  '''
  return (AUTH.httpbasic, u, p)

