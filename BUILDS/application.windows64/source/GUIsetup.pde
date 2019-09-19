import controlP5.*;
import java.util.*;
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
float videoSpeed=1.0;
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


void setupGUI() {

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
    .setRange(0.5, 2.0)
    .setValue(1.0)
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
