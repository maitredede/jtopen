///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: ClientAccessDataStream.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

// Base class for client access server data streams.  Provides methods to access common client access data stream header.
class ClientAccessDataStream extends DataStream
{
  private static final String copyright = "Copyright (C) 1997-2001 International Business Machines Corporation and others.";

  protected static final int HEADER_LENGTH = 20;

  private static final CADSPool basePool_ = new CADSPool(); //@P0A - base datastream pool

  boolean inUse_; //@P0A

  // Note: The following method is called by AS400ThreadedServer and AS400NoThreadServer.

  // Construct an appropriate client access data stream object.  Read from the InputStream to obtain the data stream data for the object.
  // @param  is  InputStream from which to read to obtain the data stream contents.
  // @param  dataStreams  Hashtable containing instances of data stream objects to receive into.  This table is searched first when a reply comes in.  If found the datastream will be removed from here as it is received.
  // @param  dataStreams  Prototypes Hashtable containing data stream objects from which to obtain a model for this object.
  // @exception  IOException  Data read from the input stream is less than 20 bytes or we are unable to read from the input stream for some other reason.
  // @return  ClientAccessDataStream object.
  static final ClientAccessDataStream construct(InputStream is, Hashtable dataStreams, Hashtable dataStreamPrototypes, AS400ImplRemote system, int connectionID) throws IOException
  {
    // Construct a client access data stream to receive the data stream header.  By using the default constructor for ClientAccessDataStream, we get a data stream of size HEADER_LENGTH.
    //@P0D ClientAccessDataStream baseDataStream = new ClientAccessDataStream();
    ClientAccessDataStream baseDataStream = basePool_.getUnusedStream(); //@P0A

    try
    {
      // Receive the header.
      if (readFromStream(is, baseDataStream.data_, 0, HEADER_LENGTH, connectionID) < HEADER_LENGTH)
      {
        if (Trace.traceOn_) Trace.log(Trace.ERROR, "Failed to read all of the data stream header."); //@P0C
        baseDataStream.inUse_ = false; //@P0A
        throw new ConnectionDroppedException(ConnectionDroppedException.CONNECTION_DROPPED);
      }

      if (baseDataStream.data_[6] != (byte)0xE0)
      {
          if (Trace.traceOn_) Trace.log(Trace.ERROR, "Incorrect data stream header detected.");
          baseDataStream.inUse_ = false;
          throw new InternalErrorException(InternalErrorException.DATA_STREAM_UNKNOWN);
      }

      // First look for an instance data stream.
      // If we found it remove it since instance datastreams are only used once.
      // Print is the only thing that uses this.
      ClientAccessDataStream newDataStream = (ClientAccessDataStream)dataStreams.remove(baseDataStream); //@P0C
      if (newDataStream == null) //@P0C
      {
        // If we couldn't find an instance datastream to receive into, look for a prototype data stream to generate one with.
        ClientAccessDataStream modelDataStream = (ClientAccessDataStream)dataStreamPrototypes.get(baseDataStream);

        if (modelDataStream == null)
        {
          // No model was found in the hash table, so we will return a generic data stream.
          newDataStream = new ClientAccessDataStream();
        }
        else
        {
          // Get a new instance of the data stream.
          newDataStream = (ClientAccessDataStream)modelDataStream.getNewDataStream();
          if (newDataStream == null)
          {
            newDataStream = new ClientAccessDataStream();
          }
        }
      }

      newDataStream.system_ = system;
      if (Trace.traceOn_) newDataStream.setConnectionID(connectionID);
      // Initialize the header section of the new data stream.
      newDataStream.data_ = new byte[baseDataStream.getLength()];
      System.arraycopy(baseDataStream.data_, 0, newDataStream.data_, 0, HEADER_LENGTH);

      if (newDataStream.data_.length - HEADER_LENGTH > 0)
      {
        // Receive any remaining bytes.
        newDataStream.readAfterHeader(is);
      }

      return newDataStream;
    }
    finally
    {
      baseDataStream.inUse_ = false;
    }
  }

  // Constructs an empty ClientAccessDataStream object.
  ClientAccessDataStream()
  {
    super(HEADER_LENGTH);
  }

  // Constructs a ClientAccessDataStream object.
  // @param  data  Byte array with which to initialize this data stream.
  ClientAccessDataStream(byte[] ds)
  {
    super(HEADER_LENGTH, ds);
  }

  // Retrieve the request correlation for this data stream.  The return value may be invalid if it has not been set.
  // @return  The request correlation number.
  int getCorrelation()
  {
    return get32bit(12);
  }

  // Retrieve the CS instance for the data stream.  The return value may be invalid if it has not been set.
  // @return  The CS instance of the data stream.
  int getCSInstance()
  {
    return get32bit(8);
  }

  // Retrieve the header ID for the data stream.  The return value may be invalid if it has not been set.
  // @return  The ID of the data stream.
  int getHeaderID()
  {
    return get16bit(4);
  }

  // Retrieve the total length of the data stream.  The return value may be invalid if it has not been set.
  // @return  The total length of this data stream.
  int getLength()
  {
    return get32bit(0);
  }

  // Retrieve the request/reply ID of the data stream.  The return value may be invalid if it has not been set.
  // @return  The request/reply ID of this data stream.
  int getReqRepID()
  {
    //@P0D return get16bit(18);
    return((data_[18] & 0xFF) << 8) + (data_[19] & 0xFF); //@P0A
  }

  // Retrieve the server ID of the data stream.  The return value may be invalid if it has not been set.
  // @return  The server ID of this data stream.
  int getServerID()
  {
    return get16bit(6);
  }

  // Retrieve the template length of the data stream.  The return value may be invalid if it has not been set.
  // @return  The template length of this data stream.
  int getTemplateLen()
  {
    return get16bit(16);
  }

  // Retrieve the hash code of the data stream.  The return value may be invalid if it has not been set.
  // Note:  Reply data stream sub-classes should override this method to return the request/reply id directly instead of calling getReqRepId().
  // @return  The hash code of this data stream.
  public int hashCode()
  {
    //@P0D return getReqRepID();
    return((data_[18] & 0xFF) << 8) + (data_[19] & 0xFF); //@P0A
  }

  // Set the request correlation for this data stream.
  // @param  id  The request correlation number.
  void setCorrelation(int id)
  {
    set32bit(id, 12);
  }

  // Set the CS instance for this data stream.
  // @param  id  The CS instance.
  void setCSInstance(int id)
  {
    set32bit(id, 8);
  }

  // Set the header ID for the data stream.  It should be set to 0 for most of the Client Access servers.
  // @param  id  The header ID to set.
  void setHeaderID(int id)
  {
    set16bit(id, 4);
  }

  // Set the length of the data stream.  This is the total length of the data stream.
  // @param  len  The length of the data stream.
  void setLength(int len)
  {
    //@P0D set32bit(len, 0);
    data_[0] = (byte)(len >>> 24);
    data_[1] = (byte)(len >>> 16);
    data_[2] = (byte)(len >>>  8);
    data_[3] = (byte) len;
  }

  // Set the request/reply ID for the data stream.
  // @param  id  The request/reply ID to set.
  void setReqRepID(int id)
  {
    set16bit(id, 18);
  }

  // Set the server ID for the data stream.  This is the ID of the server to talk to.
  // @param  id  The ID of the server.
  void setServerID(int id)
  {
    set16bit(id, 6);
  }

  // Set the template length for the data stream.
  // @param  len  The template length.
  void setTemplateLen(int len)
  {
    set16bit(len, 16);
  }
}