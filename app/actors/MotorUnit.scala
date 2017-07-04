package actors

import akka.actor._
import scala.collection.mutable.ArrayBuffer
import scala.math._
import actors.MotorSystem._

object MotorUnit { 
  //messages MotorUnit can receive
  //from MotorSystem
  case class Calculate(I : Double, tspan : Double, vi : Double, mi : Double, hi : Double, ni : Double)
  
  //constants for all MotorUnits
  val gNa = 120
  val eNa = 115
  val gK = 36
  val eK = -12
  val gL = 0.3
  val eL = 10.6
  val dt = 0.001
}

class MotorUnit extends Actor {
  import MotorUnit._
  
  var V = ArrayBuffer.empty[Double] //array containing the results of the calculation
  
  def receive = {
    case parameters: Calculate => sender ! Result(calculatePotential(parameters.I, parameters.tspan, parameters.vi, parameters.mi, parameters.ni, parameters.hi))
  }
  
  /**
   * Membrane potential calculation
   */
  def calculatePotential(I : Double, tspan : Double, vi : Double, mi : Double, hi : Double, ni : Double) : ArrayBuffer[Double] = {
    
    val NSTEPS = ceil(tspan / dt).toInt // number of steps for the forward Euler method
    
    //assigning initial values
    V.clear
    V += vi
    
    var m = mi
    var h = hi
    var n = ni
    
    for (i <- 0 until NSTEPS) {
      V += V(i) + dt * (gNa * pow(m, 3) * h * (eNa - (V(i) + 65)) + gK * pow(n, 4) * (eK - (V(i) + 65)) + gL * (eL - (V(i) + 65)) + I)
      m = m + dt * (alphaM(V(i)) * (1 - m) - betaM(V(i)) * m)
      h = h + dt * (alphaH(V(i)) * (1 - h) - betaH(V(i)) * h)
      n = n + dt * (alphaN(V(i)) * (1 - n) - betaN(V(i)) * n)
    }
        
    V //return the voltage time series
  }
  
  def alphaM(V : Double) : Double = (2.5 - 0.1 * (V + 65)) / (exp(2.5 - 0.1 * (V + 65)) - 1)
  
  def betaM(V : Double) : Double = 4 * exp(-(V + 65) / 18)

  def alphaH(V : Double) : Double = 0.07 * exp(-(V + 65) / 20)
  
  def betaH(V : Double) : Double = 1 / (exp(3.0 - 0.1 * (V + 65)) + 1)
  
  def alphaN(V : Double) : Double = (0.1 - 0.01 * (V + 65)) / (exp(1 - 0.1 * (V + 65)) - 1)
  
  def betaN(V : Double) : Double = (0.125 * exp(-(V + 65) / 80))
      
}