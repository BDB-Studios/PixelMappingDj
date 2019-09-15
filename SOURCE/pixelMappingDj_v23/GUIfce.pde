//--------------------------------------------------------------------------
//VIDEO CONTROLS FCES-------------------------------------------------------
void videos(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  println("gui index: "+n);
  playStarted = true;
  playing = n;
  //  movies[playing].loop();
}

void flipH(boolean theFlag) {
  if (theFlag) {
    flipH=-1;
  } else {
    flipH=1;
  }
}

void flipV(boolean theFlag) {
  if (theFlag) {
    flipV=-1;
  } else {
    flipV=1;
  }
}

void play(boolean theFlag) {
  play = theFlag;
  if (!play) {
    stopVideo();
  } else {
    playStarted = true;
  }
}

void pause(boolean theFlag) {
  if (playing != -1) {
    pause = theFlag;
  }
}

void quit(boolean theFlag) { 
  //debug = theFlag;
  if (theFlag == false) {
    stop();
  }
}

void save() { 
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

void debug(boolean theFlag) { 
  debug = theFlag;
  save(); //save settings...
  if (theFlag) {
    cp5.setAutoDraw(true);
  } else {
    cp5.setAutoDraw(false);
  }
}

void videoSpeed(float theSpeed) {
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


void makeEditable( Numberbox n ) {
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
      n.setValue( float( text ) );
      text = "";
    } else {
      n.getValueLabel().setText(""+n.getValue());
    }
  }
}

void channels(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  channels = n;
}

void matrixWidth(int n) {
  // println(n, cp5.get(ScrollableList.class, "dropdown").getItem(n));
  matrixWidth = n;
}

void createLED() {
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
boolean pointRect(float px, float py, float rx, float ry, float rw, float rh) {

  // is the point inside the rectangle's bounds?
  if (px >= rx &&        // right of the left edge AND
    px <= rx + rw &&   // left of the right edge AND
    py >= ry &&        // below the top AND
    py <= ry + rh) {   // above the bottom
    return true;
  }
  return false;
}
