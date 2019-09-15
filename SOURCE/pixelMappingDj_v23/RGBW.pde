//---------------------------------------------------
//COLOR CONVERSIONS RGB TO RGBW
color convertRGBtoRGBW(int Ri, int Gi, int Bi) {
  color newRGBW;

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
  int Wo = int(Luminance);
  int Bo = int(Bi - Luminance);
  int Ro = int(Ri - Luminance);
  int Go = int(Gi - Luminance);

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
