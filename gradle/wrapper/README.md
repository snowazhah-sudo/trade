# gradle-wrapper.jar

Бинарник `gradle-wrapper.jar` уже закоммичен в этой папке (в `.gitignore`
сделано исключение), поэтому `./gradlew` работает «из коробки».

Если когда-нибудь понадобится пересоздать его:

```sh
gradle wrapper --gradle-version 8.9
```
