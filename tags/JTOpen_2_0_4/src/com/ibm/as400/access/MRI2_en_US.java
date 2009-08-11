///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: MRI2_en_US.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

/**
Locale-specific objects for the AS/400 Toolbox for Java.
**/
//
// Implementation note:
//
// This class is not really necessary.  It exists to enhance performance.
// When Java searches for a resource bundle, it searches for a locale-
// specific resource bundle first, then more general resource bundles.
// By creating this subclass of the general resource bundle, we prevent
// Java from having to load multiple resource bundles.  This performance
// boost will be most noticeable for applets.
//
// We extend MRI rather than MRI_en to circumvent the loading
// of the MRI_en class, which does not contain anything, anyway.
//
public class MRI2_en_US extends MRI2
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";


}
