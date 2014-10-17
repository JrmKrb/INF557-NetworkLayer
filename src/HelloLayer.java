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

import common.Layer;

public class HelloLayer implements Layer {

  protected final String myName;
  protected final Set<Link> underLayers;
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

  /**
   * Initializes this {@code HelloLayer} and binds it to the specified name,
   * which is supposed to be unique. That is to say, gives it an identity on the
   * network. The specified name will be used as the source field of outgoing
   * packets and for filtering incoming packets on their destination field. Also
   * performs various initializations.
   * 
   * @param name
   *          the name given to this {@code HelloLayer}
   */
  public HelloLayer(String name) {
    myName = name;
    HELLO = new HelloPacket(myName, ALL); // single instance
    underLayers = new HashSet<Link>();
    linkState = new LinkState();
  }

  /**
   * Add a link to this {@code NetworkLayer}. As soon as it is added, a link
   * starts delivering packets upwards to this {@code NetworkLayer}.
   * 
   * @param link
   *          the added link
   */
  public void add(Link link) {
    // first keep track of this link
    underLayers.add(link);
    // this call allows the delivery of incoming packets,
    // see receive(Packet,Layer) below
    link.deliverTo(this);
  }

  /**
   * Don't use this method (required by the Layer interface).
   * 
   * Specifies an other layer to which this {@code NetworkLayer} must forward
   * the incoming packets that have to be processed at above layers. Thus, this
   * {@code NetworkLayer} will invoke the {@link #receive receive} method of the
   * specified {@code above} layer to pass it a packet upward.
   * 
   * @param above
   *          the {@code Layer} whose {@link #receive receive} method must be
   *          called to pass it an incoming packet
   */
  public void deliverTo(Layer above) {
    throw new UnsupportedOperationException(
        "HelloLayer doesn't deliver any packet upward");
  }

  /**
   * Starts this {@code NetworkLayer} operating. This methods launches the
   * housekeeping tasks for the HELLO protocol.
   */
  public void start() { // TO BE COMPLETED
    // starts sending HELLO periodically on each link
    // and manage the removal of neighbors that seem lost
  }

  /**
   * Adjusts the content of the LinkState table, after the receipt a
   * {@code HELLO} packet from a given source.
   * 
   * If it is coming from a new device, a new entry is added to the LinkState
   * table. Otherwise, but only when the link is unchanged, the down-counter is
   * reset to the maximal value.
   * 
   * @param name
   *          the name of the source node of the packet
   * @param from
   *          the link which receives the packet
   */
  private void handleSendingNeighbor(String name, Link from) {
    // TO BE COMPLETED
  }

  /**
   * Processes an incoming {@code HELLO} packet.
   * 
   * @param source
   *          the name of the device which sends the packet
   * @param from
   *          the link which receives the {@code HELLO} packet
   */
  private void handleHello(String source, Link from) {
    if (source.equals(myName))
      return;
    handleSendingNeighbor(source, from);
  }

  /**
   * Handles an incoming packet at this layer.
   * 
   * This method is invoked (by the receiving thread of an link below) to give
   * every incoming packet to this {@code NetworkLayer}. This method should not
   * block long and must return as soon as possible.
   * 
   * @param packet
   *          the incoming packet
   * @param from
   *          the link by which the packet arrived
   */
  public void receive(Packet packet, Layer from) {
    Link link = (Link) from;
    lock.lock();
    try {
      System.err.println("received : " + packet + " from " + from);
      switch (packet.getType()) {
      case HELLO:
        handleHello(packet.getSource(), link);
        return;
      default:
        // System.out.println("RECEIVED " + packet);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Don't use this method (required by the Layer interface).
   * 
   * Sends a packet through this {@code NetworkLayer}.
   * 
   * @param packet
   *          a {@code Packet} to be sent
   */
  public void send(Packet packet) {
    throw new UnsupportedOperationException(
        "HelloLayer doesn't accept any packet coming from above");
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

}
