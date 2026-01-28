pom.xml should have all the necessary LWJGL dependencies.
You may need to change
<lwjgl.natives>natives-linux</lwjgl.natives>
to natives-windows or natives-macos, see https://www.lwjgl.org/customize for other options or if you prefer e.g. gradle.

Entry point is src/Main.java.

The program is entirely uninteractive, except that you can press space to generate a "city" and have it connect to other
cities by roads.
