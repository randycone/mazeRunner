package mazeRunner

import org.scalajs.dom
import org.scalajs.dom.html
import scala.util.Random
import scala.scalajs.js
import js.annotation.JSExport
import scala.scalajs.js.timers._

@JSExport
object wallRunnerApp {

  // Initialize run timer:
  var startTime = js.Date.now()

  // Initialize number of steps taken by player:
  var numSteps = 0

  // Initialize game to not run immediately (without html button click):
  var runFlag = false

  // Set up canvas object:
  val canvas = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]

  // Basic geometry for canvas (as a cartesian grid of square cells):
  val numRows = 32/2
  val numCols = 42/2
  val cellSize = 70
  val cHeight = numRows * cellSize
  val cWidth = numCols * cellSize

  // Initialize portal and player initial GRID coordinates
  //  (note: there is an exterior wall along GRID position x = 0, y = 0
  //         and x = numCols, y = numRows

//  val portalX = ( numCols - 5 ).toInt
//  val portalY = ( numRows - 5 ).toInt

  // Bottom chamber, lower left:  (breaks Baldinazzo, Alfaro, Tress, Tomomeko P4,IW2) (No speedup on Kromer P4,IW3)
//  val portalX = ( 4 ).toInt
//  val portalY = ( numRows - 5 ).toInt

  // Right hallway, near bottom:  (McCall, Leach breaks P4,IW3) (No speed up on Cratty P4,IW3)
  val portalX = ( numCols - 2 ).toInt
  val portalY = ( numRows - 5 ).toInt

  // Upper Chamber, lower left:
//  val portalX = ( 4 ).toInt
//  val portalY = ( (numRows/2).toInt - 2 ).toInt

  // Upper hallway, left of door:
//  val portalX = ( numCols/2.0 - 1 ).toInt
//  val portalY = ( 1 ).toInt


//  val startX = ( numCols / 2 ).toInt
//  val startY = ( 2*numRows / 3 ).toInt

  val startX = 3
  val startY = 3

  // Instantiate Decision Factory for making player move decisions:
  val DF = DecisionFactory

  // Get context for canvas object:
  val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  // Instantiate player object:
  var player = Cell(startX, startY, cellSize)

  // Instantiate portal:
  var portal = Cell(portalX, portalY, cellSize)

  // Variable for detecting finding portal:
  var foundPortal: Option[(String, Int)] = None

  // Sequence data to contain enemies:
  var walls = Seq.empty[Cell]

  // Create wall system:
  createExteriorWalls()
  createInteriorWalls()
  //createInteriorWalls2()
  //createInteriorWalls3()

  // Set up canvas:
  canvas.height = cHeight
  canvas.width = cWidth

  // Create image tiles:
  //val playerImage = new Image("../../../images/blackmage_m.png")
  val playerImage = new Image("../../../images/p1_stand.png")
  val portalImage = new Image("../../../images/door_closed.png")

  // Create and draw initial scene:
  def initScene() = {

    // Background drawing:
    ctx.fillStyle = "black"
    ctx.fillRect(0, 0, canvas.width-1, canvas.height-1)

    // Draw walls:
    ctx.fillStyle = "gray"
    for ( w <- walls ) {
      ctx.fillRect( w.x, w.y, cellSize, cellSize ) 
    }

    // Draw player:
    ctx.drawImage(playerImage.element, player.x, player.y - 40)

    // Draw portal:
    ctx.drawImage(portalImage.element, portal.x, portal.y)
  }

  // Game logic function:
  def run() = {

    // Get move decision, decide if acceptable and take action, keep decisions and actions separate:
    var newMove = DF.Decision
    numSteps = numSteps + 1

    //println("New Move: ", newMove)

    if ( acceptable(newMove) ) {

      // Acceptable move detected, so movement allowed:
      movePlayer(newMove)

      // Detect collision of player with portal:
      if( portal.gridX == player.gridX && portal.gridY == player.gridY ){

        // Mark player as having found Portal and create message for display:
        //   NOTE: we set the timer for restart to be 100 ticks of setInterval clock
        //foundPortal = Some((s"You took $deltaT seconds", 100))
        foundPortal = Some((s"You took $numSteps steps", 100))

        // Reset starting position of player:
        player.gridX = startX
        player.gridY = startY

        // signal decision factory about finding Portal result:
        DF.receiveSignal(2) 

      } else {
        // signal decision factory about positive movement result:
        DF.receiveSignal(1) 
      }

    } else {

      // signal decision factory about negative movement result:
      DF.receiveSignal(-1) 
    }

  }

  // Function returning the difference in start of game round time and current time:
  def deltaT = ((js.Date.now() - startTime) / 1000).toInt

  // Draw the game board function:
  def draw() = {

    // Background drawing:
    ctx.fillStyle = "black"
    ctx.fillRect(0, 0, canvas.width-1, canvas.height-1)

    // Draw walls:
    ctx.fillStyle = "gray"
    for ( w <- walls ) {
      ctx.fillRect( w.x, w.y, cellSize, cellSize ) 
    }

    //println("gridX: ",player.gridX)
    //println("gridY: ",player.gridY)

    foundPortal match{

      // No foundPortal flag detected, draw gameboard as normal:
      case None =>

        // Draw player:
        //ctx.fillStyle = "green"
        //ctx.fillRect(player.x, player.y, cellSize, cellSize)
        ctx.drawImage(playerImage.element, player.x, player.y - 40)

        // Draw coordinate text (where player thinks they are):
        //ctx.font = "10px sans-serif"
        //ctx.fillStyle = "white"
        //ctx.fillText((DF.x,DF.y).toString, player.x, player.y)

        // Draw portal:
        //ctx.fillStyle = "blue"
        //ctx.fillRect(portal.x, portal.y, cellSize, cellSize)
        ctx.drawImage(portalImage.element, portal.x, portal.y)

	// Print time elapsed:
        ctx.font = "30px sans-serif"
        ctx.fillStyle = "white"
        //ctx.fillText(s"$deltaT Seconds", canvas.width / 2 - 140, canvas.height / 4)
        ctx.fillText(s"$numSteps steps", canvas.width / 2 - 140, canvas.height / 4)

      // Portal found, restart game:
      case Some((msg, time)) =>

        // Display end of game message;
        ctx.font = "30px sans-serif"
        ctx.fillStyle = "white"
        ctx.fillText(msg, canvas.width / 2 - 230, canvas.height / 2)

        // Stop game, reset foundPortal flag:
        runFlag = false
        foundPortal = None
        numSteps = 0
    }
  }

  // Determine if movement decision is acceptable:
  // (1 = up, 2 = down, 3 = left, 4 = right)
  def acceptable( d: Int ) : Boolean = {

    // For now, we allow waiting as an acceptable move:
    d match {
      case 0 => { true }
      case 1 => { !wallCollisionExists(player.gridX, player.gridY - 1) }
      case 2 => { !wallCollisionExists(player.gridX, player.gridY + 1) }
      case 3 => { !wallCollisionExists(player.gridX - 1, player.gridY) }
      case 4 => { !wallCollisionExists(player.gridX + 1, player.gridY) }
      case _ => { false }
    } 
  } 

  // Move player:
  // (1 = up, 2 = down, 3 = left, 4 = right)
  def movePlayer( d: Int ) = {

    // For now, we don't check the wait state:
    d match {
      case 1 => { player.gridY = player.gridY - 1 }
      case 2 => { player.gridY = player.gridY + 1 }
      case 3 => { player.gridX = player.gridX - 1 }
      case 4 => { player.gridX = player.gridX + 1 }
      case _ => { ;; }
    } 
  } 

  // Detect if there is a wall collision:
  def wallCollisionExists( xPos: Int, yPos: Int ) : Boolean = {
    if ( walls.exists( w => ( w.gridX == xPos && w.gridY == yPos ) ) )
      { true }
    else
      { false }
  }

  // Set up boundary walls around the edge of room:
  def createExteriorWalls() = {
    // Top and bottom walls:
    for ( loopvar <- 0 to numCols-1 ) {
      walls :+= new Cell( loopvar, 0, cellSize )
      walls :+= new Cell( loopvar, numRows-1, cellSize )
    }

    // Left and Right walls:
    for ( loopvar <- 1 to numRows-2 ) {
      walls :+= new Cell( 0, loopvar, cellSize )
      walls :+= new Cell( numCols-1, loopvar, cellSize )
    }
  }

  // Set up interior walls: 
  def createInteriorWalls() = {
    // Central wall, far door:
//    for ( loopvar <- 1 to numCols - 3 ) {
//      walls :+= new Cell( loopvar, (numRows/2).toInt, cellSize )
//    }

    for ( loopvar <- 1 to (numCols/2).toInt+2  ) {
      walls :+= new Cell( loopvar, (numRows/2).toInt, cellSize )
    }
    for ( loopvar <- (numCols/2).toInt+4 to  numCols-1 ) {
      walls :+= new Cell( loopvar, (numRows/2).toInt, cellSize )
    }
    for ( loopvar <- 1 to (numRows/4).toInt  ) {
      walls :+= new Cell( (numCols/2).toInt+1, loopvar, cellSize )
    }
    for ( loopvar <- (numRows/4).toInt+2 to (numRows/2).toInt+2 ) {
      walls :+= new Cell( (numCols/2).toInt+1, loopvar, cellSize )
    }
    for ( loopvar <- (numRows/2).toInt+4 to  numRows-1 ) {
      walls :+= new Cell( (numCols/2).toInt+1, loopvar, cellSize )
    }
  }

  // Set up interior walls: 
  def createInteriorWalls3() = {
    for ( loopvar <- 3 to numCols - 4 ) {
      walls :+= new Cell( loopvar, (numRows/2).toInt, cellSize )
    }
    for ( loopvar <- 3 to numRows.toInt - 4  ) {
     if ( loopvar != (numRows/2).toInt - 1 ) {
        walls :+= new Cell( 2, loopvar, cellSize )
        if ( loopvar != (numRows/2).toInt + 1 ) {
          walls :+= new Cell( numCols.toInt - 3, loopvar, cellSize )
        }
      }
    }
    for ( loopvar <- 2 to numCols - 3 ) {
      if ( loopvar !=  (numCols/2).toInt ) {
        walls :+= new Cell( loopvar, 2, cellSize )
        walls :+= new Cell( loopvar, (numRows - 3).toInt, cellSize )
      }
    }
  }

  // Set up interior walls:
  def createInteriorWalls2() = {
    for ( loopvar <- 3 to numCols - 4 ) {
      walls :+= new Cell( loopvar, (numRows/2).toInt, cellSize )
    }
    for ( loopvar <- 3 to numRows.toInt - 4  ) {
        walls :+= new Cell( 2, loopvar, cellSize )
        walls :+= new Cell( numCols.toInt - 3, loopvar, cellSize )
    }
    for ( loopvar <- 2 to numCols - 3 ) {
      if ( loopvar !=  (numCols/2).toInt ) {
        walls :+= new Cell( loopvar, 2, cellSize )
        walls :+= new Cell( loopvar, (numRows - 3).toInt, cellSize )
      }
    }
  }



  // Get input from HTML button and start game:
  @JSExport
  def boomShasta(): Unit = {
    runFlag = true

    // Reset starting position of player:
    player.gridX = startX
    player.gridY = startY

    // Restart timer:
    startTime = js.Date.now()
  }

  @JSExport
  def main(): Unit = {
    dom.console.log("main")

    // Draw initial layout:
    dom.window.setTimeout( () => initScene(), 0 )

    // Set up timing interval and run game:
    dom.window.setInterval( () => {

      // Run game if flag is on (by html button click):
      if (runFlag) {

        // Drop player cell into place using mouse position (on click),
        //   but disconnect mouse if game is not running:
        dom.document.onmouseup = { (e: dom.MouseEvent) =>
          player = Cell((e.clientX/cellSize).toInt, (e.clientY/cellSize).toInt, cellSize)
          (): js.Any
        }

        // Prepare current game frame:
        run()

        // Draw current game frame:
        draw()
      }
    }, 25)
  }

}
