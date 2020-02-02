#!/bin/sh

# Installer

set -eu

prodname=stew
prodnameminor=Stew6
ver=6.0.0-beta2
owner=argius
execname=stew6
execdir=/usr/local/bin

dir=~/.$prodname
baseurl=https://github.com/$owner/$prodnameminor
bindir=$dir/bin
libdir=$dir/lib
extdir=$dir/ext
zipfile=${prodname}-${ver}-bin.zip
zipurl=$baseurl/releases/download/v$ver/$zipfile
jarfile=${prodname}-${ver}.jar
jarpath=$libdir/$jarfile
binfile=$bindir/$execname
execfile=$execdir/$execname
javaopts=""
classpath="$jarpath:$extdir/*"

errexit() {
  printf "\033[31m[ERROR]\033[0m" ; echo " $1"
  echo "Installation incomplete."
  exit 1
}

onexit() {
  if [ -n "$tmpdir" ]; then
    cd $tmpdir/..
    test -f $tmpdir/$jarfile && rm $tmpdir/$jarfile
    test -f $tmpdir/$zipfile && rm $tmpdir/$zipfile
    rmdir $tmpdir
  fi
}

tmpdir=`mktemp -d /tmp/${prodname}-XXXXXX`
trap onexit EXIT
trap "trap - EXIT; onexit; exit -1" 1 2 15 # SIGHUP SIGINT SIGTERM

echo "This is the installer of \"$prodname\"."
echo ""

# OS specific settings
case "`uname -a`" in
  Linux* )
    echo "adjusting for Linux"
    if [ -d ~/.local/bin ] && [ -w ~/.local/bin ]; then
      execdir=~/.local/bin
    elif [ -d ~/bin ] && [ -w ~/bin ]; then
      execdir=~/bin
    elif [ -d /usr/local/bin ] && [ -w /usr/local/bin ]; then
      execdir=/usr/local/bin
    else
      errexit "cannot detect writable exec dir, requires ~/.local/bin or ~/bin"
    fi
    execfile=$execdir/$execname
    echo ""
    ;;
  *BSD* )
    echo "adjusting for *BSD"
    if [ -d ~/bin ] && [ -w ~/bin ]; then
      execdir=~/bin
    elif [ -d /usr/local/bin ] && [ -w /usr/local/bin ]; then
      execdir=/usr/local/bin
    else
      errexit "cannot detect writable exec dir, requires ~/bin"
    fi
    execfile=$execdir/$execname
    echo ""
    ;;
  Darwin* )
    echo "adjusting for Darwin (macOS)"
    javaopts="$javaopts -Xdock:name=$prodnameminor -Xdock:icon=$dir/stew.icns -Dapple.laf.useScreenMenuBar=true -Dstew.ui.suppressGenerateMnemonic=true"
    echo ""
    ;;
  CYGWIN* )
    echo "adjusting for Cygwin"
    jarpath=`cygpath -m $jarpath`
    javaopts="$javaopts -Djline.terminal=jline.UnixTerminal -Dstew.user.home=`cygpath -m ~/`"
    classpath="$jarpath;`cygpath -m $extdir`/*"
    echo ""
    ;;
esac

echo "installing version $ver into $execdir and $dir,"
echo "and uses $tmpdir as a working directory."
cd $tmpdir || errexit "failed to change directory"
echo "downloading: $zipurl"
curl -fsSLO $zipurl || errexit "failed to download zip"
unzip -o $zipfile $jarfile || errexit "failed to unzip"

mkdir -p $libdir && cp -fp $jarfile $libdir/
test -f $libdir/$jarfile || errexit "failed to copy jar file"
mkdir -p $bindir && ( echo "#!/bin/sh" ; echo "java -cp \"$classpath\" $javaopts $execname.App \"\$@\"" ) > $binfile
test -f $binfile || errexit "failed to create $binfile"
chmod +x $binfile || errexit "failed to change a permission"
ln -sf $binfile $execfile || errexit "failed to create a symlink of $binfile"
mkdir -p $extdir || echo "warning: failed to create $extdir"

echo "\"$prodname\" has been installed to $execfile and $dir/ ."
echo "checking installation => `$execname --version`"
test -n "`$execname --version`" || errexit "failed to check installation"
echo ""
echo "Installation completed."
