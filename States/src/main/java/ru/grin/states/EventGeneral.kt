package ru.bastion.core.util.ext.states

//interface EventGeneral<T: Any> (val value: T) {
open class EventGeneral(var name: String, var value: Any?) {

    fun setAndGet(value: Any?): EventGeneral {
        this.value = value
        return this
    }
}