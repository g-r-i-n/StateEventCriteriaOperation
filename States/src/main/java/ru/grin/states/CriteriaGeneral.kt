package ru.bastion.core.util.ext.states

class CriteriaGeneral<S: StateGeneral>(val name: String, private val criteria: (S) -> Boolean) {//<T: () -> Boolean> {

    private var isOpposite = false
    val elements: ArrayList<(S) -> Boolean> = ArrayList()

    init {
        addCriteriaElement(criteria)
    }

    fun addCriteriaElement(element: (S) -> Boolean): CriteriaGeneral<S> {
        elements.add(element)
        return this
    }

    fun createOpposite(): CriteriaGeneral<S> {
        val result = CriteriaGeneral<S>(name, elements[0])
        for (i in 1 until elements.size) {
            result.addCriteriaElement(elements[i])
        }
        result.isOpposite = true
        return result
    }

    fun check(state: S): Boolean {
        var result = true
        for (element in elements) {
            if (!element(state)) {
                result = false
                break
            }
        }
        return if (isOpposite) !result else result
    }

}