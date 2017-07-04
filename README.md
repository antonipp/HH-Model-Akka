# Introduction
This is a distributed Huxley-Hodgkin model implemented using Scala, Akka and Play. This project has been carried out under supervision of Dr. Tien Tuan Dao during the 2017 Spring Semester at Université de Technologie de Compiègne. Please see *report/report.pdf* for more information.

# Installation instructions
1. Install JDK (7 or higher) and [SBT](http://www.scala-sbt.org/0.13/docs/Setup.html).
2. Clone or download this repo on all nodes that you want to run the simulation on.
3. If you want to run the simulation on only one machine, skip this step. If you want to run the simulation on several nodes, at the bottom of *conf/application.conf* on each node, set the *akka.remote.netty.tcp.hostname* value to nodes' respective local IP addresses. Change the *akka.cluster.seed-nodes* as well: these are the IP addresses of nodes that the MotorSystems will run on. These values should be the same on all nodes.

# Run the simulation
In order to run the simulation if you are on only one machine:
1. Open three terminal windows and `cd` into the main directory.
2. Run `sbt` in all three windows (may take a long time on first launch).

*First window*:
3. Run the Play server and the Master actor in one of the windows by typing `run`.
4. Connect to *http://localhost:9000/* in your browser.

*Second window*:
5. Type `runMain actors.MotorSystemNode 2551 soleus` in another window to run the first MotorSystem. *2551* is the port specified in *akka.cluster.seed-nodes*.

*Third window*:
6. Type `runMain actors.MotorSystemNode 2552 tibialis-anterior` in the last window to run the second MotorSystem. *2552* is the port specified in *akka.cluster.seed-nodes*. The simulation will now start.

In order to run the simulation on several machines, the process is very similar, you just have to open the terminal windows on different machines and make sure that IP addresses and ports are configured properly (see step 3 of installation instructions).
