import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class JADELauncher {

    public static void main(String[] args) {
        initJade();
    }

    public static void initJade() {
        Runtime rt = Runtime.instance();

        Profile p1 = new ProfileImpl();
        ContainerController mainContainer = rt.createMainContainer(p1);

        AgentController ac1;
        try {
            ac1 = mainContainer.acceptNewAgent(Constants.MARKET_AGENT_NAME, new MarketAgent());
            ac1.start();

            AgentController ac2;
            Object[] agentArgs2 = {true, false, true, 0.5, 0.1, 0.01};
            ac2 = mainContainer.createNewAgent(Constants.BROKER_AGENT_NAMES.get(0), "BrokerAgent", agentArgs2);
            ac2.start();

            AgentController ac3;
            Object[] agentArgs3 = {true, true, false, 0.3, 0.07, 0.03};
            ac3 = mainContainer.createNewAgent(Constants.BROKER_AGENT_NAMES.get(1), "BrokerAgent", agentArgs3);
            ac3.start();

            AgentController ac7;
            Object[] agentArgs7 = {false, true, true, 0.1, 0.05, 0.02};
            ac7 = mainContainer.createNewAgent(Constants.BROKER_AGENT_NAMES.get(2), "BrokerAgent", agentArgs7);
            ac7.start();

            AgentController ac8;
            Object[] agentArgs8 = {true, true, true, 0.2, 0.1, 0.05};
            ac8 = mainContainer.createNewAgent(Constants.BROKER_AGENT_NAMES.get(3), "BrokerAgent", agentArgs8);
            ac8.start();

            AgentController ac4;
            Object[] agentArgs4 = {1};
            ac4 = mainContainer.createNewAgent("test-trader", "TraderAgent", agentArgs4);
            ac4.start();

            AgentController ac6;
            ac6 = mainContainer.acceptNewAgent("exchange-agent", new ExchangeAgent());
            ac6.start();

            AgentController ac5;
            Agent rma = new jade.tools.rma.rma();
            ac5 = mainContainer.acceptNewAgent("myRMA", rma);
            ac5.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
