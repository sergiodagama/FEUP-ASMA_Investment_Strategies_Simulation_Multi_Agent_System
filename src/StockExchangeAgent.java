import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;



public class StockExchangeAgent extends Agent {

	protected void setup() {
        System.out.println("[BROKER] Broker Agent " + getAID().getName() + " is ready.");
        addBehaviour(new StockBehaviour());
    }
	
	private class StockBehaviour extends CyclicBehaviour {
		public void action() {
			ACLMessage msg = receive();
			if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
				addBehaviour(new MyRequestResponder(myAgent, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
            } else {
                block();
            }
			
		}
		class MyRequestResponder extends AchieveREResponder {

			public MyRequestResponder(Agent a, MessageTemplate mt) {
				super(a, mt);
			}
			protected ACLMessage handleRequest(ACLMessage request) {
				System.out.println("Stock Exchange Agent received the request: " + request.getContent());
				
				ACLMessage response = new ACLMessage(ACLMessage.AGREE);
				response.setContent("AGREE to the request");
				return response;
			}
			
			protected ACLMessage prepareResultNotification(ACLMessage request,
                    ACLMessage response) {
				System.out.println("Stock Exchange Agent sending notification for the request: " 
                    + request.getContent() + ", with response: " + response.getContent());
				ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
				notification.setContent("Request has been correctly accepted");
				return notification;
			}
		}
	}
		
}
