#!/bin/bash
while true
do
  a=`netstat -s| grep passive`
  echo -ne "\r$a"
  sleep 1
done
