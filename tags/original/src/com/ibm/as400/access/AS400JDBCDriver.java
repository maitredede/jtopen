///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: AS400JDBCDriver.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



/**
<p>The AS400JDBCDriver class is a JDBC 2.0 driver that accesses
DB2 for OS/400 databases.

<p>To use this driver, the application or caller must register 
the driver with the JDBC DriverManager.  This class also registers 
itself automatically when it is loaded.

<p>After registering the driver, applications make connection
requests to the DriverManager, which dispatches them to the
appropriate driver.  This driver accepts connection requests
for databases specified by the URLs that match the following syntax:

<pre>
jdbc:as400://<em>server-name</em>/<em>default-schema</em>;<em>properties</em>
</pre>

<p>The driver uses the specified server name to connect
to a corresponding AS/400 server.  If a server name is not
specified, then the user will be prompted.  

<p>The default schema is optional and the driver uses it to resolve 
unqualified names in SQL statements.  If no default schema is set, then
the driver resolves unqualified names based on the naming convention
for the connection.  If SQL naming is being used, and no default schema
is set, then the driver resolves unqualified names using the schema with 
the same name as the user.  If system naming is being used, and no
default schema is set, then the driver resolves unqualified names using
the server job's library list.  See <a href="JDBCProperties.html">JDBC
properties</a> for more details on how to set the naming convention
and library list.

<p>Several properties can optionally be set within the URL.  They are 
separated by semicolons and are in the form:
<pre>
<em>name1</em>=<em>value1</em>;<em>name2</em>=<em>value2</em>;<em>...</em>
</pre>
See <a href="JDBCProperties.html">JDBC properties</a> for a
complete list of properties supported by this driver.

<p>The following example URL specifies a connection to the
database on server <em>mysystem.helloworld.com</em> with
<em>mylibrary</em> as the default schema.  The connection will
use the system naming convention and return full error messages:
<pre>
jdbc:as400://mysystem.helloworld.com/mylibrary;naming=system;errors=full
</pre>
**/
//
// Implementation note:
//
// 1. A goal stated in the JDBC specification is to keep
//    the Driver class as small and standalone as possible,
//    so that it can be quickly loaded when choosing a
//    driver for a particular database.
//
// 2. It was proposed that we also accept URLs with the
//    "db2" subprotocol.  This would make us consistent with
//    other IBM drivers.  In addition, it would also allow
//    developers to hardcode URLs in programs and they would
//    run as-is with both this driver and the "native" driver.
//
//    We realized, though, that if running on a client with
//    both this driver and another DB2 client for that platform,
//    how do the drivers differentiate themselves?  Therefore
//    we are chosing NOT to recognized the "db2" subprotocol.
//    Instead, suggest to developers to externalize the URL
//    to users, rather than hardcoding it.
//
public class AS400JDBCDriver
implements java.sql.Driver
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";



    // Constants.
    static final int    MAJOR_VERSION_          = 3;                                        // @D0C
    static final int    MINOR_VERSION_          = 0;
    static final String DATABASE_PROCUCT_NAME_  = "DB2 UDB for AS/400";                     // @D0A
    static final String DRIVER_NAME_            = "AS/400 Toolbox for Java JDBC Driver";    // @D0C



    // This string "9999:9999" is returned when resource
    // bundle errors occur.  No significance to this string,
    // except that Client Access used to use it.  It would
    // probably be more helpful to return some other string.
    //
    private static final String MRI_NOT_FOUND_  = "9999:9999";



    // Private data.

    // Toolbox resources needed in proxy jar file.            @A1C
    private static ResourceBundle resources_;
    // Toolbox resources NOT needed in proxy jar file.        @A1A
    private static ResourceBundle resources2_;



/**
Static initializer.  Registers the JDBC driver with the JDBC
driver manager and loads the appropriate resource bundle for
the current locale.
**/
    static {
      try {
        DriverManager.registerDriver (new AS400JDBCDriver ());
        resources_  = ResourceBundle.getBundle ("com.ibm.as400.access.JDMRI");
        resources2_ = ResourceBundle.getBundle ("com.ibm.as400.access.JDMRI2");
        // Note: When using the proxy jar file, we do not expect to find JDMRI2.
      }
      catch (MissingResourceException e) {

        // Catch the exception.  This is because exceptions
        // thrown from static initializers are hard to debug.
        // Instead, we will handle the error when the
        // driver needs to get at particular methods.
        // See getResource().
      }
      catch (SQLException e) {
        // Ignore.
      }
    }



/**
Indicates if the driver understands how to connect
to the database named by the URL.

@param  url     The URL for the database.
@return         true if the driver understands how
                to connect to the database; false
                otherwise.

@exception SQLException If an error occurs.
**/
    public boolean acceptsURL (String url)
        throws SQLException
    {
        JDDataSourceURL dataSourceUrl = new JDDataSourceURL (url);
        return dataSourceUrl.isValid ();
    }



/**
Connects to the database named by the specified URL.
There are many optional properties that can be specified.
Properties can be specified either as part of the URL or in
a java.util.Properties object.  See <a href="JDBCProperties.html">
JDBC properties</a> for a complete list of properties
supported by this driver.

@param  url     The URL for the database.
@param  info    The connection properties.
@return         The connection to the database or null if
                the driver does not understand how to connect
                to the database.

@exception SQLException If the driver is unable to make the connection.
**/
    public java.sql.Connection connect (String url,
					                    Properties info)
        throws SQLException
    {
      // Check first thing to see if the trace property is
      // turned on.  This way we can trace everything, including
      // the important stuff like loading the properties.
      JDDataSourceURL	dataSourceUrl = new JDDataSourceURL (url);
      Properties urlProperties = dataSourceUrl.getProperties ();

      if (JDProperties.isTraceSet (urlProperties, info)) {
        if (! JDTrace.isTraceOn ())
          JDTrace.setTraceOn (true);
      }
      else
        JDTrace.setTraceOn (false);

      JDProperties jdProperties = new JDProperties (urlProperties, info);

      // Initialize the connection if the URL is valid.
      Connection connection = null;                                        //@A0C
      if (dataSourceUrl.isValid ())
        connection = initializeConnection (dataSourceUrl, jdProperties,
                                           info);  //@A0C

      return connection;
    }



/**
Returns the driver's major version number.

@return         The major version number.
**/
    public int getMajorVersion ()
    {
		return MAJOR_VERSION_;
    }



/**
Returns the driver's minor version number.

@return         The minor version number.
**/
    public int getMinorVersion ()
    {
		return MINOR_VERSION_;
    }



/**
Returns an array of DriverPropertyInfo objects that
describe the properties that are supported by this
driver.

@param  url     The URL for the database.
@param  info    The connection properties.
@return         The descriptions of all possible properties or null if
                the driver does not understand how to connect to the
                database.

@exception SQLException If an error occurs.
**/
    public DriverPropertyInfo[] getPropertyInfo (String url,
						                         Properties info)
        throws SQLException
    {
        JDDataSourceURL dataSourceUrl = new JDDataSourceURL (url);

        DriverPropertyInfo[] dpi = null;
        if (dataSourceUrl.isValid ()) {
    		JDProperties properties = new JDProperties (dataSourceUrl.getProperties(), info);
	    	dpi = properties.getInfo ();
	    }

	    return dpi;
    }



/**
Returns a resource from the resource bundle.

@param  key     The resource key.
@return         The resource String.
**/
    static String getResource (String key)
    {
        // If the resource bundle or resource is not found,
        // do not thrown an exception.  Instead, return a
        // default string.  This is because some JVMs will
        // not recover quite right from such errors, and
        // claim a security exception (e.g. Netscape starts
        // looking in the client class path, which is
        // not allowed.)
        //
        String resource;
        if (resources_ == null)
            resource = MRI_NOT_FOUND_;
        else {
          try {
            resource = resources_.getString (key);
          }
          catch (MissingResourceException e) {
            if (resources2_ == null)                       //@A1A
              resource = MRI_NOT_FOUND_;                   //@A1A
            else {                                         //@A1A
              try {                                        //@A1A
                resource = resources2_.getString (key);    //@A1A
              }                                            //@A1A
              catch (MissingResourceException e1) {        //@A1A
                JDTrace.logInformation (AS400JDBCDriver.class,
                                        "Missing resource [" + key + "]"); //@A1A
                resource = MRI_NOT_FOUND_;
              }
            }
          }
        }

        return resource;
    }


    //@A0A  - This logic was formerly in the AS400JDBCConnection ctor and open() method.
    private Connection initializeConnection (JDDataSourceURL dataSourceUrl,
                                             JDProperties jdProperties,
                                             Properties info)
        throws SQLException
    {
      Connection connection                       = null;
      AS400 as400                                 = null;
      boolean proxyServerWasSpecifiedInUrl        = false;
      boolean proxyServerWasSpecifiedInProperties = false;
      boolean proxyServerWasSpecified             = false;

      // @A0A
      // See if a proxy server was specified.
    //if (jdProperties.getIndex (JDProperties.PROXY_SERVER) != -1)         //@A3D
      if (jdProperties.getString(JDProperties.PROXY_SERVER).length() != 0) //@A3C
        proxyServerWasSpecifiedInUrl = true;
      if (SystemProperties.getProperty (SystemProperties.AS400_PROXY_SERVER) != null)
        proxyServerWasSpecifiedInProperties = true;
      if (proxyServerWasSpecifiedInUrl || proxyServerWasSpecifiedInProperties)
        proxyServerWasSpecified = true;

      // If no proxy server was specified, and there is a secondary URL,
      // simply pass the secondary URL to the DriverManager and ask it for
      // an appropriate Connection object.
      if (!proxyServerWasSpecified) {
        String secondaryUrl = dataSourceUrl.getSecondaryURL ();
        if (secondaryUrl.length() != 0) {
          if (JDTrace.isTraceOn())
            JDTrace.logInformation (this,
                                    "Secondary URL [" + secondaryUrl + "]");
          return DriverManager.getConnection (secondaryUrl, info);
        }
      }

      // We must handle the different combinations of input
      // user names and passwords.
      String serverName = dataSourceUrl.getServerName();
      String userName   = jdProperties.getString (JDProperties.USER);
      String password   = jdProperties.getString (JDProperties.PASSWORD);
      boolean prompt    = jdProperties.getBoolean (JDProperties.PROMPT);
      boolean secure    = jdProperties.getBoolean (JDProperties.SECURE);

      // Create the AS400 object, so we can create a Connection via loadImpl2.
      if (secure) {
        if (serverName.length() == 0)                   
          as400 = new SecureAS400 ();                      
        else if (userName.length() == 0)               
          as400 = new SecureAS400 (serverName);
        else if (password.length() == 0)
          as400 = new SecureAS400 (serverName, userName);
        else
          as400 = new SecureAS400 (serverName, userName, password);
      }
      else {
        if (serverName.length() == 0)
          as400 = new AS400 ();
        else if (userName.length() == 0)
          as400 = new AS400 (serverName);
        else if (password.length() == 0)
          as400 = new AS400 (serverName, userName);
        else
          as400 = new AS400 (serverName, userName, password);
      }

      // Determine when the signon GUI can be presented..
      try {
        as400.setGuiAvailable (prompt);
      }
      catch (java.beans.PropertyVetoException e) {
        // This will never happen, as there are no listeners.
      }

      if (proxyServerWasSpecifiedInUrl)
      {
        // A proxy server was specified in URL,
        // so we need to inform the AS400 object.

        //boolean proxyServerSecure = jdProperties.getBoolean (JDProperties.PROXY_SERVER_SECURE);   // TBD
        String proxyServerNameAndPort = jdProperties.getString (JDProperties.PROXY_SERVER);
        // Note: The PROXY_SERVER property is of the form:
        //       hostName[:portNumber]
        //       where portNumber is optional.
        try {
          as400.setProxyServer (proxyServerNameAndPort);
          //as400.setProxyServerSecure (proxyServerSecure);  // TBD
        }
        catch (java.beans.PropertyVetoException e) {} // Will never happen.
      }

      // @A0C
      // Create the appropriate kind of Connection object.
      connection = (Connection) as400.loadImpl2 (
                                  "com.ibm.as400.access.AS400JDBCConnection",
                                  "com.ibm.as400.access.JDConnectionProxy");

      // Set the properties on the Connection object.
      if (connection != null) {

        // @A2D Class[] argClasses = new Class[] { JDDataSourceURL.class,
        // @A2D                                    JDProperties.class,
        // @A2D                                    AS400.class };
        // @A2D Object[] argValues = new Object[] { dataSourceUrl,
        // @A2D                                     jdProperties,
        // @A2D                                     as400 };
        // @A2D try {
          // Hand off the public AS400 object to keep it from getting
          // garbage-collected.
          Class clazz = connection.getClass ();          
          // @A2D Method method = clazz.getDeclaredMethod ("setSystem",
          // @A2D                                   new Class[] { AS400.class });
          // @A2D method.invoke (connection, new Object[] { as400 });

          // @A2D method = clazz.getDeclaredMethod ("setProperties", argClasses);
          // @A2D method.invoke (connection, argValues);

          String className = clazz.getName();
          if (className.equals("com.ibm.as400.access.AS400JDBCConnection")) {
              ((AS400JDBCConnection)connection).setSystem(as400);
              ((AS400JDBCConnection)connection).setProperties(dataSourceUrl, jdProperties, as400);
          }
          else if (className.equals("com.ibm.as400.access.JDConnectionProxy")) {
              ((JDConnectionProxy)connection).setSystem(as400);
              ((JDConnectionProxy)connection).setProperties(dataSourceUrl, jdProperties, as400);
          }
        // @A2D }
        // @A2D catch (NoSuchMethodException e) {
        // @A2D   JDTrace.logInformation (this,
        // @A2D                           "Could not resolve setProperties() method");
        // @A2D   throw new InternalErrorException (InternalErrorException.UNEXPECTED_EXCEPTION);
        // @A2D }
        // @A2D catch (IllegalAccessException e) {
        // @A2D   JDTrace.logInformation (this,
        // @A2D                           "Could not access setProperties() method");
        // @A2D   throw new InternalErrorException (InternalErrorException.UNEXPECTED_EXCEPTION);
        // @A2D }
        // @A2D catch (InvocationTargetException e) {
        // @A2D   Throwable e2 = e.getTargetException ();
        // @A2D   if (e2 instanceof SQLException)
        // @A2D     throw (SQLException) e2;
        // @A2D   else if (e2 instanceof RuntimeException)
        // @A2D     throw (RuntimeException) e2;
        // @A2D   else if (e2 instanceof Error)
        // @A2D     throw (Error) e2;
        // @A2D   else {
        // @A2D     JDTrace.logInformation (this,
        // @A2D                             "Could not invoke setProperties() method");
        // @A2D     throw new InternalErrorException (InternalErrorException.UNEXPECTED_EXCEPTION);
        // @A2D   }
        // @A2D }
      }

      return connection;
    }



/**
Indicates if the driver is a genuine JDBC compliant driver.

@return         Always true.
**/
    public boolean jdbcCompliant ()
    {
        return true;
    }



/**
Returns the name of the driver.

@return        The driver name.
**/
    public String toString ()
    {
        return DRIVER_NAME_;    // @D0C
    }



}