# Multi-Agent Stock Trading Simulation

This is a distributed stock trading simulation built for a university project. It uses Java for the core application, with multiple `TradingAgent` processes communicating with a central `MarketNode` via RMI.

The main goal is to demonstrate distributed systems concepts like:
*   Concurrency (multiple agents trading at once)
*   Logical Time (using Lamport Clocks to order events)
*   Fault Tolerance (detecting when an agent crashes)
*   Observability (using Prometheus and Grafana to see what's happening)

## Prerequisites

*   Java 11+
*   Apache Maven
*   Docker

## 1. Build the Project

This step is the same for all operating systems. Open a terminal or command prompt, navigate to the project root, and run:

```bash
mvn clean install
```
This creates a runnable JAR file at `target/distributed-stock-sim-1.0.0.jar`.

## 2. Run the Simulation

You will need to open multiple terminal or command prompt windows.

---

### On macOS / Linux

**Terminal 1: Start the Market**
```bash
java -jar target/distributed-stock-sim-1.0.0.jar market
```

**Terminal 2 (and 3, 4, etc.): Start the Agents**
```bash
# For the first agent
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-1

# For the second agent
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-2
```

**Optional: Run the UI**
```bash
java -jar target/distributed-stock-sim-1.0.0.jar ui
```

---

### On Windows

**Command Prompt 1: Start the Market**
```cmd
java -jar target\distributed-stock-sim-1.0.0.jar market
```

**Command Prompt 2 (and 3, 4, etc.): Start the Agents**
```cmd
:: For the first agent
java -jar target\distributed-stock-sim-1.0.0.jar agent agent-1

:: For the second agent
java -jar target\distributed-stock-sim-1.0.0.jar agent agent-2
```

**Optional: Run the UI**
```cmd
java -jar target\distributed-stock-sim-1.0.0.jar ui
```

---

## 3. View the Dashboard

The best way to see the simulation is through the Grafana dashboard.

1.  **Start Prometheus & Grafana:**
    If you have a `docker-compose.yml` file, you can simply run `docker-compose up -d`.

    If you need to run them manually:

    *   **On macOS / Linux:**
        ```bash
        # Prometheus
        docker run -d -p 9090:9090 -v $(pwd)/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml --name prometheus prom/prometheus
        # Grafana
        docker run -d -p 3000:3000 --name grafana grafana/grafana
        ```

    *   **On Windows (using Command Prompt):**
        ```cmd
        :: Prometheus
        docker run -d -p 9090:9090 -v %cd%/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml --name prometheus prom/prometheus
        :: Grafana
        docker run -d -p 3000:3000 --name grafana grafana/grafana
        ```

2.  **Open Grafana:**
    Go to `http://localhost:3000` in your browser.

3.  **View the Dashboard:**
    Log in (default `admin`/`admin`), add Prometheus as a data source (the URL should be `http://prometheus:9090` if using Docker networking, or `http://localhost:9090` if not), and import the dashboard from the `grafana/dashboard.json` file.
