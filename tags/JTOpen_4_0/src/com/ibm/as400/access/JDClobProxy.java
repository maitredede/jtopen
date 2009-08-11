///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: JDClobProxy.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.sql.Clob;
import java.sql.SQLException;



// JDBC 2.0
/**
The JDClobProxy class provides access to character large
objects.  The data is valid only within the current
transaction.
**/
class JDClobProxy
extends AbstractProxyImpl
implements Clob
{
  private static final String copyright = "Copyright (C) 1997-2001 International Business Machines Corporation and others.";


  // Copied from JDError:
  private static final String EXC_FUNCTION_NOT_SUPPORTED       = "IM001";

 

  public InputStream getAsciiStream ()
    throws SQLException
  {
    try {
      JDInputStreamProxy newStream = new JDInputStreamProxy ();
      return (JDInputStreamProxy) connection_.callFactoryMethod (
                                    pxId_, "getAsciiStream", newStream);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }


  public Reader getCharacterStream ()
    throws SQLException
  {
    try {
      JDReaderProxy newReader = new JDReaderProxy ();
      return (JDReaderProxy) connection_.callFactoryMethod (
                                     pxId_, "getCharacterStream",
                                     newReader);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }


  public String getSubString (long start, int length)
    throws SQLException
  {
    try {
      return (String) connection_.callMethod (pxId_, "getSubString",
                               new Class[] { Long.TYPE, Integer.TYPE },
                               new Object[] { new Long (start),
                                              new Integer (length) })
                         .getReturnValue();
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  public long length ()
    throws SQLException
  {
    try {
      return connection_.callMethod (pxId_, "length")
                             .getReturnValueLong ();
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  public long position (String pattern, long start)
    throws SQLException
  {
    try {
      return connection_.callMethod (pxId_, "position",
                               new Class[] { String.class, Long.TYPE },
                               new Object[] { pattern, new Long (start) })
                             .getReturnValueLong ();
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  public long position (Clob pattern, long start)
    throws SQLException
  {
    try {
      return connection_.callMethod (pxId_, "position",
                               new Class[] { Clob.class, Long.TYPE },
                               new Object[] { pattern, new Long (start) })
                             .getReturnValueLong ();
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }


// JDBC 3.0
    public OutputStream setAsciiStream (long pos)
    throws SQLException
    {
        // Avoid dragging in JDError
        throw new SQLException (
                               AS400JDBCDriver.getResource("JD" + EXC_FUNCTION_NOT_SUPPORTED),
                               EXC_FUNCTION_NOT_SUPPORTED, -99999);
    }


// JDBC 3.0
    public Writer setCharacterStream (long pos)
    throws SQLException
    {
        // Avoid dragging in JDError
        throw new SQLException (
                               AS400JDBCDriver.getResource("JD" + EXC_FUNCTION_NOT_SUPPORTED),
                               EXC_FUNCTION_NOT_SUPPORTED, -99999);
    }


// JDBC 3.0
    public int setString (long pos, String str)
    throws SQLException
    {
        try {
            return connection_.callMethod (pxId_, "setString",
                                           new Class[] { Long.TYPE, String.class},
                                           new Object[] { new Long(pos), str})
            .getReturnValueInt ();
        }
        catch (InvocationTargetException e) {
            throw JDConnectionProxy.rethrow1 (e);
        }
    }


// JDBC 3.0
    public int setString (long pos, String str, int offset, int len)
    throws SQLException
    {
        try {
            return connection_.callMethod (pxId_, "setString",
                                           new Class[] { Long.TYPE, String.class, Integer.TYPE, Integer.TYPE},
                                           new Object[] { new Long(pos), str, new Integer(offset), new Integer(len)})
            .getReturnValueInt ();
        }
        catch (InvocationTargetException e) {
            throw JDConnectionProxy.rethrow1 (e);
        }
    }


// JDBC 3.0
    public void truncate (long len)
    throws SQLException
    {
        try {
            connection_.callMethod (pxId_, "truncate",
                                    new Class[] { Long.TYPE,},
                                    new Object[] { new Long(len)} );
        }
        catch (InvocationTargetException e) {
            throw JDConnectionProxy.rethrow1 (e);
        }
    }


}