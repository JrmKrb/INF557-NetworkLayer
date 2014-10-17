import link.Link;
import link.MulticastLink;
import link.SwitchedLink;

/**
 * This is a minimal test application for the LinkState class. It implements a
 * HELLO protocol on top of a few {@link Link}s.
 * 
 * <p/>
 * Syntax : <tt>java TestHello myName [ nbLinks switcherHost switcherPort ]</tt>
 * <p/>
 * When launched with only one argument, program TestHello works on a
 * {@link MulticastLink}.<br/>
 * When launched with with four arguments, program TestHello works on the chosen
 * number of {@link SwitchedLink}.<br/>
 * 
 * @see link
 * @see MulticastLink
 * @see SwitchedLink
 * 
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2014 ƒcole Polytechnique
 * @version 1.3, 2014/10/15
 */
public class TestHello {
  public static void main(String[] args) {
    if (args.length != 1 && args.length != 4) {
      System.err
          .println("syntax : java TestHello myName [ nbLinks switcherHost switcherPort ]");
      return;
    }
    String myName = args[0];

    // initialize a network layer, giving it a name
    HelloLayer hello = new HelloLayer(myName);
    // stack it over a few links
    if (args.length == 1) {
      Link link = new MulticastLink();
      hello.add(link);
    } else {
      int nbLinks = Integer.parseInt(args[1]);
      String switcherHost = args[2];
      int switcherPort = Integer.parseInt(args[3]);
      for (int i = 0; i < nbLinks; ++i) {
        Link link = new SwitchedLink(switcherHost, switcherPort, -1, myName);
        hello.add(link);
      }
    }
    // still receiving, as soon as it is added, a link delivers packets upwards
    // now start the hello protocol
    hello.start();
    System.out.println("My network layer is running (forever)");
  }
}
