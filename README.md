# Acorn
A UI framework for making pretty, interactive websites with webgl.

See current [documentation](http://acornui.com/docs)

## Dev Environment Setup
1. Install Intellij IDEA
    - [Download Ultimate](https://www.jetbrains.com/idea/download/download-thanks.html)
    - [Download Community](https://www.jetbrains.com/idea/download/download-thanks.html?code=IIC)
2. Checkout new project from version control (or just `git clone` from the command line)  
   (https://github.com/PolyForest/Acorn)
    - Opt out of creating a project from the source
3. Open `Acorn` folder in IDEA
4. Setup your Path Variables in IDEA  
   (Settings/Preferences > Appearance & Behavior > Path Variables)
    - `ACORNUI_HOME` Path Variable to acorn local repo path  
    - `ACORNWEBDIST_HOME` Path Variable to acorn-webdist local repo path  
      (If you plan to generate acorn documentation for a release)
5. [Download OpenJDK 9.0.4](http://jdk.java.net/archive/)
6. Add a OpenJDK as a SDK, renamed to the specific version as can be seen in the Project SDK drop-down  
   (Project Structure > Platform Settings > SDKs > +)
    - Windows > point to `C:\Program Files\Java\jdk-9.0.4.jdk`
    - MAC > point to `/Library/Java/JavaVirtualMachines/jdk-9.0.4.jdk/Contents/Home`
7. Set your Project SDK to the JDK (= 9)  
   (Project Structure > Project Settings > Project > Project SDK > Project SDK)
8. Get dependencies  
   (Run > Run… > getDependencies.kts)

## Style Guide
- Git commit style guide - https://chris.beams.io/posts/git-commit/
- Kotlin code style guide - https://kotlinlang.org/docs/reference/coding-conventions.html





### Attribution:
Some icons by [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/). Licensed under a Creative Commons Attribution 3.0 License.
