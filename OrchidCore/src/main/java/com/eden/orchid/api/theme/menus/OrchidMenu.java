// Generated by delombok at Sun Mar 24 19:34:08 CDT 2019
package com.eden.orchid.api.theme.menus;

import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.theme.components.ModularPageList;
import com.eden.orchid.api.theme.pages.OrchidPage;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public final class OrchidMenu extends ModularPageList<OrchidMenu, OrchidMenuFactory> {
    @Inject
    public OrchidMenu(OrchidContext context) {
        super(context);
    }

    @Override
    protected Class<OrchidMenuFactory> getItemClass() {
        return OrchidMenuFactory.class;
    }

    public List<MenuItem> getMenuItems(OrchidPage containingPage) {
        ArrayList<MenuItem> menuItemsChildren = new ArrayList<>();
        for (OrchidMenuFactory menuItem : get(containingPage)) {
            List<MenuItem> impls = menuItem.getMenuItems();
            if (impls.size() > 0 && menuItem.isAsSubmenu()) {
                MenuItem innerMenuItem = new MenuItem.Builder(context).title(menuItem.getSubmenuTitle()).children(impls).data(menuItem.getAllData()).build();
                menuItemsChildren.add(innerMenuItem);
            } else {
                impls.forEach(impl -> impl.setAllData(menuItem.getAllData()));
                menuItemsChildren.addAll(impls);
            }
        }
        return menuItemsChildren;
    }
}
