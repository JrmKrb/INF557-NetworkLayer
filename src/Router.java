import link.Link;
import link.SwitchedLink;

import common.Keyboard;

public class Router {

  private Router() { // Don't instantiate this class.
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.out
          .println("Syntax : java Router myName nbLinks switcherHost switcherPort");
      System.exit(0);
    }
    String myName = args[0];
    int nbLinks = Integer.parseInt(args[1]);
    String switcherHost = args[2];
    int switcherPort = Integer.parseInt(args[3]);

    // initialize a network layer, giving it the local node name
    NetworkLayer network = new NetworkLayer(myName);
    // stack it over some links
    for (int i = 0; i < nbLinks; ++i) {
      Link link = new SwitchedLink(switcherHost, switcherPort, -1, myName);
      network.add(link);
    }
    // don't deliver packets upwards for now
    // network.deliverTo(...);
    // and start networking
    network.start();
    System.out.println("My network layer is running");

    // now allow commands from keyboard
    String s = Keyboard.readString();
    while (true) {
      String[] t = s.split(" ");
      network.control(t);
      s = Keyboard.readString();
    }
  }
}
