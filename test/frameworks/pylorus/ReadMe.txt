Pylorus content validation and ingestion gateway

Compares AU content hashes and poll results between servers, two local and
one optional remote.  Servers are randomly selected from available pools for
each AU.  The process is divided into stages to improve parallel efficiency.

Validate (hash):
    crawl
    hash
    compare
    report

Ingest (poll):
    crawl
    poll
    report

Run "pylorus.py -h" for usage instructions.

Pylorus configures itself in the following sequence:
  1. The file /etc/pylorus.conf
  2. The file ~/.pylorusrc
  3. The command-line configuration file or else one configured above.
  4. Other command-line parameters

The included file pylorus.conf lists the user defaults commented out; the values can be
changed in configuration files or on the command line.
