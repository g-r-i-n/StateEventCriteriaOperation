package ru.bastion.core.util.ext.states

/*
    Функционал позволяет выделить 4 отдельные сущности:
    - состояние: StateGeneral (переменные, отвечающие за значения параметров бизнес-модели)
    - событие: EventGeneral (нажатия на элементы, сетевые ответы, события оперционной системы)
    - критерий: CriteriaGeneral (объект-функция, возвращающий булево значение)
    - операция: OperationGeneral (набор действий по результатам критериев)
    а также менеджер StatesManagerGeneral для работы с ними. Этого функционала достаточно для
    построения любой логики обработки событий и отображения изменений интерфейса приложения.
    Интерфейсы и общий функционал заданы в модуле core.

    Это дает возможность:
    - частично реализовать шаблон разработки state-driven
    - иметь централизованное хранение и контроль всех возможных событий
    - иметь централизованное хранение и контроль всех возможных условий
    - иметь централизованное хранение и контроль всех возможных операций
    - разделять логические уровни ПО, делать код менее связанным
    - автоматически логировать события, операции и даже выполнение условий не перегружая исходный код
    - покрывать приложение эффективными функциональными тестами
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
 */

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import java.util.Hashtable

abstract class StatesManagerGeneral<S: StateGeneral, E: EventGeneral, C: CriteriaGeneral<S>, O: OperationGeneral<S>> {

    protected lateinit var STATE_CHANGED_EVENT: E
    private lateinit var EVENT_FOR_RECURSION: E
//    protected var ALWAYS_TRUE_CRITERIA: C = CriteriaGeneral<S>("Any state criteria") { return true }
//    protected lateinit var STATE_SETTER_OPERATION: O

    private lateinit var state: S
    protected lateinit var previousState: S
    private lateinit var event: E
    private val eventsMap: Hashtable<E, ArrayList<Pair<C, OperationGeneral<S>>>> = Hashtable()
    private var relevantCriterias = ArrayList<CriteriaGeneral<S>>()
    private var usedOperations = Hashtable<OperationGeneral<S>, Boolean>()

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    abstract protected fun onInitializeEventsToStates()
    abstract protected fun onInitializeStatesToLayout()
    abstract protected fun onInitState(): S
//    abstract protected fun onCreateStateSetterOperation(state: S): O

    fun startManager() {
        EVENT_FOR_RECURSION = EventGeneral("", null) as E
        state = onInitState()
//        STATE_SETTER_OPERATION = onCreateStateSetterOperation(state)
        onInitializeEventsToStates()
        onInitializeStatesToLayout()
        previousState = state.clone("init PREV state") as S
    }

//    fun createStateSetterOperat(): O {
//
//    }

    private var changeStateBusy = false
    fun changeStateByEvent(event: E, liveData: MutableLiveData<S>?) { // TODO: make an order of events for working in one queue to prevent conflicts and unpredictable states (it's now workarounded)
        if (!changeStateBusy || event === EVENT_FOR_RECURSION) {
            changeStateBusy = true
            if (event === EVENT_FOR_RECURSION) {
                this.event = STATE_CHANGED_EVENT
            } else {
                this.event = event            }
            consumeEvent(this.event)
            if (liveData != null) {
                liveData.value = state
                changeStateBusy = false
            } else {
                if (event === EVENT_FOR_RECURSION) {
                    changeStateBusy = false
                } else {
                    changeStateByEvent(EVENT_FOR_RECURSION, null) // It's only two levels recursion - first level for changing state, second level for changing layout
                }
            }
        } else {
            Log.d("States MANAGER", "!!! Simultaneous events are not supported now, skipped:")
            Log.d("States MANAGER", "Event not fired: ${event.name}, value: ${event.value}")
        }
    }

//    private fun changeLayoutByState() {
//        changeLayoutHappening = true
//        changeStateByEvent(STATE_CHANGED_EVENT, null)
//        changeLayoutHappening = false
//    }

    private var plannerIsBusy = false
    fun addEventCase(event: E, vararg criterias: C, operation: O) {
        if (!plannerIsBusy) {
            plannerIsBusy = true
            var criteriasToOperationsList: ArrayList<Pair<C, OperationGeneral<S>>>? =
                eventsMap[event]
            if (criteriasToOperationsList == null) {
                criteriasToOperationsList = ArrayList()
                eventsMap[event] = criteriasToOperationsList
            }
            for (criteria in criterias) {
                criteriasToOperationsList.add(Pair(criteria, operation))
            }
        } else {
            Log.d("States MANAGER", "!!! Simultaneous events are not supported now, skipped:")
            Log.d("States MANAGER", "Event name to happen: ${event.name}, event value: ${event.value}, operation name: ${operation.name}")
        }
        plannerIsBusy = false
    }

    private fun consumeEvent(event: E) {
        previousState = state.clone("PREV") as S// TODO: copyFrom?
        relevantCriterias.clear()
        usedOperations.clear()
        var criteriasToOperationsList: ArrayList<Pair<C, OperationGeneral<S>>>? = eventsMap[event]
        Log.d("States MANAGER", "--------------------")
        Log.d("States MANAGER", "Event happened: ${event.name}, value: ${event.value}")
        if (criteriasToOperationsList == null) {
            //TODO: no matching events!
            Log.d("States MANAGER", "++++++++++++++++++++")
            Log.d("States MANAGER", "No criteria existing for event \"${event.name}\" with value \"${event.value}\"")
            Log.d("States MANAGER", "++++++++++++++++++++")
        } else {
            Log.d("States MANAGER", "Prev state: ${previousState}")
            for (pair in criteriasToOperationsList) {
                val criteria = pair.first
                if (criteria.check(previousState)) {
                    Log.d("States MANAGER", "Criteria MATCHES: ${criteria.name}")
                    relevantCriterias.add(criteria)
//                        usedOperations[operation] = true // TODO: the same operatio can be invoked some times in this case! Think what to do
                    pair.second.operatio.invoke(state)
                    Log.d("States MANAGER", "Operatio invoked: ${pair.second.name}-${pair.second.operatio.hashCode()}")
                    for (operation in pair.second.subsequentOperations) {
                        if (usedOperations[operation] == null) {
                            usedOperations[operation] = true
//                            operation.operatio.invoke(state)
                            operation.operate(state)
                            Log.d("States MANAGER", "Operation element invoked: ${operation.name}-${operation.hashCode()}")
                        } else {
                            Log.d("States MANAGER", "Operation not invoked (was invoked earlier): ${operation.name}-${operation.hashCode()}")
                        }
                    }
                } else {
                    Log.d("States MANAGER", "Criteria DOESN'T match: ${criteria.name}")
                }
            }
            Log.d("States MANAGER", "State: ${state}")
        }
        if (relevantCriterias.size == 0) {
            Log.d("States MANAGER", "++++++++++++++++++++")
            Log.d("States MANAGER", "No criteria where consumed!!")
            Log.d("States MANAGER", "++++++++++++++++++++")
        }
    }
}