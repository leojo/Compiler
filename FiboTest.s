fibo(n){
	if(n<2){
		return 1;
	};
	return fibo(n-1)+fibo(n-2);
}

main(){
	writeln("fibo(30)= "++fibo(30));
}