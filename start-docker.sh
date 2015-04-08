#!/bin/sh

SCRIPT=$(find . -type f -name business-customer-frontend)
exec $SCRIPT \
  $HMRC_CONFIG
