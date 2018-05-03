# bgbilling-reference-config-generator

__bgbilling-reference-config-generator__ (далее __brcg__) -- это генератор референсных конфигов для системы [BGBilling](https://bgbilling.ru/) и модулей для неё.

## Требования

Для сборки необходимы:

* git <https://git-scm.com/downloads>
* sbt <http://www.scala-sbt.org/>
* JDK 8

Для выполнения необходимы:

* JRE 8

## Сборка из исходных кодов

Для сборки выполните команды

    git clone https://github.com/alexanderfefelov/bgbilling-reference-config-generator.git
    cd bgbilling-reference-config-generator
    sbt assembly

В случае успеха в каталоге `target\scala-2.12` будет создан файл `brcg.jar`.

## Запуск

Для запуска выполните команду

    java -jar brcg.jar [параметры]

Для получения списка всех параметров выполните команду

    java -jar brcg.jar --help

## Как это работает?

В состав дистрибутивов системы BGBilling и серверных частей модулей для неё входят файлы
`MODULE_NAME.properties` и `config.xml`. __brcg__ извлекает информацию из этих файлов
и представляет её в удобочитаемом виде.
