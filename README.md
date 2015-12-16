# UtilsLib

A general utility library for Android Development

**Checkout the [Wiki page](https://github.com/SpazeDog/utils-lib/wiki) to get started**

### Include Library
-----------

**Maven**

Reflect Tools is available in Maven respository at [Bintray](https://bintray.com/dk-zero-cool/maven/utils-lib/view) and can be accessed via jCenter. 

```
dependencies {
    compile 'com.spazedog.lib:utils-lib'
}
```

**Android Studio**

First download the [utilsLib-release.aar](https://github.com/SpazeDog/utils-lib/raw/1.x/projects/utilsLib-release.aar) file. 

Place the file in something like the `libs` folder in your module. 

Open your `build.gradle` file for your module _(not the main project version)_. 

```
dependencies {
    compile(name:'utilsLib-release', ext:'aar')
}

repositories {
    flatDir {
        dirs 'libs'
    }
}
```

**Eclipse/ADT**

Download the source and import it into eclipse. Then simply include the new library project to your main project.
