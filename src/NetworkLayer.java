import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import link.Link;
import packets.HelloPacket;
import packets.Packet;
import packets.VectorPacket;

import common.Layer;

public class NetworkLayer implements Layer {

	protected final String myName;
	protected final Set<Link> underLayers;
	protected Layer aboveLayer;
	// a lock used to implement exclusions
	protected final Lock lock = new ReentrantLock();

	/** useful timer, one enough for many purposes */
	protected static final Timer TIMER = new Timer("NetworkLayer_Timer", true);

	// ////////////////////////////////////////////////////////////////////////
	// Definitions for the HELLO protocol
	// a conventional destination name for broadcasting over a link
	protected static final String ALL = "*ALL*";
	// a single instance of HelloPacket is enough
	private final Packet HELLO;

	// parameters for "keep alive" periodic sending
	private static final int HELLO_DELAY = 2000; // send an HELLO packet every
													// HELLO_DELAY milliseconds
	private static final int MAX_HELLO_GRACE = 2;

	// These are reasonable values. Those that will flood the network with too
	// much HELLO will be severely punished!
	// Also, for compatibility, all devices must all agree on the
	// MAX_HELLO_GRACE*HELLO_DELAY minimal delay before removing a neighbor from
	// the tables.

	// the link state table itself, where the key is the name of a neighbor.
	protected final LinkState linkState;

	protected final ForwardingTable forwardingTable;

	// ////////////////////////////////////////////////////////////////////////
	// Definitions for the Distance Vector protocol

	// the distances matrix
	private final Bellman_Ford_Matrix distancesMatrix;

	// for periodic sending of vector
	private static final int VECTOR_DELAY = 5000;
	// a counter to decide of periodic sending
	private int periodicVectorSendingCount = 0;
	private static final int MAX_VECTOR_COUNT = 6;
	// send a VECTOR packet every MAX_VECTOR_COUNT*VECTOR_DELAY milliseconds, even there is no change
	// These are reasonable values. Do not overwhelm the network with these
	// periodic packets to easily observe the cascade of packets in response to
	// topology changes.

	// ////////////////////////////////////////////////////////////////////////
	// Definitions of the DV policy You can change initial values
	// These flags may be also updated through the control feature. You have to
	// use them in your DV implementation.

	private static boolean splitHorizon = false;
	private static boolean poisonedReverse = false;
	private static boolean triggeredUpdate = true;
	private static boolean periodicSending = false;
	private static boolean fullVector = true; // on triggeredUpdate, send full
												// vector or only changes

	/**
	 * Initializes this {@code NetworkLayer} and binds it to the specified name, which is supposed to be unique. That is to say, gives it an identity on the
	 * network. The specified name will be used as the source field of outgoing packets and for filtering incoming packets on their destination field. Also
	 * performs various initializations.
	 * 
	 * @param name
	 *            the name given to this {@code NetworkLayer}
	 */
	public NetworkLayer(String name) {
		myName = name;
		HELLO = new HelloPacket(myName, ALL); // single instance
		underLayers = new HashSet<Link>();
		linkState = new LinkState();
		forwardingTable = new ForwardingTable(myName, linkState);
		forwardingTable.put(new RouteEntry(myName, myName, 0));
		// DV structures initializations
		distancesMatrix = new Bellman_Ford_Matrix(myName, forwardingTable);
	}

	/**
	 * Add a link to this {@code NetworkLayer}. As soon as it is added, a link starts delivering packets upwards to this {@code NetworkLayer}.
	 * 
	 * @param link
	 *            the added link
	 */
	public void add(Link link) {
		// first keep track of this link
		underLayers.add(link);
		// this call allows the delivery of incoming packets,
		// see receive(Packet,Layer) below
		link.deliverTo(this);
	}

	/**
	 * Specifies an other layer to which this {@code NetworkLayer} must forward the incoming packets that have to be processed at above layers. Thus, this
	 * {@code NetworkLayer} will invoke the {@link #receive receive} method of the specified {@code above} layer to pass it a packet upward.
	 * 
	 * @param above
	 *            the {@code Layer} whose {@link #receive receive} method must be called to pass it an incoming packet
	 */
	public void deliverTo(Layer above) {
		aboveLayer = above;
	}

	/**
	 * Starts this {@code NetworkLayer} operating. This methods launches the housekeeping tasks for the network protocol.
	 */
	public void start() {
		// starts sending HELLO periodically on each link
		TIMER.schedule(new TimerTask() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				lock.lock();
				try {
					for (Link link : underLayers)
						link.send(HELLO);
				} finally {
					lock.unlock();
				}
			}
		}, 0, HELLO_DELAY);
		// and manage the removal of neighbors that seem lost
		// it is a separate task for clarity
		TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				lock.lock();
				try {
					linkState.decreaseCounters();
					Collection<String> removed = linkState.getDumbNeighbors();
					if (!removed.isEmpty()) {
						boolean updated = false; // a flag that indicates if there is a change or not in DV
						System.out.println("removing devices " + removed);
						for (String neighbor : removed) {
							linkState.remove(neighbor);
							// We also handle this according to the DV protocol
							if (distancesMatrix.updateDistance(neighbor, neighbor, RouteEntry.INFINITY)) updated = true;
						}
						linkState.dump(System.out);
						// We propagate the loss of neighbors
						if (updated) sendVector("Some neighbors are lost.", false);
					}
				} finally {
					lock.unlock();
				}
			}
		}, 0, HELLO_DELAY);
	}

	/**
	 * Displays a vector, for debugging purpose.
	 */
	private void printVector(String[][] vector) {
		if (vector != null) {
			for (String[] tuple : vector) {
				for (String s : tuple)
					System.out.print(s + " ");
				System.out.println();
			}
		}
		System.out.println("===========================");
	}

	/**
	 * Builds a {@code VECTOR} packet, ready to be sent. The destination is given as a parameter. When {@code destination} is {@code null}, the packet is
	 * intended to be sent to every direct neighbor. When {@code destination} is not {@code null}, a specific packet can be built for this destination.
	 * 
	 * @param destination
	 *            the intended destination for this packet, or {@code null} to specify any direct neighbor as the destination
	 * @param cause
	 *            a {@code String} indicating why this packet will be sent (for debugging purpose)
	 * @param incremental
	 *            indicating whether the routing table is sent completely or not (i.e. only recently changed entries, marked by their {@code updated} flag)
	 * @return a {@code VectorPacket} built according to the specified destination
	 */
	private Packet makeVectorPacket(String destination, String cause, boolean incremental) {
		String[][] vector = forwardingTable.makeVector(destination, poisonedReverse, true, incremental);
		// when destination is null, the packet is intended to be sent to every
		// direct neighbor
		String packetDestination = destination == null ? ALL : destination;
		System.out.println("====== " + myName + " SENDING to " + packetDestination + " (" + cause + ") ==========");
		printVector(vector);
		if (vector.length > 0) return new VectorPacket(myName, packetDestination, vector);
		return null;
	}

	/**
	 * The basic method used to send a vector. Always call this. It may be then easily patched to implement variants in the Distance Vector algorithm.
	 * 
	 * @param cause
	 *            a {@code String} indicating why this packet will be sent (for debugging purpose)
	 * @param incremental
	 *            indicating whether the routing table is sent completely or not (i.e. recent changes only)
	 */
	private void sendVector(String cause, boolean incremental) {
		System.out.println("sendVector() called: " + cause);

		for (String v : linkState.neighbors()) {
			Packet vectorsPacket = makeVectorPacket(v, cause, incremental);
			send(vectorsPacket);
		}

		// TODO: WHY ?
		forwardingTable.clearUpdated();
	}

	/**
	 * Adjusts the content of the neighborhood table, after the receipt a {@code HELLO} or {@code VECTOR} packet from a given source.
	 * 
	 * If it is coming from a new device, a new entry is added to the LinkState table. Otherwise, but only when the link is unchanged, the down-counter is reset
	 * to the maximal value.
	 * 
	 * @param name
	 *            the name of the source node of the packet
	 * @param from
	 *            the link which receives the packet
	 */
	private void handleSendingNeighbor(String name, Link from) {
		Link link = linkState.getLinkFor(name);
		if (link == null) {
			// A new neighbor is discovered
			linkState.put(new LinkEntry(name, from, MAX_HELLO_GRACE));
			// In NetWorkLayer this event will also be handled according to the DV protocol
			if (distancesMatrix.updateDistance(name, name, 0)) sendVector("There is a new neighbor on the network.",
					false);
			linkState.dump(System.out);
		} else if (link == from) linkState.resetCounter(name, MAX_HELLO_GRACE);
	}

	/**
	 * Processes an incoming {@code HELLO} packet.
	 * 
	 * @param source
	 *            the name of the device which sends the packet
	 * @param from
	 *            the link which receives the {@code HELLO} packet
	 */
	private void handleHello(String source, Link from) {
		if (source.equals(myName)) return;
		handleSendingNeighbor(source, from);
	}

	/**
	 * Processes data from an incoming {@code VECTOR} packet.
	 * 
	 * @param source
	 *            the name of the source node of the {@code VECTOR} packet
	 * @param destination
	 *            the name of the destination node of the {@code VECTOR} packet
	 * @param vector
	 *            the vector itself
	 * @param from
	 *            the link which receives the {@code VECTOR} packet
	 */
	private void handleVector(String source, String destination, String[][] vector, Link from) {
		if (source.equals(myName)) return; // We don't consider our own packets
		if (!destination.equals(myName) && !destination.equals(ALL)) return; // It may be a poisoned vector
		boolean updated = false; // A flag that indicates if it will result in a change in DV

		// We handle the packet
		handleSendingNeighbor(source, from);

		// We check if it results in an update of de BFM
		for (String[] v : vector)
			if (distancesMatrix.updateDistance(v[0], source, Integer.parseInt(v[1]))) updated = true;
		if (updated) sendVector("Updated vector received from" + source + ".", true);
	}

	/**
	 * Processes data from an incoming {@code NEXTHOP} packet.
	 * 
	 * @param ttl
	 *            the value for TTL
	 * @param payload
	 *            the packet to be forwarded
	 */
	private void handleNextHop(int ttl, Packet payload) {
		if (payload.getDestination().equals(myName)) {
			if (aboveLayer != null) aboveLayer.receive(payload, this);
		} else forwardingTable.forward(payload, ttl - 1);
	}

	/**
	 * Handles an incoming packet at this layer.
	 * 
	 * This method is invoked (by the receiving thread of an link below) to give every incoming packet to this {@code NetworkLayer}. This method should not
	 * block long and must return as soon as possible.
	 * 
	 * @param packet
	 *            the incoming packet
	 * @param from
	 *            the link by which the packet arrived
	 */
	public void receive(Packet packet, Layer from) {
		Link link = (Link) from;
		lock.lock();
		try {
			// System.err.println("received : " + packet + " from " + from);
			switch (packet.getType()) {
			case HELLO:
				handleHello(packet.getSource(), link);
				return;
			case VECTOR:
				handleVector(packet.getSource(), packet.getDestination(), packet.getArray(), link);
				return;
			case NEXTHOP:
				if (packet.getDestination().equals(myName)) handleNextHop(packet.getNum(), packet.getNestedPacket());
				return;
			default:
				System.out.println("RECEIVED wrong " + packet);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Sends a packet through this {@code NetworkLayer}.
	 * 
	 * @param packet
	 *            a {@code Packet} to be sent
	 */
	public void send(Packet packet) {
		if (packet.getDestination().equals(myName)) { // loopback
			if (aboveLayer != null) aboveLayer.receive(packet, this);
		} else forwardingTable.forward(packet, RouteEntry.INFINITY - 1); // max TTL value
	}

	/**
	 * Closes this {@code NetworkLayer}.
	 */
	public void close() {
		lock.lock();
		try {
			// at least, turn off the timer
			TIMER.cancel();
		} finally {
			lock.unlock();
		}
	}

	// Below is stuff for controlling the NetworkLayer, see Router.java

	private abstract class Command {
		final String key;

		Command(String k) {
			key = k;
		}

		abstract void doIt();

		void exec(@SuppressWarnings("unused") String[] args) {
			System.out.println("====================");
			doIt();
			System.out.println("====================");

		}

	}

	private abstract class YesNo extends Command {

		YesNo(String k) {
			super(k);
		}

		@Override
		final void doIt() { // nothing to do
		}

		@Override
		void exec(String[] args) {
			System.out.println("====================");
			if (args.length > 1) if ("yes".equals(args[1])) {
				doYes();
				return;
			} else if ("no".equals(args[1])) {
				doNo();
				return;
			}
			System.out.println(args[0] + " yes or no ?");
			System.out.println("====================");
		}

		abstract void doYes();

		abstract void doNo();

	}

	Command[] commands = { new Command("links") {
		@Override
		void doIt() {
			linkState.dump(System.out);
		}
	}, new Command("routes") {
		@Override
		void doIt() {
			forwardingTable.dump(System.out);
		}
	}, new Command("table") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doIt() {
			distancesMatrix.dump(System.out);
		}
	}, new Command("policy") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doIt() {
			System.out.println("split(Horizon): " + splitHorizon);
			System.out.println("poisoned(Reverse): " + poisonedReverse);
			System.out.println("triggered(Update): " + triggeredUpdate);
			System.out.println("periodic(Sending): " + periodicSending);
			System.out.println("full(Vector): " + fullVector);
		}
	}, new YesNo("split") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doYes() {
			splitHorizon = true;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		void doNo() {
			splitHorizon = false;
			poisonedReverse = false;
		}
	}, new YesNo("poisoned") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doYes() {
			splitHorizon = true;
			poisonedReverse = true;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		void doNo() {
			poisonedReverse = false;
		}
	}, new YesNo("triggered") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doYes() {
			triggeredUpdate = true;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		void doNo() {
			triggeredUpdate = false;
		}
	}, new YesNo("periodic") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doYes() {
			periodicSending = true;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		void doNo() {
			periodicSending = false;
		}
	}, new YesNo("full") {
		@SuppressWarnings("synthetic-access")
		@Override
		void doYes() {
			fullVector = true;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		void doNo() {
			fullVector = false;
		}
	} };

	/**
	 * Handle various control operations on this {@code NetworkLayer} operating.
	 * 
	 * @param args
	 *            an array build by splitting a command line
	 */
	public void control(String[] args) {
		for (Command c : commands)
			if (c.key.equals(args[0])) {
				lock.lock();
				try {
					c.exec(args);
				} finally {
					lock.unlock();
				}
				return;
			}
		// unknown command
		System.out.println("what!");
		System.out.println("====================");
	}

}
