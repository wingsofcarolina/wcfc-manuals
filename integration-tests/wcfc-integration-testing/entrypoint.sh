#!/bin/bash
set -e

SUPERVISORD_ARGS=""
if [ $# -ne 0 ]; then
  SUPERVISORD_ARGS="--silent"
fi

# Start supervisord in the background
/usr/bin/supervisord -c /etc/supervisord.conf ${SUPERVISORD_ARGS} &

# If no args passed, just wait on supervisord
if [ $# -eq 0 ]; then
    wait -n
else
    # Run whatever command the user supplied
    exec "$@"
fi

