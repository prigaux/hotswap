Replaces classes on a running JVM. This task can take the following arguments:

  * verbose - prints class names as they are being swapped
  * failonerror - causes task to exit if any error occurs
  * host - the host of the JVM to be attached to (defaults to localhost)
  * port - the port number to be used to attach to the JVM
  * name - if not using a socket connection, this name is the share memory label

Of these arguments, the host and port are required. Or, the name can be used instead to indicate a shared mem connection.

See the JPDA documentation for details on the JVM runtime options. http://java.sun.com/j2se/1.4.2/docs/guide/jpda/conninv.html#Invocation
These are the options that work with the example below: -Xdebug -Xrunjdwp:transport=dt\_socket,address=9000,server=y,suspend=n

Add this line to your build.xml

```
<taskdef name="hotswap" classname="dak.ant.taskdefs.Hotswap"/>
```

This is an example of how to hotswap with a JVM on port 9000 on your local machine

```
<target name="hotswap">
  <tstamp>
    <format property="class.tstamp" pattern="MM/dd/yyyy kk:mm:ss" />
  </tstamp>

  <javac destdir="${build.classes.dir}>
    <fileset dir="${dev.src.dir}" includes="**/*.java"/>
  </javac>

  <hotswap verbose="true" port="9000">
    <fileset dir="${build.classes.dir}" includes="**/*.class">
      <date datetime="${class.tstamp}" pattern="MM/dd/yyyy kk:mm:ss" when="after" granularity="0"/>
    </fileset>
  </hotswap>
</target>
```

This example illustrates creating a timestamp with granularity of seconds. Then, compiling the java files that have changed. Finally, the target uses the timestamp, formatted with seconds, to pick files that have changed since that timestamp and pass them to the target. To get this functionality in the date selector, you'll need one of these;

  * The ant 1.6.2 release or newer. OR
  * The ant 1.6.1 release, plus a jar in the lib directory. (it contains the patch for the date selector). look in the downloads dir

For Vim users, I recommend using ant.vim to allow you to call a hotswap task from Vim. It (along with this task) lets you edit code, then with a few keystrokes, hotswap the new code into your running app! Very Cool!

I can be reached via e-mail and I lurk on the Ant developers list.