I&#8217;ve been working on a plugin system for my [theme switcher project][1], and I think now it&#8217;s the time to introduce it here and describe my rationale behind its design.

<!--more-->

## Architecture of a theme switcher class

I described the actions performed by the previous, bash-based theme switcher in [the previous post][2].

The application will contain a `ThemeSwitcher` class responsible for reloading an already set theme or switching to a new one. It will have the following dependencies:

  * an instance of [`RootConfigMapping`][2]
  * a collection of objects representing themes
  * a callable prompt for choosing a theme to be set
  * a collection of theme appliers &#8211; components for performing application-specific operations necessary for setting a theme, like changing colors for dunst, setting a theme for vim, restarting i3wm, etc.

Saving color changes to X resources database will be performed by the theme switcher object itself. It will also differ from its original, Bash implementation in that it will not rely on reading .Xresources and having it source a symlink to the currently set theme &#8211; instead, the theme file will be directly merged with the current settings by executing `xrdb -merge` command, with a path to a newly selected theme as its argument.

## Coming up with a plugin system

Other than applications I&#8217;m using myself, there are many other projects for which color schemes could be configured. For example, instead of using rofi and i3wm, one could use dmenu and bspwm, or any other window manager. I could also imagine extending my project with a support for GTK themes and theming other applications that don&#8217;t read their colors from the database or that need additional operations to be performed when applying a theme change. Since I was already planning to make application-specific theme appliers as components of the theme switcher object, providing such dependencies by using a plugin system seemed like a natural choice.

I considered using [Yapsy][3], [PluginBase][4] or a [custom solution based on plugin modules with an application-specific prefix][5], and I chose the latter. It seems to be the simplest and, since it&#8217;s based on searching through top-level installed modules, it works well with package installation without any additional configuration. Yapsy and PluginBase rely on plugins being placed in specific directories, and the Yapsy plugin system requires plugins to provide things I didn&#8217;t think I needed for my use case.

## Initial design of my plugin API

My initial plans for the plugin system consisted of having it perform the following actions for each loaded plugin:

  * unpack a mapping containing configuration options for the plugin
  * call a function (let&#8217;s call it `initialize`) on a module representing the plugin, passing the options as its keyword arguments
  * get a theme applier component object returned by the function
  * add the component to the collection of theme appliers used by the theme switcher

I also considered making the application more extensible by making it possible for plugins to provide other kinds of components to it, but since I had only the theme applier components on my mind, I didn&#8217;t feel very compelled to design it that way&#8230; until I realized I could make my application use a pluggable prompt for choosing a theme. I wanted to use a rofi-based theme menu myself, but I could imagine others preferring to use a command line prompt or dmenu for this. As a result, a more general plugin system became a requirement.

Having this on my mind, I realized my current design for plugin interface might not be a very good idea. Now I knew my plugin system would need a way to recognize where a plugin-provided component belongs, and it would increase complexity of both the plugin system and the interface of a plugin module, and extending the system with more types of components would likely make the matters worse. Plus, I already knew it would be useful to provide some kind of debugging of plugin configuration. Since config options would be passed as keyword arguments of `initialize`, passing unexpected options or missing some required ones would result in a `TypeError` that should be handled in a way that would point a user to a possible error in his config file.

## Designing a better API

For these reasons, I decided to modify my design so that a theme switcher object would be passed to a plugin module function and modified by it. This way, I wouldn&#8217;t have to expect any return values from the function, and since the theme switcher object would also depend on the configuration, I wouldn&#8217;t need to provide the options as separate arguments of the function, so I wouldn&#8217;t need to implement a mechanism responsible for calling plugin module functions with different signatures. The configuration errors for a plugin would either be handled by the config object itself or by the function (now renamed to `apply_to`, as it would be responsible for applying changes brought by the plugin to the theme switcher object). Not only this solution would be simpler, but it would also be more flexible.

There were still some problems with it, though:

  * the flexibility is problematic, too. After all, by passing a whole theme switcher object to a plugin, I would expose every attribute of this object to the `apply_to` function, including ones I didn&#8217;t intend to be used by it, like methods responsible for switching a theme or any properties that shouldn&#8217;t be accessible to the function.
  * the theme-switcher object would be in an invalid state until all its required pluggable dependencies were provided by plugins.

I could solve the second problem by having the core application provide the theme switcher object with some defaults for required dependencies, but first: once I became convinced I&#8217;d like to implement a more general plugin system, I wanted all the pluggable dependencies to be provided as plugins, and second: the first problem would still persist.

I decided to modify the solution: instead of passing a theme switcher object to a plugin module function, I decided to introduce a builder object that would expose properties for setting pluggable dependencies. After all configured plugins have been activated by running their `apply_to` function with the builder object as its argument, the `build` method of the object would be called and it would return an instance of theme switcher to be used by the application.

This way I avoid giving plugins too much access to things they shouldn&#8217;t alter, having the theme switcher object in an invalid state and having core application provide defaults for pluggable dependencies.

Of course, Python is a dynamic language and the limits imposed by an interface of an object aren&#8217;t as rigid as those imposed by classes and formal interface types in statically typed languages with access modifiers. For example, a developer of a plugin could easily access an attribute of the builder that is conventionally marked as &#8220;protected&#8221; and modify or replace it, or they could replace the `build` method with an alternative one. Python is flexible in that regard, but it doesn&#8217;t mean having an object provide a set of well documented methods designed for intended use cases isn&#8217;t important.

 [1]: https://github.com/piotr-rusin/base16-theme-switcher
 [2]: {{< relref "a-new-project-base16-theme-switcher.md" >}}
 [3]: http://yapsy.sourceforge.net/index.html
 [4]: http://pluginbase.pocoo.org/
 [5]: https://packaging.python.org/guides/creating-and-discovering-plugins/#using-naming-convention
