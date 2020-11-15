import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Util {
	public static int min(int a, int b) {
		return (a < b ? a : b);
	}

	public static String generateCSVHeader() {
		String header = "" + "Nodes," + "Timeout used," + "Concurrent Proposals," + "Failures," + "Runs," + "Successes,"
				+ "Failures," + "proposal success time average," + "proposal success time stdev,"
				+ "proposal fail time average," + "proposal fail time stdev," + "messages per run average,"
				+ "messages per run stdev";
		return header;
	}

	// Rows in the CSV we're going to print. should be in the same as the header

	public static String generateDatapoint(Test results) {
		return "" + results.nodeCount + "," + results.timeout + "," + results.concurrentProposal + "," + results.failures
				+ "," + results.runs + "," + results.numberOfSuccesses + "," + results.numberOfFailures + ","
				+ results.proposalSuccessTime + "," + results.proposalSuccessTimeStdev + "," + results.proposalRejectTime + ","
				+ results.proposalRejectTimeStdev + "," + results.averageMessages + "," + results.averageMessagesStdev;
	}

	public static void writeCSV(ArrayList<String> lines, String dir) {
		try {
			File file = new File(dir);
			File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			PrintWriter writer = new PrintWriter(new FileOutputStream(file));

			for (String line : lines) {
				writer.println(line);
			}

			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
