///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                              
//                                                                             
// Filename: JobEnumeration.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2002 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.util.*;

/**
 * Helper class. Used to wrap the Job[] with an Enumeration.
 * This class is used by JobList.
**/
class JobEnumeration implements Enumeration
{
  private static final String copyright = "Copyright (C) 1997-2002 International Business Machines Corporation and others.";

  private Job[] jobCache_;
  private JobList list_;
  private int counter_;
  private int numJobs_;
  private int listOffset_ = 0;
  private int cachePos_ = 0;

  JobEnumeration(JobList list, int length)
  {
    list_ = list;
    numJobs_ = length;
  }

  public final boolean hasMoreElements()
  {
    return counter_ < numJobs_;
  }

  public final Object nextElement()
  {
    if (counter_ >= numJobs_)
    {
      throw new NoSuchElementException();
    }

    if (jobCache_ == null || cachePos_ >= jobCache_.length)
    {
      try
      {
        jobCache_ = list_.getJobs(listOffset_, 1000);
        if (Trace.traceOn_)
        {
          Trace.log(Trace.DIAGNOSTIC, "Loaded next block in JobEnumeration: "+jobCache_.length+" messages at offset "+listOffset_+" out of "+numJobs_+" total.");
        }
      }
      catch (Exception e)
      {
        if (Trace.traceOn_)
        {
          Trace.log(Trace.ERROR, "Exception while loading nextElement() in UserEnumeration:", e);
        }
        throw new NoSuchElementException();
      }
      cachePos_ = 0;
      listOffset_ += jobCache_.length;
    }
    ++counter_;
    return jobCache_[cachePos_++];
  }
}


