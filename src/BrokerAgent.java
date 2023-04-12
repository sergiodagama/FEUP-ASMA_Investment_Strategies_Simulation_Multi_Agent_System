import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;

import java.util.ArrayList;
import java.util.List;

public class BrokerAgent extends Agent {
    private List<Constants.ORDER_TYPES> allowedOrders = new ArrayList<>();
    public List<Double> commissions = new ArrayList<>();

    protected void setup() {
        System.out.println("[BROKER] Broker Agent " + getAID().getName() + " is ready.");
        addBehaviour(new HandleOrderDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));

        // get arguments related to allowed orders
        Object[] args = getArguments();

        // initialize the list of allowed orders
        if((Boolean) args[0]) allowedOrders.add(Constants.ORDER_TYPES.SELL);
        if((Boolean) args[1]) allowedOrders.add(Constants.ORDER_TYPES.BUY);
        if((Boolean) args[2]) allowedOrders.add(Constants.ORDER_TYPES.SHORT);

        // arguments related to commissions
        commissions.add((Double) args[3]);
        commissions.add((Double) args[4]);
        commissions.add((Double) args[5]);
    }

    private class HandleOrderDispatcher extends SSResponderDispatcher {
        private MessageTemplate mt;

        public HandleOrderDispatcher(Agent a, MessageTemplate mt) {
            super(a, mt);
            this.mt = mt;
        }

        @Override
        protected Behaviour createResponder(ACLMessage initiationMsg) {
            System.out.println("[BROKER] Received CFP from " + initiationMsg.getSender().getName());
            return new HandleOrderBehaviour(myAgent, initiationMsg);
        }

        class HandleOrderBehaviour extends SSContractNetResponder {

            public HandleOrderBehaviour(Agent a, ACLMessage cfp) {
                super(a, cfp);
            }

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                // check commissions and types of orders allowed
                double commission = 0.02; // default commission

                try {
                    Order order = Order.deserialize(cfp.getContent());
                    double totalValue = order.getQuantity() * order.getValuePerAsset();

                    if (allowedOrders.contains(order.getOrderType())) {
                        if (totalValue <= 1000){
                            commission = commissions.get(0);
                        } else if (totalValue > 1000 && totalValue < 10000){
                            commission = commissions.get(0);;
                        } else if (totalValue >= 10000) {
                            commission = commissions.get(0);;
                        }
                    } else {
                        // send a refuse message
                        ACLMessage refuse = cfp.createReply();
                        refuse.setPerformative(ACLMessage.REFUSE);
                        refuse.setContent(Constants.UNSUPPORTED_ORDER_TYPE);
                        myAgent.send(refuse);
                        return null;
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // respond with best commission offer
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(Double.toString(commission));
                System.out.println("[BROKER] Sending PROPOSE with commission " + commission);
                return propose;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                System.out.println("[BROKER] Received ACCEPT-PROPOSAL from trader.");

                // send order to Exchange agent
                ACLMessage orderMessage = new ACLMessage(ACLMessage.REQUEST);
                orderMessage.addReceiver(new AID(Constants.EXCHANGE_AGENT_NAME, AID.ISLOCALNAME));
                orderMessage.setContent(cfp.getContent());
                send(orderMessage);

                // send confirmation message to Trader agent
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                return inform;
            }
        }
    }
}
