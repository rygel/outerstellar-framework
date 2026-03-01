package com.outerstellar.i18n

object ParameterInjector {
    @JvmStatic
    @JvmName("inject")
    fun inject(template: String, vararg params: Any): String {
        var result = template
        params.forEachIndexed { index, param ->
            result = result.replace("{$index}", param.toString())
        }
        return result
    }

    @JvmStatic
    @JvmName("injectToList")
    fun inject(template: String, params: List<Any>): String {
        return inject(template, *params.toTypedArray())
    }
}
