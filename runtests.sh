#ensure that the line endings are LF (UNIX) before running this script otherwise the ^M character will be appended to the filenames
#build
javac -d bin *.java
#two different testing commands, one applies a set number of failures while another applies a percentage
#java -cp bin TestRunner [minNodes] [maxNodes] [minConcurent] [maxConcurrent] [minFails] [maxFails] [timeout] [runsToAggregateOver][outputFilename]
java -cp bin TestRunner 3 20 1 1 1 1 100 20 Outputs/3-20Nodes1Failure.csv
#java -cp bin TestRunnerPercentageFails [minNodes] [maxNodes] [minConcurent] [maxConcurrent] [percentFailures (between 0 and 1)] [timeout] [runsToAggregateOver][outputFilename]
java -cp bin TestRunnerPercentageFails 3 20 1 1 0 100 20 Outputs/3-20Nodes0Fails.csv
java -cp bin TestRunnerPercentageFails 3 20 1 1 0.1 100 20 Outputs/3-20Nodes10percentFails.csv
java -cp bin TestRunnerPercentageFails 3 20 1 1 0.5 100 20 Outputs/3-20Nodes50percentFails.csv
java -cp bin TestRunnerPercentageFails 3 20 1 1 0.9 100 20 Outputs/3-20Nodes90percentFails.csv

