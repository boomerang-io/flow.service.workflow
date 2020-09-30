#!/bin/zsh

counter=0

while mvn clean test > ~/testlogs/test-logs-${counter}.txt; do
   counter=$((counter+1))
   echo $counter
done


