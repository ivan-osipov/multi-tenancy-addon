/*
 * Copyright (c) 2016 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.sdbmt.web.tenant;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.ValidationException;
import com.haulmont.cuba.security.entity.GroupHierarchy;
import com.haulmont.sdbmt.entity.MtGroup;

import java.util.List;

public class TenantRootAccessGroupValidator implements Field.Validator {

    private Messages messages = AppBeans.get(Messages.class);
    private DataManager dataManager = AppBeans.get(DataManager.class);
    @Override
    public void validate(Object value) throws ValidationException {
        if (value == null) {
            return;
        }

        DataManager dm = AppBeans.get(DataManager.class);
        MtGroup group = dm.reload((MtGroup) value, "group-tenant-and-hierarchy");

        if (group.getTenant() != null) {
            throw new ValidationException(messages.getMessage(TenantRootAccessGroupValidator.class, "validation.hasTenant"));
        } else if (isRootGroup(group)) {
            throw new ValidationException(messages.getMessage(TenantRootAccessGroupValidator.class, "validation.rootGroup"));
        } else {
            if (subgroupOfOtherTenantGroup(group)) {
                throw new ValidationException(messages.getMessage(TenantRootAccessGroupValidator.class, "validation.subgroupOfOtherTenantGroup"));
            } else if (hasOtherTenantSubgroups(group)) {
                throw new ValidationException(messages.getMessage(TenantRootAccessGroupValidator.class, "validation.hasOtherTenantSubgroups"));
            }
        }
    }

    private boolean hasOtherTenantSubgroups(MtGroup group) {
        LoadContext<MtGroup> ctx = new LoadContext<>(MtGroup.class);
        ctx.setQueryString("select e.group from sec$GroupHierarchy e where e.parent.id = :group and e.group.tenant is not null")
                .setParameter("group", group.getId());

        return dataManager.getCount(ctx) > 0;
    }

    private boolean subgroupOfOtherTenantGroup(MtGroup group) {
        List<GroupHierarchy> hierarchyList = group.getHierarchyList();
        for (GroupHierarchy hierarchy : hierarchyList) {
            MtGroup parent = (MtGroup) hierarchy.getParent();
            if (parent.getTenant() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isRootGroup(MtGroup group) {
        return group.getParent() == null;
    }
}
