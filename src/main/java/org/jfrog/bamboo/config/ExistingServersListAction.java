/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.config;

import com.atlassian.bamboo.security.GlobalApplicationSecureObject;
import com.atlassian.bamboo.ww2.actions.admin.user.AbstractEntityPagerSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;

import java.util.List;

public class ExistingServersListAction extends AbstractEntityPagerSupport implements GlobalAdminSecurityAware {

    private final ServerConfigManager serverConfigManager;

    public ExistingServersListAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
    }

    @SuppressWarnings("unused")
    public String doBrowse() throws Exception {
        return super.execute();
    }

    @SuppressWarnings("unused")
    public String browse() throws Exception {
        return super.execute();
    }

    @SuppressWarnings("unused")
    public List<ServerConfig> getServerConfigs() {
        return serverConfigManager.getAllServerConfigs();
    }

    @Override
    public Object getSecuredDomainObject() {
        return GlobalApplicationSecureObject.INSTANCE;
    }
}