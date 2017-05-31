package mazeRunner

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLImageElement


// Class for cells and their arithmetic;
//   gridX and gridY are integer grid positions, x and y are actual canvas coordinates:
case class Cell(var gridX: Int, var gridY: Int, cellSize: Int){
  def x () = { gridX * cellSize } 
  def y () = { gridY * cellSize } 
}

// Class to create image tiles:
class Image(src: String) {
  private var ready: Boolean = false

  val element = dom.document.createElement("img").asInstanceOf[HTMLImageElement]
  element.onload = (e: dom.Event) => ready = true
  element.src = src

  def isReady: Boolean = ready
}
