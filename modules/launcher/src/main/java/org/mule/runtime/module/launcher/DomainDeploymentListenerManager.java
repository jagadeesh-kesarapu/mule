/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

/**
 * Manages which {@link org.mule.runtime.module.launcher.DeploymentListener} are listening for domain deployment
 * notifications.
 */
public interface DomainDeploymentListenerManager
{

    void addDomainDeploymentListener(DeploymentListener listener);

    void removeDomainDeploymentListener(DeploymentListener listener);

}
