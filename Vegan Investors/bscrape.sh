#!/usr/bin/env bash

# checks if keywords appear in a webpage
# usage: just run it (jri)

# list of stock tickers (should get these from a csv file though)
tickers=("HTG.L" "AMPE" "TSN" "BYND")

template="https://finance.yahoo.com/quote/"

for ticker in ${tickers[@]}; do
    url=$template$ticker"/profile"
    echo $url
    curl -s $url | grep -ciE "plant-based" 
done

exit

