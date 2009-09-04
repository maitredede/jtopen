///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: AS400JDBCConnectionPoolDataSourceBeanInfo.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;


/**
*  The AS400JDBCConnectionPoolDataSourceBeanInfo class provides bean information
*  for the AS400JDBCConnectionPoolDataSource class.
**/
public class AS400JDBCConnectionPoolDataSourceBeanInfo extends SimpleBeanInfo
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";


   // Class this bean info represents.
   private final static Class beanClass = AS400JDBCConnectionPoolDataSource.class;

   /**
   *  Returns additional bean information from the AS400JDBCConnectionPoolDataSource superclass.
   *  @return The bean information.
   **/
   public BeanInfo[] getAdditionalBeanInfo()
   {
      return new BeanInfo[] { new AS400JDBCDataSourceBeanInfo() };
   }

   /**
     Returns the bean descriptor.
     @return The bean descriptor.
   **/
   public BeanDescriptor getBeanDescriptor()
   {
       return new BeanDescriptor(beanClass);
   }

   /**
     Returns an image for the icon.

     @param icon    The icon size and color.
     @return        The image.
   **/
   public Image getIcon (int icon)
   {
       Image image = null;
       switch (icon) {
           case BeanInfo.ICON_MONO_16x16:
           case BeanInfo.ICON_COLOR_16x16:
               image = loadImage ("AS400JDBCConnectionPoolDataSource16.gif");
               break;
           case BeanInfo.ICON_MONO_32x32:
           case BeanInfo.ICON_COLOR_32x32:
               image = loadImage ("AS400JDBCConnectionPoolDataSource32.gif");
               break;
       }
       return image;
   }
}