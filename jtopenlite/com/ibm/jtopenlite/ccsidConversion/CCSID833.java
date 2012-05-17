///////////////////////////////////////////////////////////////////////////////
//
// JTOpen (IBM Toolbox for Java - OSS version)
//
// Filename:  CCSID833.java
//
// The source code contained herein is licensed under the IBM Public License
// Version 1.0, which has been approved by the Open Source Initiative.
// Copyright (C) 2011-2012 International Business Machines Corporation and
// others.  All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.ibm.jtopenlite.ccsidConversion;

public class CCSID833 implements SingleByteConversion{
  static CCSID833 singleton = new CCSID833();
  
  public static SingleByteConversion getInstance() {
    return singleton;
  }
  
  public int getCcsid() {
    return 833;
  }
  
  public byte[] returnFromUnicode() {
    return fromUnicode_;
  }
  
  public char[] returnToUnicode() {
    return toUnicode_;
  }
  private static final char[] toUnicode_ = { 
    '\u0000','\u0001','\u0002','\u0003','\u009C','\u0009','\u0086','\u007F','\u0097','\u008D','\u008E','\u000B','\u000C',(char)0xD,'\u000E','\u000F',
    '\u0010','\u0011','\u0012','\u0013','\u009D','\u0085','\u0008','\u0087','\u0018','\u0019','\u0092','\u008F','\u001C','\u001D','\u001E','\u001F',
    '\u0080','\u0081','\u0082','\u0083','\u0084',(char)0xA,'\u0017','\u001B','\u0088','\u0089','\u008A','\u008B','\u008C','\u0005','\u0006','\u0007',
    '\u0090','\u0091','\u0016','\u0093','\u0094','\u0095','\u0096','\u0004','\u0098','\u0099','\u009A','\u009B','\u0014','\u0015','\u009E','\u001A',
    '\u0020','\u001A','\uFFA0','\uFFA1','\uFFA2','\uFFA3','\uFFA4','\uFFA5','\uFFA6','\uFFA7','\u00A2','\u002E','\u003C','\u0028','\u002B','\u007C',
    '\u0026','\u001A','\uFFA8','\uFFA9','\uFFAA','\uFFAB','\uFFAC','\uFFAD','\uFFAE','\uFFAF','\u0021','\u0024','\u002A','\u0029','\u003B','\u00AC',
    '\u002D','\u002F','\uFFB0','\uFFB1','\uFFB2','\uFFB3','\uFFB4','\uFFB5','\uFFB6','\uFFB7','\u00A6','\u002C','\u0025','\u005F','\u003E','\u003F',
    '\u005B','\u001A','\uFFB8','\uFFB9','\uFFBA','\uFFBB','\uFFBC','\uFFBD','\uFFBE','\u0060','\u003A','\u0023','\u0040',(char)0x27,'\u003D','\u0022',
    '\u005D','\u0061','\u0062','\u0063','\u0064','\u0065','\u0066','\u0067','\u0068','\u0069','\uFFC2','\uFFC3','\uFFC4','\uFFC5','\uFFC6','\uFFC7',
    '\u001A','\u006A','\u006B','\u006C','\u006D','\u006E','\u006F','\u0070','\u0071','\u0072','\uFFCA','\uFFCB','\uFFCC','\uFFCD','\uFFCE','\uFFCF',
    '\u203E','\u007E','\u0073','\u0074','\u0075','\u0076','\u0077','\u0078','\u0079','\u007A','\uFFD2','\uFFD3','\uFFD4','\uFFD5','\uFFD6','\uFFD7',
    '\u005E','\u001A',(char)0x5C,'\u001A','\u001A','\u001A','\u001A','\u001A','\u001A','\u001A','\uFFDA','\uFFDB','\uFFDC','\u001A','\u001A','\u001A',
    '\u007B','\u0041','\u0042','\u0043','\u0044','\u0045','\u0046','\u0047','\u0048','\u0049','\u001A','\u001A','\u001A','\u001A','\u001A','\u001A',
    '\u007D','\u004A','\u004B','\u004C','\u004D','\u004E','\u004F','\u0050','\u0051','\u0052','\u001A','\u001A','\u001A','\u001A','\u001A','\u001A',
    '\u20A9','\u001A','\u0053','\u0054','\u0055','\u0056','\u0057','\u0058','\u0059','\u005A','\u001A','\u001A','\u001A','\u001A','\u001A','\u001A',
    '\u0030','\u0031','\u0032','\u0033','\u0034','\u0035','\u0036','\u0037','\u0038','\u0039','\u001A','\u001A','\u001A','\u001A','\u001A','\u009F',};


  private static byte[]  fromUnicode_ = null;  
  /* dynamically generate the inverse table */
  static { 
      fromUnicode_ = SingleByteConversionTable.generateFromUnicode(toUnicode_);
  }

}
