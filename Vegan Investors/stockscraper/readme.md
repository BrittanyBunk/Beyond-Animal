howto use the tsv files

the organization is under the following columns:
Symbol - ticker
Company - name of issuing company:
- yay - total count of yay keyword appearences in Desc (see yay.txt file)
- nay - total count of nay keyword appearences in Desc (see nay.txt file)
Desc - description pulled from finance.yahoo.com

the rows are sorted by yay column as per brittany's suggestion that we prioritize by yay keywords such as plant-based and cruelty-free.

doing so was a necessary decision since 
	some of the goodies such as BYND will have nay words like 'meat' and 'dairy'.
	some of the nasties such as IMO will have yay words like 'produce' (from 'produces')

so to make use of the system, one scans the yays (there are 35 of them in AMEX) and examines the Desc to see if it is suitable. obviously, IMO isn't.

something like BYND (from NASDAQ) will have a lot of nays in it, but will still show up with the yays because we've given these keywords the priority.

the system can still use some improvements, but it helps to locate potential yays and nays.
it should be utilized as a guide to making decisions (since it is not a fully automated system ... yet).
