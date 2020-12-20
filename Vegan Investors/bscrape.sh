#!/usr/bin/env bash

# checks if keywords appear in a webpage
# usage: just run it (jri)

# list of stock tickers (should get these from a csv file though)
tickers=("TSN" "HTG.L" "AMPE" "TSN" "BYND" "VRYYF" "EATS.CN")

# list of yes keywords
yy="vegan|plant-based|cruelty-free|dairy-free|egg-free"

template="https://finance.yahoo.com/quote/"

for ticker in ${tickers[@]}; do
    url=$template$ticker"/profile"
    isthere=`curl -s $url | grep -ciE $yy`
    echo "$ticker : $isthere occurences"
done

exit
