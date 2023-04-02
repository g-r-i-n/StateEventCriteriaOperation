package ru.bastion.core.util.ext.states

val default: Any = Any()

// Don't forget to clone every property in copyFrom!! Especially after adding new one
interface StateGeneral {

    var name: String // For debugging
    fun copyFrom(state: StateGeneral)
    fun clone(name: String): StateGeneral

//    fun changeByEvent(event: EventGeneral)
//    fun createByEvent(event: EventGeneral): StateGeneral
//    fun setCriteria(condition: CriteriaGeneral)

}