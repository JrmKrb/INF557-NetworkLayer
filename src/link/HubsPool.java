package link;

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import packets.Packet;

/**
 * This program implements a <i>central switching node</i> that forwards any
 * incoming packet to a subset of satellite nodes, following a configurable
 * <i>pool of hubs</i> policy. The considered subset corresponds to the nodes
 * connected to the <i>virtual hub</i> to which the sender of the incoming
 * packet is currently <i>wired</i> to. Note that this is only based on the
 * sender identity defined at the link layer.
 * <p/>
 * 
 * Usage : <tt>java link/HubsPool trafficPort controlPort</tt>
 * 
 * <p/>
 * To use this feature, a satellite node must bind with a {@link SwitchedLink}
 * configured to address the <i>traffic port</i> of the central switching node.
 * The topology of the emulated network is defined by commands arriving on the
 * <i>control port</i> of the central switching node. The {@link Control}
 * program is the tool designed to send well formatted commands to the central
 * switching node.
 * 
 * <p/>
 * The policy for the {@code HubsPool} program is to consider the network as a
 * <b>pool of hubs</b>. Each node is attached to only one hub and each hub
 * constitutes a link between all the nodes attached to it. The number of hubs
 * is not limited and one hub may be identified by any unique name.
 * 
 * The two main commands are:
 * <ul>
 * <li><tt>add A B</tt> &nbsp; to add (plug) node A on link (hub) B</li>
 * <li><tt>del A B</tt> &nbsp; to remove (unplug) node A from link (hub) B</li>
 * </ul>
 * Note that a newly registered node is automatically connected to the link name
 * <tt>DEFAULT</tt>. A node that is the only one plugged on a hub is connected
 * to itself. An unplugged node is not connected, not even to itself. <br/>
 * The {@link Matrix} program is a variant where the connection policy looks
 * like defining a directed graph.
 * 
 * @see SwitchedLink
 * @see Control
 * @see Matrix
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 */
public class HubsPool extends Matrix {

  private final Map<String, Set<SocketAddress>> mapNameToDestinations;

  private HubsPool() {
    super();
    mapNameToDestinations = new ConcurrentHashMap<String, Set<SocketAddress>>();
  }

  private void add(String nodeName, String linkName, PrintStream results) {
    synchronized (lock) {
      if (mapNameToAddress.get(linkName) != null) {
        displayResult("*** the intended link name " + linkName
            + " is already assigned to a known node", results);
        return;
      }
      SocketAddress nodeSocket = nameToAddress(nodeName, results);
      if (nodeSocket == null)
        return;
      Set<SocketAddress> neighbors = mapAddressToDestinations.get(nodeSocket);
      if (neighbors != null)
        neighbors.remove(nodeSocket);
      neighbors = mapNameToDestinations.get(linkName);
      if (neighbors == null) {
        neighbors = new HashSet<SocketAddress>();
        mapNameToDestinations.put(linkName, neighbors);
      }
      neighbors.add(nodeSocket);
      mapAddressToDestinations.put(nodeSocket, neighbors);
      displayResult("OK node " + nodeName + " is added to link " + linkName,
          results);
    }
  }

  private static boolean useDefaultLink = true;

  @Override
  protected void handleNewNode(String name) {
    super.handleNewNode(name);
    if (useDefaultLink)
      add(name, "DEFAULT", null);
  }

  @Override
  protected void handleAdd(Packet packet, PrintStream results) {
    String nodeName = packet.getSource();
    String linkName = packet.getDestination();
    add(nodeName, linkName, results);
  }

  @Override
  protected void handleDel(Packet packet, PrintStream results) {
    synchronized (lock) {
      String nodeName = packet.getSource();
      String linkName = packet.getDestination();
      SocketAddress nodeSocket = nameToAddress(nodeName, results);
      if (nodeSocket == null)
        return;
      Set<SocketAddress> neighbors = mapNameToDestinations.get(linkName);
      if (neighbors == null) {
        displayResult("*** the link named " + linkName + " doesn't exist",
            results);
        return;
      }
      neighbors.remove(nodeSocket);
      mapAddressToDestinations.remove(nodeSocket);
      displayResult(
          "OK node " + nodeName + " is removed from link " + linkName, results);
    }
  }

  @Override
  protected void listLinks(PrintStream stream) {
    stream.println("========== LINKS ==========");
    for (Map.Entry<String, Set<SocketAddress>> entry : mapNameToDestinations
        .entrySet()) {
      stream.print(entry.getKey() + " :");
      for (SocketAddress addr : entry.getValue())
        stream.print(' ' + mapAddressToName.get(addr));
      stream.println();
    }
    stream.println("===========================");
  }

  @Override
  protected void helpAdd(PrintStream stream) {
    stream.println("\tadd A B : add (plug) node A on link (hub) B");
  }

  @Override
  protected void helpDel(PrintStream stream) {
    stream.println("\tdel A B : remove (unplug) node A from link (hub) B");
  }

  /** See the class documentation above. */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err
          .println("syntax : java tools/Linker linkPort controlPort [-noDefaultLink]");
      System.exit(0);
    }
    int linkPort = Integer.parseInt(args[0]);
    Link link = startInterface("LINK@", linkPort);
    int controlPort = Integer.parseInt(args[1]);
    Link control = startInterface("CONTROL@", controlPort);
    if (args.length > 2 && args[2].startsWith("-no"))
      useDefaultLink = false;
    new HubsPool().start(link, control);
  }

}
