import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MarketAgent extends Agent {
    private List<AID> subscribers;
    private List<List<HashMap<String, HashMap<String, Double>>>> dailyValues;
    private int currentDay = 0;
    private AID marketAgentAID;
    private void loadDailyValues(){
        // load values from json file and fills the list with hash maps
        dailyValues = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONArray outerArray = (JSONArray) parser.parse(new FileReader(Constants.DATA_FILENAME));
            for (Object innerListObj : outerArray) {
                List<HashMap<String, HashMap<String, Double>>> innerList = new ArrayList<>();
                JSONArray innerArray = (JSONArray) innerListObj;
                for (Object innerObj : innerArray) {
                    JSONObject innerJson = (JSONObject) innerObj;
                    HashMap<String, HashMap<String, Double>> innerMap = new HashMap<>();
                    String ticker = (String) innerJson.get("ticker");
                    Double high = (Double) innerJson.get("high");
                    Double low = (Double) innerJson.get("low");
                    Double close = (Double) innerJson.get("close");
                    Double volume = (Double) innerJson.get("volume");
                    HashMap<String, Double> innerMap2 = new HashMap<>();
                    innerMap2.put("high", high);
                    innerMap2.put("low", low);
                    innerMap2.put("close", close);
                    innerMap2.put("volume", volume);
                    innerMap.put(ticker, innerMap2);
                    innerList.add(innerMap);
                }
                dailyValues.add(innerList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setup() {
        subscribers = new ArrayList<>();

        // Initialize prices map with some initial values
        loadDailyValues();

        // Add behavior to handle subscription requests
        addBehaviour(new SubscriptionRequestsBehavior());
        // Add behavior to update prices to subscribers every 10 seconds
        addBehaviour(new TickerBehaviour(this, Constants.MARKET_DAILY_PERIOD) {
            @Override
            protected void onTick() {
                try {
                    MarketAgent.this.onTick();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        // Set the static AID for the MarketAgent
        marketAgentAID = new AID(Constants.MARKET_AGENT_NAME, AID.ISLOCALNAME);
    }

    private String listOfHashMapToString(List<HashMap<String, HashMap<String, Double>>> list) throws IOException {
        // Convert the list to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(list);
        byte[] bytes = baos.toByteArray();

        // Encode the byte array into a string
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    protected void takeDown() {
        // Clean up resources
        subscribers.clear();
        dailyValues.clear();
    }

    private class SubscriptionRequestsBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            // Wait for subscription requests from traders
            ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
            if (msg != null) {
                // Add trader to subscribers list
                subscribers.add(msg.getSender());
                System.out.println("Trader with AID " + msg.getSender() + " subscribed to market.");
            } else {
                block();
            }
        }
    }

    private void sendPricesToTrader(AID trader) throws IOException {
        // if the market has reached the final day
        if (currentDay == dailyValues.size()){
            System.out.println("Market reached end of days.");
            ACLMessage msg = new ACLMessage(ACLMessage.FAILURE);
            msg.addReceiver(trader);
            msg.setContentObject(Constants.MARKET_NO_MORE_DAYS_MSG);
            send(msg);
            return;
        }

        // Create a message with the latest prices
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(trader);
        msg.setContent(listOfHashMapToString(dailyValues.get(currentDay)));
        send(msg);

        // advance to next day
        currentDay++;
    }

    private void notifySubscribers() throws IOException {
        // Send the latest prices to all subscribers
        for (AID subscriber : subscribers) {
            sendPricesToTrader(subscriber);
        }
    }

    public void onTick() throws IOException {
        // notify subscribers
        notifySubscribers();
    }
}
