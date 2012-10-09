/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;
import hudson.Util;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.Cloud;

import java.io.IOException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import hudson.Launcher.LocalLauncher;
import hudson.tasks.Shell;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import java.io.IOException;
import java.util.Collections;

import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import static hudson.model.TaskListener.NULL;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Admin
 */
public class scriptedCloudLauncher extends ComputerLauncher {

    private ComputerLauncher delegate;
    private Boolean forceLaunch;
    private String vsDescription;
    private String vmName;
    private String vmPlatform;
    private String vmExtraParams;
    private String vmGroup;
    private String snapName;
    //private Boolean isStarting = Boolean.FALSE;
    //private Boolean isDisconnecting = Boolean.FALSE;
    private MACHINE_ACTION idleAction;
    private scriptedCloud vs = null;
    private int LimitedTestRunCount = 0;
    private Boolean disconnectCustomAction = Boolean.FALSE;
    
    private Boolean enableLaunch = Boolean.FALSE;

    public enum MACHINE_ACTION {
        SHUTDOWN,
        REVERT,
        RESET,
        NOTHING
    }

    @DataBoundConstructor
    public scriptedCloudLauncher(ComputerLauncher delegate,
            String vsDescription
            , String vmName, String vmPlatform, String vmGroup
            , String snapName, String extraParams
            , Boolean forceLaunch
            , String idleOption
            /*, String LimitedTestRunCount */
            ) {
        super();
        this.delegate = delegate;
        this.forceLaunch = forceLaunch;
        this.vsDescription = vsDescription;
        this.vmName = vmName;
        this.vmPlatform = vmPlatform;
        this.vmExtraParams = extraParams;
        this.vmGroup = vmGroup;
        this.snapName = snapName;
        //this.isStarting = Boolean.FALSE;
        this.enableLaunch = Boolean.FALSE;
        if ("Shutdown".equals(idleOption)) {
            idleAction = MACHINE_ACTION.SHUTDOWN;
        } else if ("Shutdown and Revert".equals(idleOption)) {
            idleAction = MACHINE_ACTION.REVERT;
        } else if ("Reset".equals(idleOption)) {
            idleAction = MACHINE_ACTION.RESET;            
        } else {
            idleAction = MACHINE_ACTION.NOTHING;
        }
        this.LimitedTestRunCount = 0; //Util.tryParseNumber(LimitedTestRunCount, 0).intValue();
        vs = findOurVsInstance();
    }
    
    public void enableLaunch() {
    	enableLaunch = Boolean.TRUE;
    }
    public void disableLaunch() {
    	enableLaunch = Boolean.FALSE;
    }

    public scriptedCloud findOurVsInstance() throws RuntimeException {
        if (vsDescription != null && vmName != null) {
            scriptedCloud vs = null;
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof scriptedCloud && ((scriptedCloud) cloud).getVsDescription().equals(vsDescription)) {
                    vs = (scriptedCloud) cloud;
                    return vs;
                }
            }
        }
        scriptedCloud.Log("Could not find our scripted Cloud instance!");
        throw new RuntimeException("Could not find our scripted Cloud instance!");
    }
   
    private CommandInterpreter getCommandInterpreter(String script) {
        if (Hudson.getInstance().isWindows()) {
        	scriptedCloud.Log("its windows..");
            return new BatchFile(script);
        }
        scriptedCloud.Log("its unix..");
        return new Shell(script);
    }

    @Override
    public boolean isLaunchSupported() {
        return delegate.isLaunchSupported();
    }

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener listener)
    throws IOException, InterruptedException {
    	Logger LOG = Logger.getLogger("launch");
    	LOG.info("launch enter:" + vs.getStartScriptFile());
    	scriptedCloud.Log("launch enter:" + vs.getStartScriptFile());
    	scriptedCloud.Log("launch enter:slaveComputer:" + slaveComputer);
    	scriptedCloudSlaveComputer s = (scriptedCloudSlaveComputer)slaveComputer;
    	try {
    		if (s.isTemporarilyOffline()) {
    			scriptedCloud.Log(slaveComputer, listener, "Not launching VM because it's not accepting tasks; temporarily offline"); 
    			return;
    		}

    		// Slaves that take a while to start up make get multiple launch
    		// requests from Jenkins.  
    		if (s.isStarting == Boolean.TRUE) {
    			scriptedCloud.Log(slaveComputer, listener, "Slave is already being launched");
    			return;
    		}

    		disableLaunch();
    		
    		s.isStarting = Boolean.TRUE;
    		s.isDisconnecting = Boolean.FALSE;
    		File f = new File(vs.getStartScriptFile());
    		CommandInterpreter shell = getCommandInterpreter(vs.getStartScriptFile());
    		scriptedCloud.Log("script file:" + vs.getStartScriptFile());
    		//FilePath root = Hudson.getInstance().getRootPath();
    		scriptedCloud.Log("file.getpath:" + f.getParent());
    		FilePath root = new FilePath(new File("/"));
    		FilePath script = shell.createScriptFile(root);
    		scriptedCloud.Log("root path:" + root + ", script:" + script);
    		//shell.buildCommandLine(script);
    		listener.getLogger().println("running start script");
    		int r = 0;
    		HashMap envMap = new HashMap();
    		envMap.put("SCVM_ACTION","start");
    		envMap.put("SCVM_NAME", this.vmName);
    		envMap.put("SCVM_SNAPNAME", this.snapName);
    		envMap.put("SCVM_PLATFORM", this.vmPlatform);
    		envMap.put("SCVM_EXTRAPARAMS", this.vmExtraParams);    		
    		envMap.put("SCVM_GROUP", this.vmGroup);
    		if (forceLaunch == Boolean.TRUE) {
    			envMap.put("SCVM_FORCESTART", "yes");
    		}
    		scriptedCloud.Log("env:" + envMap);
    		shell.buildCommandLine(script);
    		//scriptedCloud.Log("launching:shell:" + shell.getContents());
    		
    		r = root.createLauncher(listener).launch().cmds(shell.buildCommandLine(script))
    		//.envs(Collections.singletonMap("LABEL","s"))
    		.envs(envMap)
    		.stdout(listener).pwd(root).join();
    		if (r!=0)
    			throw new AbortException("The script failed:" + r + ", " + vs.getStartScriptFile());
    		scriptedCloud.Log("script done:" + vs.getStartScriptFile());
    		
    		//satti - enabling this:    		
            if (delegate.isLaunchSupported()) {
                // Delegate is going to do launch.
            	scriptedCloud.Log("calling inbuilt launch");
                try {
                	scriptedCloud.Log("delegate:" + delegate);
                	delegate.launch(slaveComputer, listener);
                }
                catch (SocketException e) {
                	scriptedCloud.Log("Socket Exception in delegate.launch()");
                }
                catch (IOException e) {
                	scriptedCloud.Log("IO Exception in delegate.launch()");
                }
            }
            else
            	scriptedCloud.Log("delegate launch not supported");                        
            
    		scriptedCloud.Log("launch exit:"+ vs.getStartScriptFile());
    	} catch (Exception e) {    		
    		scriptedCloud.Log("launch error:"+ e);
    		throw new RuntimeException(e);            
    	}
    	finally {
    		s.isStarting = Boolean.FALSE;
    	}
		disableLaunch();

    }


    public void stopSlave(scriptedCloudSlaveComputer slaveComputer, boolean forced) {
    	scriptedCloud.Log("stopSlave processing called: forced=" + forced);
    	if (forced == false && idleAction == MACHINE_ACTION.NOTHING) {
    		scriptedCloud.Log("Do nothing for this slave");
    		return;
    	}
    	disconnectCustomAction = Boolean.TRUE;
    	slaveComputer.disconnect();
    	scriptedCloud.Log("disconnected slave.");
    }

    

    @Override
    public synchronized void afterDisconnect(SlaveComputer s,
            TaskListener taskListener) {
    	
    	scriptedCloudSlaveComputer slaveComputer = (scriptedCloudSlaveComputer)s;
    	scriptedCloud.Log("afterDisconnect ...., isDisconnecting=" + slaveComputer.isDisconnecting);
    	//delegate.afterDisconnect(slaveComputer, taskListener);
    	
    	if (slaveComputer.isStarting == Boolean.TRUE) {
    		scriptedCloud.Log("busy starting .. skipping");
    		delegate.afterDisconnect(slaveComputer, taskListener);
    		return;
    	}
    	//if (disconnectCustomAction == Boolean.FALSE) {
    	//	scriptedCloud.Log("Not called from stopslave .. skipping");
    	//	delegate.afterDisconnect(slaveComputer, taskListener);
    	//	return;
    	//}
    	disconnectCustomAction = Boolean.FALSE;
    	//if (idleAction == MACHINE_ACTION.NOTHING) {
    	//	scriptedCloud.Log("Action 'nothing' set .. skipping shutdown");
    	//	//delegate.afterDisconnect(slaveComputer, taskListener);
    	//	return;
    	//}
        if (slaveComputer.isDisconnecting == Boolean.TRUE) {
        	scriptedCloud.Log(slaveComputer, taskListener, "Already disconnecting on a separate thread");
        	//delegate.afterDisconnect(slaveComputer, taskListener);
            return;
        }
        
        if (slaveComputer.isTemporarilyOffline()) {
        	scriptedCloud.Log(slaveComputer, taskListener, "Not disconnecting VM because it's not accepting tasks");
        	//delegate.afterDisconnect(slaveComputer, taskListener);
           return;
        }
            
        try {
        	slaveComputer.isDisconnecting = Boolean.TRUE;
            scriptedCloud.Log(slaveComputer, taskListener, "Running disconnect procedure...");            
            scriptedCloud.Log(slaveComputer, taskListener, "Shutting down Virtual Machine...");
            
            //*********************************
            HashMap envMap = new HashMap();
    		envMap.put("SCVM_NAME", this.vmName);
    		envMap.put("SCVM_SNAPNAME", this.snapName);
    		envMap.put("SCVM_PLATFORM", this.vmPlatform);
    		envMap.put("SCVM_GROUP", this.vmGroup);
    		envMap.put("SCVM_EXTRAPARAMS", this.vmExtraParams);
        	if (idleAction == MACHINE_ACTION.SHUTDOWN) {
    	    	envMap.put("SCVM_ACTION","stop");
        	}
        	if (idleAction == MACHINE_ACTION.REVERT) {
    	    	envMap.put("SCVM_ACTION","revert");
        	}
        	if (idleAction == MACHINE_ACTION.RESET) {
    	    	envMap.put("SCVM_ACTION","reset");
        	}
        	String scriptToRun = vs.getStopScriptFile();
        	scriptedCloud.Log("runScript:" + scriptToRun);        	
            scriptedCloud.Log( "running script");
            File f = new File(scriptToRun);
        	//CommandInterpreter shell = getCommandInterpreter(f.getName());
        	CommandInterpreter shell = getCommandInterpreter(scriptToRun);
            scriptedCloud.Log("sript file:" + scriptToRun);
            scriptedCloud.Log("file.getpath:" + f.getParent());
            //FilePath root = new FilePath(new File(f.getParent()));
            FilePath root = new FilePath(new File("/"));
            FilePath script = shell.createScriptFile(root);
        	scriptedCloud.Log("root path:" + root + ", script:" + script);
            //shell.buildCommandLine(script);
            int r = root.createLauncher(taskListener).launch().cmds(shell.buildCommandLine(script))
                    .envs(envMap /*Collections.singletonMap("LABEL","s")*/)
                    .stdout(NULL).pwd(root).join();
            if (r!=0)
                throw new AbortException("The script failed:" + r);            
            scriptedCloud.Log("script done:" + scriptToRun);
            scriptedCloud.Log("delegate afterdisconnect");
            delegate.afterDisconnect(slaveComputer, taskListener);
            //**********************************
        } catch (Throwable t) {
        	scriptedCloud.Log(slaveComputer, taskListener, "Got an exception");
        	scriptedCloud.Log(slaveComputer, taskListener, t.toString());
        	scriptedCloud.Log(slaveComputer, taskListener, "Printed exception");
            taskListener.fatalError(t.getMessage(), t);
        } finally {
        	scriptedCloud.Log("finally setting isDisconnecting to FALSE");
        	slaveComputer.isDisconnecting = Boolean.FALSE;
        }
    }
   	
    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVsDescription() {
        return vsDescription;
    }

    public MACHINE_ACTION getIdleAction() {
        return idleAction;
    }

    public void setIdleAction(MACHINE_ACTION idleAction) {
        this.idleAction = idleAction;
    }

    public Boolean getforceLaunch() {
        return forceLaunch;
    }

    public void setforceLaunch(Boolean forceLaunch) {
        this.forceLaunch = forceLaunch;
    }

    public Integer getLimitedTestRunCount() {
        return LimitedTestRunCount;
    }

    
    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        // Don't allow creation of launcher from UI
        throw new UnsupportedOperationException();
    }
}
