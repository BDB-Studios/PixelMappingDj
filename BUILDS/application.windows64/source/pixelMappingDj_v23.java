import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.File; 
import controlP5.*; 
import java.util.*; 
import ch.bildspur.artnet.*; 
import ch.bildspur.artnet.packets.*; 
import ch.bildspur.artnet.events.*; 
import java.util.List; 
import java.net.Inet4Address; 
import gohai.glvideo.*; 
import processing.video.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class pixelMappingDj_v23 extends PApplet {

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
 //to browse folder
File dir; 
File [] files;

List filesPath;
//StringList filesPath;
String OS = "";
String winOS = "windows";
String raspiOS = "linux";
PGraphics pg;

public void setup() {
  //size(800, 600);
   //4:3 eqivalent to hd ready 
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

public void draw() {
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
            f[8]=PApplet.parseInt(px-f[1]);
            f[9]=PApplet.parseInt(py-f[2]);
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
        f[1] = PApplet.parseInt(px-f[8]);
        f[2] = PApplet.parseInt(py-f[9]);
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

public void mousePressed() {

  switch(key) {
  case 'q':
    stop();
    break;
  }
}

public void stop() {
  artnet.stop();
  stopVideo();
  exit();
}
//--------------------------------------------------------------------------
//VIDEO CONTROLS FCES-------------------------------------------------------
public void videos(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  println("gui index: "+n);
  playStarted = true;
  playing = n;
  //  movies[playing].loop();
}

public void flipH(boolean theFlag) {
  if (theFlag) {
    flipH=-1;
  } else {
    flipH=1;
  }
}

public void flipV(boolean theFlag) {
  if (theFlag) {
    flipV=-1;
  } else {
    flipV=1;
  }
}

public void play(boolean theFlag) {
  play = theFlag;
  if (!play) {
    stopVideo();
  } else {
    playStarted = true;
  }
}

public void pause(boolean theFlag) {
  if (playing != -1) {
    pause = theFlag;
  }
}

public void quit(boolean theFlag) { 
  //debug = theFlag;
  if (theFlag == false) {
    stop();
  }
}

public void save() { 
  println("Saving settings...");
  cp5.saveProperties(("settings.properties"));
  cp5debug.saveProperties(("settingsGUIon.properties"));

  int index=0;
  String dataFolder;

  if (ledList.size()>0) { //there are some led strip fixtures created...

    if (OS == winOS) {   
      dataFolder = sketchPath("data\\"); //winOS
    } else {
      dataFolder = sketchPath("data/"); //linux
    }

    for (int f[] : ledList) {

      list = new String[f.length] ;

      for (int z=0; z<f.length; z++) {
        list[z]=str(f[z]);//convert int to string
      }

      String fileName = dataFolder+"ledStrip_"+index+".txt";
      println("Led Strip "+index+" saved to "+fileName);
      saveStrings(fileName, list); //for every strip save as individual file
      index++;
    }
  }
}

public void debug(boolean theFlag) { 
  debug = theFlag;
  save(); //save settings...
  if (theFlag) {
    cp5.setAutoDraw(true);
  } else {
    cp5.setAutoDraw(false);
  }
}

public void videoSpeed(float theSpeed) {
  if (playing != -1) {
    setVSpeed(playing, theSpeed);
  }
}

//--------------------------------------------------------------------------
//LED STRIP CONTROL FCES-------------------------------------------------------

// function that will be called when controller 'numbers' changes
public void numbers(float f) {
  println("received "+f+" from Numberbox numbers ");
}


public void makeEditable( Numberbox n ) {
  // allows the user to click a numberbox and type in a number which is confirmed with RETURN

  final NumberboxInput nin = new NumberboxInput( n ); // custom input handler for the numberbox

  // control the active-status of the input handler when releasing the mouse button inside 
  // the numberbox. deactivate input handler when mouse leaves.
  n.onClick(new CallbackListener() {
    public void controlEvent(CallbackEvent theEvent) {
      nin.setActive( true );
    }
  }
  ).onLeave(new CallbackListener() {
    public void controlEvent(CallbackEvent theEvent) {
      nin.setActive( false ); 
      nin.submit();
    }
  }
  );
}



// input handler for a Numberbox that allows the user to 
// key in numbers with the keyboard to change the value of the numberbox

public class NumberboxInput {

  String text = "";

  Numberbox n;

  boolean active;


  NumberboxInput(Numberbox theNumberbox) {
    n = theNumberbox;
    registerMethod("keyEvent", this );
  }

  public void keyEvent(KeyEvent k) {
    // only process key event if input is active 
    if (k.getAction()==KeyEvent.PRESS && active) {
      if (k.getKey()=='\n') { // confirm input with enter
        submit();
        return;
      } else if (k.getKeyCode()==BACKSPACE) { 
        text = text.isEmpty() ? "":text.substring(0, text.length()-1);
        //text = ""; // clear all text with backspace
      } else if (k.getKey()<255) {
        // check if the input is a valid (decimal) number
        final String regex = "\\d+([.]\\d{0,2})?";
        String s = text + k.getKey();
        if ( java.util.regex.Pattern.matches(regex, s ) ) {
          text += k.getKey();
        }
      }
      n.getValueLabel().setText(this.text);
    }
  }

  public void setActive(boolean b) {
    active = b;
    if (active) {
      n.getValueLabel().setText("");
      text = "";
    }
  }

  public void submit() {
    if (!text.isEmpty()) {
      n.setValue( PApplet.parseFloat( text ) );
      text = "";
    } else {
      n.getValueLabel().setText(""+n.getValue());
    }
  }
}

public void channels(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  channels = n;
}

public void matrixWidth(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  matrixWidth = n;
}

public void createLED() {
  println("Creating LED strip");
  //println("LED strip Index: "+ledstripsIndex);
  //for (int x = 0; x< ledstrips[0].length; x++) { //for all arguments of a function ledstripArtnet() save args to dedicated array
  int ledstripArgs[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //init array to hold led strip vars -> should be written as class with object really but I opted for list of int array for some reason...

  for (int x = 0; x< ledstripArgs.length; x++) { 
    switch(x) {
    case 0:  //number of pixels
      if (LEDchannelStart>=0) {
        ledstripArgs[x] = LEDchannelStart;
        println("First DMX channel of LED strip: "+ LEDchannelStart);
      } else {
        ledstripArgs[x] = 0;
        println("DMX addresse cant be negative, setting 0");
      }
      break;
    case 1:  //pos x
      ledstripArgs[x] = (width/2)-(LEDpixelWidth/2);
      println("position X: "+((width/2)-(LEDpixelWidth/2))); 
      break;
    case 2: //pos y
      ledstripArgs[x] = (height/2)-((LEDpixelHeight*LEDstripPixels)/2);
      println("position Y: "+((height/2)-((LEDpixelHeight*LEDstripPixels)/2))); 
      break;
    case 3:  //number of pixels
      if (LEDstripPixels>0) {
        ledstripArgs[x] = LEDstripPixels;
        println("number of pixels in LED strip: "+LEDstripPixels);
      } else {
        ledstripArgs[x] = 1;
        println("Minimum is 1 pixel!");
      }
      break;
    case 4: //pixel width
      if (LEDpixelWidth>0) {
        ledstripArgs[x] = LEDpixelWidth;
        println("pixel Width: "+LEDpixelWidth);
      } else {
        ledstripArgs[x] = 1;
        println("pixel width must be at least 1 pixel!");
      }
      break;
    case 5: //pixel height
      if (LEDpixelHeight>0) {
        ledstripArgs[x] = LEDpixelHeight;
        println("pixel Height: "+LEDpixelHeight);
      } else {
        println("Led region height must be at least 1 pixel!");
      }
      break;
    case 6: //channels RGB/RGBW/WWW etc. switch operator in ledStrip fce..
      ledstripArgs[x] = channels;
      println("Selected channels: "+channels);
      break;
    case 7: //is it currently dragged?
      ledstripArgs[x] = 0;
      break;
    case 8: //where it is dragged? center of mass X
      ledstripArgs[x] = 0;
      break;
    case 9: //where it is dragged? center of mass Y
      ledstripArgs[x] = 0;
      break;
    case 10: //if it is led display assign how many pixels width - 1 for typical led strip
      if (matrixWidth>0) {
        ledstripArgs[x] = matrixWidth;
      } else {
        ledstripArgs[x] = 1;
        println("led strip/display must have width at least one pixel!");
      }
      break;
    }
  }

  ledList.add(ledstripArgs);        
  //println("Arraylist contains:" + ledList.get(0));


  //for (int z = 0; z<ledstrips[ledstripsIndex].length; z++) {
  for (int z = 0; z<ledstripArgs.length; z++) {
    print( ledstripArgs[z]+" ");
  }
  //ledstripsIndex++;
  println("Current number of led strips: "+ledList.size());
}

//collision detection for the led strip....
//rx = x ry = y rw = width rh = height
public boolean pointRect(float px, float py, float rx, float ry, float rw, float rh) {

  // is the point inside the rectangle's bounds?
  if (px >= rx &&        // right of the left edge AND
    px <= rx + rw &&   // left of the right edge AND
    py >= ry &&        // below the top AND
    py <= ry + rh) {   // above the bottom
    return true;
  }
  return false;
}


ControlP5 cp5;
ControlP5 cp5debug;

boolean play = true; //is playback enabled?
boolean pause = true; //is playback enabled?
int playing = -1; //index of video to play
boolean playStarted = false;//first time we issue play command this is true
int dimmer = 255;
int cold = 0;
int yellow = 0;
boolean debug = true;
float videoSpeed=1.0f;
int flipH = 1;
int flipV = 1;

int LEDstripPixels = 0;
int LEDpixelWidth = 0;
int LEDpixelHeight = 0;
int channels = 0;
int LEDchannelStart = 0;
int matrixWidth = 0; //1 for led strip, more for led displays...

int IP1;
int IP2;
int IP3;
int IP4;

boolean dragging = false;

//to save and load all created led strips
String[] list = null;
String settings = null;
PrintWriter output;


public void setupGUI() {

  cp5 = new ControlP5(this);

  if (debug) {
    cp5.setAutoDraw(true);
  } else {
    cp5.setAutoDraw(false);
  }

  //on/off gui and video render -> helps perfromance
  cp5debug = new ControlP5(this);


  CallbackListener toFront = new CallbackListener() {
    public void controlEvent(CallbackEvent theEvent) {
      theEvent.getController().bringToFront();
      ((ScrollableList)theEvent.getController()).open();
    }
  };

  CallbackListener close = new CallbackListener() {
    public void controlEvent(CallbackEvent theEvent) {
      ((ScrollableList)theEvent.getController()).close();
    }
  };

  cp5debug.addToggle("debug")
    .setBroadcast(false)
    .setLabel("DEBUG") 
    .setPosition(width-30-50, height-60)
    .setSize(50, 20)
    .setValue(true)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    ;



  //------------------------------------------------------------------------------------------
  //BASIC VIDEO CONTROL---------------------------------------------------------------------
  // create a toggle and change the default look to a (on/off) switch look

  // FPSdisplay
  cp5debug.addTextlabel("FPSdisplay")
    //.setText("DISABLE GUI (helps performance)")
    .setPosition(30, 30)
    .setFont(createFont("Standard 07_58", 10))
    ;

  Group g2 = cp5.addGroup("GUI")  
    .setPosition(30, 60)
    .setSize(200, 20)
    ;

  //--------------------------------------------
  //IP for Artnet Unicast:
  Numberbox a = cp5.addNumberbox("IP1")
    .setPosition(0, 10)
    .setSize(30, 20)
    .setLabel("")
    .setGroup(g2)
    .setValue(2)
    ;

  makeEditable(a);

  Numberbox b = cp5.addNumberbox("IP2")
    .setPosition(40, 10)
    .setSize(30, 20)
    .setLabel("")
    .setGroup(g2)
    .setValue(0)
    ;

  makeEditable(b);

  Numberbox c = cp5.addNumberbox("IP3")
    .setPosition(80, 10)
    .setSize(30, 20)
    .setLabel("")
    .setGroup(g2)
    .setValue(0)
    ;

  makeEditable(c);

  Numberbox d = cp5.addNumberbox("IP4")
    .setPosition(120, 10)
    .setSize(30, 20)
    .setLabel("")
    .setGroup(g2)
    .setValue(1)
    ;

  makeEditable(d);

  cp5.addTextlabel("IPLabel")
    .setText("IP")
    .setPosition(160, 10)
    .setGroup(g2)
    .setFont(createFont("Standard 07_58", 10))
    ;

  //------------------------------------------------
  //create brightness master dimmer
  cp5.addSlider("dimmer")
    .setLabel("Brightness")
    .setPosition(0, 40)
    .setSize(200, 20)
    .setRange(0, 255)
    .setValue(255)
    .setGroup(g2)
    ;

  //create yellow tint
  cp5.addSlider("yellow")
    .setLabel("Warmer")
    .setPosition(0, 70)
    .setSize(200, 20)
    .setRange(0, 255)
    .setValue(0)
    .setGroup(g2)
    ;

  //create blue tint
  cp5.addSlider("cold")
    .setLabel("Colder")
    .setPosition(0, 100)
    .setSize(200, 20)
    .setRange(0, 255)
    .setValue(0)
    .setGroup(g2)
    ;

  cp5.addSlider("videoSpeed")
    .setLabel("Speed")
    .setPosition(0, 140)
    .setSize(200, 20)
    .setRange(0.5f, 2.0f)
    .setValue(1.0f)
    .setNumberOfTickMarks(16)
    .setGroup(g2)
    ;

  cp5.addScrollableList("videos")
    .setPosition(0, 170)
    .setSize(200, 100)
    .setBarHeight(20)
    .setItemHeight(20)
    .addItems(arrFiles)
    .onEnter(toFront)
    .setBackgroundColor(color(0))
    .onLeave(close)
    .setGroup(g2)
    ;

  cp5.addToggle("flipH")
    .setBroadcast(false)
    .setLabel("FLIP H")
    .setPosition(0, 280)
    .setSize(50, 20)
    .setGroup(g2)
    .setValue(false)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    ;

  cp5.addToggle("flipV")
    .setBroadcast(false)
    .setLabel("FLIP V")
    .setPosition(0, 320)
    .setSize(50, 20)
    .setGroup(g2)
    .setValue(false)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    ;

  // create a toggle and change the default look to a (on/off) switch look
  cp5.addToggle("play")
    .setBroadcast(false)
    .setLabel("play / stop")
    .setPosition(0, 360)
    .setSize(50, 20)
    .setValue(true)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    .setGroup(g2)
    ;

  // create a toggle and change the default look to a (on/off) switch look
  cp5.addToggle("pause")
    .setBroadcast(false)
    .setLabel("play / pause")
    .setPosition(0, 400)
    .setSize(50, 20)
    .setGroup(g2)
    .setValue(true)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    ;

  cp5.addToggle("quit")
    .setBroadcast(false)
    .setLabel("QUIT") 
    .setPosition(0, 440)
    .setSize(50, 20)
    .setValue(true)
    .setGroup(g2)
    .setMode(ControlP5.SWITCH)
    .setBroadcast(true)
    ;

  cp5.addBang("save")
    .setBroadcast(false)
    .setPosition(0, 480)
    .setLabel("SAVE")
    .setSize(50, 20)
    .setGroup(g2)
    .setBroadcast(true)
    ;

  //println(PFont.list()); //lis all avaliable fonts
  //------------------------------------------------------------------------------------------
  //LED STRIP CONTROL---------------------------------------------------------------------

  Group g1 = cp5.addGroup("LED strip")
    .setPosition(width-230, 60)
    .setSize(200, 20)
    ;


  cp5.addTextlabel("help")
    .setText("Left click + drag to move; Right click to delete")
    .setPosition(0, -30)
    .setGroup(g1)
    .setFont(createFont("Standard 07_58", 10))
    ;

  cp5.addScrollableList("channels")
    .setPosition(0, 10)
    .setGroup(g1)
    .setSize(200, 100)
    .setBarHeight(20)
    .setItemHeight(20)
    .onEnter(toFront)
    .onLeave(close)
    .addItems(java.util.Arrays.asList("RGB", "RGBW", "WWW", "W", "RGB+WWW"))
    .setValue(0)
    .setBackgroundColor(color(0))
    .close()
    //.setType(ScrollableList.LIST) // currently supported DROPDOWN and LIST
    ;

  Numberbox n = cp5.addNumberbox("LEDstripPixels")
    .setSize(100, 20)
    .setLabel("how many pixels")
    .setGroup(g1)
    .setPosition(0, 40)
    .setValue(6)
    ;

  makeEditable( n );

  Numberbox m = cp5.addNumberbox("LEDpixelWidth")
    .setSize(100, 20)
    .setLabel("Pixel width")
    .setGroup(g1)
    .setPosition(0, 80)
    .setValue(50)
    ;

  makeEditable( m );

  Numberbox o = cp5.addNumberbox("LEDpixelHeight")
    .setSize(100, 20)
    .setLabel("Pixel height")
    .setGroup(g1)
    .setPosition(0, 120)
    .setValue(10)
    ;

  makeEditable(o);

  Numberbox x = cp5.addNumberbox("LEDchannelStart")
    .setSize(100, 20)
    .setLabel("DMX addresse")
    .setGroup(g1)
    .setPosition(0, 160)
    .setValue(0)
    ;

  makeEditable(x);

  Numberbox k= cp5.addNumberbox("matrixWidth")
    .setSize(100, 20)
    .setLabel("LED display width - 1 for strip")
    .setGroup(g1)
    .setPosition(0, 200)
    .setValue(1)
    ;

  makeEditable(k);

  cp5.addBang("createLED")
    .setBroadcast(false)
    .setPosition(0, 240)
    .setLabel("Create LED strip")
    .setSize(70, 20)
    .setGroup(g1)
    .setBroadcast(true)
    ;

  cp5.loadProperties(("settings.properties"));
  cp5debug.loadProperties(("settingsGUIon.properties"));
}
//---------------------------------------------------
//COLOR CONVERSIONS RGB TO RGBW
public int convertRGBtoRGBW(int Ri, int Gi, int Bi) {
  int newRGBW;

  //Get the maximum between R, G, and B
  float tM = max(Ri, Gi, Bi);

  //If the maximum value is 0, immediately return pure black.
  if (tM == 0)
  { 
    newRGBW = color(0, 0, 0, 0);
    return newRGBW;
  }

  //This section serves to figure out what the color with 100% hue is
  float multiplier = 255.0f / tM;
  float hR = Ri * multiplier;
  float hG = Gi * multiplier;
  float hB = Bi * multiplier;  

  //This calculates the Whiteness (not strictly speaking Luminance) of the color
  float M = max(hR, hG, hB);
  float m = min(hR, hG, hB);
  float Luminance = ((M + m) / 2.0f - 127.5f) * (255.0f/127.5f) / multiplier;

  //Calculate the output values
  int Wo = PApplet.parseInt(Luminance);
  int Bo = PApplet.parseInt(Bi - Luminance);
  int Ro = PApplet.parseInt(Ri - Luminance);
  int Go = PApplet.parseInt(Gi - Luminance);

  //Trim them so that they are all between 0 and 255
  if (Wo < 0) Wo = 0;
  if (Bo < 0) Bo = 0;
  if (Ro < 0) Ro = 0;
  if (Go < 0) Go = 0;
  if (Wo > 255) Wo = 255;
  if (Bo > 255) Bo = 255;
  if (Ro > 255) Ro = 255;
  if (Go > 255) Go = 255;

  newRGBW = color(Ro, Go, Bo, Wo);

  return newRGBW;
}
/*
 Send Artnet data out of processing
 Send up to 16 universes * 16 subnets = max 256 universes
 designed for RGB LED strip data out
 ensure that all channels of physical pixels are in one universe 
 */







ArtNetClient artnet;

byte[][] dmxData = new byte[16][512]; //subnet, universe, data to send = max number of universes in subnet is 15!

final List<int[]> ledList = new ArrayList<int[]>();

int lastLEDChannel = 0;

public void initArtnet()
{
  // create artnet client without buffer (no receving needed)
  artnet = new ArtNetClient(null);
  artnet.start();
}


public int ledStripArtnet(int CurrChannStart, float posXo, float posYo, int stripLength, int matrixScaleXo, int matrixScaleYo, int RGBW, int matrixWidth) {

  float posX = PApplet.parseInt(map(posXo, 0, width, 0, pg.width)); //resize screen coordinates to pg graphics actual size (frame is being resized to fit the screen...
  float posY = PApplet.parseInt(map(posYo, 0, height, 0, pg.height));
  int matrixScaleX = PApplet.parseInt(map(matrixScaleXo, 0, width, 0, pg.width));
  int matrixScaleY = PApplet.parseInt(map(matrixScaleYo, 0, height, 0, pg.height));

  PImage img = pg.get(PApplet.parseInt(posX), PApplet.parseInt(posY), matrixScaleX*matrixWidth, matrixScaleY*stripLength); //if we were to draw to offscreen buffer
  //------------------------------------------------------------------------------------------------------------------------------ 
  // PImage img = get(int(posX), int(posY), matrixScaleX, matrixScaleY*stripLength); //better for performance?

  img.resize(matrixWidth, stripLength);//resize to fit led strip - effectively averaging color for each pixel
  img.loadPixels(); //init resized pixel array
  int r = 0, g = 0, b = 0; //init color vars

  //save first pixel in subnet so we can decrement it from runnning iterator, essentially = ((currSubnet*tresholdPerUniverse*16)/3)+1;
  int tresholdPerUniverse = 521-3; //init
  int numOfChannPerPixel = 3; //init
  int currUniverse = floor(CurrChannStart/512); //get first universe to start in ie when channel is less than 512, current universe will be 0

  int CurrChann = CurrChannStart;

  switch(RGBW) {
  case 0: //RGB
    tresholdPerUniverse = 512-3; //when we need additional universe to fit all channels of pixel
    numOfChannPerPixel = 3;
    break;
  case 1: //RGBW
    tresholdPerUniverse = 512-3;
    numOfChannPerPixel = 3;
    break;
  case 2: //WWW
    tresholdPerUniverse = 512-3;
    numOfChannPerPixel = 3;
    break;
  case 3: //W
    tresholdPerUniverse = 512-1;
    numOfChannPerPixel = 1;
    break;
  case 4: //RGB+WWW
    tresholdPerUniverse = 512-6;
    numOfChannPerPixel = 6;
    break;
  }

  int lastChannel = CurrChannStart+img.pixels.length*numOfChannPerPixel;

  // for (int i=0; i<img.pixels.length; i++) { //for all pixels <-allows for only 1D manipulation
  for (int y = 0; y < img.height; y++) { //however for led displays we need to know at what collumn we are so 2d loop is necessary
    // Loop through every pixel row      
      
    for (int x = 0; x < img.width; x++) {

      // Use the formula to find the 1D location
      int i = x + y*img.width; //this is current pixel index

      if (i < 0 || i >= width*height) // If the pixel is outside the canvas, skip it
        continue;

      int c = img.pixels[i];
      r = c>>16&0xFF;
      g = c>>8&0xFF;
      b = c&0xFF;

      //compute current channel to write to------------------------------------
      if ((i*numOfChannPerPixel) > ((currUniverse+1)*tresholdPerUniverse)) { 
        currUniverse++; //write to next universe
        println("add");
      }    
      //max number of universes per subnet is 16
      if (currUniverse<=15) {  //so far I am limiting this to just 1 subnet ie 16 DMX universes = 8192 channels, can be eventually expanded to 16subnets..
        CurrChann = ((i)*numOfChannPerPixel)-(currUniverse*tresholdPerUniverse);


        int converted;

        switch(RGBW) {
        case 0: //RGB
          dmxData[currUniverse][CurrChann] = (byte) r; //save artnet data to appropriate array index
          dmxData[currUniverse][CurrChann+1] = (byte) g;
          dmxData[currUniverse][CurrChann+2] = (byte) b;
          break;
        case 1: //RGBW
          converted = convertRGBtoRGBW(r, g, b); //convert to rgbw - white is encoded in alpha
          dmxData[currUniverse][CurrChann] = (byte) red(converted); //save artnet data to appropriate array index
          dmxData[currUniverse][CurrChann+1] = (byte) green(converted);
          dmxData[currUniverse][CurrChann+2] = (byte) blue(converted);
          dmxData[currUniverse][CurrChann+3] = (byte) alpha(converted);//actually white
          break;
        case 2: //WWW
          converted = convertRGBtoRGBW(r, g, b); //convert to rgbw - white is encoded in alpha
          dmxData[currUniverse][CurrChann] = (byte)alpha(converted);//actually white
          dmxData[currUniverse][CurrChann+1] = (byte)alpha(converted);//actually white
          dmxData[currUniverse][CurrChann+2] = (byte)alpha(converted);//actually white
          break;
        case 3: //W
          converted = convertRGBtoRGBW(r, g, b); //convert to rgbw - white is encoded in alpha
          dmxData[currUniverse][CurrChann] = (byte)alpha(converted);//actually white
          break;
        case 4: //RGB+WWW
          converted = convertRGBtoRGBW(r, g, b); //convert to rgbw - white is encoded in alpha
          dmxData[currUniverse][CurrChann] = (byte) red(converted); //save artnet data to appropriate array index
          dmxData[currUniverse][CurrChann+1] = (byte) green(converted);
          dmxData[currUniverse][CurrChann+2] = (byte) blue(converted);
          dmxData[currUniverse][CurrChann+3] = (byte) alpha(converted);//actually white
          dmxData[currUniverse][CurrChann+4] = (byte) alpha(converted);//actually white
          dmxData[currUniverse][CurrChann+5] = (byte) alpha(converted);//actually white
          break;
        }
      } else {
        println("All 16 DMX universes are filled...doing nothing. Implement full subnets if you want to.");
      } 

      //-------------------------------------------
      //RENDER VISUALISATION OF DATA BEING SENT
      if (debug) { //display always makes more sense so it is possible to manuevre the strip over portion of video covered by GUI...but might affect performance..maybe another button to turn it off?

        stroke(255, 255, 255);
        strokeWeight(1);
        fill(r, g, b);
        //rect(posXo, posYo+(i*matrixScaleYo), matrixScaleXo, matrixScaleYo); //use original screen coordinates <- for one 1d loop
        rect(posXo+(x*matrixScaleXo), posYo+(y*matrixScaleYo), matrixScaleXo, matrixScaleYo); //use original screen coordinates <- for 2d loop allowing for led displays
      }
      
    }
  }//2d for loop
  return lastChannel; //return last channel send ie startChannel + all pixels of led strip * number of channels per pixel
}

public void sendArtnet(int maxChannelSend) {
  for (int z = 0; z<floor(maxChannelSend/512)+1; z++) { //for all universes needed - minimal 1
    //artnet.unicastDmx("2.0.0.1", 0, z, dmxData[z]); //unicast to subnet 0
    artnet.unicastDmx(IP1+"."+IP2+"."+IP3+"."+IP4, 0, z, dmxData[z]); //unicast to subnet 0
    //artnet.broadcastDmx(0, z, dmxData[z]);//broadcast to subnet 0
  }
}
public void setupFiles() {
  //filesPath = new StringList(); //init path list
  List<String> filesPath = new ArrayList<String>(); //init path list

  java.io.File dir = new java.io.File(sketchPath("data")); //locate data folder
  files= dir.listFiles();

  int videoFilesCount=0;

  for (int i = 0; i <= files.length - 1; i++)
  {
    String path = files[i].getAbsolutePath();
    String[] rpath;
    if (OS == winOS) {   
      rpath = split(path, '\\'); //bacsklash path returned on win
    } else {
      rpath = split(path, '/'); //forward slash path returned on linux
    }

    if (path.toLowerCase().endsWith(".mp4"))
    {
      filesPath.add(videoFilesCount, rpath[rpath.length-1]);
      videoFilesCount++;
    }

    if (path.toLowerCase().endsWith(".mkv"))
    {
      filesPath.add(videoFilesCount, rpath[rpath.length-1]);
      videoFilesCount++;
    }

    if (path.toLowerCase().endsWith(".avi"))
    {
      filesPath.add(videoFilesCount, rpath[rpath.length-1]);
      videoFilesCount++;
    }

    if (path.toLowerCase().endsWith(".mov"))
    {
      filesPath.add(videoFilesCount, rpath[rpath.length-1]);
      videoFilesCount++;
    }

    if (path.toLowerCase().endsWith(".txt"))
    {
  
      String ledpathset = rpath[rpath.length-1]; //relative path to file
      int stripIndex =  PApplet.parseInt(ledpathset.substring(8,  ledpathset.length()-4));  // removes .txt extension and trim beginning "ledStrip_" part of the name created with save() fce in GUI
      String[] lines = loadStrings(rpath[rpath.length-1]); //load every row of file as string array
      int[] savedStrip = new int[lines.length];
      //println("there are " + lines.length + " lines");
      for (int t = 0; t < lines.length; t++) {
        savedStrip[t] = PApplet.parseInt(lines[t]);//save data from the line as integer into array
      }
      ledList.add(savedStrip); //add saved led strip to collection
    }
  }

  println(filesPath.size()+" files in data folder");
  println("From which "+videoFilesCount+" is supported video files");

  //arrFiles = new String[filesPath.size()]; // Create array from list for GUi
  arrFiles = new String[videoFilesCount]; // Create array from list for GUi

  //init videos with appropriate library based on OS
  if (OS == winOS) { 
    //movies = new Movie[filesPath.size()];
    movies = new Movie[videoFilesCount];
    for (int x=0; x<movies.length; x++) {
      movies[x]  = new Movie(this, ""); //init but do not load anything
      //movies[x]  = new Movie(this, filesPath.get(x));
      //movies[x].frameRate(30); //set framerate
      //movies[x].loop();//???? might help against stutter?
      //movies[x].stop();
      arrFiles[x] = filesPath.get(x);
      println(filesPath.get(x));
    }
  } else {
    videos =new GLMovie[videoFilesCount];
    for (int x=0; x<videos.length; x++) {
      videos[x]  = new GLMovie(this, filesPath.get(x));
      //videos[x].loop();//???? might help against stutter?
      //videos[x].close();
      arrFiles[x] = filesPath.get(x);
      println(filesPath.get(x));
    }
  }
}
//for raspi video--------------------

GLMovie[] videos;
//for windows etc video---------------

Movie[] movies;

boolean paused = false;
int countFps = 0; //counting how many times is video frame displayed during one second
int countedFPS = 0; //hold resolut of last countFPS

final int WAIT_TIME = (int) (1 * 1000); // 1 second
int startTime;

public void OSvideo(int vIndex) {

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

public void stopVideo() {
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

public void setVSpeed(int vIndex, float newSpeed) {
  if (OS == winOS) {   //WIN_OS -------------------------------------
    movies[vIndex].speed(newSpeed);
  } else { //OTHER_OS ---------------------------------------------
    videos[vIndex].speed(newSpeed);
  }
}

public boolean hasFinished() {
  return millis() - startTime > WAIT_TIME;
}
/* //is it better? than have in the draw loop? dunno
 void movieEvent(Movie movies) {
 movies.read();
 }
 */
public void renderVideoFrame(boolean WinOSS, int vIndex) {
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
  public void settings() {  size(800, 600, P2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "pixelMappingDj_v23" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
