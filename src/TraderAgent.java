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

import java.util.stream.Collectors;

public class TraderAgent extends Agent {
    private int strategyId;
    private double agent_money;
    private List<List<HashMap<String, HashMap<String, Double>>>> marketHistory;
    private HashMap<String,Integer> agent_stocks;
    private List<Order> agent_orders;
    private Double current_comission;

    protected Double getTotalStocks(){
        Double total = 0.0;
        List<HashMap<String, HashMap<String, Double>>> lastDay = marketHistory.get(marketHistory.size()-1);

        for (Map.Entry<String, Integer> entry : agent_stocks.entrySet()) {
            String stock = entry.getKey();
            for (int j = 0; j < lastDay.size(); j++) {
                String currentStock = lastDay.get(j).keySet().iterator().next().toString();
                if (currentStock.equals(stock)) {
                    total += lastDay.get(j).get(currentStock).get("close")*entry.getValue();
                }
            }
        }
        return total;
    }

    protected Boolean hasStock(String stock, Integer quantity){
        if (agent_stocks.containsKey(stock)){
            if (agent_stocks.get(stock)>quantity)
                return true;
        }
        return false;
    }

    protected void buyStock(String stock, Integer quantity){
        if (hasStock(stock,quantity)) {
            Integer currVal = agent_stocks.get(stock);
            agent_stocks.put(stock, currVal + quantity);
        }
        else{
            agent_stocks.put(stock, quantity);
        }
    }

    protected void sellStock(String stock, Integer quantity){
        Integer currVal = agent_stocks.get(stock);
        agent_stocks.put(stock, currVal-quantity);
    }
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

        agent_money = 2000;
        marketHistory = new ArrayList<>();
        agent_stocks = new HashMap<String, Integer>();
        agent_orders = new ArrayList<>();
        current_comission = 0.0;
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
                    System.out.println("[AGENT] Ended market days with " + (getTotalStocks()+agent_money) + " and made " + agent_orders.size() + " orders");
                    doDelete();
                }

                // convert string to hashmap
                List<HashMap<String, HashMap<String, Double>>> dailyInfo = null;
                try {
                    dailyInfo = stringToListHashMaps(msg.getContent());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // add daily stock info to trader knowledge base
                TraderAgent.this.marketHistory.add(dailyInfo);
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
                    current_comission = Double.parseDouble(propose.getContent());
                    System.out.println("currcom");
                    System.out.println(current_comission);
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
                        System.out.println("[TRADER] Strategy 0 - Trend following");
                        result = this.trendStrategy();
                        break;
                    case 1:
                        System.out.println("[TRADER] Strategy 1 - Value Investing");
                        result = this.valueStrategy();
                        break;
                    case 2:
                        System.out.println("[TRADER] Strategy 2 - Momentum investing");
                        //result = this.momentumStrategy();
                        break;
                    case 3:
                        System.out.println("[TRADER] Strategy 3 - Contrarian Investing");
                        result = this.contrarianStrategy();
                        break;
                    default:
                        System.out.println("[TRADER] Unknown strategy.");
                }
                System.out.println("[AGENT] Ended market days with " + (getTotalStocks()+agent_money) + " and made " + agent_orders.size() + " orders");
                agent_orders.addAll(result);
                return result;
            }

            private Double getDayStockClose(Integer day, String stock){
                List<HashMap<String, HashMap<String, Double>>> currentDay = marketHistory.get(day);
                for (int i=0; i< currentDay.size(); i++) {
                    String currentStock = currentDay.get(i).keySet().iterator().next().toString();
                    if (currentStock.equals(stock)) {
                        return currentDay.get(i).get(currentStock).get("close");
                    }
                }
                return 0.0;
            }

            public Set<String> getStockList(){
                Set<String> stocks = new HashSet<>();
                // Get list of all stocks
                for (HashMap<String, HashMap<String, Double>> hashmap : marketHistory.get(0)) {
                    stocks.addAll(hashmap.keySet());
                }
                return stocks;
            }

            private List<Order> trendStrategy(){
                List<Order> orders = new ArrayList<>();
                Map<String, Double> lastCloses = new HashMap<>();

                List<HashMap<String, HashMap<String, Double>>> currentDay = marketHistory.get(marketHistory.size()-1);
                //iterate through stocks
                for (int i=0; i< currentDay.size(); i++) {
                    String currentStock = currentDay.get(i).keySet().iterator().next().toString();
                    //System.out.println("stock " + currentStock);
                    Double currentClose = currentDay.get(i).get(currentStock).get("close");
                    Double prevClose;

                    // Check if stock has trended up or down over last 6 days
                    boolean uptrend = true;
                    boolean downtrend = true;
                    if (marketHistory.size() < 6){
                        uptrend = false;
                        downtrend = false;
                    }
                    for (int j = marketHistory.size() - 2; j > marketHistory.size() - 7 && marketHistory.size() - 6 >= 0; j--) {
                        //String cmpStock = marketHistory.get(j).get(i).keySet().iterator().next().toString();
                        //System.out.println("cmp inner stock " + currentStock  + " it " + Integer.toString(marketHistory.size()-j-2));
                        prevClose = getDayStockClose(j, currentStock);
                        //System.out.println("Iteration " + Integer.toString(marketHistory.size()-j-2));
                        if (marketHistory.size() < 6){
                            uptrend = false;
                            downtrend = false;
                        }
                        if (prevClose <= currentClose) {
                            uptrend = false;
                        }
                        if (prevClose >= currentClose) {
                            downtrend = false;
                        }
                        currentClose = prevClose;
                    }

                    // Make order if trend is up or down
                    if (uptrend) {
                        if (agent_money>currentClose) {
                            orders.add(new Order(Constants.ORDER_TYPES.BUY, currentClose, 1, currentStock));
                            buyStock(currentStock, 1);
                            agent_money-=currentClose*(1-current_comission);
                        } /*else {
                            orders.add(new Order(Constants.ORDER_TYPES.SHORT, currentClose, 1, currentStock));
                        }*/
                    } else if (downtrend) {
                        if (hasStock(currentStock,1)) {
                            orders.add(new Order(Constants.ORDER_TYPES.SELL, currentClose, 1, currentStock));
                            sellStock(currentStock,1);
                            agent_money+=currentClose*(1-current_comission);
                        } /*else {
                            orders.add(new Order(Constants.ORDER_TYPES.SHORT, currentClose, 1, currentStock));
                        }*/
                    }
                }
                System.out.println("[AGENT] made " + Integer.toString(orders.size()) + " orders on day " + marketHistory.size());
                return orders;
            }

            private List<Order> valueStrategy(){
                List<Order> orders = new ArrayList<>();
                Map<String, Double> lastCloses = new HashMap<>();

                List<HashMap<String, HashMap<String, Double>>> currentDay = marketHistory.get(marketHistory.size()-1);
                //iterate through stocks
                for (int i=0; i< currentDay.size(); i++) {
                    String currentStock = currentDay.get(i).keySet().iterator().next().toString();
                    System.out.println("stock " + currentStock);
                    Double currentClose = currentDay.get(i).get(currentStock).get("close");

                    Double closeSum = currentClose;
                    List<Double> closes = new ArrayList<>();
                    Double close;
                    // Check standard deviation and averages for at least 7 days
                    for (int j = marketHistory.size() - 2; j > 0 && marketHistory.size()  >= 7; j--) {
                        close = getDayStockClose(j, currentStock);
                        closeSum += close;
                        closes.add(close);
                    }

                    Double avgPrice = closeSum/marketHistory.size();
                    Double stdDev = Math.sqrt(closes.stream().mapToDouble(number -> Math.pow(number - avgPrice, 2)).sum()/(marketHistory.size()-1));

                    // Determine whether to buy, sell, or hold
                    if (currentClose < avgPrice + stdDev && currentClose > avgPrice) {
                        // Buy order
                        if (agent_money>currentClose) {
                            orders.add(new Order(Constants.ORDER_TYPES.BUY, currentClose, 1, currentStock));
                            buyStock(currentStock, 1);
                        }
                    } else if (currentClose < avgPrice - 2 * stdDev) {
                        // Sell order
                        if (hasStock(currentStock,1)) {
                            orders.add(new Order(Constants.ORDER_TYPES.SELL, currentClose, 1, currentStock));
                            sellStock(currentStock,1);
                        }
                    }
                }
                System.out.println("[AGENT] made " + Integer.toString(orders.size()) + " orders on day " + marketHistory.size());
                return orders;
            }

            /*private List<Order> momentumStrategy(){
                List<Order> orders = new ArrayList<>();
                Map<String, Double> lastPrices = new HashMap<>();
                Map<String, Double> lastReturns = new HashMap<>();
                Map<String, Double> lastVolumes = new HashMap<>();
                int numDays = marketHistory.size();
                int lookbackPeriod = 6; // Lookback period of 6 months

                // Loop over each day in the market history
                for (int i = 0; i < numDays; i++) {
                    List<HashMap<String, HashMap<String, Double>>> day = marketHistory.get(i);

                    // Loop over each stock on the current day
                    for (HashMap<String, HashMap<String, Double>> stock : day) {
                        String ticker = stock.keySet().iterator().next();
                        double price = stock.get(ticker).get("close");
                        double volume = stock.get(ticker).get("volume");

                        // Compute the stock's return since the lookback period
                        if (lastPrices.containsKey(ticker)) {
                            double lastPrice = lastPrices.get(ticker);
                            double lastVolume = lastVolumes.get(ticker);
                            double lastReturn = (price - lastPrice) / lastPrice;
                            double momentum = 0;

                            // Compute the momentum of the stock
                            for (int j = i - lookbackPeriod; j < i; j++) {
                                if (j >= 0) {
                                    List<HashMap<String, HashMap<String, Double>>> prevDay = marketHistory.get(j);
                                    HashMap<String, HashMap<String, Double>> prevStock = prevDay.get(0);
                                    String prevTicker = prevStock.keySet().iterator().next();
                                    double prevPrice = prevStock.get(prevTicker).get("close");
                                    double prevReturn = (price - prevPrice) / prevPrice;
                                    momentum += prevReturn;
                                }
                            }

                            // Place order based on momentum and returns
                            if (lastReturn > 0 && momentum > 0) {
                                orders.add(new Order(Constants.ORDER_TYPES.BUY, ticker, volume, price));
                            } else if (lastReturn < 0 && momentum < 0) {
                                orders.add(new Order(Constants.ORDER_TYPES.SHORT, ticker, volume, price));
                            } else if (lastReturn < 0 && momentum > 0) {
                                orders.add(new Order(Constants.ORDER_TYPES.SELL, ticker, volume, price));
                            }
                        }

                        // Update the last price, volume and return for the stock
                        lastPrices.put(ticker, price);
                        lastVolumes.put(ticker, volume);
                    }
                }

                return orders;
            }*/

            private List<Order> contrarianStrategy(){
                List<Order> orders = new ArrayList<>();
                Map<String, Double> lastCloses = new HashMap<>();

                List<HashMap<String, HashMap<String, Double>>> currentDay = marketHistory.get(marketHistory.size()-1);
                //iterate through stocks
                for (int i=0; i< currentDay.size(); i++) {
                    String currentStock = currentDay.get(i).keySet().iterator().next().toString();
                    System.out.println("stock " + currentStock);
                    Double currentClose = currentDay.get(i).get(currentStock).get("close");
                    Double prevClose;

                    // Check if stock has trended up or down over last 6 days
                    boolean uptrend = true;
                    boolean downtrend = true;
                    if (marketHistory.size() < 6){
                        uptrend = false;
                        downtrend = false;
                    }
                    for (int j = marketHistory.size() - 2; j > marketHistory.size() - 7 && marketHistory.size() - 6 >= 0; j--) {
                        //String cmpStock = marketHistory.get(j).get(i).keySet().iterator().next().toString();
                        //System.out.println("cmp inner stock " + currentStock  + " it " + Integer.toString(marketHistory.size()-j-2));
                        prevClose = getDayStockClose(j, currentStock);
                        //System.out.println("Iteration " + Integer.toString(marketHistory.size()-j-2));
                        if (marketHistory.size() < 6){
                            uptrend = false;
                            downtrend = false;
                        }
                        if (prevClose <= currentClose) {
                            uptrend = false;
                        }
                        if (prevClose >= currentClose) {
                            downtrend = false;
                        }
                        currentClose = prevClose;
                    }

                    // Make order if trend is up or down
                    if (downtrend) {
                        if (agent_money>currentClose) {
                            orders.add(new Order(Constants.ORDER_TYPES.BUY, currentClose, 1, currentStock));
                            buyStock(currentStock, 1);
                        } /*else {
                            orders.add(new Order(Constants.ORDER_TYPES.SHORT, currentClose, 1, currentStock));
                        }*/
                    } else if (uptrend) {
                        if (hasStock(currentStock,1)) {
                            orders.add(new Order(Constants.ORDER_TYPES.SELL, currentClose, 1, currentStock));
                            sellStock(currentStock,1);
                        } /*else {
                            orders.add(new Order(Constants.ORDER_TYPES.SHORT, currentClose, 1, currentStock));
                        }*/
                    }
                }
                System.out.println("[AGENT] made " + Integer.toString(orders.size()) + " orders on day " + marketHistory.size());
                return orders;
            }
        }
    }
}
