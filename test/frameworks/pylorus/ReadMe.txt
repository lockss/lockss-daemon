Pylorus content validation and ingestion gateway

By Michael R Bax <michael.bax@gmail.com>, 10/08+


Summary

Compares AU content hashes and poll results between servers.  Two local servers and (if available) remote servers are chosen for each AU.  Servers are randomly selected from the available local and remote pools; one remote server for validation and two for ingestion).  


Configuration

The included file pylorus.conf lists the user defaults (commented out); the values can be changed in configuration files or on the command line.

Pylorus configures itself in the following sequence:
    1. The file /etc/pylorus.conf
    2. The file ~/.pylorusrc
    3. The command-line configuration file
    4. Other command-line parameters

If one configuration file specifies another, the second will be processed immediately after the first.  Note that a configuration file will be processed only the first time that it is seen, even if it is specified in multiple places.


Usage

If you wish to validate (hash) an AU using the settings in a local file named pylorus.conf, then you would run
    pylorus.py -c pylorus.conf validate:some|plugin~base_url~http%3A%2F%2F...

If you have set up your default personalized configuration in ~/.pylorusrc and have a list of AU ID's to ingest (poll) in a local file named AU_IDs.txt, then you would run
    pylorus.py ingest:@AU_IDs.txt

Run "pylorus.py -h" for more command-line usage help.


Details

The process is divided into stages to improve parallel efficiency.

    Validate (hash):
        crawl
        hash
        compare
        report

    Ingest (poll):
        crawl
        poll
        report
