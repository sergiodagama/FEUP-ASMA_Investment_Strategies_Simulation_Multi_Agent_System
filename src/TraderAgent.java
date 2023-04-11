import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.SSResponderDispatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class TraderAgent extends Agent {
    protected void setup() {
        System.out.println("[TRADER] Trader Agent " + getAID().getName() + " is ready.");

        // Subscribe to Market agent
        ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
        subscription.addReceiver(new AID(Constants.MARKET_AGENT_NAME, AID.ISLOCALNAME));
        send(subscription);

        addBehaviour(new ExecuteStrategyDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
//        addBehaviour(new ExecuteStrategyBehaviour());
    }

    private class ExecuteStrategyDispatcher extends SSResponderDispatcher {
        public ExecuteStrategyDispatcher(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected Behaviour createResponder(ACLMessage initiationMsg) {
            return new ExecuteStrategyBehaviour(initiationMsg);
        }


        private class ExecuteStrategyBehaviour extends Behaviour {
            private boolean isDone = false;
            private ACLMessage msg;

            public ExecuteStrategyBehaviour(ACLMessage initiationMsg) {
                super();
                this.msg = initiationMsg;
            }

            public void action() {
                // Receive daily info from Market agent

                // if no more days message from market -> terminate agent
                if (msg.getContent().equals(Constants.MARKET_NO_MORE_DAYS_MSG)) {
                    doDelete();
                }

                // convert string to hashmap
                List<HashMap<String, HashMap<String, Double>>> dailyInfo = null;
                try {
                    dailyInfo = stringToListHashMaps(msg.getContent());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }


                // Execute strategy and create order message
                List<Order> strategyResults = executeStrategy(dailyInfo);

                for (Order order : strategyResults) {
                    // Send order message using Contract Net Protocol
                    addBehaviour(new MyNetInitiator(this.getAgent(), new ACLMessage(ACLMessage.CFP), order) {

                    });
                }
                isDone = true;
            }

            @Override
            public boolean done() {
                return isDone;
            }

            class MyNetInitiator extends ContractNetInitiator {
                private Order order;

                public MyNetInitiator(Agent a, ACLMessage cfp, Order order) {
                    super(a, cfp);
                    this.order = order;
                }

                @Override
                protected Vector prepareCfps(ACLMessage cfp) {
                    Vector v = new Vector();

                    for (String brokerAgent : Constants.BROKER_AGENT_NAMES) {
                        cfp.addReceiver(new AID(brokerAgent, AID.ISLOCALNAME));
                    }
                    cfp.setContent("test");
                /*try {
                    cfp.setContent(order.serialize());
                    cfp.setContent("test");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/

                    v.add(cfp);
                    return v;
                }

                @Override
                protected void handlePropose(ACLMessage propose, Vector v) {
                    // Handle commission offer from Broker agent
                    System.out.println("Trader Agent " + getAID().getName() + " received commission offer: " + propose.getContent() + " from " + propose.getSender());
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    // Handle refusal from Broker agent
                    System.out.println("Trader Agent " + getAID().getName() + " received refusal from Broker Agent.");
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    // Handle failure from Broker agent
                    System.out.println("Trader Agent " + getAID().getName() + " received failure from Broker Agent.");
                }

                @Override
                protected void handleAllResponses(Vector v, Vector a) {
                    System.out.println("In all responses!!");

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
                        System.out.println("In ACCEPT PROP!!");
                    }
                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    // Handle confirmation from Broker agent
                    System.out.println("Trader Agent " + getAID().getName() + " received confirmation from Broker Agent.");
                }
            }

            private List<HashMap<String, HashMap<String, Double>>> stringToListHashMaps(String encoded) throws IOException, ClassNotFoundException {
                // Decode the string back into the object
                byte[] decodedBytes = Base64.getDecoder().decode(encoded);
                ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
                ObjectInputStream ois = new ObjectInputStream(bais);
                return (List<HashMap<String, HashMap<String, Double>>>) ois.readObject();
            }

            private List<Order> executeStrategy(List<HashMap<String, HashMap<String, Double>>> dailyInfo) {
                List<Order> result = new ArrayList<>();

                // TODO: add actual strategy implementation

                // THE FOLLOWING line IS JUST FOR TESTING
                result.add(new Order(Constants.ORDER_TYPES.BUY, 15, 100, "IF"));

                return result;
            }
        }
    }
}