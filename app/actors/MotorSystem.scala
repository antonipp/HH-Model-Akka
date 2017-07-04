package actors

import akka.actor._
import akka.cluster._
import akka.cluster.ClusterEvent._
import scala.collection.immutable.Vector
import scala.collection.mutable.ArrayBuffer
import scala.math.ceil
import actors.MotorUnit._
import actors.Master._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import java.util.concurrent.ThreadLocalRandom

object MotorSystem { //messages that MotorSystem can receive
  //from Master
  case object ProcessTick
  //from MotorUnit
  case class Result(value : ArrayBuffer[Double])
}

class MotorSystem(nbMotorUnits : Int, percentGaitCycle: Array[Double]) extends Actor {
  import MotorSystem._
  
  val cluster = Cluster(context.system)
  var masterPath = "" //path to Master
  
  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)
  
  val motorUnits = Vector.fill(nbMotorUnits)(context.actorOf(Props[MotorUnit])) //create the MotorUnits
  var currentTick = 0
  var nbActiveMotorUnits = 0 // number of active MotorUnits this tick
  var received = 0 // number of received results from MotorUnits
  
  val tspan = 100 // time span value for MotorUnit calculations
  val dt = MotorUnit.dt 
  val resultLength = ceil(tspan / dt).toInt + 1 // length of the result array
  var resultSum = ArrayBuffer.fill(resultLength)(0.0) // resulting sum
 
  println(self.path.name + " : Created a MotorSystem with " + nbMotorUnits + " units")
  
  def receive = {
    case ProcessTick => {
      nbActiveMotorUnits = ceil(nbMotorUnits * percentGaitCycle(currentTick)).toInt //calculating the number of active MotorUnits
      println(self.path.name + ": Processing tick " + currentTick + " with " + nbActiveMotorUnits + " active motor units")
      motorUnits.take(nbActiveMotorUnits).foreach(_ ! generateParameters(0.8, 1.2)) //sending messages to MotorUnits
    }
    
    case Result(value) => {
      received += 1
      resultSum = (resultSum, value).zipped.map(_ + _) //element-wise sum
      
      if (received == nbActiveMotorUnits) { // if all results are received, tick is over
        received = 0
        context.actorSelection(masterPath) ! ResultSum(resultSum, percentGaitCycle(currentTick)) //send result to Master
        currentTick += 1
        resultSum = ArrayBuffer.fill(resultLength)(0.0)
      }
    }
    
    case state: CurrentClusterState => state.members.filter(_.status == MemberStatus.Up) foreach register
    
    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit = {
    if (member.hasRole("master")) {
      masterPath = RootActorPath(member.address).toString + "user/master*" //save the path to master
      context.system.actorSelection(masterPath) ! SystemRegistration //register system
    }
  }
  
  /**
   * Generate random parameters for the calculation. The values will be { x * variationPercentageLow, x * variationPercentageHigh}
   */
  def generateParameters(variationPercentageLow: Double, variationPercentageHigh: Double) : Calculate =
    Calculate(10 * ThreadLocalRandom.current.nextDouble(variationPercentageLow, variationPercentageHigh), 
        tspan, 
        -65 * ThreadLocalRandom.current.nextDouble(variationPercentageLow, variationPercentageHigh),
        0.5 * ThreadLocalRandom.current.nextDouble(variationPercentageLow, variationPercentageHigh),
        0.06 * ThreadLocalRandom.current.nextDouble(variationPercentageLow, variationPercentageHigh),
        0.5 * ThreadLocalRandom.current.nextDouble(variationPercentageLow, variationPercentageHigh))
}