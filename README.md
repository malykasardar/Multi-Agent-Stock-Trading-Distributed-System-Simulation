# Multi-Agent Distributed Stock Trading Simulation

This project implements a distributed stock trading simulation using Java, Java RMI, Prometheus, and Grafana. Multiple autonomous TradingAgent processes communicate with a central MarketNode, allowing the system to model key distributed system concepts such as concurrency, event ordering, fault tolerance, and observability.

The simulation exposes metrics from each node through HTTP `/metrics` endpoints, which are scraped by Prometheus and visualized in Grafana.

## Features

### Distributed Concurrency

Multiple agents operate independently, each generating BUY and SELL orders in parallel.

### Lamport Logical Clocks

All events (messages, trades, and heartbeats) are timestamped using Lamport clocks to preserve causal ordering across distributed processes.

### Failure Detection

Agents periodically send heartbeats to the MarketNode. If heartbeats stop, the MarketNode marks the agent as failed.

### Metrics Exporter

Each component exposes Prometheus metrics, including:

* lamport_timestamp
* trade_count_total
* heartbeat_count_total
* failure_detected_total
* node_status

### Observability Dashboard

Grafana is used to visualize system behavior, including:

* Lamport clock progression
* Heartbeat frequency
* Trade throughput
* BUY vs SELL distribution
* Node status and failure detection

## Prerequisites

* Java 11 or higher
* Maven
* Docker (for Prometheus and Grafana)

## Building the Project

```bash
mvn clean install
```

This produces the runnable JAR file:

```
target/distributed-stock-sim-1.0.0.jar
```

## Running the Simulation

The system consists of one MarketNode and multiple TradingAgents. Each component must be run in its own terminal.

### 1. Start the MarketNode

```bash
java -jar target/distributed-stock-sim-1.0.0.jar market
```

The MarketNode exposes metrics on port 8080.

### 2. Start TradingAgents

Each TradingAgent must receive a unique ID:

```bash
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-1
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-2
java -jar target/distributed-stock-sim-1.0.0.jar agent agent-3
```

Agents expose metrics on ports 9091, 9092, 9093, and so on.

## Monitoring the System

Prometheus and Grafana can be started using Docker. A typical setup uses a `docker-compose.yml` file that launches both services and mounts the Prometheus configuration file located at:

```
prometheus/prometheus.yml
```

### Accessing Grafana

Once Docker is running, open:

```
http://localhost:3000
```

Add Prometheus as a data source with the URL:

```
http://prometheus:9090
```

### Importing the Dashboard

Use the Grafana interface to upload:

```
grafana/dashboard.json
```

The dashboard provides real-time visualization of metrics produced by the MarketNode and TradingAgents.

## Project Structure

```
src/
├── main/java/
│   ├── market/        MarketNode implementation
│   ├── agent/         TradingAgent implementation
│   ├── metrics/       Prometheus instrumentation
│   └── common/        Shared RMI interfaces and utilities
prometheus/
│   └── prometheus.yml Prometheus scraping configuration
grafana/
    └── dashboard.json Grafana dashboard definition
```

## Summary

This project demonstrates distributed-system principles through a realistic simulation of autonomous trading agents interacting with a central market. Through logical clocks, heartbeats, failure detection, message passing, and a full observability pipeline, the system provides a clear visualization of distributed behaviors in practice.


