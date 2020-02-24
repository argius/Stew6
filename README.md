Stew6
=====
Stew6 is a tiny utility tool for database via JDBC.

Stew6 requires Java 1.8 and higher, and JDBC drivers to connect your DBMS.

See [the project's wiki page](https://github.com/argius/Stew6/wiki) for further information.


To Install
----------

Run the following command.

```sh
$ curl -fsSL http://bit.ly/inststew6 | sh

# shorter => $ curl -L j.mp/inststew6|sh
```

Both of these urls are shortened of `https://raw.githubusercontent.com/argius/Stew6/master/install.sh`.

Or download `install.sh` and run `$ sh install.sh`.
To get `install.sh`, clone or download sources, or download a release package.

To uninstall, remove `~/.stew` and `$(which stew6)`.

You need only to download, see [the releases page](https://github.com/argius/Stew6/releases).


How To Start App
----------------

Run following commands.

```
# Run with Swing-GUI mode
$ stew6 --gui

# Run with console mode
$ stew6 --cui

# Run commands and exit
$ stew6 --cui -c connector1 "select * from table1 limit 3"

# Run with JavaFX-GUI mode (experimental)
$ stew6 --fx
```

License
-------

Apache License 2.0
