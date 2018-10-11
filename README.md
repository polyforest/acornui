# Acorn

## Dev Environment Setup
1. Install Intellij IDEA
    - [Download Ultimate](https://www.jetbrains.com/idea/download/download-thanks.html?platform=mac)
    - [Download Community](https://www.jetbrains.com/idea/download/download-thanks.html?platform=mac&code=IIC)
2. Checkout new project from version control  
   (https://github.com/PolyForest/Acorn)
    - Opt out of creating a project from the source
3. Open `Acorn` folder in IDEA
4. Change `ACORNUI_HOME` Path Variable to acorn project path  
   (Settings/Preferences > Appearance & Behavior > Path Variables)
5. [Download JDK](http://www.oracle.com/technetwork/java/javase/downloads/jre10-downloads-4417026.html)
6. Add a JDK (>= 1.8) as a SDK  
   (Project Structure > Platform Settings > SDKs > +)
    - Windows > point to `C:\Program Files\Java\jdk10.0.1`
    - MAC > point to `/Library/Java/JavaVirtualMachines/jdk-10.x.x.jdk/Contents/Home`
7. Set your Project SDK to the JDK (>= 1.8)  
   (Project Structure > Project Settings > Project > Project SDK > Project SDK)
8. Get dependencies  
   (Run > Run… > getDependencies.kts)

## Style Guide
- Git commit style guide - https://chris.beams.io/posts/git-commit/
- Kotlin code style guide - https://kotlinlang.org/docs/reference/coding-conventions.html





### Attribution:
Some icons by [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/). Licensed under a Creative Commons Attribution 3.0 License.

https://polyforest.com
