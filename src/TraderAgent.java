import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TraderAgent extends Agent {
    protected void setup() {
        System.out.println("Trader Agent " + getAID().getName() + " is ready.");

        // Subscribe to Market agent
        ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
        subscription.addReceiver(new AID(Constants.MARKET_AGENT_NAME, AID.ISLOCALNAME));
        send(subscription);

        addBehaviour(new ExecuteStrategyBehaviour());
    }

    private class ExecuteStrategyBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive daily info from Market agent
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                // Execute strategy and create order message
                List<Order> strategyResults = executeStrategy(msg.getContent());  // TODO: convert this to the hashmap

                for(String brokerAgent : Constants.BROKER_AGENT_NAMES) {
                    for(Order order : strategyResults) {
                        ACLMessage orderMessage = new ACLMessage(ACLMessage.PROPOSE);
                        orderMessage.addReceiver(new AID(brokerAgent, AID.ISLOCALNAME));
                        orderMessage.setContent(order.toString());  // TODO: check the conversion here as well

                        // Send order message using Contract Net Protocol
                        addBehaviour(new ContractNetInitiator(myAgent, orderMessage) {
                            protected void handlePropose(ACLMessage propose, java.util.Vector v) {
                                // Handle commission offer from Broker agent
                                System.out.println("Trader Agent " + getAID().getName() + " received commission offer: " + propose.getContent());
                            }

                            protected void handleRefuse(ACLMessage refuse) {
                                // Handle refusal from Broker agent
                                System.out.println("Trader Agent " + getAID().getName() + " received refusal from Broker Agent.");
                            }

                            protected void handleFailure(ACLMessage failure) {
                                // Handle failure from Broker agent
                                System.out.println("Trader Agent " + getAID().getName() + " received failure from Broker Agent.");
                            }

                            protected void handleAllResponses(java.util.Vector v) {
                                // Choose the best commission offer and accept it
                                ACLMessage bestOffer = null;
                                double bestCommission = Double.MAX_VALUE;
                                for (Object obj : v) {
                                    ACLMessage offer = (ACLMessage) obj;
                                    if (offer.getPerformative() == ACLMessage.PROPOSE) {
                                        double commission = Double.parseDouble(offer.getContent());
                                        if (commission < bestCommission) {
                                            bestOffer = offer;
                                            bestCommission = commission;
                                        }
                                    }
                                }
                                if (bestOffer != null) {
                                    ACLMessage accept = bestOffer.createReply();
                                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    send(accept);
                                }
                            }

                            protected void handleInform(ACLMessage inform) {
                                // Handle confirmation from Broker agent
                                System.out.println("Trader Agent " + getAID().getName() + " received confirmation from Broker Agent.");
                            }
                        });
                    }
                }
            } else {
                block();
            }
        }

        private List<Order> executeStrategy(HashMap<String, HashMap<String, Double>> dailyInfo) {
            List<Order> result = new ArrayList<>();

            // TODO: add actual strategy implementation

            return result;
        }
    }
}

