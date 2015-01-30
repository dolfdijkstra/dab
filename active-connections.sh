#!/bin/bash
while true
do
  a=`netstat -s| grep active`
  echo -ne "\r$a"
  sleep 1
done
