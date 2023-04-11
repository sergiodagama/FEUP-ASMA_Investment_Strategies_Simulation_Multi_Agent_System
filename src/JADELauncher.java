import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class JADELauncher {

    public static void main(String[] args) {
        Runtime rt = Runtime.instance();

        Profile p1 = new ProfileImpl();
        ContainerController mainContainer = rt.createMainContainer(p1);

        AgentController ac1;
        try {
            ac1 = mainContainer.acceptNewAgent("market-agent", new MarketAgent());
            ac1.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        Object[] agentArgs = new Object[0];
        AgentController ac2;
        try {
            ac2 = mainContainer.acceptNewAgent("broker-agent", new BrokerAgent());
            ac2.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        AgentController ac3;
        try {
            ac3 = mainContainer.acceptNewAgent("broker-agent-2", new BrokerAgent());
            ac3.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        AgentController ac4;
        try {
            ac4 = mainContainer.acceptNewAgent("trader", new TraderAgent());
            ac4.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

}
