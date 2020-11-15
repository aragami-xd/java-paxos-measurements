import java.util.ArrayList;

public class TestRunnerPercentageFails {
	
	public static void main(String[] args) {
		// new Test(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
		// Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		// Create a CSV with results
		ArrayList<String> lines = new ArrayList<String>();

		String header = Util.generateCSVHeader();

		lines.add(header);

		// variables
		int minNumNodes = Integer.parseInt(args[0]);
		int maxNumNodes = Integer.parseInt(args[1]);

		int minConcurrent = Integer.parseInt(args[2]);
		int maxConcurrent = Integer.parseInt(args[3]);

		// constants
		double percentFailures = Double.parseDouble(args[4]);

		int timeout = Integer.parseInt(args[5]);
		int runs = Integer.parseInt(args[6]);
		String output = args[7];

		// Usually the first test takes wayy longer and then the rest take way less
		// time for some reason, probably low level computer caching related
		// To account for this, a dummy-test with the minimum required parameters is
		// being run before the actual tests
		new Test(3, 1, 1, 0, 250);

		for (int numNodes = minNumNodes; numNodes <= maxNumNodes; numNodes++) {
			for (int concurrentProposals = Util.min(minConcurrent, numNodes); concurrentProposals <= Util.min(numNodes,
					maxConcurrent); concurrentProposals++) {

				int failures = ((int)(Math.floor(((double)numNodes)*percentFailures)));
				if(failures<0)
						failures=0;

				Test results = new Test(numNodes, concurrentProposals, runs, failures, timeout);

				String row = Util.generateDatapoint(results);

				lines.add(row);
			}
		}

		Util.writeCSV(lines, output);

		System.exit(0);
	}
}
