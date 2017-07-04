package actors

import akka.actor._
import com.typesafe.config.ConfigFactory
import java.io.File
import MotorSystem._

object MotorSystemNode {
  def main(args : Array[String]) {
    if (args.length < 2) throw new IllegalArgumentException("You must specify the port and the system name. Available system names: soleus, tibialis-anterior.")
    
    val port = args(0)
    val sysName = args(1)
    
    val percentGaitCycleFileName = sysName match { //find the name of the CSV file containing percent gait cycle values
      case "soleus" => "SoleusInterpolatedDataset.csv"
      case "tibialis-anterior" => "TibialisInterpolatedDataset.csv"
      case _ => throw new IllegalArgumentException("Invalid system name")
    }
    
    //read lines from the corresponding csv
    val sourceLines = scala.io.Source.fromFile("." + File.separator + "conf" + File.separator + "resources" + File.separator + percentGaitCycleFileName).getLines 
    val percentGaitCycle = sourceLines.flatMap(_.split(",")).toArray.map(_.toDouble) //array containing percent gait cycle values
    
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [motorSystem]")).
      withFallback(ConfigFactory.parseString("akka.cluster.failure-detector.acceptable-heartbeat-pause = 4s")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    
    val nbOfMotorUnits = sysName match {
      case "soleus" => 458
      case "tibialis-anterior" => 150
      case _ => throw new IllegalArgumentException("Invalid system name")
    }
    
    val motorSystem = system.actorOf(Props(classOf[MotorSystem], nbOfMotorUnits, percentGaitCycle), name = sysName) //create the MotorSystem
    
    system.actorOf(Props(classOf[Terminator], motorSystem), "terminator")
  
    class Terminator(ref: ActorRef) extends Actor with ActorLogging {
      context watch ref
      def receive = {
        case Terminated(_) =>
          log.info("{} has terminated, shutting down system", ref.path)
          context.system.terminate()
      }
    }
  }
}