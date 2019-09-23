//for raspi video--------------------
import gohai.glvideo.*;
GLMovie[] videos;
//for windows etc video---------------
import processing.video.*;
Movie[] movies;

boolean paused = false;
int countFps = 0; //counting how many times is video frame displayed during one second
int countedFPS = 0; //hold resolut of last countFPS

final int WAIT_TIME = (int) (1 * 1000); // 1 second
int startTime;

void OSvideo(int vIndex) {

  if (playStarted) {
    stopVideo();
    playStarted = false;
    if (OS == winOS) {
      movies[vIndex]  = new Movie(this, arrFiles[vIndex]); //load into memory
      movies[vIndex].loop();
      movies[vIndex].frameRate(30); //does it actually do anything???
      println("starting video....");
    } else {
      videos[vIndex].loop();
      println("starting video....");
    }
  }

  if (OS == winOS) {   //WIN_OS ----------------------

    if (movies[vIndex].available()) {
      movies[vIndex].read();
      countFps++;
    }


    if ((!pause) && (!paused)) { // PAUSE--------------
      //movies[vIndex].speed(0.0);
      movies[vIndex].pause();
      paused = true;
      println("playback paused");
    }

    if ((pause) && (paused)) { // RESUME--------------
      movies[vIndex].play();
      paused = false;
      println("playback resumed");
    }

    renderVideoFrame(true, vIndex); //apply transformations and color tints and draw frame into pg buffer
  
} else { //OTHER_OS - linux --------------------------------

    if (videos[vIndex].available()) {
      videos[vIndex].read();
      countFps++;
    }

    if ((!pause) && (!paused)) { // PAUSE--------------
      videos[vIndex].pause();
      paused = true;
      println("playback paused");
    }

    if ((pause) && (paused)) { // RESUME--------------
      videos[vIndex].loop();
      paused = false;
      println("playback resumed");
    }

    renderVideoFrame(false, vIndex); //apply transformations and color tints and draw frame into pg buffer

    // image(videos[vIndex], 0, 0, width, height);
  }
}

void stopVideo() {
  println("stop command");
  if (OS == winOS) {   //WIN_OS -------------------------------------
    for (int r=0; r<movies.length; r++) { //stop all movies
      movies[r].stop();
    }
  } else { //OTHER_OS ---------------------------------------------
    for (int r=0; r<videos.length; r++) { //stop all movies
      //videos[r].close(); //it will completely clear video from memory...
      videos[r].pause();//pause playback
      videos[r].jump(0);//rewind
    }
  }
}

void setVSpeed(int vIndex, float newSpeed) {
  if (OS == winOS) {   //WIN_OS -------------------------------------
    movies[vIndex].speed(newSpeed);
  } else { //OTHER_OS ---------------------------------------------
    videos[vIndex].speed(newSpeed);
  }
}

boolean hasFinished() {
  return millis() - startTime > WAIT_TIME;
}
/* //is it better? than have in the draw loop? dunno
 void movieEvent(Movie movies) {
 movies.read();
 }
 */
void renderVideoFrame(boolean WinOSS, int vIndex) {
  pg.beginDraw();
  pg.tint(255-cold, 255-cold, 255-yellow);
  pg.pushMatrix();
  if (flipH==-1) { 
    pg.translate(pg.width, 0);
  }
  if (flipV==-1) { 
    pg.translate(0, pg.height);
  }
  pg.scale(flipH, flipV);
    
  if (WinOSS) {
    pg.image(movies[vIndex], 0, 0, pg.width, pg.height);
  } else {
    pg.image(videos[vIndex], 0, 0, pg.width, pg.height);
  }
  pg.popMatrix();
  pg.fill(0, 0, 0, 255-dimmer);
  pg.rect(0, 0, pg.width, pg.height);
  pg.endDraw();
}
