///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: JDBCResultSet.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.micro;

import java.io.*;
import com.ibm.as400.access.MEConstants;

/**
 The JDBCResultSet class represents the results of executing an  SQL statement.  This class provides access 
 to a table of data generated by a JDBCCommand.
 
 <P>The following example demonstrates the use of JDBCResultSet:
 <br>
 <pre>
    AS400 system = new AS400("mySystem", "myUserid", "myPwd", "myMEServer");
    try
    {
        // Execute a JDBC statement.
        JDBCResultSet rs = JDBCCommand.execute(system, "select * from qiws.qcustcdt");
        
        // Get the fist row of the result set.
        String[] columns = rs.next();
        
        // While there are more rows, continue to call next() and
        // print out the selected columns.
        while (columns != null)
        {
            System.out.println(columns[2]+" "+columns[3]);
            columns = rs.next();
        }
        
        // Close the result set.
        rs.close();
    }
    catch (Exception e)
    {
        System.out.println("JDBCResultSet issued an exception!");
        e.printStackTrace();
    }
    // Done with the system.
    system.disconnect();
 </pre>
 **/
public final class JDBCResultSet
{
  private static final String copyright = "Copyright (C) 1997-2001 International Business Machines Corporation and others.";

    private AS400 system_ = null;
    private int transactionID_ = -1;


    /**
     *  Construct a JDBCResultSet object with the specified <i>system</i> and transaction <i>id</i>.
     **/
    JDBCResultSet(AS400 system, int id)
    {
        system_ = system;
        transactionID_ = id;
    }


    /**
     *  Get the row data from an absolute <i>rowNumber</i>.
     *
     *  @param rowNumber - The absolute row number. If the absolute row number is positive, this positions
     *                                  the cursor with respect to the beginning of the result set. If the absolute row number is negative,
     *                                  this positions the cursor with respect to the end of result set.
     *
     *  @exception  IOException  If an error occurs while communicating with the system.
     *  @exception  MEException  If an error occurs while processing the ToolboxME request.
     *
     *  @return The column data for the row.
     **/
    public String[] absolute(int rowNumber) throws IOException, MEException
    {
        return positionCursor(MEConstants.JDBC_ABSOLUTE);
    }


    /**
     *  Close the JDBCResultSet.
     *
     *  @exception  IOException  If an error occurs while communicating with the system.
     *  @exception  MEException  If an error occurs while processing the ToolboxME request.
     **/
    public void close() throws IOException, MEException
    {
        if (system_ == null)
            return;

        synchronized(system_)
        {
            system_.toServer_.writeInt(MEConstants.JDBC_CLOSE);
            system_.toServer_.writeInt(transactionID_);
            system_.toServer_.flush();

            int retVal = system_.fromServer_.readInt();

            if (retVal != MEConstants.JDBC_RESULT_SET_CLOSED)
                throw new MEException(system_.fromServer_.readUTF(), retVal);

            system_ = null; // Invalidate this result set object.
        }
    }


    /**
     *  Get the next row of data.
     *
     *  @exception  IOException  If an error occurs while communicating with the system.
     *  @exception  MEException  If an error occurs while processing the ToolboxME request.
     *
     *  @return The column data for the row.
     **/
    public String[] next() throws IOException, MEException
    {
        return positionCursor(MEConstants.JDBC_NEXT);
    }

    
    /**
     *  Get the previous row of data.
     *
     *  @exception  IOException  If an error occurs while communicating with the system.
     *  @exception  MEException  If an error occurs while processing the ToolboxME request.
     *
     *  @return The column data for the row.
     **/
    public String[] previous() throws IOException, MEException
    {
        return positionCursor(MEConstants.JDBC_PREVIOUS);
    }


    /**
     *  Get the result set data based on the requested action.  The
     *  actions can be next, previous, or absolute.
     *
     *  @param action The cursor position.
     *
     *  @exception  IOException  If an error occurs while communicating with the system.
     *  @exception  MEException  If an error occurs while processing the ToolboxME request.
     *
     *  @return The column data for the row.
     **/
    private String[] positionCursor(int action) throws IOException, MEException
    {
        if (system_ == null)
            throw new MEException("Result set is closed.", MEException.RESULT_SET_CLOSED);

        synchronized(system_)
        {
            system_.toServer_.writeInt(action);
            system_.toServer_.writeInt(transactionID_);

            system_.toServer_.flush();

            int retVal = system_.fromServer_.readInt();

            if (retVal == MEConstants.JDBC_NEXT_RECORD)
            {
                int numFields = system_.fromServer_.readInt();
                String[] fields = new String[numFields];

                for (int i=0; i<fields.length; ++i)
                {
                    fields[i] = system_.fromServer_.readUTF();
                }

                return fields;
            }
            else if (retVal == MEConstants.JDBC_POSITION_CURSOR_FAILED)
            {
                return null;
            }
            else
            {
                throw new MEException(system_.fromServer_.readUTF(), retVal);
            }
        }
    }
}


