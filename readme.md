<a name="services_accounts"></a>
# Введение

vUpdate выполняет создание версий, обновление, запуск и мониторинг работы сервисов.
Хранение истории позволяет откатывать изменения назад. Централизованное хранение журналов работы
и отчётов о сбоях является удобным для мониторинга работы системы и оперативного устранения ошибок.

## Требуемые платформы

В настоящее время vUpdate работает на серверах Linux Ubuntu.
Исходные коды должны находиться в репозитории Git VCS.
Поддерживается работа с облачным сервисом Microsoft Azure.

## Соглашения к оформлению

Для отображения структуры файлов _JSON_ принято следующее.

Пара имя/значение:
- _name_ : строка
- _name:number_ : число
- _name:boolean_ : булевское

Объект
- _name_
  - содержимое объекта

Массив
- _name[]_ - массив строк
- _name:number[]_ - массив чисел
- _name[]_ - массив объектов
  - ... - содержимое объекта
  
Необязательное значение:
- `name`

## Сервисы

Сервисом может считаться целое приложение, или его часть.

### Жизненный цикл сервиса
- Создание разработческой версии сервиса
- Создание клиентской версии сервиса с кастомизацией настроек для конкретного клиента
- Установка/обновление инстанций серверных приложений
- Запись журналов работы сервиса в базу данных
- Запись отчётов о сбоях сервиса в базу данных

## Версии сервисов

### Версия разработчика
Разработчик создаёт на сервере дистрибуции версию сервиса, в которую входят:
- выполнимые файлы
- скрипты с макрорасширениями
- правила создания клиентской версии, установки и обновления

Версия сервиса от разработчика имеет формат _distribution-build_:
- _distribution_ - имя сервера дистрибуции, на котором создана версия.
- _build_ - номер сборки из чисел, разделённых точками, например *4.21.2*.

Таким образом, для сервера дистрибуции _jetto_ и сборки _3.5.1_, 
будет версия _jetto-3.5.1_.

### Клиентская версия
Из версии разработчика создаётся клиентская версия на том сервере дистрибуции, 
где она будет использоваться. 
Клиентская версия содержит кастомизацию обшей версии под конкретного клиента.
Она может уточнять конкретные настройки, задавать ключи, сертификаты, и т.д.

Начальная клиентская версия имеет тоже название, что и версия разработчика, 
с которой она была создана.
Если настройки клиента поменялись, клиентская версия пересоздаётся заново. 
Новая версия будет иметь окончание ._N, где N - номер генерации клиентской версии.
Например, созданная с версии разработчика _jetto-3.5.1_, клиентская версия будет называться _jetto-3.5.1_, 
следующие сборки будут называться _jetto-3.5.1_1_, _jetto-3.5.1_2_, ...

### Желательные версии (Desired Versions)

Сервер дистрибуции хранит некоторое количество последних сгенерированных версий сервиса.
Для того, чтобы указать, какую именно версию использовать в текущий момент, предназначен список желательных версий
в формате _сервис-версия_.
Есть список желательных девелоперских версий и клиентских версий.

При генерации новой версии, номер новой версии становится желательным для данного сервиса.
Номер желательной версии можно изенить вручную. 

# Сервисы vUpdate

vUpdate сам состоит из сервисов. Таким образом, vUpdate содаёт версии самого себя и обновляет сам себя.
Сервисы vUpdate:

- scripts - Скрипты (Shell, YAML)
- distribution - Сервер дистрибуции
  - Backend
    - Distribution Web Server (платформа [Scala Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html))
      - Поддерживает запросы GraphQL (платформа [Sangria GraphQL](https://github.com/sangria-graphql/sangria))
    - База данных [MongoDB](https://github.com/mongodb/mongo)
  - Frontend 
    - Distribution Dashboard (платформа [React Js](https://reactjs.org)
- builder - Генератор версий (Scala command line application)
- updater - Установщик инстанции сервисов (Scala command line application)

## Скрипты

Производят начальную установку и обновление сервисов.

- _.update.sh_ - базовый скрипт, обновляет сами скрипты, обновляет, запускает сервис
  - _distribution_ - скипты поддержки сервера дистрибуции
    - _.create_distribution_service.sh_ - прописывает сервер дистрибуции как сервис операционной системы, 
                                          в настоящее время поддерживается только Linux systemd. 
    - _.make_distribution_config.sh_ - создание начального файла конфигурации, используется _builder_
    -                                  при создании сервера дистрибуции
    - _distribution.sh_ - запуск сервера дистрибуции
  - _builder_ - скипты поддержки _builder_
    - _builder.sh_ - запускает _builder_
  - _updater_ - скипты поддержки _updater_
    - _.update.sh_ - запускает _updater_

## Сервер дистрибуции

Ключевым компонентом vUpdate является сервер дистрибуции, с управлением через Web-интерфейс.
На сервере дистрибуции создаются новые версии сервисов. Для создания новой версии,
_distribution_ запускает _builder_.

<a name="create_distribution_server"></a>
### Создание сервера дистрибуции

Сервер дистрибуции может быть собран и установлен с исходных кодов, 
или с другого сервера дистрибуции.

#### Сборка и установка сервера дистрибуции с исходных кодов

Установите базу данных MongoDB.

Клонируйте репозиторий vUpdate на сервер дистрибуции:

`git clone git@github.com:jorkey/vUpdate.git`

Выполните команду:

`sbt "project builder; run buildProviderDistribution [cloudProvider=?] distribution=? directory=? port=? title=? mongoDbName=? [serviceOS=?]"`

Вместо `?` подставьте значения:
- _cloudProvider_
  - тип облачного сервиса, если сервер дистрибуции находится в облаке:
      - Azure - единственное на данный момент поддерживаемое облако
- _distribution_
  - уникальное имя сервера дистрибуции, без пробелов
- _directory_
  - каталог для установки
- _port_
  - порт для сервиса http (сервис https можно определить позднее в настройках)
- _title_
  - короткое описание сервера дистрибуции
- _mongoDbName_
  - имя базы данных MongoDB
- _serviceOS_
  - в настоящее время только для установки на Linux. 
  - true, если сервер дистрибуции устанавливается как сервис systemd Linux. 

После завершения команды должен запуститься процесс сервера дострибуции.

#### Установка сервера дистрибуции с другого сервера дистрибуции

Заведите учётную запись для нового _customer_ на исходном сервере дистрибуции (см [Учётные записи сервисов](#services_accounts)).

Установите базу данных MongoDB.

Скопируйте во временный каталог из каталога исходного сервера дистрибуции файл _.update.sh_.
Создайте там же скрипт _clone_distribution.sh_:

```
#!/bin/bash -e
set -e

serviceToRun=builder

distributionUrl=?
accessToken=?

. .update.sh "$@" buildConsumerDistribution providerUrl=$distributionUrl [cloudProvider=?] distribution=? directory=? port=? title=? mongoDbName=? provider=? consumerAccessToken=? testConsumerMatch=? [serviceOS=?] 
```

Вместо `?` подставьте значения:
- _distributionUrl_
  - URL исходного сервера дистрибуции
- _accessToken_
  - Access Token сервиса _builder_ исходного сервера дистрибуции
    - Зайдите в _Settings/Accounts/Services_ и нажмите на значок ключа для _builder_.  
- _cloudProvider_
  - тип облачного сервиса, если сервер дистрибуции находится в облаке:
    - Azure - единственное на данный момент поддерживаемое облако
- _distribution_
  - уникальное имя сервера дистрибуции
- _directory_
  - каталог для установки
- _port_
  - порт для сервиса http (сервис https можно определить позднее в настройках)
- _title_
  - короткое описание сервера дистрибуции
- _mongoDbName_
  - имя базы данных MongoDB
- _provider_
  - имя исходного сервера дистрибуции
- _consumerAccessToken_
  - Access Token для учётной записи _consumer_
    - Зайдите в _Settings/Accounts/Distribution Consumers_ и нажмите на значок ключа для соответствующей записи.
- _testConsumerMatch_
  - имя сервера дистрибуции тестовой системы
- _serviceOS_
  - в настоящее время только для установки на Linux.
  - true, если сервер дистрибуции устанавливается как сервис systemd Linux.

Выполните скрипт _clone_distribution.sh_. 
После завершения скрипта должен запуститься процесс сервера дострибуции.

### Настройка сервера дистрибуции

#### Конфигурационный файл 

Файл конфигурации _distribution.json_ находится в рабочем каталоге сервера дистрибуции и имеет следующий формат:

- _distribution_ : уникальное имя сервера дистрибуции
- _title_ : заголовок сервера дистрибуции
- _instance_ : идентификатор инстанции виртуальной мащины 
- _jwtSecret_ : ключ кодирования JWT
- _mongoDb_ - настройка подключения к Mongo DB
  - _connection_ : URL подключения 
  - _name_ : имя базы
- _network_
  - _port:number_ : номер серверного порта http или https
  - `ssl` - настройки для сервиса https
    - _keyStoreFile_ : файл jks с ключами и сертификатами
    - _keyStorePassword_ : пароль доступа к key store
- _builder_ - запуск _builder_
  - _distribution_ : имя сервера дистрибуции, на котором запускать _builder_
- _versions_ - история версий
  - _maxHistorySize:number_ : максимальное количество версий сервиса в истории
- _serviceStates_ - состояние сервисов 
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе последнего состояния сервиса. Если в течение этого времени состояние не обновляется, инстанция сервиса считается умершей.
- _logs_ - журналы сервисов
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе записи в журнал
- _faultReports_ - отчёты о сбоях
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе записи отчёта о сбое
  - _maxReportsCount:number_ : максимальное количество отчётов о сбоях в базе

Структура _FiniteDuration_:
- unit - единицы времени: "SECONDS", "HOURS", "MINUTES", "DAYS"
- length:number - количество единиц
  
#### Настройки в Distribution Frontend

Откройте ссылку на сервер дистрибуции в бравсере. 
Введите Account Name: admin и Password: admin.
Вы зашли в систему, как администратор. 

Смените начальный пароль. Для этого войдите в _Settings/Accounts/Users_, выберите пользователя _admin_,
и нажмите "Change Password".

##### Учётные записи

Администрируются в _Settings/Accounts_.

###### Пользователи

Заведите пользователей Dashboard, разработчиков и администраторов.
В информации о верси сервиса будет отображаться имя создавшего его пользователя. 

<a name="services_accounts"></a>
###### Сервисы

Учётные записи сервисов vUpdate. После установки сервера дистрибуции уже присутствуют 
записи _builder_ и _updater_.
Нажав на иконку с ключом, можно полусить Access Key для данного сервиса.
Access Key используется в скриптах запуска и обновления сервисов.

###### Distribution Consumers  

Учётные записи других серверов дистрибуции. 
В учётной записи, помимо прочего, указываются: 
- URL сервера дистрибуции. Его использует _builder_ для удалённой сборки сервиса.
- Services Profile - список разрабатываемых сервисов, которые поставляются данному серверу дистрибуции.

##### Настройка сервисов

###### Development

Описание сервисов для разработки. 
Для каждого сервиса указываются источники исходных кодов:
- URL - ссылка на Git репозиторий в формате git@_host_:...  
- Branch - branch Git репозитория
- Clone Submodules - скачивать вместе с подмодулями

###### Service Profiles

Для определения, как раздавать разрабатываемые сервисы, 
назначаются именованные группы сервисов, именуемые профилями.
Таким образом, профиль является подмножеством списка сервисов для разработки.
Профиль назначается _distribution consumer_ в настройках.
Для обозначения разрабатываемых сервисов, для которых также создаются клиентские версии
на данном сервере дистрибуции, определяется профиль _self_.

##### Providers

Здесь определяются _distribution providers_ данного сервера дистрибуции.
Среди прочего, для _distribution provider_ определяются:
- _Access Token_ - ключ доступа при запросах к серверу
- _Test Consumer_ - имя _consumer distribution_, после тестирования которым, сервисы могут быть установлены на данном сервере
- _Upload State Interval_ - интервал загрузки состояния на _distribution provider_   

### Обновление сервера дистрибуции

Сервер дистрибуции является сервисом vUpdate, а это означает, что он собирается и обновляется по
общим правилам. Сервер периодически сверяет свою текущую версию с клиентской версией в базе данных,
и если они различаются, перезапускается. Скрипты после завершения сервера дистрибуции устанавливают и запускают 
новую версию.

Завершение сервера дистрибуции также происходит, если обновилась клиентская версия скриптов. В этом случае
скрипты обновляют сами себя и запускают сервер дистрибуции снова.

### Объединение в сеть серверов дистрибуции

Сервера дистрибуции могут объединяться в сеть по принципу _provider/consumer_.

_Provider_ предоставляет для _consumer_ заданные в его profile сервисы.
Если на _provider_ появились новые версии, _consumer_ может скачать их, и установить у себя.
Таким образом компания, разрабатывающая сервисы, может завести учётную запись _consumer_
клиента на своём сервере дистрибуции и определить список сервисов, которые она хочет отдавать этому клиенту.

_Consumer_ отправляет _provider_-у:
- Информацию об установленных версиях сервисов _provider_-а.
- Отчёты о сбоях сервисов

_Provider_ также может выполнять _builder_ для _consumer_.
Это необходимо, если на сервере _consumer_ нет достаточных ресурсов для сборки версии.

### История документов в Mongo DB

Все документы vUpdate хранятся в коллекциях специального формата.

У каждого документа, кроме содержательной части, есть дополнительные поля:
- __sequence_ - порядковый номер добавления/модификации документа в коллекции
  - индексировано во возрастанию и убыванию
- __modifyTime_ - время добавления/модификации документа
- __archiveTime_ - время удаления документа
  - индексировано во возрастанию с опцией expire after 7 days

Добавление специальных полей позволяет:
- Сортировать документы в коллекции по порябку их добавления/изменения
- Выяснить дату последней модификации документа
- Хранить историю удалённых документов в течение 7 дней и при необходимости восстанавливать старые версии

## Builder

Производит:
- Установку сервера дистрибуции
- Сборку версий разработчика
- Сборку клиентских версий

### Установка и обновление

Builder устанавливается сервером дистрибуции в каталог _<distributionDir>_/builder/_<distribution>_.
Здесь:
- _distributionDir_ - каталог сервера дистрибуции
- _distribution_ - имя сервера дистрибуции, для которого производится сборка

Установка и обновление производится скриптами vUpdate с сервера дистрибуции, для которого производится сборка.

### Установка сервера дистрибуции

Была описана в разделе (см [Создание сервера дистрибуции](#create_distribution_server).

### Сборка версии разработчика

#### Запуск

Builder запускается сервером дистрибуции.

`builder.sh buildDeveloperVersion service=? version=? sources=? [buildClientVersion=true/false] comment=?`

Здесь:
- _service_ - имя сервиса
- _version_ - номер новой версии
- _sources_ - конфигурация исходных репозиториев, JSON формата:
  - []
    - _name_ - имя репозитория, является именем каталога, куда копируются исходники
    - _git_
      - _url_ - URL доступа к GIT репозиторию
      - _branch_ - branch GIT репозитория
      - `cloneSubmodules` - клинировать вместе с submodules
- _buildClientVersion_ - собирать также клиентскую версию,
- _comment_ - комментарий к новой версии

##### Подготовка каталогов исходных текстов

В каталог `<builderDir>/developer/services/<service>/source` прозводится _clone_ или _pull_ с репозиториев GIT
в подкаталоги с названием _name_ из конфигурации.

#### Конфигурация vUpdate для сервиса

В первом описанном в _sources_ репозитории разработки нужно создать файл _update.json_, 
описывающий правила сборки, установки и запуска сервисов.

Формат файла:

- _update[]_ - описания сервисов
  - _service_ : имя сервиса
  - _build_ - объект, правила сборки версии сервиса
    - `buildCommands[]` - команды сборки версии
      - _command_ : команда сборки
      - `args[]` - параметры сборки
      - `env[]` - окружение запуска команды
        - _'имя'_ : значение
      - `directory` : каталог для выполнения, если не указано, текущий
      - `exitCode:number` : код возврата
      - `outputMatch` : соответствие вывода в stderr и stdout, регулярное выражение
    - _copyFiles[]_ - каталоги файлов для копирования в сборку
      - _sourceFile_ : исходный файл в репозитории сервиса
      - _destinationFile_ : файл в образе версии
      - _except[]_ - массив исключений файлов для копирования в сборку
      - `settings[]` - объект, значения макросов в исходных файлах. При копировании в сборку происходит замена макроса на значение.
        - _'имя макроса'_ : значение макроса
    - _install_
      - `installCommands` : команды установки версии, как в buildCommands
      - `postInstallCommands` : команды после установки версии, как в buildCommands
      - `runService` - правила запуска сервиса
        - _command_ : команда запуска
        - `args[]` - аргументы запуска
        - `logWriter` - запись журналов сервиса на локальный диск
          - _directory_ : каталог для записи журналов
          - _filePrefix_ : начало имени файла журнала, полное имя <filePrefix>.<number>
          - _maxFileSizeMB_ : максимальный размер файла журнала
          - _maxFilesCount_ : максимальное количество файлов журналов
        - `uploadLogs:boolean` - выгрузка журналов сервиса на сервер update
        - `faultFilesMatch` - регулярное выражение для писка файлов с информацией о падении
          при неожиданном завершении сервиса. Файлы включаются в отчёт о сбое.
        - `restartOnFault:boolean` - перезапускать сервис после сбоя
        - `restartConditions` - условия для принудительного завершения сервиса
          - `maxMemoryMB:number` : сервис использовал более указанного количества мегабайт памяти
          - `maxCpu` - сервис использовал более ресурсов CPU, чем указано
            - _percents:number_ : проценты потребления CPU
            - _durationSec:number_ : в течение указанного интервала времени
          - `makeCore:boolean` : для Unix-подобных систем, завершение сервиса сигналом SIGQUIT,
            для образования core dump
          - `checkTimeoutMs:number` :  интервал проверки условий

В строковых значениях настроек могут присутствовать макро:
- _%%version%%_ - версия сборки
- _%%profile%%_ - профиль установки сервиса (см ???)

#### Сборка версии

Сборка версии происходит в первом каталоге, описанном в _sources_.
Ссылка на другой каталог из _sources_ должна выглядеть, как `../<name>`.
При сборке выполняются команды из _build/buildCommands_. 
Если задан _exitCode_, его значение сравнивается с кодом возврата команды.
Если задано _outputMatch_, выход команды должен соответствовать этому регулярному выражению.  

После выполнения команд сборки выполняется копирование файлов из каталога 
`<builderDir>/developer/services/<service>/source` в каталог `<builderDir>/developer/services/<service>/build`
файлов, указанных в _copyFiles_.

Секция _install_ файла update.json записывается в файл install.json каталога сборки.

Далее производится упаковка в ZIP-файл каталога сборки и закачка на сервер дистрибуции. 


### Сборка клиентской версии

## Updater

Клиентские версии сервисов скачиваются, устанавливаются и запускаются _updater_-ом.
_Updater_ отправляет журналы работы сервиса на сервер дистрибуции.

Если сервис неожиданно завершается, _updater_ отправляет на сервер дистрибуции отчёт о сбое.
В отчёте содержится:
- общая информация
- последние журналы
- специфические файлы с информацией о сбое, такие, как core dump, или Java hprof

Если на сервере дистрибуции меняется желательная версия сервиса, _updater_ скачивает новую версию,
останавливает старую версию, и запускает новую.


# Версия разработчика

## Сборка версии разработчика

Откройте _Build/Developer_. Покажется таблица сервисов разработки.
Для каждого сервиса отображается информация по последней созданной, или создаваемой в 
текущий момент версии.

Если в текущий момент не производится сборки версии сервиса, можно запустить задачу
создания новой версии, выбрав запись.
Номер новой версии будет на единицу больше старой, либо его можно назначить вручную.
Если данный сервис присутствует в профиле клиентских сервисов 'self', по-умолчанию
будет предложено создать также клиентскую версию.
После запуска задачи создания новой версии отобразится журнал выполнения задачи
в реальном времени. Можно прервать задачу создания версии.

Если версия создаётся в текущий момент, выбрав запись, можно посмотреть журнал создания в 
реальном времени. Можно также прервать задачу создания версии.

# Клиентские версии

## Конфигурация клиента

Секция _update/install_ из файла _update.json_ записывается в файл _install.json_ и включается в образ версии.
Клиент может дополнить или изменить эту секцию. Для этого в каталоге services/<service> административного
репозитория создаётся файл install.json.
Этот файл будет слит с основным при установке версии на клиенте.

## Сборка клиентских версий

## Пометка версий как протестированных

Перед установкой сервисов на рабочие сервера, желательно провести комплексное тестирование.
Для того, чтобы сервисы могли ставиться на рабочие сервера только после того, как они будут
протестированы, vUpdate имеет механим пометки списка версий сервисов, как протестированных.

Например, есть некий сервер дистрибуции для рабочих серверов с именем _production_, в конфигурации которого задан
тестовый сервер дистрибуции _test_. Установка новых версий сервисов на сервер _production_
произойдёт только в случае, если они были протестированы на _test_, и помечены _test_-ом,
как протестированные.


