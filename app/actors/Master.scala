package actors

import akka.actor._
import MotorSystem._
import scala.util.Random
import play.api.libs.json._

import scala.collection.mutable.ArrayBuffer
import java.io._

object Master { //messages that Master can receive
  //from MotorSystem
  case object SystemRegistration
  case class ResultSum(sum : ArrayBuffer[Double], activeMotorUnits : Double)
}

class Master(out : ActorRef) extends Actor {
  import Master._
  
  var systems = IndexedSeq.empty[ActorRef] //array of MotorSystems
  val numberOfSystems = 2
  var resultsReceived = 0 //how many results received this tick
  var totalTicks = 101 //total number of ticks for the sim
  var currentTick = 0
  
  def receive = {
        
    case ResultSum(sum, activeMotorUnits) => {
      println("Master: Received result from " + sender.path.name)
      //send result to browser via WebSocket
      out ! Json.obj("type" -> "result", "value" -> Seq(activeMotorUnits.doubleValue()), "sender" -> sender.path.name) 
      resultsReceived += 1
      
      if (resultsReceived == numberOfSystems) { //received all results
        println("Master: Received all results. Tick " + currentTick + " done.")
        currentTick += 1
        
        if (currentTick == totalTicks) { //stop the sim
          systems.foreach(_ ! PoisonPill)
          context.system.terminate()
        }
        else { //continue the sim
          resultsReceived = 0
          advanceSimulation()
        }
      }
    }
    
    case SystemRegistration if !systems.contains(sender) => {
      println("Master: " + sender.path + " registered!")
      context watch sender
      systems = systems :+ sender //add a new system
      if (systems.length == numberOfSystems) advanceSimulation() //if all systems are registered, start the simulation
    }
    
    case Terminated(a) => systems = systems.filterNot(_ == a)

  }
  
  def advanceSimulation() : Unit = {
      println("Master: Started tick " + currentTick)
      //send the ProcessTick message to all systems to start the tick calculations
      systems.foreach(_ ! ProcessTick) 
  }
}