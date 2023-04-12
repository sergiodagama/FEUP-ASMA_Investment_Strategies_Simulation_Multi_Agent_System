import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.SSResponderDispatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class TraderAgent extends Agent {
    private int strategyId;

    protected void setup() {
        System.out.println("[TRADER] Trader Agent " + getAID().getName() + " is ready.");

        // subscribe to Market agent
        ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
        subscription.addReceiver(new AID(Constants.MARKET_AGENT_NAME, AID.ISLOCALNAME));
        send(subscription);

        addBehaviour(new ExecuteStrategyDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE)));

        // get arguments
        Object[] args = getArguments();
        strategyId = (int) args[0];
        System.out.println("[TRADER] argument " + strategyId);

    }

    private class ExecuteStrategyDispatcher extends SSResponderDispatcher {
        public ExecuteStrategyDispatcher(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected Behaviour createResponder(ACLMessage initiationMsg) {
            return new ExecuteStrategyBehaviour(initiationMsg);
        }

        // receive daily info from Market agent
        private class ExecuteStrategyBehaviour extends Behaviour {
            private boolean isDone = false;
            private ACLMessage msg;

            public ExecuteStrategyBehaviour(ACLMessage initiationMsg) {
                super();
                this.msg = initiationMsg;
            }

            public void action() {
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

                // execute strategy and create order message
                List<Order> strategyResults = executeStrategy(dailyInfo);

                for (Order order : strategyResults) {
                    // send order message using Contract Net Protocol
                    addBehaviour(new MyNetInitiator(this.getAgent(), new ACLMessage(ACLMessage.CFP), order) {} );
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
                try {
                    cfp.setContent(order.serialize());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                    v.add(cfp);
                    return v;
                }

                @Override
                protected void handlePropose(ACLMessage propose, Vector v) {
                    // handle commission offer from Broker agent
                    System.out.println("[TRADER] " + getAID().getName() + " received commission offer: " + propose.getContent() + " from " + propose.getSender());
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    // handle refusal from Broker agent
                    System.out.println("[TRADER] " + getAID().getName() + " received refusal from Broker Agent.");
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    // handle failure from Broker agent
                    System.out.println("[TRADER] " + getAID().getName() + " received failure from Broker Agent.");
                }

                @Override
                protected void handleAllResponses(Vector v, Vector a) {
                    // choose the best commission offer and accept it
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

                @Override
                protected void handleInform(ACLMessage inform) {
                    // handle confirmation from Broker agent
                    System.out.println("[TRADER] " + getAID().getName() + " received confirmation from Broker Agent.");
                }
            }

            private List<HashMap<String, HashMap<String, Double>>> stringToListHashMaps(String encoded) throws IOException, ClassNotFoundException {
                // decode the string back into the object
                byte[] decodedBytes = Base64.getDecoder().decode(encoded);
                ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
                ObjectInputStream ois = new ObjectInputStream(bais);
                return (List<HashMap<String, HashMap<String, Double>>>) ois.readObject();
            }

            private List<Order> executeStrategy(List<HashMap<String, HashMap<String, Double>>> dailyInfo) {
                List<Order> result = new ArrayList<>();

                switch (strategyId) {
                    case 0: // used for testing purposes
                        System.out.println("[TRADER] Strategy 0");
                        result.add(new Order(Constants.ORDER_TYPES.BUY, 15, 100, "TEST"));
                        break;
                    case 1:
                        System.out.println("[TRADER] Strategy 1");
                        // TODO: call strategy 1 function
                        break;
                    case 2:
                        System.out.println("[TRADER] Strategy 2");
                        // TODO: call strategy 2 function
                        break;
                    case 3:
                        System.out.println("[TRADER] Strategy 3");
                        // TODO: call strategy 3 function
                        break;
                    default:
                        System.out.println("[TRADER] Unknown strategy.");
                }
                return result;
            }
        }
    }
}
