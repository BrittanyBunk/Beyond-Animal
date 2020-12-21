#!/usr/bin/env bash

# checks if keywords appear in a webpage
# usage: just run it (jri)

FILE=$1

ar=()
while read -ra a b; do
    ar+=("$a")
done < "$FILE"
tickers="${ar[@]:1}"

# list of yes keywords
yy="vegan|plant-based|cruelty-free|dairy-free|egg-free"

template="https://finance.yahoo.com/quote/"

for ticker in ${tickers[@]}; do
    url=$template$ticker"/profile"
    isthere=`curl -s $url | sed -e "s/Description(.*)Corporate Governance/\0/" | grep -ciE $yy`
    if [ $isthere -gt 0 ]
    then
       echo "$ticker : $isthere occurences"
    fi
done

exit
