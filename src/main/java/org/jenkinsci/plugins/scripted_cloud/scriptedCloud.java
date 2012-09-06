/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;
/*
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;*/
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.Extension;
import hudson.Util;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;
import hudson.slaves.SlaveComputer;
import hudson.util.Scrambler;
import java.lang.String;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;


class ServiceInstance {
}

class VirtualMachineSnapshot {
	
}

class VirtualMachine {
	
}

class VirtualMachineSnapshotTree {
	
}

class ManagedObjectReference {
	
}
/**
 *
 * @author Admin
 */
public class scriptedCloud extends Cloud {

    private final String vsHost;
    private final String vsDescription;
    private String    startScriptFile;
    private String    stopScriptFile;
    
    private static java.util.logging.Logger VSLOG = java.util.logging.Logger.getLogger("scripted-cloud");
    private static void InternalLog(Slave slave, SlaveComputer slaveComputer, TaskListener listener, String format, Object... args)
    {
        String s = "";
        if (slave != null)
            s = String.format("[%s] ", slave.getNodeName());
        if (slaveComputer != null)
            s = String.format("[%s] ", slaveComputer.getName());
        s = s + String.format(format, args);
        s = s + "\n";
        if (listener != null)
            listener.getLogger().print(s);
        VSLOG.log(Level.INFO, s);
    }
    public static void Log(String msg) {
        InternalLog(null, null, null, msg, null);
    }
    public static void Log(String format, Object... args) {
        InternalLog(null, null, null, format, args);
    }            
    public static void Log(TaskListener listener, String msg) {
        InternalLog(null, null, listener, msg, null);
    }
    public static void Log(TaskListener listener, String format, Object... args) {
        InternalLog(null, null, listener, format, args);
    }
    public static void Log(Slave slave, TaskListener listener, String msg) {
        InternalLog(slave, null, listener, msg, null);
    }
    public static void Log(Slave slave, TaskListener listener, String format, Object... args) {
        InternalLog(slave, null, listener, format, args);
    }
    public static void Log(SlaveComputer slave, TaskListener listener, String msg) {
        InternalLog(null, slave, listener, msg, null);
    }
    public static void Log(SlaveComputer slave, TaskListener listener, String format, Object... args) {
        InternalLog(null, slave, listener, format, args);
    }

    @DataBoundConstructor
    public scriptedCloud(String vsDescription 
    		, String vsHost
    		,String startScriptFile, String stopScriptFile) {
        super("scriptedCloud");
        this.vsDescription = vsDescription;        
        Log("STARTTING SCRIPTED CLOUD");
        this.startScriptFile = startScriptFile;
        this.stopScriptFile = stopScriptFile;
        this.vsHost = vsHost;
    }
    
    public String getVsDescription() {
        return vsDescription;
    }

    public String getVsHost() {
        return vsHost;
    }

    public void setStopScriptFile(String scriptPath) {
        stopScriptFile = scriptPath;
    }
    
    public String getStopScriptFile() {
        return stopScriptFile;
    }
    public void setStartScriptFile(String scriptPath) {
        startScriptFile = scriptPath;
    }
    public String getStartScriptFile() {
        return startScriptFile;
    }

    @Override
    public boolean canProvision(Label label) {
        return false;
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        return Collections.emptySet();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("scriptedCloud");
        sb.append(" {Description='").append(vsDescription).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public ServiceInstance getSI()
            throws Exception {
        ServiceInstance si = new ServiceInstance();
        return si;
    }

    public synchronized Boolean canMarkVMOnline(String slaveName, String vmName) {
        return Boolean.TRUE;
    }
    
    public synchronized Boolean markVMOnline(String slaveName, String vmName) {
        return Boolean.TRUE;
    }

    public synchronized void markVMOffline(String slaveName, String vmName) {
    }

    public VirtualMachineSnapshot getSnapshotInTree(
            VirtualMachine vm, String snapName) throws Exception {
        if (vm == null || snapName == null) {
            return null;
        }
        return null;
    }

    public ManagedObjectReference findSnapshotInTree(
            VirtualMachineSnapshotTree[] snapTree, String snapName) {
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        public final ConcurrentMap<String, scriptedCloud> hypervisors = new ConcurrentHashMap<String, scriptedCloud>();
        private String vsHost;

        @Override
        public String getDisplayName() {
            return "scripted Cloud";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o)
                throws FormException {
            vsHost = o.getString("vsHost");
            save();
            return super.configure(req, o);
        }

        /**
         * For UI.
         */
        public FormValidation doTestConnection(
                @QueryParameter String vsDescription) {
            try {
                /* We know that these objects are not null */
                if (vsDescription.length() == 0) {
                    return FormValidation.error("scripted Host is not specified");
                }
                ServiceInstance si = new ServiceInstance();                
                return FormValidation.ok("Connected successfully");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
