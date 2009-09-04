///////////////////////////////////////////////////////////////////////////////
//
// JTOpen (IBM Toolbox for Java - OSS version)
//
// Filename:  RemoteCommandImplNative.java
//
// The source code contained herein is licensed under the IBM Public License
// Version 1.0, which has been approved by the Open Source Initiative.
// Copyright (C) 1999-2007 International Business Machines Corporation and
// others.  All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
//
// @A1 - 9/18/2007 - Changes to follow proxy command chain.
//
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.IOException;
import java.util.StringTokenizer;


// The RemoteCommandImplNative class is the native implementation of CommandCall and ProgramCall.
class RemoteCommandImplNative extends RemoteCommandImplRemote
{
  private static final String CLASSNAME = "com.ibm.as400.access.RemoteCommandImplNative";
  static
  {
    if (Trace.traceOn_) Trace.logLoadPath(CLASSNAME);
  }

    static
    {
        System.load("/QSYS.LIB/QYJSPART.SRVPGM");
    }

    protected void open(boolean threadSafety) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
      if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native implementation object open.");
      if (!threadSafety)
      {
        if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Sending native open request to super class.");
        super.open(false);
        return;
      }

      // If converter was not set with a user override ccsid, set converter to job ccsid.
      if (!ccsidIsUserOveride_)
      {
        converter_ = ConverterImplRemote.getConverter(system_.getCcsid(), system_);
      }
      if (AS400.nativeVRM.vrm_ >= 0x00050300)
      {
        if (AS400.nativeVRM.vrm_ >= 0x00060100)
        {
          serverDataStreamLevel_ = 10;
          unicodeConverter_ = ConverterImplRemote.getConverter(1200, system_);
        }
        else
        {
          serverDataStreamLevel_ = 7;
        }
      }

      // Set the secondary language library on the server.
      if (system_.isMustAddLanguageLibrary() &&
          !system_.isSkipFurtherSettingOfLanguageLibrary()) // see if we should try
      {
        // Note: If we were going through the Remote Command Host Server, the host server would set the secondary language library for us.
        // Since we're not using the host server, we need to handle this ourselves.
        // We need to do this on every open, since several different threads may be using this RemoteCommandImpl object.

        // Retrieve the name of the secondary language library (if any).
        String secLibName = retrieveSecondaryLanguageLibName();  // never returns null
        // Set the NLV on server to match the client's locale.
        if (secLibName.length() != 0)
        {
          setNlvOnServer(secLibName);
        }
        // Retain result, to avoid repeated lookups for same system object.
        system_.setLanguageLibrary(secLibName);
        // Set to non-null, to indicate we already looked-up the value.
      }

    }


    // Retrieves the secondary language library (if any).
    // If fail to retrieve library name, or name is blank, returns "".
    private String retrieveSecondaryLanguageLibName()
    {
      String secLibName = system_.getLanguageLibrary();
      if (secLibName == null)  // 'null' implies not already looked-up
      {
        String clientNLV = system_.getNLV(); // NLV of client (based on locale)
        try
        {
          int ccsid = system_.getCcsid();
          ConvTable conv = ConvTable.getTable(ccsid, null);

          ProgramParameter[] parameterList = new ProgramParameter[6];
          int len = 108+10; // length of PRDR0100, plus first 10 bytes of PRDR0200
          parameterList[0] = new ProgramParameter(len); // receiver variable - PRDR0100 plus first 10 bytes of PRDR0200
          parameterList[1] = new ProgramParameter(BinaryConverter.intToByteArray(len)); // length of receiver variable
          parameterList[2] = new ProgramParameter(conv.stringToByteArray("PRDR0200")); // format name

          byte[] productInfo = new byte[36];  // product information
          AS400Text text4 = new AS400Text(4, ccsid, system_);
          AS400Text text6 = new AS400Text(6, ccsid, system_);
          AS400Text text7 = new AS400Text(7, ccsid, system_);
          AS400Text text10 = new AS400Text(10, ccsid, system_);
          text7.toBytes("*OPSYS", productInfo, 0);  // product ID
          text6.toBytes("*CUR", productInfo, 7);  // release level
          text4.toBytes("0000", productInfo, 13);  // product option
          text10.toBytes(clientNLV, productInfo, 17); // load ID (specifies desired NLV)
          BinaryConverter.intToByteArray(36, productInfo, 28);  // length of product information parm
          BinaryConverter.intToByteArray(ccsid, productInfo, 32);  // ccsid for returned directory
          parameterList[3] = new ProgramParameter(productInfo); // product information
          parameterList[4] = new ProgramParameter(new byte[4]); // error code
          parameterList[5] = new ProgramParameter(conv.stringToByteArray("PRDI0200")); // product information format name

          // Call QSZRTVPR (Change System Library List) to add the library for the secondary language.
          // Note: QSZRTVPR is documented as non-threadsafe. However, the API owner has indicated that this API will never alter the state of the system, and that it cannot damage the system; so it can safely be called on-thread.
          boolean succeeded = runProgram("QSYS", "QSZRTVPR", parameterList, true, AS400Message.MESSAGE_OPTION_UP_TO_10, true);
          // Note: This method is only called from within open().
          // The final parm indicates that the on-thread open() has already been done (on this thread).

          if (!succeeded)
          {
            Trace.log(Trace.WARNING, "Unable to retrieve secondary language library name for NLV " + clientNLV, new AS400Exception(messageList_));
          }
          else
          {
            byte[] outputData = parameterList[0].getOutputData();
            int offsetToAddlInfo = BinaryConverter.byteArrayToInt(outputData, 84);
            secLibName = conv.byteArrayToString(outputData, offsetToAddlInfo, 10).trim();
            if (secLibName.length() == 0) {
              Trace.log(Trace.WARNING, "Unable to retrieve secondary language library name for NLV " + clientNLV + ": Blank library name returned.");
            }
          }
        }
        catch (Throwable t) {
          Trace.log(Trace.WARNING, "Unable to retrieve secondary language library name for NLV " + clientNLV, t);
        }
      }

      return (secLibName == null ? "" : secLibName);
    }

    // Sets the NLV (for the current thread) on the server, so that system msgs are returned in correct language.
    private void setNlvOnServer(String secondaryLibraryName)
    {
      if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native implementation object setting national language for messages.");
      try
      {
        // Call CHGSYSLIBL (Change System Library List) to add the library for the secondary language.
        // Note: According to the spec, CHGSYSLIBL "changes the system portion of the library list for the current thread".
        // Prior to V6R1, CHGSYSLIBL is documented as non-threadsafe.  However, the CL owner has indicated that this CL has actually been threadsafe all along, and that it cannot damage the system; so it can safely be called on-thread.
        // At worst, if system value QMLTTHDACN == 3, the system will simply refuse to execute the command.  In which case, the secondary language library won't get added.
        String cmd = "QSYS/CHGSYSLIBL LIB("+secondaryLibraryName+") OPTION(*ADD)";
        boolean succeeded = runCommand(cmd, true, AS400Message.MESSAGE_OPTION_UP_TO_10, true);
        // Note: This method is only called from within open().
        // The final parm indicates that the on-thread open() has already been done (on this thread).

        if (!succeeded)
        {
          if (messageList_.length !=0)
          {
            if (messageList_[0].getID().equals("CPF2103")) // lib is already in list
            {
              // Tolerate this error.  It means that we're good to go.
              // If this is the very native open() for this system_, set flag to indicate that the lib is already in list by default.  This will eliminate clutter in the job log, from subsequent attempts to set it.
              if (system_.getLanguageLibrary() == null) { // null implies first native open
                system_.setSkipFurtherSettingOfLanguageLibrary(); // don't keep trying on subsequent open's
              }
            }
            else if (messageList_[0].getID().equals("CPD0032")) // not auth'd to call CHGSYSLIBL
            {
              system_.setSkipFurtherSettingOfLanguageLibrary(); // don't keep trying on subsequent open's
              Trace.log(Trace.DIAGNOSTIC, "Profile " + system_.getUserId() + " not authorized to use CHGSYSLIBL to add secondary language library " + secondaryLibraryName + " to liblist.");
              // Note: The Remote Command Host Server runs this command under greater authority.
            }
            else if (messageList_[0].getID().equals("CPF2110")) // library not found
            {
              system_.setSkipFurtherSettingOfLanguageLibrary();  // don't keep trying on subsequent open's
              Trace.log(Trace.WARNING, "Secondary language library " + secondaryLibraryName + " was not found.");
            }
            else
            {
              Trace.log(Trace.ERROR, "Unable to add secondary language library " + secondaryLibraryName + " to library list.", new AS400Exception(messageList_));
            }
          }
          else  // no system messages returned
          {
            Trace.log(Trace.WARNING, "Unable to add secondary language library " + secondaryLibraryName + " to library list.");
          }
        }
      }
      catch (Throwable t)
      {
        Trace.log(Trace.WARNING, "Failed to add secondary language library " + secondaryLibraryName + " to library list.", t);
      }
    }

    // Indicates whether or not the command will be considered thread-safe.
    // @return  true if the command is declared to be thread-safe; false otherwise.
    public boolean isCommandThreadSafe(String command) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
        if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native implementation object checking command thread safety.");
        open(true);

        // Isolate out the command name from the argument(s), as the first token.
        StringTokenizer tokenizer = new StringTokenizer(command);
        String cmdLibAndName = tokenizer.nextToken().toUpperCase();
        String libName;
        String cmdName;
        // If there's a slash, parse out the library/commandName.
        int slashPos = cmdLibAndName.indexOf('/');
        if (slashPos == -1)  // No slash.
        {
            libName = "*LIBL";
            cmdName = cmdLibAndName;
        }
        else
        {
            libName = cmdLibAndName.substring(0, slashPos);
            cmdName = cmdLibAndName.substring(slashPos + 1);
        }

        // Fill the commandname array with blanks.
        byte[] commandName = {(byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40};
        // The first 10 characters contain the name of the command.
        converter_.stringToByteArray(cmdName, commandName);
        // The second 10 characters contain the name of the library where the command is located.
        converter_.stringToByteArray(libName, commandName, 10);

        // Set up the parameter list for the program that we will use to retrieve the command information (QCDRCMDI).
        // First parameter:  receiver variable - output - char(*).
        // Second parameter:  length of receiver variable - input - binary(4).
        // Third parameter:  format name - input - char(8).
        // Set to EBCDIC "CMDI0100".
        // Fourth parameter:  qualified command name - input - char(20).
        // Fifth parameter:  error code - input/output - char(*).
        // Eight bytes of zero's indicates to throw exceptions.
        // Send as input because we are not interested in the output.
        // Sixth parameter:  optional - follow proxy chain - input - char(1)							//@A1A
		// Set to 1 - If the specified command is a proxy command, follow the proxy command 			//@A1A
		// chain to the target non-proxy command and retrieve information for the target command. 		//@A1A
		// If the command is not a proxy command, retrieve information for the specified command. 		//@A1A
		int numParms;
		if ((AS400.nativeVRM.vrm_ >= 0x00060100) ||
		   (AS400.nativeVRM.vrm_ >= 0x00050400 && !system_.isMissingPTF())) {
		   numParms = 6;	// @A1C - added support for proxy commands
		}
		else numParms = 5;

		ProgramParameter[] parameterList = new ProgramParameter[numParms];
		parameterList[0] = new ProgramParameter(350);
		parameterList[1] = new ProgramParameter(new byte[] { 0x00, 0x00, 0x01, 0x5e });
		parameterList[2] = new ProgramParameter(new byte[] { (byte) 0xC3, (byte) 0xD4, (byte) 0xC4, (byte) 0xC9, (byte) 0xF0, (byte) 0xF1, (byte) 0xF0, (byte) 0xF0 });
		parameterList[3] = new ProgramParameter(commandName);
		parameterList[4] = new ProgramParameter(new byte[8]);
		if (numParms > 5)											//@A1A
			parameterList[5] = new ProgramParameter(new byte[] { (byte) 0xF1 });		//@A1A

        try
        {
          // Retrieve command information.  Failure is returned as a message list.
          boolean succeeded = runProgram("QSYS", "QCDRCMDI", parameterList, true, AS400Message.MESSAGE_OPTION_UP_TO_10, true);
          if (!succeeded)
          {
            // If the exception is "MCH0802: Total parameters passed does not match number required" and we're running to V5R4, that means that the user hasn't applied PTF SI29629.  In that case, we will re-issue the program call, minus the new "follow proxy chain" parameter.
            if (numParms > 5 &&
                AS400.nativeVRM.vrm_ < 0x00060100 && AS400.nativeVRM.vrm_ >= 0x00050400 &&
                messageList_[messageList_.length - 1].getID().equals("MCH0802"))
            {
              if (Trace.traceOn_) Trace.log(Trace.WARNING, "PTF SI29629 is not installed: (MCH0802) " + messageList_[messageList_.length - 1].getText());
              // Retain result, to avoid repeated 6-parm attempts for same system object.
              system_.setMissingPTF();
              ProgramParameter[] shorterParmList = new ProgramParameter[5];
              System.arraycopy(parameterList, 0, shorterParmList, 0, 5);
              succeeded = runProgram("QSYS", "QCDRCMDI", shorterParmList, true, AS400Message.MESSAGE_OPTION_UP_TO_10, true);
            }
            if (!succeeded)
            {
              Trace.log(Trace.ERROR, "Unable to retrieve command information.");
              String id = messageList_[messageList_.length - 1].getID();
              byte[] substitutionBytes = messageList_[messageList_.length - 1].getSubstitutionData();

              // CPF9801 - Object &2 in library &3 not found.
              if (id.equals("CPF9801") && cmdName.equals(converter_.byteArrayToString(substitutionBytes, 0, 10).trim()) && libName.equals(converter_.byteArrayToString(substitutionBytes, 10, 10).trim()) && "CMD".equals(converter_.byteArrayToString(substitutionBytes, 20, 7).trim()))
              {
                Trace.log(Trace.DIAGNOSTIC, "Command not found.");
                return false;  // If cmd doesn't exist, it's not threadsafe.
              }
              // CPF9810 - Library &1 not found.
              if (id.equals("CPF9810") && libName.equals(converter_.byteArrayToString(substitutionBytes).trim()))
              {
                Trace.log(Trace.DIAGNOSTIC, "Command library not found.");
                return false;  // If cmd doesn't exist, it's not threadsafe.
              }
              throw new AS400Exception(messageList_);
            }
          }
        }
        catch (ObjectDoesNotExistException e)
        {
            Trace.log(Trace.ERROR, "Unexpected ObjectDoesNotExistException:", e);
            throw new InternalErrorException(InternalErrorException.UNEXPECTED_EXCEPTION);
        }

        // Get the data returned from the program.
        byte[] dataReceived = parameterList[0].getOutputData();
        if (Trace.traceOn_)
        {
            Trace.log(Trace.DIAGNOSTIC, "Command information retrieved:", dataReceived);

            // Examine the "multithreaded job action" field.
            // The "multithreaded job action" field is a single byte at offset 334.
            // Multithreaded job action. The action to take when a command that is not threadsafe is called in a multithreaded job.  The possible values are:
            // 0  Use the action specified in QMLTTHDACN system value.
            // 1  Run the command. Do not send a message.
            // 2  Send an informational message and run the command.
            // 3  Send an escape message, and do not run the command.
            // System value . . . . . :   QMLTTHDACN
            // Description  . . . . . :   Multithreaded job action
            // Interpretation:
            // 1  Perform the function that is not threadsafe without sending a message.
            // 2  Perform the function that is not threadsafe and send an informational message.
            // 3  Do not perform the function that is not threadsafe.
            Trace.log(Trace.DIAGNOSTIC, "Multithreaded job action: " + (dataReceived[334] & 0x0F));
        }

        // Examine the "threadsafe indicator" field.
        // The "threadsafe indicator" field is a single byte at offset 333.
        // Threadsafe indicator:  Whether the command can be used safely in a multithreaded job.
        // The possible values are:
        // 0   The command is not threadsafe and should not be used in a multithreaded job.
        // 1   The command is threadsafe and can be used safely in a multithreaded job.
        // 2   The command is threadsafe under certain conditions.  See the documentation for the command to determine the conditions under which the command can be used safely in a multithreaded job.
        // If the threadsafe indicator is either threadsafe or conditionally threadsafe, the multithreaded job action value will be returned as 1.
        switch (dataReceived[333] & 0x0F)
        {
            case 0:
                if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Command not threadsafe: " + cmdLibAndName);
                return false;
            case 1:
                if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Command threadsafe: " + cmdLibAndName);
                return true;
            case 2:
                if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Conditionally threadsafe: " + cmdLibAndName);
                return false;
        }
        if (Trace.traceOn_) Trace.log(Trace.ERROR, "Invalid threadsafe indicator: " + cmdLibAndName);
        return false;  // Assume the command is not thread-safe.
    }

    // Runs the command.
    // @return  true if command is successful; false otherwise.
    public boolean runCommand(String command, boolean threadSafety, int messageOption) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
        return runCommand(command, threadSafety, messageOption, false);
    }

    // Runs the command.
    // @return  true if command is successful; false otherwise.
    private boolean runCommand(String command, boolean threadSafety, int messageOption, boolean alreadyOpenedOnThisThread) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
        if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Native implementation running command: " + command);
        if (!threadSafety)
        {
            if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Sending command to super class.");
            return super.runCommand(command, false, messageOption);
        }
        if (!alreadyOpenedOnThisThread) open(true);

        if (AS400.nativeVRM.vrm_ >= 0x00060100)
        {
            return runCommand(unicodeConverter_.stringToByteArray(command), messageOption, 1200);
        }
        return runCommand(converter_.stringToByteArray(command), messageOption, 0);
    }

    // Runs the command.
    // @return  true if command is successful; false otherwise.
    public boolean runCommand(byte[] command, boolean threadSafety, int messageOption) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
        if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Native implementation running command:", command);
        if (!threadSafety)
        {
            if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Sending command to super class.");
            return super.runCommand(command, false, messageOption);
        }

        open(true);

        return runCommand(command, messageOption, 0);
    }

    private boolean runCommand(byte[] commandBytes, int messageOption, int ccsid) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException
    {
        byte[] swapToPH = new byte[12];
        byte[] swapFromPH = new byte[12];
        boolean didSwap = system_.swapTo(swapToPH, swapFromPH);
        try
        {
            if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Invoking native method.");
            if (AS400.nativeVRM.vrm_ < 0x00050300)
            {
                try
                {
                    byte[] replyBytes = runCommandNative(commandBytes);
                    if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native reply bytes:", replyBytes);

                    if (replyBytes == null) replyBytes = new byte[0];

                    // Get info from reply.
                    messageList_ = RemoteCommandImplNative.parseMessages(replyBytes, converter_);
                    return true;
                }
                catch (NativeException e)  // Exception found by C code.
                {
                    messageList_ = RemoteCommandImplNative.parseMessages(e.data, converter_);
                    return false;
                }
            }
            else
            {
                try
                {
                    byte[] replyBytes = AS400.nativeVRM.vrm_ < 0x00060100 ? runCommandNativeV5R3(commandBytes, messageOption) : NativeMethods.runCommand(commandBytes, ccsid, messageOption);
                    if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native reply bytes:", replyBytes);

                    // Get info from reply.
                    messageList_ = RemoteCommandImplNative.parseMessagesV5R3(replyBytes, converter_);
                    return true;
                }
                catch (NativeException e)  // Exception found by C code.
                {
                    messageList_ = RemoteCommandImplNative.parseMessagesV5R3(e.data, converter_);
                    return false;
                }
            }
        }
        finally
        {
            if (didSwap) system_.swapBack(swapToPH, swapFromPH);
        }
    }

    // Run the program.
    public boolean runProgram(String library, String name, ProgramParameter[] parameterList, boolean threadSafety, int messageOption) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException
    {
        return runProgram(library, name, parameterList, threadSafety, messageOption, false);
    }

    // Run the program.
    private boolean runProgram(String library, String name, ProgramParameter[] parameterList, boolean threadSafety, int messageOption, boolean alreadyOpenedOnThisThread) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException
    {
        if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Native implementation running program: " + library + "/" + name);
        if (!threadSafety)
        {
            if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Sending program to super class.");
            return super.runProgram(library, name, parameterList, false, messageOption);
        }
        // Run the program on-thread.
        if (!alreadyOpenedOnThisThread) open(true);

        if (AS400.nativeVRM.vrm_ < 0x00050300)
        {
            // Create a "call program" request, and write it as raw bytes to a byte array.
            // Set up the buffer that contains the program to call.  The buffer contains three items:
            //  10 characters - the program to call.
            //  10 characters - the library that contains the program.
            //   4 bytes      - the number of parameters.
            byte[] programNameBuffer = {(byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x40, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
            converter_.stringToByteArray(name, programNameBuffer);
            converter_.stringToByteArray(library, programNameBuffer, 10);
            BinaryConverter.intToByteArray(parameterList.length, programNameBuffer, 20);

            // Set up the parameter structure.  There is one structure for each parameters.
            // The structure contains:
            //   4 bytes - the length of the parameter.
            //   2 bytes - the parameters usage (input/output/inout).
            //   4 bytes - the offset into the parameter buffer.
            byte[] programParameterStructure = new byte[parameterList.length * 10];
            int totalParameterLength = 0;
            for (int i = 0, offset = 0; i < parameterList.length; ++i)
            {
                int parameterMaxLength = parameterList[i].getMaxLength();
                int parameterUsage = parameterList[i].getUsage();
                if (parameterUsage == ProgramParameter.NULL)
                {
                    // Server does not allow null parameters.
                    parameterUsage = ProgramParameter.INPUT;
                }
                BinaryConverter.intToByteArray(parameterMaxLength, programParameterStructure, i * 10);
                BinaryConverter.unsignedShortToByteArray(parameterUsage, programParameterStructure, i * 10 + 4);
                BinaryConverter.intToByteArray(offset, programParameterStructure, i * 10 + 6);

                offset += parameterMaxLength;
                totalParameterLength += parameterMaxLength;
            }

            // Set up the Parameter area.
            byte[] programParameters = new byte[totalParameterLength];
            for (int i = 0, offset = 0; i < parameterList.length; ++i)
            {
                byte[] inputData = parameterList[i].getInputData();
                int parameterMaxLength = parameterList[i].getMaxLength();
                if (inputData != null)
                {
                    System.arraycopy(inputData, 0, programParameters, offset, inputData.length);
                }
                offset += parameterMaxLength;
            }

            if (Trace.traceOn_)
            {
                Trace.log(Trace.DIAGNOSTIC, "Program name bytes:", programNameBuffer);
                Trace.log(Trace.DIAGNOSTIC, "Program parameter bytes:", programParameterStructure);
                Trace.log(Trace.DIAGNOSTIC, "Program parameters:", programParameters);
            }
            byte[] swapToPH = new byte[12];
            byte[] swapFromPH = new byte[12];
            boolean didSwap = system_.swapTo(swapToPH, swapFromPH);
            try
            {
                // Call native method.
                if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Invoking native method.");
                byte[] replyBytes = runProgramNative(programNameBuffer, programParameterStructure, programParameters);
                if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native reply bytes:", replyBytes);

                // Reset the message list.
                messageList_ = new AS400Message[0];

                // For each output/inout parm, in order, set data returned.
                for (int index = 0, i = 0; i < parameterList.length; ++i)
                {
                    int parameterMaxLength = parameterList[i].getMaxLength();
                    int outputDataLength = parameterList[i].getOutputDataLength();
                    if (outputDataLength > 0)
                    {
                        byte[] outputData = new byte[outputDataLength];
                        System.arraycopy(replyBytes, index, outputData, 0, outputDataLength);
                        parameterList[i].setOutputData(outputData);
                    }
                    index += parameterMaxLength;
                }
                return true;
            }
            catch (NativeException e)  // Exception found by C code.
            {
                messageList_ = RemoteCommandImplNative.parseMessages(e.data, converter_);
                if (messageList_.length == 0) return false;

                // Parse information from byte array.
                String id = messageList_[messageList_.length - 1].getID();

                if (id.equals("MCH3401"))
                {
                    byte[] substitutionBytes = messageList_[messageList_.length - 1].getSubstitutionData();
                    if (substitutionBytes[0] == 0x02 && substitutionBytes[1] == 0x01 && name.equals(converter_.byteArrayToString(substitutionBytes, 2, 30).trim()))
                    {
                        throw new ObjectDoesNotExistException(QSYSObjectPathName.toPath(library, name, "PGM"), ObjectDoesNotExistException.OBJECT_DOES_NOT_EXIST);
                    }
                    if (substitutionBytes[0] == 0x04 && substitutionBytes[1] == 0x01 && library.equals(converter_.byteArrayToString(substitutionBytes, 2, 30).trim()))
                    {
                        throw new ObjectDoesNotExistException(QSYSObjectPathName.toPath(library, name, "PGM"), ObjectDoesNotExistException.LIBRARY_DOES_NOT_EXIST);
                    }
                }
                return false;
            }
            finally
            {
                if (didSwap) system_.swapBack(swapToPH, swapFromPH);
            }
        }
        else
        {
            byte[] tempBytes = converter_.stringToByteArray(name);
            byte[] nameBytes = new byte[tempBytes.length + 1];
            System.arraycopy(tempBytes, 0, nameBytes, 0, tempBytes.length);
            tempBytes = converter_.stringToByteArray(library);
            byte[] libraryBytes = new byte[tempBytes.length + 1];
            System.arraycopy(tempBytes, 0, libraryBytes, 0, tempBytes.length);

            byte[] offsetArray = new byte[parameterList.length * 4];
            int totalParameterLength = 0;
            for (int i = 0; i < parameterList.length; ++i)
            {
                if (parameterList[i].getUsage() == ProgramParameter.NULL)
                {
                    BinaryConverter.intToByteArray(-1, offsetArray, i * 4);
                }
                else
                {
                    BinaryConverter.intToByteArray(totalParameterLength, offsetArray, i * 4);
                }
                totalParameterLength += parameterList[i].getMaxLength();
            }

            // Set up the Parameter area.
            byte[] programParameters = new byte[totalParameterLength];
            for (int i = 0, offset = 0; i < parameterList.length; ++i)
            {
                byte[] inputData = parameterList[i].getInputData();
                if (inputData != null)
                {
                    System.arraycopy(inputData, 0, programParameters, offset, inputData.length);
                }
                offset += parameterList[i].getMaxLength();
            }

            if (Trace.traceOn_)
            {
                Trace.log(Trace.DIAGNOSTIC, "Program name bytes:", nameBytes);
                Trace.log(Trace.DIAGNOSTIC, "Program library bytes:", libraryBytes);
                Trace.log(Trace.DIAGNOSTIC, "Number of parameters:", parameterList.length);
                Trace.log(Trace.DIAGNOSTIC, "Offset array:", offsetArray);
                Trace.log(Trace.DIAGNOSTIC, "Program parameters:", programParameters);
            }
            byte[] swapToPH = new byte[12];
            byte[] swapFromPH = new byte[12];
            boolean didSwap = system_.swapTo(swapToPH, swapFromPH);
            try
            {
                // Call native method.
                if (Trace.traceOn_) Trace.log(Trace.INFORMATION, "Invoking native method.");
                byte[] replyBytes = AS400.nativeVRM.vrm_ < 0x00060100 ? runProgramNativeV5R3(nameBytes, libraryBytes, parameterList.length, offsetArray, programParameters, messageOption) : NativeMethods.runProgram(nameBytes, libraryBytes, parameterList.length, offsetArray, programParameters, messageOption);
                if (Trace.traceOn_) Trace.log(Trace.DIAGNOSTIC, "Native reply bytes:", replyBytes);

                // Reset the message list.
                messageList_ = new AS400Message[0];

                // For each output/inout parm, in order, set data returned.
                for (int index = 0, i = 0; i < parameterList.length; ++i)
                {
                    int outputDataLength = parameterList[i].getOutputDataLength();
                    if (outputDataLength > 0)
                    {
                        byte[] outputData = new byte[outputDataLength];
                        System.arraycopy(replyBytes, index, outputData, 0, outputDataLength);
                        parameterList[i].setOutputData(outputData);
                    }
                    index += parameterList[i].getMaxLength();
                }
                return true;
            }
            catch (NativeException e)  // Exception found by C code.
            {
                messageList_ = RemoteCommandImplNative.parseMessagesV5R3(e.data, converter_);
                if (messageList_.length == 0) return false;

                // Parse information from byte array.
                String id = messageList_[messageList_.length - 1].getID();

                if (id.equals("MCH3401"))
                {
                    byte[] substitutionBytes = messageList_[messageList_.length - 1].getSubstitutionData();
                    if (substitutionBytes[0] == 0x02 && substitutionBytes[1] == 0x01 && name.equals(converter_.byteArrayToString(substitutionBytes, 2, 30).trim()))
                    {
                        throw new ObjectDoesNotExistException(QSYSObjectPathName.toPath(library, name, "PGM"), ObjectDoesNotExistException.OBJECT_DOES_NOT_EXIST);
                    }
                    if (substitutionBytes[0] == 0x04 && substitutionBytes[1] == 0x01 && library.equals(converter_.byteArrayToString(substitutionBytes, 2, 30).trim()))
                    {
                        throw new ObjectDoesNotExistException(QSYSObjectPathName.toPath(library, name, "PGM"), ObjectDoesNotExistException.LIBRARY_DOES_NOT_EXIST);
                    }
                }
                return false;
            }
            finally
            {
                if (didSwap) system_.swapBack(swapToPH, swapFromPH);
            }
        }
    }

    static AS400Message[] parseMessages(byte[] data, ConverterImplRemote converter)
    {
        int messageNumber = data.length / 10240;
        AS400Message[] messageList = new AS400Message[messageNumber];

        for (int offset = 0, i = 0; i < messageNumber; ++i)
        {
            messageList[i] = new AS400Message();
            messageList[i].setID(converter.byteArrayToString(data, offset + 12, 7));
            messageList[i].setType((data[offset + 19] & 0x0F) * 10 + (data[offset + 20] & 0x0F));
            messageList[i].setSeverity(BinaryConverter.byteArrayToInt(data, offset + 8));
            messageList[i].setFileName(converter.byteArrayToString(data, offset + 25, 10).trim());
            messageList[i].setLibraryName(converter.byteArrayToString(data, offset + 45, 10).trim());

            int substitutionDataLength = BinaryConverter.byteArrayToInt(data, offset + 80);
            int textLength = BinaryConverter.byteArrayToInt(data, offset + 88);

            byte[] substitutionData = new byte[substitutionDataLength];
            System.arraycopy(data, offset + 112, substitutionData, 0, substitutionDataLength);
            messageList[i].setSubstitutionData(substitutionData);

            messageList[i].setText(converter.byteArrayToString(data, offset + 112 + substitutionDataLength, textLength));
            offset += 10240;
        }
        return messageList;
    }

    static AS400Message[] parseMessagesV5R3(byte[] data, ConverterImplRemote converter)
    {
        int messageNumber = BinaryConverter.byteArrayToInt(data, 0);
        AS400Message[] messageList = new AS400Message[messageNumber];

        for (int offset = 4, i = 0; i < messageNumber; ++i)
        {
            messageList[i] = new AS400Message();
            messageList[i].setID(converter.byteArrayToString(data, offset + 12, 7));
            messageList[i].setType((data[offset + 19] & 0x0F) * 10 + (data[offset + 20] & 0x0F));
            messageList[i].setSeverity(BinaryConverter.byteArrayToInt(data, offset + 8));
            messageList[i].setFileName(converter.byteArrayToString(data, offset + 25, 10).trim());
            messageList[i].setLibraryName(converter.byteArrayToString(data, offset + 45, 10).trim());

            int substitutionDataLength = BinaryConverter.byteArrayToInt(data, offset + 80);
            int textLength = BinaryConverter.byteArrayToInt(data, offset + 88);
            int helpLength = BinaryConverter.byteArrayToInt(data, offset + 96);

            byte[] substitutionData = new byte[substitutionDataLength];
            System.arraycopy(data, offset + 112, substitutionData, 0, substitutionDataLength);
            messageList[i].setSubstitutionData(substitutionData);

            messageList[i].setText(converter.byteArrayToString(data, offset + 112 + substitutionDataLength, textLength));
            messageList[i].setHelp(converter.byteArrayToString(data, offset + 112 + substitutionDataLength + textLength, helpLength));

            offset += BinaryConverter.byteArrayToInt(data, offset);
            offset += BinaryConverter.byteArrayToInt(data, offset);
        }
        return messageList;
    }

    private native byte[] runCommandNative(byte[] command) throws NativeException;
    private static native byte[] runCommandNativeV5R3(byte[] command, int messageOption) throws NativeException;
    private native byte[] runProgramNative(byte[] programNameBuffer, byte[] programParameterStructure, byte[] programParameters) throws NativeException;
    private static native byte[] runProgramNativeV5R3(byte[] name, byte[] library, int numberParameters, byte[] offsetArray, byte[] programParameters, int messageOption) throws NativeException;
}