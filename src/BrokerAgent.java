import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;

import java.util.ArrayList;
import java.util.List;

public class BrokerAgent extends Agent {
    // TODO: check that only the commissions if, the allowed array and the AID are going to be different
    private List<Constants.ORDER_TYPES> allowedOrders = new ArrayList<>();

    protected void setup() {
        System.out.println("[BROKER] Broker Agent " + getAID().getName() + " is ready.");
//        addBehaviour(new HandleOrderBehaviour());
        addBehaviour(new HandleOrderDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
        // initialize the list of allowed orders
        allowedOrders.add(Constants.ORDER_TYPES.SELL);
        allowedOrders.add(Constants.ORDER_TYPES.BUY);
        allowedOrders.add(Constants.ORDER_TYPES.SHORT);
    }

    private class HandleOrderDispatcher extends SSResponderDispatcher {
        private MessageTemplate mt;

        public HandleOrderDispatcher(Agent a, MessageTemplate mt) {
            super(a, mt);
            this.mt = mt;
        }

        @Override
        protected Behaviour createResponder(ACLMessage initiationMsg) {
            System.out.println("[BROKER] Received CFP from in dispatcher" + initiationMsg.getSender().getName());
            return new HandleOrderBehaviour(myAgent, initiationMsg);
        }

        class HandleOrderBehaviour extends SSContractNetResponder {

            public HandleOrderBehaviour(Agent a, ACLMessage cfp) {
                super(a, cfp);
                System.out.println("[BROKER] HandleOrderBehaviour created");
            }

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                System.out.println("[BROKER] Received CFP from " + cfp.getSender().getName());
                // check commissions and types of orders allowed
                double commission = 0.02; // default commission
/*
                try {
                    Order order = Order.deserialize(cfp.getContent());

                    double totalValue = order.getQuantity() * order.getValuePerAsset();

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
                }*/

                // Respond with best commission offer
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(Double.toString(commission));
                System.out.println("[BROKER] Sending PROPOSE with commission " + commission);
                return propose;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                System.out.println("IN ACCEPT PROPOSAL");
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
        }

    }


}
