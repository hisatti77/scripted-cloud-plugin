/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.scripted_cloud;
import hudson.slaves.SlaveComputer;
import hudson.model.Slave;
import java.util.concurrent.Future;

/**
 *
 * @author Admin
 */
public class scriptedCloudSlaveComputer extends SlaveComputer {
    public scriptedCloudSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        return super._connect(forceReconnect);
    }
    
}
