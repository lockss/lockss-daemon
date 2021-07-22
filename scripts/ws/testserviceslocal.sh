#!/usr/bin/env bash
# TODO: add checks in this file instead of forcing the user to inspect terminal outputs.
# Step 1. Run a local daemon
# Step 2. Confirm the port that the UI is served on is 8081, otherwise, set 'LOCAL8081' variable on line 10 of this file
# Step 3. Get an AUid or else use the one already assigned to 'AUID' on line 11
# Step 4. Make sure you have environment variables LOCKSSU and LOCKSSUPW set to the local daemon user and password
# Step 5. Run this file $. testserviceslocal.sh
# Step 6. Inspect the output for errors. Note: --verbose, and --debug-zeep are set, unset them if desired

LOCAL8081='localhost:8081'
AUID='org|lockss|plugin|springer|link|SpringerLinkJournalsPlugin&base_url~https%3A%2F%2Flink%2Espringer%2Ecom%2F&download_url~http%3A%2F%2Fdownload%2Espringer%2Ecom%2F&journal_eissn~1179-2043&volume_name~831'

echo 'BASIC USAGE TEST: '
python contentconfigurationservice.py
python daemonstatusservice.py
python exportservice.py
python hasherservice.py
python aucontrolservice.py
echo 'BASIC USAGE TEST DONE: '

echo '************ TESTING daemonstatusservice.py ******************************'
python daemonstatusservice.py --host=$LOCAL8081 --auid=$AUID --get-au-status --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
echo '************  DONE W daemonstatusservice.py ******************************'

echo '************ TESTING hasherservice.py ************************************'
echo '************ ******* HASHING CAN TAKE SOME TIME **************************'
echo '************ ******* CHECK THE WORKING DIRECTORY FOR A HASH FILE *********'
echo '# Block hashes from host, TIMESTAMPE'
echo "# AU: AU_NAME"
echo '# Hash algorithm: SHA-1'
echo '# Encoding: Hex'
echo ''
echo "55B74FC7B865E0FFEF16D5813E48C87A7D050261   $AUID"
echo '# end'
python hasherservice.py --host=$LOCAL8081   --auid=$AUID --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
echo '************  DONE W hasherservice.py ************************************'

echo '************ TESTING aucontrolservice.py *********************************'
python aucontrolservice.py --host=$LOCAL8081 --auid=$AUID --request-deep-crawl-by-id --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
echo '************  DONE W aucontrolservice.py *********************************'

echo '************ TESTING contentconfigurationservice.py **********************'
python contentconfigurationservice.py --host=$LOCAL8081  --deactivate-aus --auid=$AUID --verbose --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
python contentconfigurationservice.py --host=$LOCAL8081  --reactivate-aus --auid=$AUID --verbose --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
echo '************  DONE W contentconfigurationservice.py **********************'

echo '************ TESTING exportservice.py ************************************'
echo '************ ******* EXPORTING CAN TAKE SOME TIME **************************'
echo '************ ******* CHECK THE WORKING DIRECTORY FOR A ZIP FILE *********'
python exportservice.py --host=$LOCAL8081  --auid=$AUID --create-export-files --username=$LOCKSSU --password=$LOCKSSUPW --debug-zeep
echo '************  DONE W exportservice.py **********************'
