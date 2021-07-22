#!/usr/bin/env python3

'''Utilities common to Web Services tools.'''

__copyright__ = '''\
Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.
'''

__license__ = '''\
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
'''

__version__ = '0.3.0'

from datetime import date, datetime
import sys, os

try: import requests.auth
except ImportError: sys.exit('The Python Requests module must be installed (or on the PYTHONPATH)')
try: import zeep
except ImportError: sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')

import logging.config

host_help_prefix = 'add [protocol://]host:port URI/Authority'

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
  given. The result is of one of the following forms (or None if the argument is
  None):
  - 123ms (milliseconds)
  - 12s (seconds)
  - 12m34s (minutes and seconds)
  - 12h34m56s (hours, minutes and seconds)
  - 1d23h45m (days, hours and minutes)
  - 4w3d21h (weeks, days and hours)
  Parameters:
  - ms (numeric): a number of milliseconds (or None)
  '''
  if ms is None: return None
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

# Last modified 2021-07-22 @markom
def file_lines(fstr):
  '''Returns a cleaned list of lines in a file.
  Parameters:
    :param fstr (string): file name
  '''
  with open(os.path.expanduser(fstr)) as f:
    ret = list(
      filter(
        lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f]
      )
    )
  if len(ret) == 0:
    sys.exit('Error: {} contains no meaningful lines'.format(fstr))
  return ret

def make_client(host, username, password, service, https=False):
  '''Returns a cleaned list of lines in a file.
  Parameters:
    :param host: a [protocol://]host:port pair (string)
            if protocol left empty, defaults to http
    :param username: a username for the host (string)
    :param password: a password for the host (string)
    :param service: a WS service on the host.(string)
              One of: HasherService, ExportService, DaemonStatusService, ContentConfigurationService, AuControlService
  '''
  session = requests.Session()
  session.auth = requests.auth.HTTPBasicAuth(username, password)
  session.verify = False
  transport = zeep.transports.Transport(session=session)
  wsdl = '{}/ws/{}?wsdl'.format(
    add_protocol(host, https),
    service)
  client = zeep.Client(wsdl, transport=transport)
  return client

def add_protocol(host, https=False):
  '''Given a host returns a <protocol><domain>:<port>
  Parameters:
    :param host: a [protocol://]host:port URI/Authority (string)
            if protocol left empty, defaults to http
  '''
  # if the protocal is already assigned, there is nothing to do
  if ('http://' == host[:7]) | ('https://' == host[:8]):
    return host
  if https:
    return 'https://' + host
  return 'http://' + host

def remove_protocol(host):
  '''Given a <protocol><domain>:<port> returns  host:port Authority
  Parameters:
    :param host: a [protocol://]host:port URI/Authority (string)
  '''
  if 'http://' == host[:7]:
    return host[7:]
  elif 'https://' == host[:8]:
    return host[8:]
  return host

def enable_zeep_debugging():
  logging.config.dictConfig({
    'version': 1,
    'formatters': {
      'verbose': {
        'format': '%(name)s: %(message)s'
      }
    },
    'handlers': {
      'console': {
        'level': 'DEBUG',
        'class': 'logging.StreamHandler',
        'formatter': 'verbose',
      },
    },
    'loggers': {
      'zeep.transports': {
        'level': 'DEBUG',
        'propagate': True,
        'handlers': ['console'],
      },
    }
  })