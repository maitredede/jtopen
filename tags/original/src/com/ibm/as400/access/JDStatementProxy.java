///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: JDStatementProxy.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;


class JDStatementProxy
extends AbstractProxyImpl
implements java.sql.Statement
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";


  // Protected data.
  protected JDConnectionProxy       jdConnection_;
         // The object that caused this object to be created.

  protected JDResultSetProxy        cachedResultSet_;

  


  public JDStatementProxy (JDConnectionProxy jdConnection)
  {
    jdConnection_ = jdConnection;
  }


// JDBC 2.0
    public void addBatch (String sql)
        throws SQLException
    {
      callMethod ("addBatch",
                  new Class[] { String.class },
                  new Object[] { sql });
    }


  // Call a method.  No return value is expected.
  protected void callMethod (String methodName)
    throws SQLException
  {
    try {
      connection_.callMethod (pxId_, methodName);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }


  protected void callMethod (String methodName,
                           Class[] argClasses,
                           Object[] argValues)
    throws SQLException
  {
    try {
      connection_.callMethod (pxId_, methodName, argClasses, argValues);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  // Call a method, and return a boolean.
  protected boolean callMethodRtnBool (String methodName)
    throws SQLException
  {
    try {
      return connection_.callMethodReturnsBoolean (pxId_, methodName);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  // Call a method, and return an int.
  protected int callMethodRtnInt (String methodName)
    throws SQLException
  {
    try {
      return connection_.callMethodReturnsInt (pxId_, methodName);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  // Call a method, and return an Object.
  protected Object callMethodRtnObj (String methodName)
    throws SQLException
  {
    try {
      return connection_.callMethodReturnsObject (pxId_, methodName);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }

  // Call a method, and return a 'raw' ProxyReturnValue.
  protected ProxyReturnValue callMethodRtnRaw (String methodName,
                                             Class[] argClasses,
                                             Object[] argValues)
    throws SQLException
  {
    try {
      return connection_.callMethod (pxId_, methodName,
                                          argClasses, argValues);
    }
    catch (InvocationTargetException e) {
      throw JDConnectionProxy.rethrow1 (e);
    }
  }



    public void cancel ()
      throws SQLException
    {
      cachedResultSet_ = null;
      callMethod ("cancel");
    }



// JDBC 2.0
    public void clearBatch ()
        throws SQLException
    {
      callMethod ("clearBatch");
    }



    public void clearWarnings ()
      throws SQLException
    {
      callMethod ("clearWarnings");
    }



    public void close ()
      throws SQLException
    {
      cachedResultSet_ = null;
      callMethod ("close");
    }


    public boolean execute (String sql)
      throws SQLException
    {
      cachedResultSet_ = null;
      return callMethodRtnRaw ("execute",
                               new Class[] { String.class },
                               new Object[] { sql })
        .getReturnValueBoolean ();
    }



// JDBC 2.0
    public int[] executeBatch ()
        throws SQLException
    {
      cachedResultSet_ = null;
      return (int[]) callMethodRtnObj ("executeBatch");
    }



    public ResultSet executeQuery (String sql)
        throws SQLException
    {
      cachedResultSet_ = null;
      try {
        JDResultSetProxy newResultSet = new JDResultSetProxy (jdConnection_, this);
        cachedResultSet_ = (JDResultSetProxy) connection_.callFactoryMethod (
                                            pxId_,
                                            "executeQuery",
                                            new Class[] { String.class },
                                            new Object[] { sql },
                                            newResultSet);
        return cachedResultSet_;
      }
      catch (InvocationTargetException e) {
        throw JDConnectionProxy.rethrow1 (e);
      }
    }



    public int executeUpdate (String sql)
      throws SQLException
    {
      cachedResultSet_ = null;
      return callMethodRtnRaw ("executeUpdate",
                               new Class[] { String.class },
                               new Object[] { sql })
        .getReturnValueInt ();
    }



// JDBC 2.0
    public Connection getConnection ()
    {
      return jdConnection_;
    }


// JDBC 2.0
    public int getFetchDirection ()
        throws SQLException
    {
      return callMethodRtnInt ("getFetchDirection");
    }



// JDBC 2.0
    public int getFetchSize ()
        throws SQLException
    {
      return callMethodRtnInt ("getFetchSize");
    }



    public int getMaxFieldSize ()
        throws SQLException
    {
      return callMethodRtnInt ("getMaxFieldSize");
    }



    public int getMaxRows ()
        throws SQLException
    {
      return callMethodRtnInt ("getMaxRows");
    }



    public boolean getMoreResults ()
      throws SQLException
    {
      cachedResultSet_ = null;
      return callMethodRtnBool ("getMoreResults");
    }



    public int getQueryTimeout ()
      throws SQLException
    {
      return callMethodRtnInt ("getQueryTimeout");
    }



    public ResultSet getResultSet ()
      throws SQLException
    {
      if (cachedResultSet_ == null)
      {
        try {
          JDResultSetProxy newResultSet = new JDResultSetProxy (jdConnection_, this);
          cachedResultSet_ = (JDResultSetProxy) connection_.callFactoryMethod (
                                       pxId_, "getResultSet", newResultSet);
        }
        catch (InvocationTargetException e) {
          throw JDConnectionProxy.rethrow1 (e);
        }
      }
      return cachedResultSet_;
    }



// JDBC 2.0
    public int getResultSetConcurrency ()
        throws SQLException
    {
      return callMethodRtnInt ("getResultSetConcurrency");
    }



// JDBC 2.0
    public int getResultSetType ()
        throws SQLException
    {
      return callMethodRtnInt ("getResultSetType");
    }



    public int getUpdateCount ()
      throws SQLException
    {
      return callMethodRtnInt ("getUpdateCount");
    }



    public SQLWarning getWarnings ()
      throws SQLException
    {
      return (SQLWarning) callMethodRtnObj ("getWarnings");
    }


    public void setCursorName (String cursorName)
      throws SQLException
    {
      cachedResultSet_ = null;
      callMethod ("setCursorName",
                  new Class[] { String.class },
                  new Object[] { cursorName });
    }



    public void setEscapeProcessing (boolean escapeProcessing)
      throws SQLException
    {
      callMethod ("setEscapeProcessing",
                  new Class[] { Boolean.TYPE },
                  new Object[] { new Boolean (escapeProcessing) });
    }



// JDBC 2.0
    public void setFetchDirection (int fetchDirection)
        throws SQLException
    {
      callMethod ("setFetchDirection",
                  new Class[] { Integer.TYPE },
                  new Object[] { new Integer (fetchDirection) });
    }



// JDBC 2.0
    public void setFetchSize (int fetchSize)
        throws SQLException
    {
      callMethod ("setFetchSize",
                  new Class[] { Integer.TYPE },
                  new Object[] { new Integer (fetchSize) });
    }



    public void setMaxFieldSize (int maxFieldSize)
      throws SQLException
    {
      callMethod ("setMaxFieldSize",
                  new Class[] { Integer.TYPE },
                  new Object[] { new Integer (maxFieldSize) });
    }


    public void setMaxRows (int maxRows)
      throws SQLException
    {
      callMethod ("setMaxRows",
                  new Class[] { Integer.TYPE },
                  new Object[] { new Integer (maxRows) });
    }



    public void setQueryTimeout (int queryTimeout)
        throws SQLException
    {
      callMethod ("setQueryTimeout",
                  new Class[] { Integer.TYPE },
                  new Object[] { new Integer (queryTimeout) });
    }


    // This method is not required by java.sql.Statement,
    // but it is used by the JDBC testcases, and is implemented
    // in the public class.
    public String toString ()
    {
      try {
        return (String) connection_.callMethodReturnsObject (pxId_, "toString");
      }
      catch (InvocationTargetException e) {
        throw ProxyClientConnection.rethrow (e);
      }
    }

}
