///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: ProgramCall.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Vector;

/**
 The ProgramCall class allows a user to call an AS/400 program, pass parameters to it (input and output), and access data returned in the output parameters after the program runs.  Use ProgramCall to call AS/400 programs.  To call AS/400 service programs, use ServiceProgramCall.
 <P>The following example demonstrates the use of Program Call:
 <br>
 <pre>
    // Call programs on system "Hal"
    AS400 system = new AS400("Hal");
    ProgramCall pgm = new ProgramCall(system);
    try
    {
        // Initialize the name of the program to run
        String progName = "/QSYS.LIB/TESTLIB.LIB/TESTPROG.PGM";
        // Setup the 3 parameters
        ProgramParameter[] parameterList = new ProgramParameter[3];
        // First parameter is to input a name
        AS400Text nametext = new AS400Text(8);
        parameterList[0] = new ProgramParameter(nametext.toBytes("John Doe"));
        // Second parmeter is to get the answer, up to 50 bytes long
        parameterList[1] = new ProgramParameter(50);
        // Third parmeter is to input a quantity and return a value up to 30 bytes long
        byte[] quantity = new byte[2];
        quantity[0] = 1;  quantity[1] = 44;
        parameterList[2] = new ProgramParameter(quantity, 30);
        // Set the program name and parameter list
        pgm.setProgram(progName, parameterList);
        // Run the program
        if (pgm.run() != true)
        {
            // If there was an error
            System.out.println("Program failed!");
            // Show the messages
            AS400Message[] messagelist = pgm.getMessageList();
            for (int i = 0; i < messagelist.length; ++i)
            {
                // show each message
                System.out.println(messagelist[i]);
            }
        }
        // else no error, get output data
        else
        {
            AS400Text text(50);
            System.out.println(text.toObject(parameterList[1].getOutputData()));
            System.out.println(text.toObject(parameterList[2].getOutputData()));
        }
    }
    catch (Exception e)
    {
        System.out.println("Program " + pgm.getProgram() + " did not run!");
    }
    // done with the system
    system.disconnectAllServices();
 </pre>

 @see  ProgramParameter
 @see  AS400Message
 @see  ServiceProgramCall
 **/
public class ProgramCall implements Serializable
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

    private AS400 system_;
    private String ifsName_ = "";
    private ProgramParameter[] parameterList_ = {};
    AS400Message[] messageList_ = new AS400Message[0];

    transient RemoteCommandImpl impl_;

    transient private Vector actionCompletedListeners_;
    transient PropertyChangeSupport propertyChangeListeners_;
    transient VetoableChangeSupport vetoableChangeListeners_;

    /**
     Constructs a ProgramCall object.  The system, program, and parameters must be provided later.
     **/
    public ProgramCall()
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Constructing ProgramCall object.");
        initializeTransient();
    }

    /**
     Constructs a ProgramCall object.  It uses the specified system.  The program and parameters must be provided later.
     @param  system  The AS/400 on which to run the program.
     **/
    public ProgramCall(AS400 system)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Constructing ProgramCall object, system: " + system);
        if (system == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'system' is null.");
            throw new NullPointerException("system");
        }

        system_ = system;
        initializeTransient();
    }

    /**
     Constructs a program call object.  It uses the specified system, program name, and parameter list.
     @param  system  The AS/400 on which to run the program.
     @param  program  The program name as a fully qualified path name in the library file system.  The library and program name must each be 10 characters or less.
     @param  parameterList  A list of up to 35 parameters with which to run the program.
     **/
    public ProgramCall(AS400 system, String program, ProgramParameter[] parameterList)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Constructing ProgramCall object, system: " + system + " program: " + program);
        if (system == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'system' is null.");
            throw new NullPointerException("system");
        }

        system_ = system;
        initializeTransient();

        try 
        {
            setProgram(program, parameterList);
        }
        catch (PropertyVetoException e)
        {
            Trace.log(Trace.ERROR, "Unexpected PropertyVetoException:", e);
            throw new InternalErrorException(InternalErrorException.UNEXPECTED_EXCEPTION);
        }
    }

    /**
     Adds an ActionCompletedListener.  The specified ActionCompletedListeners <b>actionCompleted</b> method will be called each time a program has run.  The ActionCompletedListener object is added to a list of ActionCompletedListeners managed by this ProgramCall. It can be removed with removeActionCompletedListener.
     @param  listener  The ActionCompletedListener.
     @see  #removeActionCompletedListener
     **/
    public void addActionCompletedListener(ActionCompletedListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Adding action completed listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException("listener");
        }
        actionCompletedListeners_.addElement(listener);
    }

    /**
     Adds a ProgramParameter to the parameter list.
     @param  parameter  The ProgramParameter.
     @exception  PropertyVetoException  If the change is vetoed.
     **/
    public void addParameter(ProgramParameter parameter) throws PropertyVetoException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Adding parameter to parameter list.");
        if ( parameter == null )
        {
            Trace.log(Trace.ERROR, "Parameter 'parameter' is null.");
            throw new NullPointerException("parameter");
        }

        int oldLength = parameterList_.length;
        ProgramParameter[] newParameterList = new ProgramParameter[oldLength + 1];
        System.arraycopy(parameterList_, 0, newParameterList, 0, oldLength);
        newParameterList[oldLength] = parameter;
        setParameterList(newParameterList);
    }

    /**
     Adds a PropertyChangeListener.  The specified PropertyChangeListeners <b>propertyChange</b> method will be called each time the value of any bound property is changed.  The PropertyListener object is added to a list of PropertyChangeListeners managed by this ProgramCall, it can be removed with removePropertyChangeListener.
     @param listener The PropertyChangeListener.
     @see  #removePropertyChangeListener
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Adding property change listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException("listener");
        }
        propertyChangeListeners_.addPropertyChangeListener(listener);
    }

    /**
     Adds a VetoableChangeListener.  The specified VetoableChangeListeners <b>vetoableChange</b> method will be called each time the value of any constrained property is changed.
     @param  listener  The VetoableChangeListener.
     @see  #removeVetoableChangeListener
     **/
    public void addVetoableChangeListener(VetoableChangeListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Adding vetoable change listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException ("listener");
        }
        vetoableChangeListeners_.addVetoableChangeListener(listener);
    }

    // Chooses the appropriate implementation, synchronize to protect impl_ object.
    synchronized void chooseImpl() throws AS400SecurityException, IOException
    {
        if (impl_ == null)
        {
            if (system_ == null)
            {
                Trace.log(Trace.ERROR, "Attempt to run before setting system.");
                throw new ExtendedIllegalStateException("system", ExtendedIllegalStateException.PROPERTY_NOT_SET);
            }

            impl_ = (RemoteCommandImpl)system_.loadImpl2("com.ibm.as400.access.RemoteCommandImplRemote", "com.ibm.as400.access.RemoteCommandImplProxy");
            system_.connectService(AS400.COMMAND);
            impl_.setSystem(system_.getImpl());
        }
    }

    // Fires the action completed event.
    void fireActionCompleted()
    {
        Vector targets = (Vector)actionCompletedListeners_.clone();
        ActionCompletedEvent event = new ActionCompletedEvent(this);
        for (int i = 0; i < targets.size(); ++i)
        {
            ActionCompletedListener target = (ActionCompletedListener)targets.elementAt(i);
            target.actionCompleted(event);
        }
    }

    /**
     Returns the list of AS/400 messages returned from running the program.  It will return an empty list if the program has not been run yet.
     @return  The array of messages returned by the AS/400 for the program.
     **/
    public AS400Message[] getMessageList()
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Getting message list.");
        return messageList_;
    }

    /**
     Returns the list of parameters.  It will return an empty list if not previously set.
     @return  The list of parameters.
     **/
    public ProgramParameter[] getParameterList()
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Getting parameter list.");
        return parameterList_;
    }

    /**
     Returns the integrated file system pathname for the program.  It will return an empty string ("") if not previously set.
     @return  The integrated file system pathname for the program.
     **/
    public String getProgram()
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Getting program: " + ifsName_);
        return ifsName_;
    }

    /**
     Returns the AS/400 on which the program is to be run.
     @return  The AS/400 on which the program is to be run.  If the system has not been set, null is returned.
     **/
    public AS400 getSystem()
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Getting system.");
        return system_;
    }

    // Initializes all transient data.
    private void initializeTransient()
    {
        // impl_ remains null
        actionCompletedListeners_ = new Vector();
        propertyChangeListeners_ = new PropertyChangeSupport(this);
        vetoableChangeListeners_ = new VetoableChangeSupport(this);
    }

    // Deserializes and initializes the transient data.
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "De-serializing ProgramCall object.");
        in.defaultReadObject();
        initializeTransient();
    }

    /**
     Removes this ActionCompletedListener.  If the ActionCompletedListener is not on the list, nothing is done.
     @param  listener  The ActionCompletedListener.
     @see  #addActionCompletedListener
     **/
    public void removeActionCompletedListener(ActionCompletedListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Removing action completed listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException("listener");
        }
        actionCompletedListeners_.removeElement(listener);
    }

    /**
     Removes this PropertyChangeListener.  If the PropertyChangeListener is not on the list, nothing is done.
     @param  listener  The PropertyChangeListener.
     @see  #addPropertyChangeListener
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Removing property change listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException("listener");
        }
        propertyChangeListeners_.removePropertyChangeListener(listener);
    }

    /**
     Removes this VetoableChangeListener.  If the VetoableChangeListener is not on the list, nothing is done.
     @param  listener  The VetoableChangeListener.
     @see  #addVetoableChangeListener
     **/
    public void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Removing vetoable change listener.");
        if (listener == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'listener' is null.");
            throw new NullPointerException("listener");
        }
        vetoableChangeListeners_.removeVetoableChangeListener(listener);
    }

    /**
     Runs the program on the AS/400.  The program and parameter list need to be set prior to this call.
     @return  true if program ran successfully; false otherwise.
     @exception  AS400SecurityException  If a security or authority error occurs.
     @exception  ConnectionDroppedException  If the connection is dropped unexpectedly.
     @exception  ErrorCompletingRequestException  If an error occurs before the request is completed.
     @exception  IOException  If an error occurs while communicating with the AS/400.
     @exception  InterruptedException  If this thread is interrupted.
     @exception  ObjectDoesNotExistException  If the AS/400 object does not exist.
     **/
    public boolean run() throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.INFORMATION, "Running program: " + ifsName_);
        if (ifsName_.length() == 0)
        {
            Trace.log(Trace.ERROR, "Attempt to run before setting program.");
            throw new ExtendedIllegalStateException("program", ExtendedIllegalStateException.PROPERTY_NOT_SET );
        }
        //  Validate that all the program input parameters have been set.
        for (int i = 0; i < parameterList_.length; ++i)
        {
            if (parameterList_[i] == null)
            {
                Trace.log(Trace.ERROR, "Parameter list value " + i + " not valid.");
                throw new ExtendedIllegalArgumentException("parameterList", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
            }
        }

        chooseImpl();

        boolean success = impl_.runProgram(ifsName_, parameterList_);
        messageList_ = impl_.getMessageList();

        fireActionCompleted();  // notify listeners
        return success;
    }

    /**
     Sets the program name and the parameter list and runs the program on the AS/400.
     @param  program  The fully qualified integrated file system path name to the program.  The library and program name must each be 10 characters or less.
     @param  parameterList  The list of parameters with which to run the program.
     @return  true if program ran successfully, false otherwise.
     @exception  AS400SecurityException  If a security or authority error occurs.
     @exception  ConnectionDroppedException  If the connection is dropped unexpectedly.
     @exception  ErrorCompletingRequestException  If an error occurs before the request is completed.
     @exception  IOException  If an error occurs while communicating with the AS/400.
     @exception  InterruptedException  If this thread is interrupted.
     @exception  ObjectDoesNotExistException  If the AS/400 object does not exist.
     @exception  PropertyVetoException  If a change is vetoed.
     **/
    public boolean run(String program, ProgramParameter[] parameterList) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException, PropertyVetoException
    {
        setProgram(program, parameterList);
        return run();
    }

    /**
     Sets the list of parameters to pass to the AS/400 program.
     @param  parameterList  A list of up to 35 parameters with which to run the program.
     @exception  PropertyVetoException  If a change is vetoed.
     **/
    public void setParameterList(ProgramParameter[] parameterList) throws PropertyVetoException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Setting parameter list.");
        if (parameterList == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'parameterList' is null.");
            throw new NullPointerException("parameterList");
        }
        else if (parameterList.length > 35)
        {
            Trace.log(Trace.ERROR, "Length of parameter 'parameterList' is not valid:", parameterList.length);
            throw new ExtendedIllegalArgumentException("parameterList", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
        }

        ProgramParameter[] old = parameterList_;
        vetoableChangeListeners_.fireVetoableChange("parameterList", old, parameterList );
        parameterList_ = parameterList;
        propertyChangeListeners_.firePropertyChange("parameterList", old, parameterList );
    }

    /**
     Sets the path name of the program and the parameter list.
     @param  program  The fully qualified integrated file system path name to the program.  The library and program name must each be 10 characters or less.
     @param  parameterList  A list of up to 35 parameters with which to run the program.
     @exception  PropertyVetoException  If a change is vetoed.
     **/
    public void setProgram(String program, ProgramParameter[] parameterList) throws PropertyVetoException
    {
        setProgram(program);
        setParameterList(parameterList);
    }

    /**
     Sets the path name of the program.
     @param  program  The fully qualified integrated file system path name to the program.  The library and program name must each be 10 characters or less.
     @exception  PropertyVetoException  If a change is vetoed.
     **/
    public void setProgram(String program) throws PropertyVetoException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Setting program: " + program);
        // validate program
        if (program == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'program' is null.");
            throw new NullPointerException("program");
        }

        if (this.getClass().getName().equals("com.ibm.as400.access.ServiceProgramCall"))
        {
            new QSYSObjectPathName(program, "SRVPGM");
        }
        else
        {
            new QSYSObjectPathName(program, "PGM");
        }

        String old = ifsName_;
        vetoableChangeListeners_.fireVetoableChange("program", old, program);
        // set values
        ifsName_ = program;

        propertyChangeListeners_.firePropertyChange("program", old, program);
    }

    /**
     Sets the AS/400 to run the program.
     @param  system  The AS/400 on which to run the program.  The system cannot be changed once a connection is made to the server.
     @exception  PropertyVetoException  If a change is vetoed.
     **/
    public void setSystem(AS400 system) throws PropertyVetoException
    {
        if (Trace.isTraceOn()) Trace.log(Trace.DIAGNOSTIC, "Setting system: " + system);
        if (system == null)
        {
            Trace.log(Trace.ERROR, "Parameter 'system' is null.");
            throw new NullPointerException("system");
        }

        if (impl_ != null)
        {
            Trace.log(Trace.ERROR, "Cannot set property 'system' after connect.");
            throw new ExtendedIllegalStateException("system", ExtendedIllegalStateException.PROPERTY_NOT_CHANGED);
        }

        AS400 old = system_;
        vetoableChangeListeners_.fireVetoableChange("system", old, system);
        system_ = system;
        propertyChangeListeners_.firePropertyChange("system", old, system);
    }

    /**
     Returns the string representation of this program call object.
     @return  The string representing this program call object.
     **/
    public String toString()
    {
        return "ProgramCall (system: " + system_ + " program: " + ifsName_ + "):" + super.toString();
    }
}
