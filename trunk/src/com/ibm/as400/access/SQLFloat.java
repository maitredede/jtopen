///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: SQLFloat.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;



class SQLFloat
implements SQLData
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";




    private String x = Copyright.copyright;


    // Private data.
    private SQLConversionSettings   settings_;
    private int                     truncated_;
	private double	                value_;


    SQLFloat (SQLConversionSettings settings)
    {
        settings_   = settings;
        truncated_  = 0;
        value_      = 0.0d;
    }



    public Object clone ()
    {
        return new SQLFloat (settings_);
    }



//---------------------------------------------------------//
//                                                         //
// CONVERSION TO AND FROM RAW BYTES                        //
//                                                         //
//---------------------------------------------------------//



    public void convertFromRawBytes (byte[] rawBytes, int offset, ConverterImplRemote ccsidConverter)
        throws SQLException
    {
        value_ = BinaryConverter.byteArrayToDouble(rawBytes, offset);                   // @D0C
	}



    public void convertToRawBytes (byte[] rawBytes, int offset, ConverterImplRemote ccsidConverter)
        throws SQLException
    {
        BinaryConverter.doubleToByteArray(value_, rawBytes, offset);                    // @D0C
    }



//---------------------------------------------------------//
//                                                         //
// SET METHODS                                             //
//                                                         //
//---------------------------------------------------------//



    public void set (Object object, Calendar calendar, int scale)
        throws SQLException
    {
        truncated_ = 0;

        if (object instanceof String) {
            try {
                value_ = Double.valueOf ((String) object).doubleValue ();
                int objectLength = ((String) object).length ();
                int valueLength = Double.toString (value_).length ();
                if (valueLength < objectLength)
                    truncated_ = objectLength - valueLength;
            }
            catch (NumberFormatException e) {
                JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
            }
        }

        else if (object instanceof BigDecimal) {
            value_ = ((BigDecimal) object).doubleValue ();
            int objectLength = SQLDataFactory.getPrecision ((BigDecimal) object);
            int valueLength = SQLDataFactory.getPrecision (new BigDecimal (value_));
            if (valueLength < objectLength)
                truncated_ = objectLength - valueLength;
        }

        else if (object instanceof Number)
            value_ = ((Number) object).doubleValue();

        else if (object instanceof Boolean)
            value_ = (((Boolean) object).booleanValue() == true) ? 1d : 0d;

        else
            JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
    }



//---------------------------------------------------------//
//                                                         //
// DESCRIPTION OF SQL TYPE                                 //
//                                                         //
//---------------------------------------------------------//



    public String getCreateParameters ()
    {
        return null;
    }


    public int getDisplaySize ()
    {
        return 22;
    }


    public String getLiteralPrefix ()
    {
        return null;
    }


    public String getLiteralSuffix ()
    {
        return null;
    }


    public String getLocalName ()
    {
        return "FLOAT";
    }


    public int getMaximumPrecision ()
    {
        return 15;
    }


    public int getMaximumScale ()
    {
        return 0;
    }


    public int getMinimumScale ()
    {
        return 0;
    }


    public int getNativeType ()
    {
        return 480;
    }


    public int getPrecision ()
    {
        return 15;
    }


    public int getRadix ()
    {
        return 10;
    }


    public int getScale ()
    {
        return 0;
    }


	public int getType ()
	{
		return java.sql.Types.FLOAT;
	}



	public String getTypeName ()
	{
		return "FLOAT";
	}


    public boolean isGraphic ()
    {
        return false;
    }



    public boolean isSigned ()
    {
        return true;
    }



    public boolean isText ()
    {
        return false;
    }



//---------------------------------------------------------//
//                                                         //
// CONVERSIONS TO JAVA TYPES                               //
//                                                         //
//---------------------------------------------------------//



    public int getActualSize ()
    {
        return SQLDataFactory.getPrecision (Double.toString (value_));
    }



    public int getTruncated ()
    {
        return truncated_;
    }



	public InputStream toAsciiStream ()
	    throws SQLException
	{
		JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public BigDecimal toBigDecimal (int scale)
	    throws SQLException
	{
	    // Convert the value to a String before creating the
	    // BigDecimal.  This will create the exact BigDecimal
	    // that we want.  If you pass the value directly to
	    // BigDecimal, then the value is not exact, and the
	    // scale becomes bigger than expected.

        // @A0A
        // Modified the code to deal with numbers in scientific
        // notations. The numbers that are in scientific notation
        // are parsed to a base (the part before 'E') and an
        // exponent (the part after 'E'). The base is then used
        // to construct the BigDecimal object and then the exponent
        // is used to shift the decimal point to its rightful place.

        BigDecimal bigDecimal = null;                                           // @A0A

        String numString = Double.toString(value_);                             // @A0A
        int eIndex = numString.indexOf("E");                                    // @A0A
        if (eIndex == -1) {                                                     // @A0A
            bigDecimal = new BigDecimal(numString);                             // @A0A
        }                                                                       // @A0A
        else {                                                                  // @A0A
            String base = numString.substring(0, eIndex);                       // @A0A
            int exponent = Integer.parseInt(numString.substring(eIndex+1));     // @A0A
            bigDecimal = new BigDecimal(base);                                  // @A0A
            bigDecimal = bigDecimal.movePointRight(exponent);                   // @A0A
        }                                                                       // @A0A

        if (scale >= 0) {
            if (scale >= bigDecimal.scale()) {
                truncated_ = 0;
                return bigDecimal.setScale (scale);
            }
            else {
                truncated_ = bigDecimal.scale() - scale;
                return bigDecimal.setScale (scale, BigDecimal.ROUND_HALF_UP);
            }
        }
        else
            return bigDecimal;
	}



	public InputStream toBinaryStream ()
	    throws SQLException
	{
		JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public Blob toBlob ()
	    throws SQLException
	{
		JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public boolean toBoolean ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (value_ != 0.0d);
	}



	public byte toByte ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (byte) value_;
	}



	public byte[] toBytes ()
	    throws SQLException
	{
	    JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public Reader toCharacterStream ()
	    throws SQLException
	{
		JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public Clob toClob ()
	    throws SQLException
	{
		JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public Date toDate (Calendar calendar)
	    throws SQLException
	{
	    JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public double toDouble ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (double) value_;
	}



	public float toFloat ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (float) value_;
	}



	public int toInt ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (int) value_;
	}



	public long toLong ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (long) value_;
	}



	public Object toObject ()
	{
	    truncated_ = 0;
	    return new Double (value_);
	}




	public short toShort ()
	    throws SQLException
	{
	    truncated_ = 0;
	    return (short) value_;
	}



	public String toString ()
	{
	    truncated_ = 0;
	    String stringRep = Double.toString (value_);
	    int decimal = stringRep.indexOf ('.');
	    if (decimal == -1)
	        return stringRep;
	    else
    	    return stringRep.substring (0, decimal)
    	        + settings_.getDecimalSeparator()
    	        + stringRep.substring (decimal+1);
	}



	public Time toTime (Calendar calendar)
	    throws SQLException
	{
	    JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public Timestamp toTimestamp (Calendar calendar)
	    throws SQLException
	{
	    JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



	public InputStream	toUnicodeStream ()
	    throws SQLException
	{
	    JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
  		return null;
	}



}
