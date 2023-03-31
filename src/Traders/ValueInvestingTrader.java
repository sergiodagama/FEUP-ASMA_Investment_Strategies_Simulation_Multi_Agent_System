package Traders;

import Traders.Behaviour.TraderFIPAContractNetInit;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.Vector;

public class ValueInvestingTrader extends Agent {
    public void setup() {
        addBehaviour(new ContractInitValueInvesting(this, new ACLMessage(ACLMessage.CFP)));
    }

    class ContractInitValueInvesting extends TraderFIPAContractNetInit {
        public ContractInitValueInvesting(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
        }

        protected void handleAllResultNotifications(Vector resultNotifications) {
        }

        public void strategy(){
        }
    }
}
