///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                              
//                                                                             
// Filename: ConvTable420.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2002 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

class ConvTable425 extends ConvTableBidiMap
{
  private static final String copyright = "Copyright (C) 1997-2002 International Business Machines Corporation and others.";

  private static final String toUnicode_ = 
    "\u0000\u0001\u0002\u0003\u009C\t\u0086\u007F\u0097\u008D\u008E\u000B\f\r\u000E\u000F" +
    "\u0010\u0011\u0012\u0013\u009D\u0085\b\u0087\u0018\u0019\u0092\u008F\u001C\u001D\u001E\u001F" +
    "\u0080\u0081\u0082\u0083\u0084\n\u0017\u001B\u0088\u0089\u008A\u008B\u008C\u0005\u0006\u0007" +
    "\u0090\u0091\u0016\u0093\u0094\u0095\u0096\u0004\u0098\u0099\u009A\u009B\u0014\u0015\u009E\u001A" +
    "\u0020\u00A0\u00E2\u060C\u00E0\u061B\u0640\u061F\u00E7\u0621\u0622\u002E\u003C\u0028\u002B\u007C" +
    "\u0026\u00E9\u00EA\u00EB\u00E8\u0623\u00EE\u00EF\u0624\u0625\u0021\u0024\u002A\u0029\u003B\u005E" +
    "\u002D\u002F\u00C2\u0626\u00C0\u0627\u0628\u0629\u00C7\u062A\u062B\u002C\u0025\u005F\u003E\u003F" +
    "\u062C\u00C9\u00CA\u00CB\u00C8\u062D\u00CE\u00CF\u062E\u0060\u003A\u0023\u0040\'\u003D\"" +
    "\u062F\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u00AB\u00BB\u0630\u0631\u0632\u0633" +
    "\u0634\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0635\u0636\u00E6\u0637\u00C6\u20AC" +
    "\u00B5\u007E\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0638\u0639\u063A\u005B\u0641\u0642" +
    "\u0643\u0644\u0645\u0646\u00A9\u00A7\u0647\u0152\u0153\u0178\u0648\u0649\u064A\u005D\u064B\u00D7" +
    "\u007B\u0041\u0042\u0043\u0044\u0045\u0046\u0047\u0048\u0049\u00AD\u00F4\u064C\u064D\u064E\u064F" +
    "\u007D\u004A\u004B\u004C\u004D\u004E\u004F\u0050\u0051\u0052\u0650\u00FB\u00FC\u00F9\u0651\u00FF" +
    "\\\u00F7\u0053\u0054\u0055\u0056\u0057\u0058\u0059\u005A\u0652\u00D4\u200C\u200D\u200E\u200F" +
    "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037\u0038\u0039\u001A\u00DB\u00DC\u00D9\u00A4\u009F";


  private static final String fromUnicode_ = 
    "\u0001\u0203\u372D\u2E2F\u1605\u250B\u0C0D\u0E0F\u1011\u1213\u3C3D\u3226\u1819\u3F27\u1C1D\u1E1F" +
    "\u405A\u7F7B\u5B6C\u507D\u4D5D\u5C4E\u6B60\u4B61\uF0F1\uF2F3\uF4F5\uF6F7\uF8F9\u7A5E\u4C7E\u6E6F" +
    "\u7CC1\uC2C3\uC4C5\uC6C7\uC8C9\uD1D2\uD3D4\uD5D6\uD7D8\uD9E2\uE3E4\uE5E6\uE7E8\uE9AD\uE0BD\u5F6D" +
    "\u7981\u8283\u8485\u8687\u8889\u9192\u9394\u9596\u9798\u99A2\uA3A4\uA5A6\uA7A8\uA9C0\u4FD0\uA107" +
    "\u2021\u2223\u2415\u0617\u2829\u2A2B\u2C09\u0A1B\u3031\u1A33\u3435\u3608\u3839\u3A3B\u0414\u3EFF" +
    "\u413F\u3F3F\uFE3F\u0000\u0006\u3FB5\uB48A\uCA3F\u3F3F\uA03F\u3F8B\u3F3F\u643F\u623F\u3F3F\u9E68" +
    "\u7471\u7273\u3F3F\u7677\u3F3F\u3F3F\uEB3F\u3FBF\u3FFD\u3FFB\uFC3F\u3F3F\u443F\u423F\u3F3F\u9C48" +
    "\u5451\u5253\u3F3F\u5657\u3F3F\u3F3F\uCB3F\u3FE1\u3FDD\u3FDB\uDC3F\u3FDF\uFFFF\u0029\u3F3F\uB7B8" +
    "\uFFFF\u0012\u3F3F\uB93F\uFFFF\u0249\u3F3F\u433F\uFFFF\u0006\u3F3F\u3F45\u3F3F\u3F47\u3F49\u4A55" +
    "\u5859\u6365\u6667\u696A\u7075\u7880\u8C8D\u8E8F\u909A\u9B9D\uAAAB\uAC3F\u3F3F\u3F3F\u46AE\uAFB0" +
    "\uB1B2\uB3B6\uBABB\uBCBE\uCCCD\uCECF\uDADE\uEA3F\uFFFF\u0006\u3F3F\uF0F1\uF2F3\uF4F5\uF6F7\uF8F9" +
    "\u6C6B\u4B5C\uFFFF\u0CCF\u3F3F\uECED\uEEEF\uFFFF\u004E\u3F3F\u9F3F\uFFFF\u6EE1\u3F3F\uBEBE\uCC3F" +
    "\uCD3F\uCECE\uCFCF\uDADA\uDEDE\uEAEA\u494A\u4A55\u5558\u5859\u5963\u6363\u6365\u6566\u6666\u6667" +
    "\u6769\u6969\u696A\u6A6A\u6A70\u7070\u7075\u7575\u7578\u7878\u7880\u808C\u8C8D\u8D8E\u8E8F\u8F8F" +
    "\u8F90\u9090\u909A\u9A9A\u9A9B\u9B9B\u9B9D\u9D9D\u9DAA\uAAAA\uAAAB\uABAB\uABAC\uACAC\uACAE\uAEAE" +
    "\uAEAF\uAFAF\uAFB0\uB0B0\uB0B1\uB1B1\uB1B2\uB2B2\uB2B3\uB3B3\uB3B6\uB6B6\uB6BA\uBABB\uBBBC\uBCBC" +
    "\uBC3F\uFFFF\u0005\u3F3F\u3F5A\u7F7B\u5B3F\u507D\u4D5D\u3F4E\u6B60\u4B61\uF0F1\uF2F3\uF4F5\uF6F7" +
    "\uF8F9\u7A5E\u4C7E\u6E6F\u7CC1\uC2C3\uC4C5\uC6C7\uC8C9\uD1D2\uD3D4\uD5D6\uD7D8\uD9E2\uE3E4\uE5E6" +
    "\uE7E8\uE9AD\uE0BD\u5F6D\u7981\u8283\u8485\u8687\u8889\u9192\u9394\u9596\u9798\u99A2\uA3A4\uA5A6" +
    "\uA7A8\uA9C0\u4FD0\uA13F\uFFFF\u0050\u3F3F";


  ConvTable425()
  {
    super(425, toUnicode_.toCharArray(), fromUnicode_.toCharArray());
  }
}
