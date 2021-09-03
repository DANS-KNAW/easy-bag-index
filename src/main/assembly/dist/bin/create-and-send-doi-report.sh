#!/usr/bin/env bash
#
# Helper script to create and send a list of all DOIs stored in a given bag-store
#
# Usage: ./create-and-send-doi-report.sh <dark-host> <bag-store> <doi-prefix> <from> <to> <bcc>
#

DARK_HOST=$1
DOI_PREFIX=$2
FROM=$3
TO=$4
BCC=$5
TMPDIR=/tmp
DATE=$(date +%Y-%m-%d)
REPORT=$TMPDIR/$DOI_PREFIX-doi-report-$DATE.csv
MINDEPTH=3
MAXDEPTH=5


if [ "$FROM" == "" ]; then
    FROM_EMAIl=""
else
    FROM_EMAIL="-r $FROM"
fi

if [ "$BCC" == "" ]; then
    BCC_EMAILS=""
else
    BCC_EMAILS="-b $BCC"
fi

TO_EMAILS="$TO"

exit_if_failed() {
    local EXITSTATUS=$?
    if [ $EXITSTATUS != 0 ]; then
        echo "ERROR: $1, exit status = $EXITSTATUS"
        echo "Report generation FAILED. Contact the system administrator." |
        mail -s "FAILED: Report: List of archived DOIs with prefix $DOI_PREFIX" \
             $FROM_EMAIL $BCC_EMAILS $TO
        exit 1
    fi
    echo "OK"
}

echo -n "Creating list of DOIs with prefix $DOI_PREFIX..."
psql -U easy_bag_index easy_bag_index -c "\copy (select doi from bag_info where doi like '$DOI_PREFIX/%') to $REPORT"
exit_if_failed "DOI list creation failed"

echo -n "Compressing report..."
zip ${REPORT}.zip $REPORT

echo -n "Sending e-mail..."
echo
echo -e "List of archived DOIs with prefix $DOI_PREFIX in attached file." | mail -s "$DARK_HOST DOI report for prefix $DOI_PREFIX" -a ${REPORT}.zip $BCC_EMAILS $FROM_EMAIL $TO_EMAILS
exit_if_failed "sending of e-mail failed"
