///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: FileListRenderer.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.util.html;

import java.io.File;
import javax.servlet.http.HttpServletRequest;


/**
*  The FileListRenderer class renders the name field for directories and files
*  in a FileListElement.
*
*  If the behavior of the default FileListRenderer is not desired, subclass
*  FileListRenderer and override the appropriate methods until the
*  FileListElement achieves the desired behavior.
*
*  Subclassing FileListRenderer will allow your servlet to include/exclude
*  or change the action of any directory or file in the FileListElement.
*  For example, if a servlet did not want users to see any *.exe files,
*  A subclass of FileListRenderer would be created and the new class
*  would override the getFileName() method to figure out if the File object
*  passed to it was a *.exe file, if it is, null could be returned, which
*  would indicate that the file should not be displayed.
*
*  <P>
*  This example creates an FileListElement object with a renderer:
*  <P>
*  <PRE>
*   // Create a FileListElement.
*  FileListElement fileList = new FileListElement(sys, httpservletrequest);
*  <p>
*  // Set the renderer specific to this servlet, which extends
*  // FileListRenderer and overrides applicable methods.
*  fileList.setRenderer(new myFileListRenderer(request));
*  </PRE>
**/
public class FileListRenderer
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

   
   private HttpServletRequest request_;
   private String uri_;
   private String path_;


   /**
    *  Constructs a FileListRenderer with the specified <i>request</i>.
    *
    *  @param request The Http servlet request.
    **/
   public FileListRenderer(HttpServletRequest request)
   {
      if (request == null)
         throw new NullPointerException("request");

      uri_ = request.getServletPath();
      path_ = request.getPathInfo();
   }
   

   /**
    *  Return the directory name string.  A link to the calling servlet with the
    *  directory included in the path info by default.  If the directory should 
    *  not be added to the FileListElement, a null string should be returned.
    *
    *  @return The directory name string.
    **/
   public String getDirectoryName(File file)
   {
      if (file == null)
         throw new NullPointerException("file");

      String name = file.getName();

      StringBuffer buffer = new StringBuffer("<a href=\"");
      buffer.append(uri_);
      buffer.append(URLEncoder.encode(path_.replace('\\','/'), false));        // @A1C
      buffer.append(path_.endsWith("/") ? "" :"/");
      buffer.append(URLEncoder.encode(name, false));                           // @A1C
      buffer.append("\">");
      buffer.append(name);
      buffer.append("</a>");

      return buffer.toString();
   }
   
   
   /**
    *  Return the file name string.  The file name will be returned by default.  
    *  If the file should not be displayed in the FileListElement, a null string 
    *  should be returned.
    *
    *  @return The file name string.
    **/
   public String getFileName(File file)
   {
      if (file == null)
         throw new NullPointerException("file");

      return file.getName();
   }

   /**
    *  Return the parent directory name string.  A link to the calling servlet with the 
    *  parent directory included in the path info will be returned by default.  If the 
    *  parent should not be display in the FileListElement, a null string should be returned.
    *
    *  @return The parent name string.
    **/
   public String getParentName(File file)
   {
      if (file == null)
         throw new NullPointerException("file");

      String parent = file.getParent();

      StringBuffer buffer = new StringBuffer("<a href=\"");
      buffer.append(uri_);
      buffer.append(parent!=null ? URLEncoder.encode(parent.replace('\\','/'), false) : "");  // @A1C
      buffer.append("\">../ (Parent Directory)</a>");

      return buffer.toString();
   }
}