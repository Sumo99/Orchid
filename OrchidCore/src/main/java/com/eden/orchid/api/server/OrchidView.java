package com.eden.orchid.api.server;

import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.generators.OrchidCollection;
import com.eden.orchid.api.options.Descriptive;
import com.eden.orchid.api.resources.resource.StringResource;
import com.eden.orchid.api.server.admin.AdminList;
import com.eden.orchid.api.tasks.OrchidCommand;
import com.eden.orchid.api.tasks.OrchidTask;
import com.eden.orchid.api.theme.assets.CssPage;
import com.eden.orchid.api.theme.assets.JsPage;
import com.eden.orchid.api.theme.pages.OrchidPage;
import com.eden.orchid.api.theme.pages.OrchidReference;
import com.google.inject.Provider;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrchidView extends OrchidPage {

    public enum Type {
        Page, Fullscreen
    }

    @Getter
    @Setter
    private String[] breadcrumbs;

    @Getter
    private final OrchidController controller;

    @Setter
    @Inject
    private Provider<Set<AdminList>> adminLists;

    @Getter
    @Setter
    private Object params;

    @Getter
    @Setter
    private Type type;

    public OrchidView(OrchidContext context, OrchidController controller, String... views) {
        this(context, controller, null, views);
    }

    public OrchidView(OrchidContext context, OrchidController controller, Map<String, Object> data, String... views) {
        super(new StringResource("", new OrchidReference(context, "view.html"), data), "view", "Admin");
        this.controller = controller;
        this.layout = "layoutBase";
        this.template = views;

        context.getInjector().injectMembers(this);
    }

// Assets
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void loadAssets() {
        addJs("assets/js/shadowComponents.js");
    }

    @Override
    protected void collectThemeScripts(List<JsPage> scripts) {
        scripts.addAll(context.getAdminTheme().getScripts());
    }

    @Override
    protected void collectThemeStyles(List<CssPage> styles) {
        styles.addAll(context.getAdminTheme().getStyles());
    }

// View renderer
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public String getTemplateBase() {
        return "server/" + super.getTemplateBase();
    }

    @Override
    public String getLayoutBase() {
        return "server/" + super.getLayoutBase();
    }

// Other
//----------------------------------------------------------------------------------------------------------------------

    public List<AdminList> getAdminLists() {
        return this.adminLists.get()
                .stream()
                .sorted(Comparator.comparing(o -> o.getListClass().getSimpleName()))
                .collect(Collectors.toList());
    }

    public List<AdminList> getImportantAdminLists() {
        return getAdminLists()
                .stream()
                .filter(AdminList::isImportantType)
                .collect(Collectors.toList());
    }

    public AdminList getGenerators() {
        for (AdminList list : adminLists.get()) {
            if (list.getKey().equals("OrchidGenerator")) {
                return list;
            }
        }

        return null;
    }

    public String getDescriptionLink(Object o) {
        Class className = (o instanceof Class) ? (Class) o : o.getClass();

        try {
            return context.getBaseUrl() + "/admin/describe?className=" + URLEncoder.encode(className.getName(), "UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getDescriptiveName(Object o) {
        return Descriptive.getDescriptiveName((o instanceof Class) ? (Class) o : o.getClass());
    }

    public String getDescription(Object o) {
        return Descriptive.getDescription((o instanceof Class) ? (Class) o : o.getClass());
    }

    public String getDescriptionSummary(Object o) {
        return Descriptive.getDescriptionSummary((o instanceof Class) ? (Class) o : o.getClass());
    }

    public List<OrchidTask> getTasks() {
        return this.context.resolveSet(OrchidTask.class)
                .stream()
                .sorted(Comparator.comparing(OrchidTask::getName))
                .collect(Collectors.toList());
    }

    public List<OrchidCommand> getCommands() {
        return this.context.resolveSet(OrchidCommand.class)
                .stream()
                .sorted(Comparator.comparing(OrchidCommand::getKey))
                .collect(Collectors.toList());
    }

    public List<OrchidCollection> getCollections() {
        return this.context.getCollections()
                .stream()
                .sorted(Comparator.comparing(OrchidCollection::getCollectionType))
                .collect(Collectors.toList());
    }

    public boolean isFullscreen() {
        return this.type != null && this.type == Type.Fullscreen;
    }

}
