#!/bin/bash

instance=$(aws rds describe-db-instances | fx '.DBInstances[0]')
status=$(echo "$instance" | fx '.DBInstanceStatus')
identifier=$(echo "$instance" | fx '.DBInstanceIdentifier')

if [ "$status" = "stopped" ]; then
    echo "AWS RDS instance $identifier is stopped, starting..."
    aws rds start-db-instance --db-instance-identifier "$identifier"
else
    echo "Required RDS instance is already running.."
fi