///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: UserListEnumeration.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Bin4;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import java.io.IOException;
import java.util.Enumeration;


/**
The UserListEnumeration class implements an enumeration for
user lists.
**/
class UserListEnumeration
      implements Enumeration
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";



    // Private data.
    private User[]                  array_          = null;
    private UserListParser          parser_         = null;
    private int                     index_          = 0;
    private int                     overallIndex_   = 0;
    private AS400                   system_         = null;



    UserListEnumeration (AS400 system, byte[] listInfoData, byte[] receiverData)
        throws IOException
    {
        system_ = system;
        parser_ = new UserListParser ();
        parser_.setSystem (system);
        parser_.parseLists (system, listInfoData, receiverData );
        array_ = parser_.getUserList();
    }



    /**
     * Copyright.
     **/
    private static String getCopyright ()
    {
        return Copyright.copyright;
    }

    int getLength ()
    {
        return parser_.totalRecs_;
    }



    public boolean hasMoreElements ()
    {
        return overallIndex_ < parser_.totalRecs_;
    }



    public Object nextElement ()
    {
        // If we have read them all, then return null.
        if (overallIndex_ >= parser_.totalRecs_)
            return null;

        // If the row is not cached, then read the next chunk.
        if (index_ >= array_.length) {
            try {
                parser_.more (system_);
                index_ = 0;
                array_ = parser_.getUserList ();

                // If this is the last element, then close the list.
                if (overallIndex_ == parser_.totalRecs_ - 1)
                    parser_.closeList (system_);
            }
            catch (AS400Exception e) {
                // Signal artificial end of list.
                overallIndex_ = parser_.totalRecs_;
                return null;
            }
        }

        Object element = array_[index_];
        ++index_;
        ++overallIndex_;
        return element;
    }

}