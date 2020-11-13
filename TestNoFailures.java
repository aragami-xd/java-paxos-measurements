public class TestNoFailures {

	/**
	 * run test	
	 * @param nodeCount how many nodes in the network
	 * @param concurrentProposal how many nodes propose at the same time
	 * @param runs how many runs (recommend 10 runs)
	 */
	public TestNoFailures(int nodeCount, int concurrentProposal, int runs) {
		System.out.println("config");
		System.out.println("nodes:                " + nodeCount);
		System.out.println("concurrent proposals: " + concurrentProposal);
		System.out.println("runs:                 " + runs + "\n");
		
		Node[] nodes = new Node[nodeCount];

		// create nodes
		for (int i = 0; i < nodes.length; i++) {
			final int t = i;

			new Thread(() -> {
				nodes[t] = new Node(nodes.length, t);
				nodes[t].start();
			}).start();
		}

		// all variables to run the test
		Result[] results = new Result[concurrentProposal];
		Thread[] threads = new Thread[concurrentProposal];

		long[] successNodes = new long[runs]; // timers for successful runs
		long successTotal = 0; // total time for all successful runs

		long[] failedNodes = new long[concurrentProposal]; // timers for failed nodes each run
		long[] failedNodesAverage = new long[runs]; // average time for all failed nodes each run
		long failedTotal = 0; // total time for all average failed nodes above

		// nodes propose
		for (int i = 0; i < runs; i++) {
			System.out.println("run " + i);
			final int i_ = i;

			// new thread for each concurrent proposal
			for (int t = 0; t < concurrentProposal; t++) {
				final int t_ = t;

				threads[t_] = new Thread(() -> {
					long start = System.nanoTime();
					results[t_] = nodes[t_].propose(0, 0, 0);
					long end = System.nanoTime();

					if (results[t_].p3) // if Result.p3 == true, proposal successful (phase 3 successful)
						successNodes[i_] = end - start;
					else
						failedNodes[t_] = end - start;
				});
				threads[t_].start();
			}

			// wait for all threads to join and calculate the result
			long failedNodesTotal = 0;
			for (int t = 0; t < concurrentProposal; t++) {
				try {
					threads[t].join();
				} catch (Exception e) {
					System.err.println(e.toString());
					e.printStackTrace();
				}

				failedNodesTotal += failedNodes[t];
			}

			successTotal += successNodes[i_];
			if (concurrentProposal > 1) {
				failedNodesAverage[i] = failedNodesTotal / (concurrentProposal - 1);
				failedTotal += failedNodesAverage[i]; // 1 successful node
			}
		}

		// stop all threads
		for (int i = 0; i < nodes.length; i++)
			nodes[i].stop();

		// print result
		System.out.println("\nruntime result for each run (in nanoseconds): ");
		for (int i = 0; i < runs; i++)
			System.out.println("success: " + successNodes[i] + " -- average failed: " + failedNodesAverage[i]);

		System.out.println("\naverage success time: " + successTotal / (runs * 1000000) + "(ms)");
		System.out.println("average failed time:  " + failedTotal / (runs * 1000000) + "(ms)");

		System.exit(0); // will it close all sockets?
	}

	public static void main(String[] args) {
		if (args.length > 3 && args[2].equals("t")) // if print node running result
			Node.PRINT = true;

		new TestNoFailures(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	}
}