Configurability and availability of multiple desktop environments and window managers are some of the things I like about using Linux-based operating systems. In recent months I gradually migrated from using full-fledged desktop environments like [Cinnamon][1] and [Xfce][2] to using [i3wm][3] in combination with [urxvt][4], [dunst][5] and other applications commonly used with light window managers. While experimenting with their configuration, I discovered [base16][6] and its derivative: [base16-xresources][7] &#8211; projects providing color schemes for syntax highlighting and application theming, each based on 16 colors.

<!--more-->

## Theme-switching commands

To easily test its constituent color themes, I implemented [a few bash functions][8] in my [dotfiles][9] project. Their task was to allow me to switch color schemes for a few applications I&#8217;ve been using by simply running a console command.

The script finds all files with .Xresources extension under a specific directory for storing the color themes and generates a command performing a set of actions for each theme. The actions are:

  * [symlinking the file of a selected theme in my home directory][10]. The symlink [is sourced by .Xresources][11] file which additionally configures colors for [rofi][12], based on definitions provided in the theme file.
  * [loading .Xresources file][13] in my home directory with [xrdb (X resource database manager)][14] command. That way, the colors provided by the selected theme can be used by applications using colors provided for them in the database. In my case, the applications affected by this are rofi and urxvt. I3wm is also affected, but the mechanism of configuration is slightly different &#8211; in i3wm config, the colors are loaded from X resource database and then set for various elements of i3wm graphical user interface.
  * [setting a corresponding theme for neovim][15] &#8211; as console applications, vim and neovim use colors configured in X resources database when run in a terminal emulator relying on these colors, but the results don&#8217;t always look good. For this reason, when a theme from base16-xresources project is set, a corresponding theme from [base16-vim][16] project is set for neovim.
  * [setting colors from a theme for dunst][17]. Dunst doesn&#8217;t use colors from X resources database, but relies on color values set in its own configuration file. To apply colors from a base16 theme, the script reads contents of the theme file, replaces [dunstrc][18] configuration file with [dunstrc-template][19] and edits resulting dunstrc, setting window background, foreground and border colors. There is a [base16-dunst][20] project, but I decided not to use it, mainly because of the differences between the way the project configures dunst color themes and the way I wanted to do it. The project sets different background colors for different message urgency levels, while I wanted to use the same background color for all of them, and it doesn&#8217;t set the border color. Considering this, I thought it would be simpler to just read the colors from a base16-xresources theme file instead of relying on another project.
  * [performing additional application-specific operations][21], like restarting i3wm and desktop notification daemon for the color changes to take effect.

## Replacing commands with something better

I decided to extend my theme switcher with proper error handling and support for choosing themes from a menu. I also decided to handle its increasing complexity by switching to object oriented programming and moving the code to a [separate project][22]. At first I wanted to continue with bash, partly because I could keep using existing code and partly because I felt like OOP in bash was an interesting challenge, but soon I changed my mind &#8211; I decided I prefer the convenience of a language with a large, general purpose standard library and a built-in support of OOP, so I switched to Python.

The application is all about configuring color themes for other applications, and it maintains its own configuration, too, so naturally its most basic parts are the functions and classes related to handling configs. They are all implemented and documented in [`config_structures` module][23], so I&#8217;m not going to describe them here.

In future posts, I&#8217;d like to describe the plugin system I designed for the application and how it will handle themes and theme changes.

 [1]: https://en.wikipedia.org/wiki/Cinnamon_(software)
 [2]: https://xfce.org/
 [3]: https://i3wm.org/
 [4]: https://wiki.archlinux.org/index.php/rxvt-unicode
 [5]: https://dunst-project.org/
 [6]: https://chriskempson.github.io/base16/
 [7]: https://github.com/chriskempson/base16-xresources
 [8]: https://github.com/piotr-rusin/dotfiles/blob/5beebb4ab2371aec4e34ff3043cd6c3573e8ae42/.zshrc#L103
 [9]: https://github.com/piotr-rusin/dotfiles
 [10]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.zshrc#L146
 [11]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.Xresources#L33
 [12]: https://github.com/DaveDavenport/rofi
 [13]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.zshrc#L147
 [14]: https://linux.die.net/man/1/xrdb
 [15]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.zshrc#L115
 [16]: https://github.com/chriskempson/base16-vim
 [17]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.zshrc#L127
 [18]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.config/dunst/dunstrc
 [19]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.config/dunst/dunstrc-template
 [20]: https://github.com/khamer/base16-dunst
 [21]: https://github.com/piotr-rusin/dotfiles/blob/0a8690406b52553729b347c9f3d63b72e70bb231/.zshrc#L150
 [22]: https://github.com/piotr-rusin/base16-theme-switcher
 [23]: https://github.com/piotr-rusin/base16-theme-switcher/blob/4071e28e8afb4cc92736fc9db7ba0f10f3bc163c/base16_theme_switcher/config_structures.py
