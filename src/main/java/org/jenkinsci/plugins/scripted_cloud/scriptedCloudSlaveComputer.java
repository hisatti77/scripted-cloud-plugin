/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;
import hudson.slaves.SlaveComputer;
import hudson.model.Slave;
import java.util.concurrent.Future;

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
public class scriptedCloudSlaveComputer extends SlaveComputer {
    public Boolean isStarting = Boolean.FALSE;
    public Boolean isDisconnecting = Boolean.FALSE;

	public enum MACHINE_ACTION {
        SHUTDOWN,
        REVERT,
        RESET,
        NOTHING
    }

    public enum SC_SLAVE_STATE {
    	INITIAL, STARTING, STOPPING, STARTED, STOPPED, ERROR
    } 
    
    public SC_SLAVE_STATE state;
    public SC_SLAVE_STATE prevState;
    
    private Boolean forceLaunch;
    private String vmDescription;
    private String vmName;
    private String vmPlatform;
    private String vmExtraParams;
    private String vmGroup;
    private String snapName;
    private String idleActionString;
    private MACHINE_ACTION idleAction;
    
    private boolean needed;
    
    public scriptedCloudSlaveComputer(Slave slave
            , String vmDescription
            , String vmName, String vmPlatform, String vmGroup
            , String snapName, String extraParams
            , Boolean forceLaunch
            , String idleOption) {
        super(slave);
    	isStarting = Boolean.FALSE;
    	isDisconnecting = Boolean.FALSE;
    	state = SC_SLAVE_STATE.INITIAL;
    	prevState = state;
        this.forceLaunch = forceLaunch;
        this.vmDescription = vmDescription;
        this.vmName = vmName;
        this.vmPlatform = vmPlatform;
        this.vmExtraParams = extraParams;
        this.vmGroup = vmGroup;
        this.snapName = snapName;    
        this.idleActionString = idleOption;
       
        if ("Shutdown".equals(idleOption)) {
            idleAction = MACHINE_ACTION.SHUTDOWN;
        } else if ("Shutdown and Revert".equals(idleOption)) {
            idleAction = MACHINE_ACTION.REVERT;
        } else if ("Reset".equals(idleOption)) {
            idleAction = MACHINE_ACTION.RESET;            
        } else {
            idleAction = MACHINE_ACTION.NOTHING;
        }
        
        needed = false;
    }
    
    public void fillEnv(HashMap envMap) {
		envMap.put("SCVM_NAME", this.vmName);
		envMap.put("SCVM_SNAPNAME", this.snapName);
		envMap.put("SCVM_PLATFORM", this.vmPlatform);
		envMap.put("SCVM_EXTRAPARAMS", this.vmExtraParams);    		
		envMap.put("SCVM_GROUP", this.vmGroup);
		switch(idleAction) {
		case SHUTDOWN:
			envMap.put("SCVM_STOPACTION", "shutdown");
			break;
		case REVERT:
			envMap.put("SCVM_STOPACTION", "revert");
			break;
		case RESET:
			envMap.put("SCVM_STOPACTION", "reset");
			break;
		case NOTHING:
			envMap.put("SCVM_STOPACTION", "nothing");
			break;
		}		
		if (forceLaunch == Boolean.TRUE) {
			envMap.put("SCVM_FORCESTART", "yes");
		}
		else {
			envMap.put("SCVM_FORCESTART", "no");
		}
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        return super._connect(forceReconnect);
    }    
    
    public String toString() {
    	return String.format("%s[state:%s, idleaction:%s] "
    			,this.vmName , getState()
    		    ,idleActionString);
    }
    
    //============= set/get functions
    public void revertState() {
    	state = prevState;
    }
    
    public boolean initialized() {
    	return state == SC_SLAVE_STATE.INITIAL;
    }
    
    public boolean stopped() {
    	return state == SC_SLAVE_STATE.STOPPED;
    }
    
    public boolean stopping() {
    	return state == SC_SLAVE_STATE.STOPPING;
    }
    public void setStopping() {
    	prevState = state;
    	state = SC_SLAVE_STATE.STOPPING;
    }
    public void setStopped() {
    	prevState = state;
    	state = SC_SLAVE_STATE.STOPPED;
    }
    
    public boolean starting() {
    	return state == SC_SLAVE_STATE.STARTING;
    }
    public void setStarting() {
    	prevState = state;
    	state = SC_SLAVE_STATE.STARTING;
    }
    
    public boolean started() {
    	return state == SC_SLAVE_STATE.STARTED;
    }
    public void setStarted() {
    	prevState = state;
    	state = SC_SLAVE_STATE.STARTED;
    }
    
    public boolean needed() {
    	return needed;
    }
    
    public void setNeeded() {
    	needed = true;
    }
    
    public void setNotNeeded() {
    	needed = false;
    }
    
    //member get/set
    public String getVmName() {
        return vmName;
    }

    public String getVsDescription() {
        return vmDescription;
    }
    
    public String getState() {
    	switch(state) {
    	case INITIAL:
    		return "initial";
    	case STARTING:
    		return "starting";
    	case STOPPING:
    		return "stopping";
    	case STARTED:
    		return "started";
    	case STOPPED:
    		return "stopped";
    	case ERROR:
    		return "error";
    	}
    	return "Unknown";
    }
    
    public boolean doNothing() {
    	return idleAction == MACHINE_ACTION.NOTHING;
    }
}
