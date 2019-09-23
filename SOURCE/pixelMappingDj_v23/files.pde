void setupFiles() {
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
      int stripIndex =  int(ledpathset.substring(8,  ledpathset.length()-4));  // removes .txt extension and trim beginning "ledStrip_" part of the name created with save() fce in GUI
      String[] lines = loadStrings(rpath[rpath.length-1]); //load every row of file as string array
      int[] savedStrip = new int[lines.length];
      //println("there are " + lines.length + " lines");
      for (int t = 0; t < lines.length; t++) {
        savedStrip[t] = int(lines[t]);//save data from the line as integer into array
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
