package ru.bastion.core.util.ext.states

import android.util.Log
import androidx.collection.ArraySet

open class OperationGeneral<S: StateGeneral>(var name: String, val operatio: (S) -> Unit) {

    var subsequentOperations: ArrayList<OperationGeneral<S>> = ArrayList() // TODO: can be optimized with lateinit

    init {
        name = "\"$name\""
        Log.d("States MANAGER", "OperationGeneral ${name} initing")
    }

    fun defaultOperation() {}

    private fun hasRecursion(operation: OperationGeneral<S>, operationsSet: ArraySet<OperationGeneral<S>>) : Boolean {
        var hasRecursion = false
        operationsSet.add(operation)
        if (operationsSet.contains(this)) {
            hasRecursion = true
        } else {
            for (op in operation.subsequentOperations) {
                hasRecursion = hasRecursion(op, operationsSet)
                if (hasRecursion) {
                    Log.d("States MANAGER", "Recursion in ${op.name}")
                    break
//                } else {
//                    operationsSet.add(operation)
                }
            }
        }
        return hasRecursion
    }

    fun andOperation(operation: OperationGeneral<S>): OperationGeneral<S> {
        val operationsSet = ArraySet<OperationGeneral<S>>()
        if (hasRecursion(operation, operationsSet)) {
            Log.d("States MANAGER", "OperationGeneral ${name} didn't insert ${operation.name} because of recursion")
            throw Exception("RECURSION!!!")
        } else {
            val result = clone("CLONE OF " + name)
            result.subsequentOperations.add(operation)
            return result
        }
    }

    fun clone(name: String): OperationGeneral<S> {
        val result = OperationGeneral(name, operatio)
        for (operation in subsequentOperations) {
            result.subsequentOperations.add(operation)
        }
        return result
    }

    fun operate(state: S) {
        operatio.invoke(state)
        for (operation in subsequentOperations) {
            operation.operate(state)
        }
    }
}