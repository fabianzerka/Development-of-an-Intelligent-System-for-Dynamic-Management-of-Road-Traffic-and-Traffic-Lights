# Sumo

This project contains a small Java/Swing demo that visualizes traffic flow
using data exported from the [SUMO](https://www.eclipse.org/sumo/) traffic
simulator. The application reads a network and vehicle routes from SUMO output
files and animates vehicles moving through intersections controlled by simple
two-phase traffic lights.

## Modules

- `com.example.sumo` – core data structures and utilities for parsing SUMO
  files and running the simulation.
- `com.example.sumo.gui` – contains the `TrafficSimulation` entry point which
  launches the Swing visualizer.

To run the demo:

```bash
mvn -q -f com.example.sumo/pom.xml exec:java -Dexec.mainClass="com.example.sumo.gui.TrafficSimulation"
```
