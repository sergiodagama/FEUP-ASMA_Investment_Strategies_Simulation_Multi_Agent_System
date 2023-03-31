public abstract class Order extends ContractNetInitiator {
      private final String type; // buy | sell | short-selling
        	private final Double quantity;
			private final Double valuePerAsset;
			private final String assetId;

		public Order(Agent a, ACLMessage msg, String type, Double quantity, Double valuePerAsset, String assetId) {
			super(a, msg);
			this.type = type;
			this.quantity = quantity;
			this.valuePerAsset = valuePerAsset;
			this.assetId = assetId;
		}

		protected Vector prepareCfps(ACLMessage cfp) {
			Vector v = new Vector();

			JSONObject order = new JSONObject();

			order.put("type", "buy");  // buy | sell | short-sell
			order.put("num", new Integer(100));
			order.put("balance", new Double(1000.21));
			order.put("is_vip", new Boolean(true));
			
			cfp.addReceiver(new AID("a1", false));
			cfp.addReceiver(new AID("a2", false));
			cfp.addReceiver(new AID("a3", false));
			cfp.setContent("this is a call...");
			
			v.add(cfp);
			
			return v;
    	}

		protected void handleAllResponses(Vector responses, Vector acceptances) {
			
			// System.out.println("got " + responses.size() + " responses!");
			
			// for(int i=0; i<responses.size(); i++) {
			// 	ACLMessage msg = ((ACLMessage) responses.get(i)).createReply();
			// 	msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
			// 	acceptances.add(msg);
			// }
		}
		
		protected void handleAllResultNotifications(Vector resultNotifications) {
			System.out.println("got " + resultNotifications.size() + " result notifs!");
		}
    
}