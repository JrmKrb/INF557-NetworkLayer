import link.Link;
import link.SwitchedLink;
import packets.DataPacket;
import packets.Packet;
import packets.PacketType;

import common.Keyboard;
import common.Layer;
import common.TopLayer;

class TalkLayer extends TopLayer {
  // invoked from bottom (by the receiving thread of the link layer)
  public void receive(Packet packet, Layer from) {
    if (packet.getType() == PacketType.DATA)
      System.out.println("from \"" + packet.getSource() + "\" ["
          + packet.getNum() + "] to \"" + packet.getDestination() + "\" : "
          + packet.getContent());
  }
}

public class TalkR {
  public static void main(String[] args) {
    if (args.length != 3) {
      System.err
          .println("syntax : java TalkR myName switcherHost switcherPort");
      return;
    }
    String myName = args[0];
    String switcherHost = args[1];
    int switcherPort = Integer.parseInt(args[2]);

    // setting of a link layer
    Link link = new SwitchedLink(switcherHost, switcherPort, -1, myName);

    // initialize a network layer, giving it the local node name
    NetworkLayer network = new NetworkLayer(myName);
    // stack it over the link
    network.add(link);
    // packets of higher levels are thrown to this new TalkLayer
    network.deliverTo(new TalkLayer());
    // and start networking
    network.start();
    System.out.println("My network layer is running");

    // now send lines read from keyboard
    // the first word is intended to be the name of the destination
    String line = Keyboard.readString();
    int n = 0;
    while (true) {
      String[] words = line.split(" ", 2);
      if (words.length != 2)
        System.out.println("syntax of line: destination message");
      else
        network.send(new DataPacket(myName, words[0], ++n, words[1]));
      line = Keyboard.readString();
    }
  }
}
