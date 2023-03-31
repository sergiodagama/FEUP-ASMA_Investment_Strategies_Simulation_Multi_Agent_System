package Traders.Behaviour;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.Vector;

public abstract class TraderFIPAContractNetInit extends ContractNetInitiator {
    public TraderFIPAContractNetInit(Agent a, ACLMessage cfp) {
        super(a, cfp);
    }

    protected Vector prepareCfps(ACLMessage cfp) { // connect to brokers
            Vector v = new Vector();

            cfp.addReceiver(new AID("a1", false));
            cfp.addReceiver(new AID("a2", false));
            cfp.addReceiver(new AID("a3", false));
            cfp.setContent("this is a call...");

            v.add(cfp);

            return v;
        }

        protected abstract void handleAllResponses(Vector responses, Vector acceptances); // handle response from broker

        protected abstract void handleAllResultNotifications(Vector resultNotifications);

        public abstract void strategy(); // trader's strategy
}
