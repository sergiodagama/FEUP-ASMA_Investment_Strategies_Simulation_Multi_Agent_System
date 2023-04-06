import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;

public class MarketAgent extends Agent {
    private List<AID> subscribers;
    private List<List<HashMap<String, HashMap<String, Double>>>> dailyValues;
    private int currentDay = 0;
    private Boolean stopSubscriptionUpdate = false;
    private void loadDailyValues(){
        // load values from json file and fills the list with hash maps
        dailyValues = new ArrayList<>();

        try {
            String currentWorkingDir = System.getProperty("user.dir");
            System.out.println("Current working directory: " + currentWorkingDir);

            Scanner scanner = new Scanner(new File(Constants.DATA_FILENAME));
            String jsonString = scanner.useDelimiter("\\A").next();
            scanner.close();

            JSONArray outerArray = (JSONArray) new JSONTokener(jsonString).nextValue();

            for (Object innerListObj : outerArray) {
                List<HashMap<String, HashMap<String, Double>>> innerList = new ArrayList<>();
                JSONArray innerArray = (JSONArray) innerListObj;
                for (Object innerObj : innerArray) {
                    JSONObject innerJson = (JSONObject) innerObj;
                    HashMap<String, HashMap<String, Double>> innerMap = new HashMap<>();
                    String ticker = (String) innerJson.get("ticker");
                    Double high = ((Number) innerJson.get("high")).doubleValue();
                    Double low = ((Number) innerJson.get("low")).doubleValue();
                    Double close = ((Number) innerJson.get("close")).doubleValue();
                    Double volume = ((Number) innerJson.get("volume")).doubleValue();
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

        // Add behavior to handle subscription requests
        addBehaviour(new SubscriptionRequestsBehavior());

        // Initialize prices map with some initial values
        loadDailyValues();
        System.out.println("Finish loading data from file");

        // Add behavior to update prices to subscribers every 10 seconds
        addBehaviour(new TickerBehaviour(this, Constants.MARKET_DAILY_PERIOD) {
            @Override
            protected void onTick() {
                try {
                    MarketAgent.this.onTick();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (stopSubscriptionUpdate) {
                    System.out.println("HERE");
                    this.stop();
                }
            }
        });
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
            stopSubscriptionUpdate = true;
            return;
        }

        // Create a message with the latest prices
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(trader);
        msg.setContent(listOfHashMapToString(dailyValues.get(currentDay)));
        send(msg);

        // printDailyInfo(dailyValues.get(currentDay));
    }

    private void notifySubscribers() throws IOException {
        // Send the latest prices to all subscribers
        for (AID subscriber : subscribers) {
            sendPricesToTrader(subscriber);
        }

        // advance to next day
        currentDay++;
    }

    public void printDailyInfo(List<HashMap<String, HashMap<String, Double>>> list){
        for (HashMap<String, HashMap<String, Double>> map : list) {
            System.out.println(map.toString());
        }
    }

    public void onTick() throws IOException {
        // notify subscribers
        notifySubscribers();
        if (currentDay != dailyValues.size()) {
            System.out.println("Notifying subscribers | Day " + Integer.toString(currentDay));
        }
    }
}
