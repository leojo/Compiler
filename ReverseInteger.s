reverse(num){
	var numRev, remainingNum, digit;
	remainingNum = num;
	numRev = 0;
	while(remainingNum>0){
		digit = remainingNum%10;
		remainingNum = (remainingNum-digit)/10;
		numRev = (numRev*10)+digit;
	};
	return numRev;
}

main(){
	writeln("12345 reversed is "++reverse(12345));
}