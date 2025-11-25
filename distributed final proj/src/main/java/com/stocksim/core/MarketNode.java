package com.stocksim.core;

import com.stocksim.data.*;
import com.stocksim.metrics.Metrics;
import com.stocksim.net.HeartbeatMonitor;
import com.stocksim.net.MarketNodeRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * This is the main server for the whole simulation. It's the central point
 * that all the agents talk to. I used RMI for the remote communication.
 */
public class MarketNode extends UnicastRemoteObject implements MarketNodeRemote {

    public static final String RMI_NAME = "MarketNode";
    private static final int METRICS_PORT = 8080;
    private static final int MAX_TRADES_IN_STATE = 50;
    private static final long AGENT_TIMEOUT_MS = 10000; // 10 seconds

    private final String nodeId;
    private final LamportClock clock;
    // Using CopyOnWriteArrayList for the trade log since it's mostly reads
    // and I don't want to lock it every time the UI polls for state.
    private final List<Trade> tradeLog;
    private final HeartbeatMonitor monitor;
    private final Map<String, String> agentStatuses;

    public MarketNode() throws RemoteException {
        super();
        this.nodeId = "market-node-01";
        this.clock = new LamportClock();
        this.tradeLog = new CopyOnWriteArrayList<>();
        this.monitor = new HeartbeatMonitor();
        this.agentStatuses = new ConcurrentHashMap<>();

        Metrics.startMetricsServer(METRICS_PORT);
        Metrics.NODE_STATUS.labels(this.nodeId).set(1);
        tick();

        // The failure detector needs to run in the background so it doesn't
        // block the main message processing loop.
        Thread failureDetectorThread = new Thread(this::runFailureDetector);
        failureDetectorThread.setDaemon(true);
        failureDetectorThread.start();

        System.out.println("MarketNode initialized. Failure detector started.");
    }

    // Just a helper to tick my clock and update the metric at the same time.
    private void tick() {
        this.clock.tick();
        Metrics.LAMPORT_CLOCK.labels(this.nodeId).set(this.clock.getTime());
    }

    // This is the background thread loop that checks for dead agents.
    private void runFailureDetector() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(2000); // Check every couple of seconds.
                Map<String, String> currentStatuses = monitor.getStatuses(AGENT_TIMEOUT_MS);
                for (Map.Entry<String, String> entry : currentStatuses.entrySet()) {
                    String agentId = entry.getKey();
                    String currentStatus = entry.getValue();
                    String previousStatus = agentStatuses.getOrDefault(agentId, "ACTIVE");

                    // Only print/log the failure on the transition from ACTIVE to FAILED.
                    if ("FAILED".equals(currentStatus) && "ACTIVE".equals(previousStatus)) {
                        System.out.printf("[FAULT DETECTOR] Agent %s has failed (no heartbeat). Marking as FAILED.%n", agentId);
                        Metrics.FAILURES_DETECTED_TOTAL.inc();
                        Metrics.NODE_STATUS.labels(agentId).set(0);
                    }
                    agentStatuses.put(agentId, currentStatus);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * This is the main RMI entry point. I synchronized it to be safe,
     * so I only process one agent message at a time. It keeps the state
     * consistent without needing more complex locks.
     */
    @Override
    public synchronized void submitMessage(TradeMessage message) throws RemoteException {
        Metrics.MESSAGES_RECEIVED_TOTAL.labels(this.nodeId).inc();

        System.out.printf("MarketNode: Local clock before receiving message from %s (LT=%d): %d%n",
                message.getSenderId(), message.getLamportTimestamp(), clock.getTime());
        
        // The most important part of the Lamport clock logic.
        // My clock has to be greater than any message I receive.
        clock.updateOnReceive(message.getLamportTimestamp());
        Metrics.LAMPORT_CLOCK.labels(this.nodeId).set(this.clock.getTime());
        
        System.out.printf("MarketNode: Local clock after updateOnReceive: %d%n", clock.getTime());

        switch (message.getType()) {
            case ORDER:
                handleOrder(message);
                break;
            case HEARTBEAT:
                handleHeartbeat(message);
                break;
        }
    }

    // Handles an ORDER message.
    private void handleOrder(TradeMessage message) {
        Order order = message.getOrder();
        if (order == null) return;

        Metrics.TRADES_TOTAL.labels(order.getType().toString()).inc();

        // I didn't build a real order book. For this simulation, I'm just
        // treating every order as an instant trade. The main point is to
        // log it with the correct Lamport timestamp.
        Trade executedTrade = new Trade(
                UUID.randomUUID().toString(),
                order.getAgentId(),
                order.getStockSymbol(),
                order.getQuantity(),
                order.getPrice(),
                clock.getTime(), // This is the official timestamp of the trade.
                System.currentTimeMillis()
        );
        tradeLog.add(executedTrade);
        System.out.printf("[LT=%d] MarketNode: Processed %s order from %s (Msg LT=%d)%n",
                clock.getTime(), order.getType(), order.getAgentId(), message.getLamportTimestamp());
    }

    // Handles a HEARTBEAT by just resetting the agent's timer in the monitor.
    private void handleHeartbeat(TradeMessage message) {
        monitor.updateHeartbeat(message.getSenderId());
        // If an agent was marked down, a heartbeat brings it back online in the metrics.
        Metrics.NODE_STATUS.labels(message.getSenderId()).set(1);
        System.out.printf("[LT=%d] MarketNode: Received heartbeat from %s (Msg LT=%d)%n",
                clock.getTime(), message.getSenderId(), message.getLamportTimestamp());
    }

    /**
     * This is the RMI method for the UI to get the latest state.
     */
    @Override
    public SystemState getState() throws RemoteException {
        // I'm only sending the last 50 trades to keep the UI from lagging
        // if the simulation runs for a long time.
        List<Trade> recentTrades = tradeLog.stream()
                .skip(Math.max(0, tradeLog.size() - MAX_TRADES_IN_STATE))
                .collect(Collectors.toList());
        return new SystemState(recentTrades, monitor.getStatuses(AGENT_TIMEOUT_MS), "UP");
    }
}
