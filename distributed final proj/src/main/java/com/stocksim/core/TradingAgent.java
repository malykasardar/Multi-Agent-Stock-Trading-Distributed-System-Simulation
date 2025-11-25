package com.stocksim.core;

import com.stocksim.data.*;
import com.stocksim.metrics.Metrics;
import com.stocksim.net.MarketNodeRemote;

import java.rmi.RemoteException;
import java.util.Random;

/**
 * This is the code for a single trading agent. Each one runs in its own thread
 * and acts like an independent person trading on the market.
 */
public class TradingAgent implements Runnable {

    private static final int BASE_METRICS_PORT = 9090;

    private final String agentId;
    private final MarketNodeRemote market;
    private final LamportClock clock;
    private final Random random = new Random();
    private final String[] stockSymbols = {"AAPL", "GOOG", "TSLA"};
    private final boolean simulateFailure;
    private int messageCount = 0;

    public TradingAgent(String agentId, MarketNodeRemote market, boolean simulateFailure) {
        this.agentId = agentId;
        this.market = market;
        this.clock = new LamportClock();
        this.simulateFailure = simulateFailure;

        // This is a simple way to give each agent its own metrics port.
        // It just parses the number from the agentId (e.g., "agent-1" -> 1).
        try {
            int agentNumericId = Integer.parseInt(agentId.substring(agentId.lastIndexOf("-") + 1));
            int metricsPort = BASE_METRICS_PORT + agentNumericId;
            Metrics.startMetricsServer(metricsPort);
            Metrics.NODE_STATUS.labels(this.agentId).set(1); // 1 for UP
            tick();
        } catch (NumberFormatException e) {
            System.err.println("Could not parse agent ID for metrics port: " + agentId);
        }
    }

    // My own clock tick. I call this before I send any message.
    private long tick() {
        long timestamp = this.clock.updateOnSend();
        Metrics.LAMPORT_CLOCK.labels(this.agentId).set(timestamp);
        return timestamp;
    }

    /**
     * This is the main loop for the agent. It's basically the agent's entire life.
     * It just sleeps for a bit, then decides to either trade or send a heartbeat.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000 + random.nextInt(2000));

                // I added this flag mainly for the demo, to prove the failure
                // detector on the MarketNode actually works.
                if (simulateFailure && messageCount > (5 + random.nextInt(5))) {
                    System.out.printf("!!! Agent %s is now SIMULATING FAILURE - stopping all messages. !!!%n", agentId);
                    Metrics.NODE_STATUS.labels(this.agentId).set(0);
                    break; // Stop sending messages.
                }

                // I made it more likely to send an order than a heartbeat to keep things interesting.
                boolean sendOrder = random.nextDouble() > 0.3;
                if (sendOrder) {
                    sendOrderMessage();
                } else {
                    sendHeartbeatMessage();
                }
                messageCount++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RemoteException e) {
            // If I can't talk to the market, I should probably just shut down.
            System.err.printf("Agent %s lost connection to MarketNode: %s%n", agentId, e.getMessage());
            Metrics.NODE_STATUS.labels(this.agentId).set(0);
        }
    }

    // Creates and sends a random trade order.
    private void sendOrderMessage() throws RemoteException {
        // The "trading strategy" is just to pick everything randomly. The goal here
        // wasn't to make a smart agent, but to generate traffic for the system.
        OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
        String symbol = stockSymbols[random.nextInt(stockSymbols.length)];
        int quantity = 1 + random.nextInt(100);
        double price = 10.0 + (190.0 * random.nextDouble());
        Order order = new Order(agentId, symbol, quantity, price, type);

        System.out.printf("Agent %s: Local clock before sending ORDER: %d%n", agentId, clock.getTime());
        long timestamp = tick(); // Important: get the timestamp *before* sending.
        System.out.printf("Agent %s: Local clock after tick (ORDER): %d. Sending timestamp: %d%n", agentId, clock.getTime(), timestamp);

        TradeMessage message = new TradeMessage(agentId, MarketNode.RMI_NAME, MessageType.ORDER, order, timestamp);
        market.submitMessage(message);

        Metrics.MESSAGES_SENT_TOTAL.labels(this.agentId).inc();

        System.out.printf("[LT=%d] Agent %s -> Market: Sent ORDER %s %d %s @ %.2f%n",
                timestamp, agentId, type, quantity, symbol, price);
    }

    // Sends a heartbeat to let the MarketNode know I'm still alive.
    private void sendHeartbeatMessage() throws RemoteException {
        System.out.printf("Agent %s: Local clock before sending HEARTBEAT: %d%n", agentId, clock.getTime());
        long timestamp = tick(); // Timestamp the heartbeat too.
        System.out.printf("Agent %s: Local clock after tick (HEARTBEAT): %d. Sending timestamp: %d%n", agentId, clock.getTime(), timestamp);

        TradeMessage message = new TradeMessage(agentId, MarketNode.RMI_NAME, MessageType.HEARTBEAT, null, timestamp);
        market.submitMessage(message);

        Metrics.HEARTBEATS_TOTAL.labels(this.agentId).inc();
        Metrics.MESSAGES_SENT_TOTAL.labels(this.agentId).inc();

        System.out.printf("[LT=%d] Agent %s -> Market: Sent HEARTBEAT%n", timestamp, agentId);
    }
}
