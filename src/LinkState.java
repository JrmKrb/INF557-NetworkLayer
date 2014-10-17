import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import link.Link;

/**
 * A {@code LinkState} object is a table that keeps track of all known
 * neighbors, through which respective link they are reachable and primitives
 * for the detection of which neighbors are off.
 * 
 * No synchronization is required here, as calling sequences must already be
 * synchronized.
 */

public class LinkState {

  // the Link-State table itself, where the name of a neighbor is a key.
  private final Map<String, LinkEntry> table;

  public LinkState() {
    table = new HashMap<String, LinkEntry>();
  }

  /**
   * Adds or updates an entry in this {@code LinkState} table. The neighbor
   * field of the entry is a key in the table, hence if the table previously
   * contained an entry for the same neighbor, then the former entry is replaced
   * by the specified value, otherwise the specified value is added as a new
   * entry.
   * 
   * @param entry
   *          the new entry to put into the table
   */
  public void put(LinkEntry entry) {
    table.put(entry.getNeighbor(), entry);
  }

  /**
   * Returns a set of all the neighbors registered in this {@code LinkState}
   * table. The iteration order of the result is not specified.
   * 
   * @return a set of the names of known neighbors
   */
  public Set<String> neighbors() {
    return table.keySet();
  }

  /**
   * Returns the link through which the specified neighbor can be reached.
   * Returns {@code null} if this {@code LinkState} table contains no entry for
   * the specified neighbor.
   * 
   * @param neighbor
   *          the name of the searched neighbor
   * @return the link through which this neighbor can be reached, or
   *         {@code null} if this name does not correspond to a known neighbor
   */
  public Link getLinkFor(String neighbor) {
    LinkEntry entry = table.get(neighbor);
    if (entry == null) {
      return null;
    }
    return entry.getLink();
  }

  /**
   * Decreases by one the down-counter for all entries in this {@code LinkState}
   * table. This method does no more but is involved in the detection of which
   * neighbors are off.
   * 
   * It will be called by a repetitive timer task to pull down every counter,
   * while an other mechanism will keep the counters of 'active' neighbors at a
   * fairly positive value.
   */
  public void decreaseCounters() {
    for (LinkEntry entry : table.values())
      entry.decreaseCounter();
  }

  /**
   * Resets the current value of the down-counter for the specified neighbor,
   * which is supposed already registered into this {@code LinkState} table.
   * This method will be called in turn for each neighbor detected as 'active'
   * on a link.
   * 
   * @param neighbor
   *          the name of the neighbor whose counter is reset
   * @param newCount
   *          the new value for the counter
   */
  public void resetCounter(String neighbor, int newCount) {
    table.get(neighbor).setCounter(newCount);
  }

  /**
   * Returns a collection of the neighbors whose down-counter is currently
   * negative. A counter ends below 0 if a relatively long time elapses without
   * receiving a packet from the corresponding neighbor. The iteration order of
   * the result is not specified.
   * 
   * We use the collection as an intermediate structure to fetch these entries
   * to remove. It is a way to avoid the {@code ConcurrentModificationException}
   * which occurs when one tries to modify a structure from inside a
   * {@code for(:)} loop over elements of this structure. Instead, it is safe to
   * iterate over the collection returned by {@code getDumbNeighbors} and, among
   * other processing, remove them one by one from this {@code LinkState} table.
   * 
   * @see link #remove(String)
   * 
   * @return a collection of the neighbors whose down-counter is currently
   *         negative
   */
  public Collection<String> getDumbNeighbors() {
    Collection<String> list = new LinkedList<String>();
    for (LinkEntry entry : table.values())
      if (entry.getCounter() < 0)
        list.add(entry.getNeighbor());
    return list;
  }

  /**
   * Removes the entry for the specified neighbor from this {@code LinkState}
   * table.
   * 
   * @param neighbor
   *          the name of the neighbor whose entry is removed
   */
  public void remove(String neighbor) {
    table.remove(neighbor);
  }

  /**
   * Dumps the whole content of this {@code LinkState} table onto the specified
   * {@code PrintStream}. The display order is not specified.
   * 
   * @param out
   *          the stream on which the content is printed
   */
  public void dump(PrintStream out) {
    out.println("\n Link State:");
    if (table.isEmpty())
      out.println("<empty>");
    for (LinkEntry entry : table.values())
      out.println(entry);
    out.println();
  }

}
