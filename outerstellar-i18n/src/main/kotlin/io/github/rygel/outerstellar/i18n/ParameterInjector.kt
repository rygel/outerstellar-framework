package io.github.rygel.outerstellar.i18n

object ParameterInjector {
    @JvmStatic
    @JvmName("inject")
    fun inject(template: String, vararg params: Any): String {
        if (params.isEmpty()) return template
        val sb = StringBuilder(template.length + params.size * 8)
        var i = 0
        while (i < template.length) {
            if (template[i] == '{') {
                val close = template.indexOf('}', i + 1)
                if (close != -1) {
                    val index = template.substring(i + 1, close).toIntOrNull()
                    if (index != null && index in params.indices) {
                        sb.append(params[index].toString())
                        i = close + 1
                        continue
                    }
                }
            }
            sb.append(template[i])
            i++
        }
        return sb.toString()
    }

    @JvmStatic
    @JvmName("injectToList")
    fun inject(template: String, params: List<Any>): String {
        return inject(template, *params.toTypedArray())
    }
}
