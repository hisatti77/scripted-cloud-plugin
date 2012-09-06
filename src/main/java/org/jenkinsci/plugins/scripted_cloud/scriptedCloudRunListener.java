/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Executor;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Admin
 */
@Extension
public final class scriptedCloudRunListener extends RunListener<Run> {
    
    private List<Run> LimitedRuns = new ArrayList<Run>();

    public scriptedCloudRunListener() {
    }
        
    @Override
    public void onStarted(Run r, TaskListener listener) {
        super.onStarted(r, listener);
        scriptedCloud.Log("onStarted .....");
        if (r != null) {
            Executor exec = r.getExecutor();
            if (exec != null) {
                Computer owner = exec.getOwner();
                if (owner != null) {
                    Node node = owner.getNode();
                    if ((node != null) && (node instanceof scriptedCloudSlave)) {
                    	//listener.getLogger.println("Got node:" + node);                        	
                        LimitedRuns.add(r);
                        scriptedCloudSlave s = (scriptedCloudSlave)node;
                        s.StartLimitedTestRun(r, listener);
                    }
                }
            }
        }
    }

    @Override
    public void onFinalized(Run r) {
        super.onFinalized(r);
        if (LimitedRuns.contains(r)) {
            LimitedRuns.remove(r);
            Node node = r.getExecutor().getOwner().getNode();
            if (node instanceof scriptedCloudSlave) {
                scriptedCloudSlave s = (scriptedCloudSlave)node;
                s.EndLimitedTestRun(r);
            }                    
        }
    }
        
}


