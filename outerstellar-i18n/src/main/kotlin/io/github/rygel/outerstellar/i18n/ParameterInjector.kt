package io.github.rygel.outerstellar.i18n

object ParameterInjector {
    private val PLACEHOLDER = Regex("\\{(\\d+)}")

    @JvmStatic
    @JvmName("inject")
    fun inject(template: String, vararg params: Any): String {
        if (params.isEmpty()) return template
        return PLACEHOLDER.replace(template) { match ->
            val index = match.groupValues[1].toInt()
            if (index in params.indices) params[index].toString() else match.value
        }
    }

    @JvmStatic
    @JvmName("injectToList")
    fun inject(template: String, params: List<Any>): String {
        if (params.isEmpty()) return template
        return PLACEHOLDER.replace(template) { match ->
            val index = match.groupValues[1].toInt()
            if (index in params.indices) params[index].toString() else match.value
        }
    }
}
