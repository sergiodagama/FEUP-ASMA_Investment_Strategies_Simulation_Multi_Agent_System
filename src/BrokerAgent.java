import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;

import java.util.HashMap;
import java.util.List;

public class BrokerAgent extends Agent {
    // TODO: check that only the commissions if, the allowed array and the AID are going to be different
    private AID brokerAgentAID;
    private List<String> allowedOrders;

    protected void setup() {
        System.out.println("Broker Agent " + getAID().getName() + " is ready.");
        addBehaviour(new HandleOrderBehaviour());

        // set the static AID for the BrokerAgent
        brokerAgentAID = new AID(Constants.BROKER_AGENT_NAMES.get(0), AID.ISLOCALNAME);

        // initialize the list of allowed orders
        allowedOrders.add(String.valueOf(Constants.ORDER_TYPES.SELL));
        allowedOrders.add(String.valueOf(Constants.ORDER_TYPES.BUY));
        allowedOrders.add(String.valueOf(Constants.ORDER_TYPES.SHORT));
    }

    private class HandleOrderBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive order messages from Trader agents
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.PROPOSE) {
                // Handle order using Contract Net Protocol

                addBehaviour(new ContractNetResponder(myAgent, MessageTemplate.MatchPerformative(ACLMessage.CFP)) {
                    protected ACLMessage handleCfp(ACLMessage cfp) {
                        // check commissions and types of orders allowed
                        Double commission = 0.1; // default commission

                        try {
                            Order order = Order.deserialize(cfp.getContent());

                            Double totalValue = order.getQuantity() * order.getValuePerAsset();

                            if (allowedOrders.contains(order.getOrderType())) {
                                if (totalValue <= 1000){
                                    commission = 0.1;
                                } else if (totalValue > 1000 && totalValue < 10000){
                                    commission = 0.05;
                                } else if (totalValue >= 10000) {
                                    commission = 0.01;
                                }
                            } else {
                                // Send a refuse message
                                ACLMessage refuse = cfp.createReply();
                                refuse.setPerformative(ACLMessage.REFUSE);
                                refuse.setContent(Constants.UNSUPPORTED_ORDER_TYPE);
                                myAgent.send(refuse);
                                return null;
                            }

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        // Respond with best commission offer
                        ACLMessage propose = cfp.createReply();
                        propose.setPerformative(ACLMessage.PROPOSE);
                        propose.setContent(Double.toString(commission));
                        return propose;
                    }

                    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) {
                        // Send order to Exchange agent
                        ACLMessage orderMessage = new ACLMessage(ACLMessage.REQUEST);
                        orderMessage.addReceiver(new AID(Constants.EXCHANGE_AGENT_NAME, AID.ISLOCALNAME));
                        orderMessage.setContent(cfp.getContent());
                        send(orderMessage);

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
