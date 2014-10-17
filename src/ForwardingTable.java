import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import link.Link;
import packets.NextHopPacket;
import packets.Packet;

/**
 * A {@code ForwardingTable} object is a table that registers, for each known
 * destination, the next hop for the best path toward this destination and the
 * corresponding metrics. A known but now unreachable destination is stored with
 * RouteEntry.INFINITY as metrics.
 * 
 * A {@code ForwardingTable} also has a mechanism to consider (recently) updated
 * destination.
 * 
 * No synchronization is required here, as calling sequences must already be
 * synchronized.
 */

public class ForwardingTable {

  private final String myName;
  private final LinkState neighbors;
  private final Map<String, RouteEntry> table;
  private final Set<String> touched;

  public ForwardingTable(String localName, LinkState linkState) {
    this.myName = localName;
    this.neighbors = linkState;
    this.table = new HashMap<String, RouteEntry>();
    this.touched = new HashSet<String>();
  }

  /**
   * Adds or updates an entry, in this {@code ForwardingTable} where the entry
   * is specified as a {@code RouteEntry}. The destination field of the entry is
   * a key in the table, hence if the table previously contained an entry for
   * the same destination, then the former entry is replaced by the specified
   * value, otherwise the specified value is added as a new entry.
   * 
   * Whatever the state before this call, the corresponding destination is now
   * marked as updated and will remain marked until a call to
   * {@code clearUpdated}.
   * 
   * @param entry
   *          the new entry to put into the table
   */
  public void put(RouteEntry entry) {
    table.put(entry.getDestination(), entry);
    touched.add(entry.getDestination());
  }

  /**
   * Returns the {@code RouteEntry} stored for the specified destination, or
   * {@code null} if it is an unknown destination.
   * 
   * @param destination
   *          the name of the searched destination
   * @return the routing entry for this destination, or {@code null} if this
   *         destination is not registered into this {@code ForwardingTable}
   */
  public RouteEntry get(String destination) {
    return table.get(destination);
  }

  /**
   * Returns a set of all the destinations registered in this
   * {@code ForwardingTable}, even those tagged with infinite metrics. The
   * iteration order of the result is not specified.
   * 
   * @see #removeLostDestination()
   * 
   * @return a set of the names of known destination
   */
  public Set<String> destinations() {
    return table.keySet();
  }

  /**
   * Indicates whether the specified destination is currently marked as updated
   * in this {@code ForwardingTable}.
   * 
   * @param destination
   *          the name of the destination
   * @return {@code true} if the destination is currently marked as updated;
   *         {@code false} otherwise.
   */
  public boolean hasUpdated(String destination) {
    return touched.contains(destination);
  }

  /**
   * Removes from this {@code ForwardingTable} every destination tagged with
   * infinite metrics but those marked as updated.
   * 
   * @see #clearUpdated()
   * 
   * @return a set of the names of known destination
   */
  public void removeLostDestinations() {
    Iterator<RouteEntry> it = table.values().iterator();
    while (it.hasNext()) {
      RouteEntry entry = it.next();
      if (entry.getMetrics() == RouteEntry.INFINITY
          && !this.hasUpdated(entry.getDestination()))
        it.remove();
    }
  }

  /**
   * Whatever their state before this call, all destinations registered in this
   * {@code ForwardingTable} are no longer marked as updated.
   */
  public void clearUpdated() {
    touched.clear();
  }

  /**
   * Dumps the whole content of this {@code ForwardingTable} onto the specified
   * {@code PrintStream}. The display order is not specified.
   * 
   * @param out
   *          the stream on which the content is printed
   */
  public void dump(PrintStream out) {
    out.println("\n Forwarding Table:");
    if (table.isEmpty())
      out.println("<empty>");
    for (RouteEntry entry : table.values())
      out.println(entry);
    out.println();
  }

  /**
   * Builds a vector, extracted from this {@code ForwardingTable} and ready to
   * be sent for a specified variant of the Distance Vector protocol. The
   * following rules are applied.
   * 
   * A {@code recipient} parameter, for which the returned vector is intended,
   * may be specified and this also acts as a "split-horizon" flag.
   * 
   * If {@code recipient} parameter is {@code null}, the returned vector is a
   * general vector, intended to be sent to any direct neighbor. In this case,
   * the "split-horizon" rule is disabled and the {@code poisonedReverse}
   * parameter is also ignored.
   * 
   * If {@code recipient} parameter is not {@code null}, a specific vector for
   * this recipient is returned. In this case, the "split-horizon" rule is
   * enabled and the produced vector also depends on the {@code poisonedReverse}
   * parameter.
   * 
   * If the {@code addLost} parameter is {@code false}, the returned vector does
   * never contain any destination registered in this {@code ForwardingTable}
   * with RouteEntry.INFINITY as metrics. This should not be confused with an
   * infinite metrics forced by the "poisoned-reverse" rule.
   * 
   * If the {@code addLost} parameter is {@code true}, the returned vector may
   * also contain the destinations registered in this {@code ForwardingTable}
   * with RouteEntry.INFINITY as metrics, according to the two last rules below.
   * This allows "route poisoning".
   * 
   * If the {@code incremental} parameter is {@code true}, the returned vector
   * contains only the destinations both marked as updated in this
   * {@code ForwardingTable} and satisfying the above rules. This allows to
   * produce an empty vector when the network has converged.
   * 
   * If the {@code incremental} parameter is {@code false}, the returned vector
   * contains all the destinations satisfying the above rules. This may be used
   * for periodically sending a 'full' vector or for feeding a newly discovered
   * neighbor.
   * 
   * The size of the returned vector is adjusted to the number of selected
   * destination. Data for the i-th destination of the result, are stored as
   * follow:
   * 
   * vector[i][0] is the name of the destination
   * 
   * vector[i][1] is the corresponding metric, converted as a String.
   * 
   * @param recipient
   *          the specific destination for this packet, or {@code null} if any
   *          direct neighbor will be the destination
   * @param poisonedReverse
   *          indicating whether the "poisoned-reverse" variant is applied in
   *          addition to the "split-horizon" rule
   * @param addLost
   *          indicating whether lost destinations, those registered with
   *          infinite metrics, may be included or not in the result
   * @param incremental
   *          indicating whether the routing table is sent completely or not
   *          (i.e. only recently changed entries, marked by their
   *          {@code updated} flag)
   * 
   * @return a vector, sized and filled according to the specified destination
   *         and to the specified Distance-Vector rule
   */
  public String[][] makeVector(String recipient, boolean poisonedReverse,
      boolean addLost, boolean incremental) {
    return null; // TO BE MODIFIED
  }

  /**
   * Forwards the given payload packet with the specified (already decremented)
   * TTL. The packet is discarded in any of the following cases:
   * 
   * - if the TTL is less or equals to 0,
   * 
   * - if there is currently no route for the destination of the payload,
   * 
   * - if the relay for the destination is not on-link.
   * 
   * Otherwise, a {@code NextHopPacket} is built from the payload and sent to
   * the relay through the proper link.
   * 
   * @param payload
   *          the packet to be forwarded
   * @param ttl
   *          the TTL value for the forwarded packet
   */
  public void forward(Packet payload, int ttl) {
    // TO BE COMPLETED
  }

}
