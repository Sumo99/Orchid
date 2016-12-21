package com.eden.orchid;

import com.caseyjbrooks.clog.Clog;
import com.eden.orchid.compilers.Compiler;
import com.eden.orchid.compilers.ContentCompiler;
import com.eden.orchid.compilers.PreCompiler;
import com.eden.orchid.compilers.SiteCompilers;
import com.eden.orchid.generators.Generator;
import com.eden.orchid.generators.SiteGenerators;
import com.eden.orchid.options.Option;
import com.eden.orchid.options.SiteOptions;
import com.sun.javadoc.RootDoc;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.json.JSONObject;

// TODO: Handle priority clashes
// TODO: create a json data-getter which parses input like 'options.d' and make root private
// TODO: Create a self-start method so that it can be run from within a server. Think: always up-to-date documentation at the same url (www.myjavadocs.com/latest) running from JavaEE, Spring, even Ruby on Rails with JRuby!
public final class Orchid {
    public static int optionLength(String option) {
        return SiteOptions.optionLength(option);
    }

    private static JSONObject root = new JSONObject();

    private static Theme theme;

    /**
     * Start the Javadoc generation process
     *
     * @param rootDoc  the root of the project to generate sources for
     * @return Whether the generation was successful
     */
    public static boolean start(RootDoc rootDoc) {
        pluginScan();
        parseOptions(rootDoc);
        if(shouldContinue()) {
            generationScan(rootDoc);
            generateIndex(rootDoc);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Step one in the Orchid compilation process: scan the classpath for all classes tagged with @AutoRegister and
     * register them according to their type.
     *
     * If you need to register plugins that are not one of the classes or
     * interfaces defined in Orchid Core, you can register it within a static initializer. Every matching AutoRegister
     * class has an instance created, so you are guaranteed to run the static initializer of any AutoRegister class.
     */
    private static void pluginScan() {
        FastClasspathScanner scanner = new FastClasspathScanner();
        scanner.matchClassesWithAnnotation(AutoRegister.class, (matchingClass) -> {
            try {
                Clog.d("AutoRegistering class: #{$1}", new Object[]{matchingClass.getName()});
                Object instance = matchingClass.newInstance();

                // Register compilers
                if(instance instanceof Compiler) {
                    Compiler compiler = (Compiler) instance;
                    SiteCompilers.compilers.put(compiler.priority(), compiler);
                }
                if(instance instanceof ContentCompiler) {
                    ContentCompiler compiler = (ContentCompiler) instance;
                    SiteCompilers.contentCompilers.put(compiler.priority(), compiler);
                }
                if(instance instanceof PreCompiler) {
                    PreCompiler compiler = (PreCompiler) instance;
                    SiteCompilers.precompilers.put(compiler.priority(), compiler);
                }

                // Register generators
                else if(instance instanceof Generator) {
                    Generator generator = (Generator) instance;
                    SiteGenerators.generators.put(generator.priority(), generator);
                }

                // Register command-line options
                if(instance instanceof Option) {
                    Option option = (Option) instance;
                    SiteOptions.optionsParsers.put(option.priority(), option);
                }
            }
            catch (IllegalAccessException|InstantiationException e) {
                e.printStackTrace();
            }
        });
        scanner.scan();
    }

    /**
     * Step two in the Orchid compilation process: parse all command-line args using the registered SiteOptions.
     *
     * @param rootDoc
     */
    private static void parseOptions(RootDoc rootDoc) {
        root.put("options", new JSONObject());
        SiteOptions.startDiscovery(rootDoc, root.getJSONObject("options"));
    }

    /**
     * Perform a sanity-check to make sure all required site components have been set. In some cases, warnings are
     * issued instead of failing
     */
    private static boolean shouldContinue() {
        boolean shouldContinue = true;

        Clog.d(query("options").getElement().toString());

        if(OrchidUtils.isEmpty(query("options.d"))) {
            Clog.e("You MUST define an output directory with the '-d' flag. It should be an absolute directory which will contain all generated files.");
            shouldContinue = false;
        }

        if(theme == null) {
            Clog.e("You MUST define a theme with the '-theme` flag. It should be the fully-qualified class name of the desired theme.");
            shouldContinue = false;
        }
        else {
            if(SiteCompilers.getContentCompiler(theme.getContentCompilerClass()) == null) {
                Clog.e("Your selected theme's content compiler could not be found.");
                shouldContinue = false;
            }

            for(Class<? extends Compiler> compiler : theme.getRequiredCompilers()) {
                if(SiteCompilers.getCompiler(compiler) == null) {
                    Clog.e("Your selected theme's depends on a compiler that could not be found: #{$1}.", new Object[]{compiler.getName()});
                    shouldContinue = false;
                }
            }
        }

        if(OrchidUtils.isEmpty(query("options.resourcesDir"))) {
            Clog.w("You should consider defining source resources with the '-resourcesDir' flag to customize the final styling or add additional content to your Javadoc site. It should be the absolute path to your project's local resources directory.");
        }

        return shouldContinue;
    }

    /**
     * Step four in the Orchid compilation process: run all registered generators, gathering all global data and
     * generating partial files to be loaded as part of the page in the index
     *
     * @param rootDoc
     */
    private static void generationScan(RootDoc rootDoc) {
        root.put("generators", new JSONObject());
        SiteGenerators.startDiscovery(rootDoc, root.getJSONObject("generators"));
    }

    /**
     * Step five in the Orchid compilation process: generate the final site from the theme
     *
     * @param rootDoc
     */
    private static void generateIndex(RootDoc rootDoc) {
        root.put("root", new JSONObject(root.toString()));
        theme.generate(rootDoc, root);
    }

    public static JSONElement query(String pointer) {
        if(OrchidUtils.isEmpty(pointer)) {
            return new JSONElement(root);
        }

        pointer = pointer.replaceAll("\\.", "/");

        if(!pointer.startsWith("/")) {
            pointer = "/" + pointer;
        }

        Object object = root.query(pointer);

        try {
            return new JSONElement(object);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject getRoot() {
        return root;
    }

    public static Theme getTheme() {
        return theme;
    }

    public static void setTheme(Theme theme) {
        Orchid.theme = theme;
    }

    //    private static int selfStart() {
//        //https://github.com/arafalov/Solr-Javadoc/blob/master/JavadocIndex/Indexer/src/com/solrstart/javadoc/indexer/Index.java#L224
//        return Main.execute("JavadocIndexer", "com.solrstart.javadoc.indexer.Index", new String[]{
//                "-sourcepath", sourcepath,
//                "-subpackages", subpackages
//        });
//    }
}