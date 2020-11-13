all:
	javac -d bin *.java

clean:
	rm bin/*.class

s := 9   # size (no. nodes)
c := 2   # concurrent (no. nodes proposal at a time)
r := 10  # runs
f := 2   # no failure nodes

# no failures
run:
	java -cp bin Test ${s} ${c} ${r} ${f}