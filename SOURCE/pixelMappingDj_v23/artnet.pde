/*
 Send Artnet data out of processing
 Send up to 16 universes * 16 subnets = max 256 universes
 designed for RGB LED strip data out
 ensure that all channels of physical pixels are in one universe 
 */

import ch.bildspur.artnet.*;
import ch.bildspur.artnet.packets.*;
import ch.bildspur.artnet.events.*;
import java.util.List;
import java.net.Inet4Address ;

ArtNetClient artnet;

byte[][] dmxData = new byte[16][512]; //subnet, universe, data to send = max number of universes in subnet is 15!

final List<int[]> ledList = new ArrayList<int[]>();

int lastLEDChannel = 0;

void initArtnet()
{
  // create artnet client without buffer (no receving needed)
  artnet = new ArtNetClient(null);
  artnet.start();
}


int ledStripArtnet(int CurrChannStart, float posXo, float posYo, int stripLength, int matrixScaleXo, int matrixScaleYo, int RGBW, int matrixWidth) {

  float posX = int(map(posXo, 0, width, 0, pg.width)); //resize screen coordinates to pg graphics actual size (frame is being resized to fit the screen...
  float posY = int(map(posYo, 0, height, 0, pg.height));
  int matrixScaleX = int(map(matrixScaleXo, 0, width, 0, pg.width));
  int matrixScaleY = int(map(matrixScaleYo, 0, height, 0, pg.height));

  PImage img = pg.get(int(posX), int(posY), matrixScaleX*matrixWidth, matrixScaleY*stripLength); //if we were to draw to offscreen buffer
  //------------------------------------------------------------------------------------------------------------------------------ 
  // PImage img = get(int(posX), int(posY), matrixScaleX, matrixScaleY*stripLength); //better for performance?

  img.resize(matrixWidth, stripLength);//resize to fit led strip - effectively averaging color for each pixel
  img.loadPixels(); //init resized pixel array
  color r = 0, g = 0, b = 0; //init color vars

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

      color c = img.pixels[i];
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


        color converted;

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

void sendArtnet(int maxChannelSend) {
  for (int z = 0; z<floor(maxChannelSend/512)+1; z++) { //for all universes needed - minimal 1
    //artnet.unicastDmx("2.0.0.1", 0, z, dmxData[z]); //unicast to subnet 0
    artnet.unicastDmx(IP1+"."+IP2+"."+IP3+"."+IP4, 0, z, dmxData[z]); //unicast to subnet 0
    //artnet.broadcastDmx(0, z, dmxData[z]);//broadcast to subnet 0
  }
}
