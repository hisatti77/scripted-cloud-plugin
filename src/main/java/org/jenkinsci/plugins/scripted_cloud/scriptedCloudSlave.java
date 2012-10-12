/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;

import com.trilead.ssh2.log.Logger;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Queue.BuildableItem;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.Cloud;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.NodeProperty;
import hudson.slaves.SlaveComputer;
import hudson.Extension;
import hudson.Functions;
import hudson.AbortException;
import hudson.util.FormValidation;
import hudson.slaves.OfflineCause;

/*import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;
*/
import hudson.Util;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.model.Run;
import hudson.slaves.OfflineCause;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author Admin
 */
public class scriptedCloudSlave extends Slave {

    private  String vsDescription;
    private  String vmName;
    private  String snapName;
    private  String vmPlatform;
    private  String vmExtraParams;
    private  String vmGroup;
    private  String idleOption;
    private  Boolean forceLaunch;    
    
    private Integer LimitedTestRunCount = 0; // If limited test runs enabled, the number of tests to limit the slave too.
    private transient Integer NumberOfLimitedTestRuns = 0;

        
    @DataBoundConstructor
    public scriptedCloudSlave(String name, String nodeDescription,
            String remoteFS, String numExecutors, Mode mode,
            String labelString, ComputerLauncher delegateLauncher,
            RetentionStrategy retentionStrategy,
            List<? extends NodeProperty<?>> nodeProperties,
            String vsDescription 
            ,String vmName, String vmPlatform, String vmGroup
            ,String vmExtraParams
            ,Boolean forceLaunch,
            String snapName, String idleOption
            /*,String LimitedTestRunCount*/
            )
            throws FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
        		
                new scriptedCloudLauncher(delegateLauncher, vsDescription, vmName)
        
        		,retentionStrategy, nodeProperties);
        this.vsDescription = vsDescription;
        this.vmName = vmName;
        this.vmPlatform = vmPlatform;
        this.vmExtraParams = vmExtraParams;
        this.vmGroup = vmGroup;
        this.snapName = snapName;
        this.idleOption = idleOption;
        this.forceLaunch = forceLaunch;
        this.LimitedTestRunCount = 0; //Util.tryParseNumber(LimitedTestRunCount, 0).intValue();        
        this.NumberOfLimitedTestRuns = 0;
        scriptedCloud.Log("scriptedCloudSlave constructor");
    }

    public String getVmName() {
        return vmName;
    }
    public void setVmName(String name) {
        vmName = name;
    }

    public String getVmPlatform() {
        return vmPlatform;
    }
    public void setVmPlatform(String name) {
    	vmPlatform = name;
    }

    public String getVmExtraParams() {
        return vmExtraParams;
    }
    public void setVmExtraParams(String name) {
    	vmExtraParams = name;
    }

    public String getVmGroup() {
        return vmGroup;
    }
    public void setVmGroup(String name) {
    	vmGroup = name;
    }

    public String getVsDescription() {
        return vsDescription;
    }
    public void setVsDescription(String name) {
    	vsDescription = name;
    }

    public String getSnapName() {
        return snapName;
    }
    public void setSnapName(String name) {
    	snapName = name;
    }

    public Integer getLimitedTestRunCount() {
        return LimitedTestRunCount;
    }

    public String getIdleOption() {
        return idleOption;
    }
    public void setIdleOption(String name) {
    	idleOption = name;
    }
    
    public Boolean getForceLaunch() {
        return forceLaunch;
    }
    public void setForceLaunch(Boolean name) {
    	forceLaunch = name;
    }

    @Override
    public Computer createComputer() {
    	scriptedCloud.Log("createComputer " + name + "\n");
        return new scriptedCloudSlaveComputer(this
                , this.vsDescription
                ,  this.vmName,  this.vmPlatform,  this.vmGroup
                ,  this.snapName,  this.vmExtraParams
                ,  this.forceLaunch
                ,  this.idleOption);
    }
        
    public boolean StartLimitedTestRun(Run r, TaskListener listener) {
    	scriptedCloud.Log("StartLimitedTestRun"); 
    	scriptedCloudSlaveComputer s = (scriptedCloudSlaveComputer)r.getExecutor().getOwner();
        s.setNeeded();   
        return true;
    }

    
    public boolean EndLimitedTestRun(Run r) {
        boolean ret = true;
        boolean forced = false;
        scriptedCloud.Log("EndLimitedTestRun"); 
        scriptedCloudLauncher c = (scriptedCloudLauncher)getLauncher();
        c.stopSlave((scriptedCloudSlaveComputer)r.getExecutor().getOwner(), forced);
        scriptedCloudSlaveComputer s = (scriptedCloudSlaveComputer)r.getExecutor().getOwner();
        s.setNotNeeded();
        return ret;
    }
    
    
    /**
     * For UI.
     *
     * @return original launcher
     */
    public ComputerLauncher getDelegateLauncher() {
        return ((scriptedCloudLauncher) getLauncher()).getDelegate();
    }

    @Extension
    public static class scriptedCloudComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
            /* We may be called on any slave type so check that we should
             * be in here. */
        	taskListener.getLogger().println("sCCL::prelaunch\n");
            if (!(c.getNode() instanceof scriptedCloudSlave)) {
            	taskListener.getLogger().println("sCCL::not interested\n");
                return;
            }            
            //scriptedCloudLauncher vsL = (scriptedCloudLauncher) ((SlaveComputer) c).getLauncher();
            //scriptedCloud vsC = vsL.findOurVsInstance();
            //if (!vsC.markVMOnline(c.getDisplayName(), vsL.getVmName()))
            //    throw new AbortException("The scripted cloud will not allow this slave to start at this time.");
        }                
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Slave virtual computer running under scripted Cloud";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public List<scriptedCloud> getscriptedClouds() {
            List<scriptedCloud> result = new ArrayList<scriptedCloud>();
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof scriptedCloud) {
                    result.add((scriptedCloud) cloud);
                }
            }
            return result;
        }

        public scriptedCloud getSpecificscriptedCloud(String vsDescription)
                throws Exception {
            for (scriptedCloud vs : getscriptedClouds()) {
                if (vs.getVsDescription().equals(vsDescription)) {
                    return vs;
                }
            }
            throw new Exception("The scripted Cloud doesn't exist");
        }

        public List<Descriptor<ComputerLauncher>> getComputerLauncherDescriptors() {
            List<Descriptor<ComputerLauncher>> result = new ArrayList<Descriptor<ComputerLauncher>>();
            for (Descriptor<ComputerLauncher> launcher : Functions.getComputerLauncherDescriptors()) {
                if (!scriptedCloudLauncher.class.isAssignableFrom(launcher.clazz)) {
                    result.add(launcher);
                }
            }
            return result;
        }

        public List<String> getIdleOptions() {
            List<String> options = new ArrayList<String>();
            options.add("Shutdown");
            options.add("Shutdown and Revert");
            //options.add("Suspend");
            options.add("Reset");
            options.add("Nothing");                    
            return options;
        }

        public FormValidation doTestConnection(@QueryParameter String vsDescription,
                @QueryParameter String vmName,
                @QueryParameter String snapName) {
            try {
                scriptedCloud vsC = getSpecificscriptedCloud(vsDescription);
                ServiceInstance si = vsC.getSI();
                return FormValidation.ok("Virtual Machine found successfully");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
