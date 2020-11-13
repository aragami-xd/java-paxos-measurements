all:
	javac -d bin *.java

clean:
	rm bin/*.class

s := 99  # size (no. nodes)
c := 2   # concurrent (no. nodes proposal at a time)
r := 10  # runs

# no failures
nf:
	java -cp bin TestNoFailures ${s} ${c} ${r}


# with failures
wf:
	java -cp bin TestFailures ${s} ${c} ${r}