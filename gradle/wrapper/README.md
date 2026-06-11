# gradle-wrapper.jar

В этой папке должен лежать бинарный файл `gradle-wrapper.jar` — без него
`./gradlew` не запустится. Его нельзя создать текстом, он генерируется Gradle.

Сгенерируй его один раз на машине с установленным Gradle (или в Android Studio,
который сделает это автоматически при открытии проекта):

```sh
gradle wrapper --gradle-version 8.9
```

После этого `gradle/wrapper/gradle-wrapper.jar` появится и его нужно
закоммитить (в `.gitignore` для него сделано исключение).
