public class Test {
	public static void main(String[] args) {
		ForwardingTable table = new ForwardingTable("test", new LinkState());

		// Test routes
		RouteEntry[] entries = new RouteEntry[] {
		        new RouteEntry("Alfa", "Alfa", 0),
		        new RouteEntry("Bravo", "Bravo", 1),
		        new RouteEntry("Charlie", "Charlie", 2),
		        new RouteEntry("Delta", "Zulu", 3),
		        new RouteEntry("Echo", "Zulu", RouteEntry.INFINITY / 2 + 1),
		        new RouteEntry("Foxtrot", "Yankee", 1),
		        new RouteEntry("Golf", "Yankee", RouteEntry.INFINITY),
		        new RouteEntry("Hotel", "Hotel", RouteEntry.INFINITY),
		        new RouteEntry("Yankee", "Yankee", 1),
		        new RouteEntry("Zulu", "Zulu", 1),
		    };

		// Fill the table :
		for(int i=0; i<entries.length; i++)
			table.put(entries[i]);
		
		// Example for one recipient :
		String[][] vectorTable = table.makeVector("Alfa", true, true, false);
		for (int i = 0; i < vectorTable.length; i++)
			System.out.println("Route "+i+" : dest = " + vectorTable[i][0] + ", metrics = " + vectorTable[i][1]);
	}
}

