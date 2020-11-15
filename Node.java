import java.io.*;
import java.net.*;
import java.util.Vector;

/**
 * acceptor promise in phase 1b and 2b
 */
class AcceptorResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	public boolean accepted; // message accepted or not
	public int currentIdentifier; // current identifier on that acceptor
	public int currentValue; // current value on that acceptor

	public AcceptorResponse(boolean accepted, int proposalIdentifier, int proposalValue) {
		this.accepted = accepted;
		this.currentIdentifier = proposalIdentifier;
		this.currentValue = proposalValue;
	}
}

/**
 * proposer accept request in phase 2a
 */
class AcceptRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	public int proposalIdentifier;
	public int proposalValue;

	public AcceptRequest(int proposalIdentifier, int proposalValue) {
		this.proposalIdentifier = proposalIdentifier;
		this.proposalValue = proposalValue;
	}
}

/**
 * proposal result
 */
class Result {
	public boolean p1 = false, p2 = false, p3 = false; // status for each phase, true if success, false otherwise
	public int p1Ack = 0, p2Ack = 0, p3Ack = 0; // how many nodes acked back for each phase
	public int identifier = 0, value = 0; // final identifier & value used for proposal (ignore this if proposal failed)

	// added for the assignment for extra measurement info
	public int messages;

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Result))
			return false;

		Result res = (Result) obj;
		return p1 == res.p1 && p2 == res.p2 && p3 == res.p3 && p1Ack == res.p1Ack && p2Ack == res.p2Ack
				&& p3Ack == res.p3Ack && identifier == res.identifier && value == res.value && messages == res.messages;
	}

	@Override
	public String toString() {
		String res = "";
		res += p1 ? p2 ? p3 ? "success\n" : "failed at p3\n" : "failed at p2\n" : "failed at p1\n";
		res += ("p1 ack = " + p1Ack + "\np2 ack = " + p2Ack + "\np3 ack = " + p3Ack + "\n");
		res += ("identifier = " + identifier + "\n");
		res += ("value = " + value + "\n");
		res += ("messages = " + messages + "\n");
		return res;
	}
}

/**
 * nodes in the solution. each node can be the proposer and acceptor at the same time
 */
public class Node {
	/**
	 * ------------------------------------------------------------------------
	 * misc
	 */
	private int id; // used for identifying
	private Vector<Integer> address = new Vector<>(); // address to connect with other nodes
	public int timeout = 1000;

	boolean online = true; // whether node is online

	/**
	 * ------------------------------------------------------------------------
	 * networking stuff
	 */

	// acceptor is essentially a server, and proposer is a client sending request to them (create socket upon request)
	ServerSocket acceptorSocket;

	/**
	 * node constructor initializes the acceptor server
	 * @param count how many nodes are there
	 * @param id id of the node => used to determine listen port (300x)
	 * @param timeout The time that a node will wait for a response before assuming a sender has failed
	 */
	public Node(int count, int id, int timeout) {
		this.id = id;
		this.timeout = timeout;
		
		address.setSize(count);
		for (int i = 0; i < address.size(); i++)
			if (i != id)
				address.set(i, 50000 + i);

		try {
			acceptorSocket = new ServerSocket(50000 + id);

		} catch (Exception e) {
		}

		threads.setSize(count);
		p1Return.setSize(count);
		p2Return.setSize(count);
		p3Return.setSize(count);
	}

	/**
	 * start acceptor server. proposer doesn't need to start or stop
	 */
	public void start() {
		while (!acceptorSocket.isClosed()) {
			try {
				Socket socket = acceptorSocket.accept();

				new AcceptorThread(this, socket).start();

			} catch (SocketException e) {
			} catch (Exception e) {
			}
		}
	}

	/**
	 * stop acceptor
	 */
	public void stop() {
		try {
			acceptorSocket.close();

		} catch (Exception e) {
		}
	}

	/**
	* write an object to the socket stream, the object must be serializable
	* @param socket socket to write to
	* @param data data to write to
	*/
	public void write(Socket socket, Object data) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(data);
			out.flush();

		} catch (Exception e) {
		}
	}

	/**
	 * read object from the socket stream, can be manually casted later
	 * @param socket socket to read from
	 * @return read object
	 */
	public Object read(Socket socket) throws SocketTimeoutException {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			return in.readObject();

		} catch (SocketTimeoutException e) { // forward timeout exception to caller
			throw new SocketTimeoutException();

		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * ------------------------------------------------------------------------
	 * proposer stuff
	 */
	int proposalIdentifier = 0; // proposal indentifier value (to be sent in phase 1)
	int proposalValue = 0; // proposal value (to be set and used in phase 2)
	Vector<Thread> threads = new Vector<>(); // thread pool

	// responses from the acceptors
	Vector<AcceptorResponse> p1Return = new Vector<>();
	Vector<AcceptorResponse> p2Return = new Vector<>();
	Vector<Boolean> p3Return = new Vector<>();

	private Object proposerLock = new Object(); // to make sure a node can't propose more than once at a time

	/**
	 * propose to all nodes. set offline to 0 so node won't go to sleep after a proposal phaes
	 * 
	 * @param p1Offline how long node goes offline after phase 1
	 * @param p2Offline ^                                      2
	 * @param p3Offline ^                                      3 (although this thing is pretty useless)
	 * @return result, whether proposal was successful or not, and what are the values received
	 */
	public Result propose(int p1Offline, int p2Offline, int p3Offline) {
		synchronized (proposerLock) {
			Result result = new Result();

			proposalIdentifier++;
			result = phase1a(result, p1Offline);
			if (result.p1) {
				result = phase2a(result, p2Offline);
				if (result.p2)
					result = phase3a(result, p3Offline);
			}

			result.value = proposalValue;
			return result;
		}
	}

	/**
	 * set node to offline for a certain duration
	 * @param offline offline duration in ms
	 */
	public void goOffline(int offline) {
		if (offline > 0) {
			try {
				online = false;
				Thread.sleep(offline);
				online = true;

			} catch (Exception e) {
			}
		}
	}

	/**
	 * propser phase 1-3 functions
	 */

	/**
	 * phase 1a - proposer prepare: send request to everyone
	 *
	 * @param result result object, passed to the next phase
	 * @param offline how long proposer will go offline
	 * @return result object
	 */
	private Result phase1a(Result result, int offline) {
		int acceptedVotes = 0;
		int messages = 0;
		int ackCount = 0;
		int highestValue = 0;

		// run thread
		for (int i = 0; i < address.size(); i++)
			if (i != id) {
				threads.set(i, new ProposerPhase1Thread(this, address.get(i), i));
				threads.get(i).start();
			}

		goOffline(offline); // go offline after send

		// join thread (join on separate loop so it wouldn't block the start loop)
		for (int i = 0; i < address.size(); i++)
			if (i != id) { // if not itself & not null
				try {
					threads.get(i).join();

					if (p1Return.get(i) != null) {
						ackCount++;
						messages += 2; // 2 messages for a successful conversation, 1 for node failure 
						AcceptorResponse res = p1Return.get(i);

						if (res.accepted) { // if accepted, increment votes
							acceptedVotes++;
							if (res.currentValue > highestValue)
								highestValue = res.currentValue;

						} else if (!res.accepted && res.currentIdentifier > proposalIdentifier) // if reject, save the highest identifier
							proposalIdentifier = res.currentIdentifier;

					} else {
						messages++;
					}
				} catch (Exception e) {
				}
			}

		result.identifier = proposalIdentifier;
		result.p1Ack = ackCount;
		result.messages += messages;
		if (acceptedVotes > (address.size() - 2) / 2) { // subtract 2 but not 1 to compensate to the node itself
			proposalValue = highestValue == 0 ? 1 : highestValue;
			result.p1 = true;
		} else {
			result.p1 = false;
		}

		return result;
	}

	/**
	 * phase 2a - proposer accept requests: count votes & set proposer value
	 * @param result see phase 1
	 * @param offline see phase 1
	 * @return see phase 1
	 */
	private Result phase2a(Result result, int offline) {
		int acceptedVotes = 0;
		int ackCount = 0;
		int messages = 0;

		// run thread
		for (int i = 0; i < address.size(); i++)
			if (i != id) {
				threads.set(i, new ProposerPhase2Thread(this, address.get(i), i));
				threads.get(i).start();
			}

		goOffline(offline);

		// join thread (join on separate loop so it wouldn't block the start loop)
		for (int i = 0; i < address.size(); i++)
			if (i != id) {
				try {
					threads.get(i).join();

					if (p2Return.get(i) != null) {
						ackCount++;
						messages += 2;
						AcceptorResponse res = p2Return.get(i);

						if (res.accepted) // if accepted, increment votes
							acceptedVotes++;
						else if (!res.accepted && res.currentIdentifier > proposalIdentifier) // if reject, save the highest identifier
							proposalIdentifier = res.currentIdentifier;

					} else {
						messages++;
					}
				} catch (Exception e) {
				}
			}

		result.identifier = proposalIdentifier;
		result.p2Ack = ackCount;
		result.messages += messages;
		result.p2 = acceptedVotes > (address.size() - 2) / 2;
		return result;

	}

	/**
	 * phase 3 - proposer decision: send final decision to all nodes
	 * @param result see phase 1
	 * @param offline see phase 1
	 * @return see phase 1
	 */
	private Result phase3a(Result result, int offline) {
		int ackCount = 0;
		int messages = 0;

		for (int i = 0; i < address.size(); i++)
			if (i != id) {
				threads.set(i, new ProposerPhase3Thread(this, address.get(i), i));
				threads.get(i).start();
			}

		goOffline(offline);

		for (int i = 0; i < address.size(); i++)
			if (i != id) {
				try {
					threads.get(i).join();
					if (p3Return.get(i) != null) {
						messages += 2;
						ackCount++;
					} else {
						messages++;
					}

				} catch (Exception e) {
				}
			}

		result.p3Ack = ackCount;
		result.p3 = true;
		result.messages += messages;
		return result;
	}

	/**
	 * ------------------------------------------------------------------------
	 * acceptor stuff
	 */
	int acceptorIndentifier = 0; // currently accepted proposal identifier
	int acceptorValue = 0; // currently accepted proposal value

	private Object acceptorLock = new Object();

	/**
	 * phase 1b - acceptor promise: promise if identifier > current identifier
	 * @param socket
	 * @param identifier proposer identifier (prepare message)
	 */
	void phase1b(Socket socket, int identifier) {
		synchronized (acceptorLock) {
			try {
				// send true if identifier > current one, and current identifier & value
				write(socket,
						new AcceptorResponse(identifier > acceptorIndentifier, acceptorIndentifier, acceptorValue));

				// promise this node
				if (identifier > acceptorIndentifier) {
					acceptorValue = 0;
					acceptorIndentifier = identifier;
					proposalIdentifier = identifier; // set proposal identifier in case it'll propose later
				}

			} catch (Exception e) {
			}
		}
	}

	/**
	 * phase 2b - acceptor accept request: only accept the promised proposer
	 * @param socket
	 * @param accept Request accept request sent by proposer
	 */
	void phase2b(Socket socket, AcceptRequest acceptRequest) {
		synchronized (acceptorLock) {
			try {
				// send true if identifier == current one & current indentifier (in case send false)
				write(socket, new AcceptorResponse(acceptRequest.proposalIdentifier == acceptorIndentifier,
						acceptorIndentifier, 0));

			} catch (Exception e) {
			}
		}
	}

	/**
	 * final commit (probably just ACK back)
	 * @param socket
	 */
	void phase3b(Socket socket) {
		write(socket, true);
	}
}
