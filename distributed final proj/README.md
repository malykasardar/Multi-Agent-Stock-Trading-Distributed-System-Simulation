# Multi-Agent Stock Trading Simulation

This is a distributed stock trading simulation built for a university project. It uses Java for the core application, with multiple `TradingAgent` processes communicating with a central `MarketNode` via RMI.

The main goal is to demonstrate distributed systems concepts like:
*   Concurrency (multiple agents trading at once)
*   Logical Time (using Lamport Clocks to order events)
*   Fault Tolerance (detecting when an agent crashes)
*   Observability (using Prometheus and Grafana to see what's happening)

## Prerequisites

*   Java 11+
*   Maven
*   Docker

## How to Run

### 1. Build the Project

```bash
mvn clean install
```
This creates a runnable JAR at `target/distributed-stock-sim-1.0.0.jar`.

### 2. Run the Simulation

You'll need to open a few separate terminals.

**Terminal 1: Start the Market**
```bash
java -jar target/distributed-stock-sim-1.0.0.jar market
```
The market node exposes its metrics on port `8080`.

**Terminal 2 (and 3, 4, etc.): Start the Agents**
```bash
# For the first agent
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-1

# For the second agent
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-2
```
Each agent gets a unique ID and exposes metrics on its own port (9091, 9092, etc.).

**Optional: Run the UI**
```bash
java -jar target/distributed-stock-sim-1.0.0.jar ui
```
This opens a simple Swing UI that shows the trade log and agent statuses.

### 3. View the Dashboard

The best way to see the simulation is through the Grafana dashboard.

1.  **Start Prometheus & Grafana:**
    You'll need a `docker-compose.yml` file to run these. If you don't have one, you can run them manually, making sure to mount the `prometheus/prometheus.yml` config file for Prometheus.

2.  **Open Grafana:**
    Go to `http://localhost:3000` in your browser.

3.  **View the Dashboard:**
    Log in, add Prometheus as a data source (`http://prometheus:9090`), and import the dashboard from `grafana/dashboard.json`.

The dashboard will show you the Lamport clock progression, which nodes are online, trade activity, and any detected failures.