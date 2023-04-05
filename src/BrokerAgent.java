import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetResponder;

public class BrokerAgent extends Agent {  // TODO: change this name after
    private double commission = 0.1; // 10%
    // TODO: later the commission has to be dependent on the asset, quantity and value
    private AID brokerAgentAID;

    protected void setup() {
        System.out.println("Broker Agent " + getAID().getName() + " is ready.");
        addBehaviour(new HandleOrderBehaviour());

        // Set the static AID for the BrokerAgent
        brokerAgentAID = new AID(Constants.BROKER_AGENT_NAMES.get(0), AID.ISLOCALNAME);
    }

    private class HandleOrderBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive order messages from Trader agents
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.PROPOSE) {
                // Handle order using Contract Net Protocol
                addBehaviour(new ContractNetResponder(myAgent, msg) {
                    protected ACLMessage handleCfp(ACLMessage cfp) {
                        // Respond with best commission offer
                        ACLMessage propose = cfp.createReply();
                        propose.setPerformative(ACLMessage.PROPOSE);
                        propose.setContent(Double.toString(commission));
                        return propose;
                    }

                    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) {
                        // Send order to Exchange agent
                        ACLMessage order = new ACLMessage(ACLMessage.REQUEST);
                        order.addReceiver(new AID(Constants.EXCHANGE_AGENT_NAME, AID.ISLOCALNAME));
                        order.setContent(cfp.getContent());
                        send(order);

                        // Send confirmation message to Trader agent
                        ACLMessage inform = accept.createReply();
                        inform.setPerformative(ACLMessage.INFORM);
                        return inform;
                    }
                });
            } else {
                block();
            }
        }
    }
}
