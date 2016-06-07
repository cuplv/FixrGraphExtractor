# Instruction on processing an Android APP

## Process the compiled class files

Paths:
- `<android-Camera2Basic>` is the path to the android-Camera2Basic sample app
- `<soot-jar-path>` is the path to soot
- `<android-jar-path>` is the path to the android jar
- `<rt-path>` path to $JAVA_HOME/lib/rt.jar
- `<output-dir>` is the output directory

The following command process the `CameraActivity` class and generates its Jimple representation:
```java -jar <soot-jar-path> -allow-phantom-refs --f J -src-prec java -cp <android-jar-path>:<rt-path>:<android-Camera2Basic>/Application/build/intermediates/classes/debug  -output-dir <output-dir> com.example.android.camera2basic.CameraActivity```

