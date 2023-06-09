# StateEventCriteriaOperation

    Функционал позволяет выделить 4 отдельные сущности:
    - состояние: StateGeneral (переменные, отвечающие за значения параметров бизнес-модели)
    - событие: EventGeneral (нажатия на элементы, сетевые ответы, события оперционной системы)
    - критерий: CriteriaGeneral (объект-функция, возвращающий булево значение)
    - операция: OperationGeneral (набор действий по результатам критериев)
    а также менеджер StatesManagerGeneral для работы с ними. Этого функционала достаточно для
    построения любой логики обработки событий и отображения изменений интерфейса приложения.
    Интерфейсы и общий функционал заданы в модуле States.

    Это дает возможность:
    - частично реализовать шаблон разработки state-driven
    - иметь централизованное хранение и контроль всех возможных событий
    - иметь централизованное хранение и контроль всех возможных условий
    - иметь централизованное хранение и контроль всех возможных операций
    - разделять логические области ПО, делать код менее связанным
    - автоматически логировать события, операции и даже выполнение условий не перегружая исходный код
    - покрывать приложение функциональными тестами
    - если нужно - добавлять логику динамически, например подгружать извне правила доступа к ресурсу

    Статическая часть - создание нужных объектов и функций. Состояние для каждого экрана хранится
     в едином классе, например SearchVehicleState. Изменение объекта состояния допустимо только методом use().
     Под каждое событие создается отдельный объект одного и того же типа, в нем хранится имя события
     и переменная для хранения его значения. Все события хранятся в контейнере EventsContainer.
     Критерий позволяет хранить исчерпывающее (достаточное) условие чего угодно, в первую очередь
     соответствия состояния каким-то значениям. Все условия хранятся в контейнере CriteriasContainer.
     Операции предназначены для двух основных задач - изменения параметров состояния либо UI-элементов,
     хранятся в контейнере OperationsContainer, а также, для простоты и наглядности, могут задаваться
     внутри реализации StatesManagerGeneral (в этом случае наглядно будет задавать изменения состояния)

    Когда созданы все необходимые сущности, то нужно в унаследованном от StatesManagerGeneral
     менеджере реализовать две абстрактные функции:
     - onInitializeEventsToStates()
     - onInitializeStatesToLayout()
     В первой будет происходить с помощью функции addEventCase() добавление в Map критериев и
     операций с состоянием, которые нужно выполнять при выполнении этих критериев, ключами являются
     события. Во второй та же логика, но операции уже с визуальными элементами экрана. Это все
     создается статически, на этапе компиляции проекта (хотя можно и динамически, например получая
     новую логику с сервера)

    Динамическая часть - процесс обработки событий и отображения изменений элементов экрана.
     При получении внешнего события, например при нажатии кнопки "Далее", из контейнера событий
     берется нужный инстанс, ему передается значение события, например текст поля ввода, и затем
     он передается в менеджер функцией changeStateByEvent(). Далее менеджер автоматически проходит
     по всем значениям с данным ключом-событием, берет из значений критерии, проверяет и если они
     выполняются, то оттуда же берет операции и выполняет их.
     Когда функция changeStateByEvent() пройдет по всему MAP, то затем менеджер автоматически
     запустит ее еще раз но уже с константным событием STATE_CHANGED_EVENT и описанный цикл выполнения
     операций повторится уже по этому ключу.

    В итоге из фрагментов и activity всё управление приложением сводится только к вызову одного и того
     же метода changeStateByEvent() по факту появления событий, на которые эти фрагменты и activity
     (или их ViewModel) подписаны.
 
