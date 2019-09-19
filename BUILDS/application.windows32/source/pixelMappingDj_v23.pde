/**
 *  Map video to artnet pixels - made for driving RGB pixel strips on ARM
 *  with simple GUI and library of effects
 *  OS independant code...sort of switching between gl.gohai and video lib
 *  On Raspberry Pi: increase your GPU memory, to avoid
 *  OpenGL error 1285 at top endDraw(): out of memory
 */

String[] arrFiles; //list of files in array - cant handle list :-(

int stmivacka = 0;
int rozstmivacka = 255;
//to list files-------------------
import java.io.File; //to browse folder
File dir; 
File [] files;

List filesPath;
//StringList filesPath;
String OS = "";
String winOS = "windows";
String raspiOS = "linux";
PGraphics pg;

void setup() {
  //size(800, 600);
  size(800, 600, P2D); //4:3 eqivalent to hd ready 
  pg = createGraphics(800, 600, P2D); //independat from size() <-800*600 is recommended for performance, 600 will fit 5meter 120 led strip 
  //pg = createGraphics(800, 600); 

  OS = platformNames[platform];
  println(OS);

  setupFiles();
  initArtnet();
  setupGUI();
  frameRate(60);
  //colorMode(RGB, 255, 255, 255);
}

void draw() {
  background(0);

  if ((playing != -1) && (play) ) {
    OSvideo(playing);//play video //already in OSvideo() fce - apply brightness correction over a video :-)

    if (debug) { //show the video playing
      image(pg, 0, 0, width, height);
    }
  }

  if (ledList.size()>0) {
    lastLEDChannel = 0;
    float px = mouseX;
    float py = mouseY;

    int index = 0;
    for (int f[] : ledList) {
      //0=dmx_addresse 1=posX 2=posY 3=numberOfPixels 4 =pixelRegionWidth 5=pixelRegionHeight 6=whatChannels(RGB etc switch) 10=pixelMatrixWidth
      lastLEDChannel= lastLEDChannel + ledStripArtnet(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[10]);

      if ((mousePressed == true)) {//byla zmacknuta mys  && (!dragged[f])
        // pointRect(float px, float py, float rx, float ry, float rw, float rh) {
        if ((pointRect(px, py, f[1], f[2], f[4]*f[10], f[3]*f[5])) && (!dragging)) { //mouse over led strip visualisation....
          if (mouseButton == LEFT) {
            println("mouse clicked and over led strip");
            f[7]=1;
            f[8]=int(px-f[1]);
            f[9]=int(py-f[2]);
            dragging = true;
          } else if (mouseButton == RIGHT) {
            ledList.remove(index);
            println("Led strip "+index+" was deleted");
            return;
          }
        }
      } else {
        int pasek[] = ledList.get(index);
        pasek[7] = 0;
        dragging = false;
      }

      if (f[7] == 1) { //pokud je led pásek tažený myší uprav jeho pozici 
        f[1] = int(px-f[8]);
        f[2] = int(py-f[9]);
      }

      index++;
    }

    sendArtnet(lastLEDChannel);
  }


  if (hasFinished()) {
    countedFPS = countFps;
    startTime = millis();
    countFps = 0; //reset Video FPS
  }

  if (frameCount % 10 == 0) {
    //FPSdisplay = "Program FPS: "+str(round(frameRate))+" Video FPS: "+countedFPS;
    cp5debug.getController("FPSdisplay").setValueLabel("Program FPS: "+str(round(frameRate))+" Video FPS: "+countedFPS);
  }
}

void mousePressed() {

  switch(key) {
  case 'q':
    stop();
    break;
  }
}

void stop() {
  artnet.stop();
  stopVideo();
  exit();
}
