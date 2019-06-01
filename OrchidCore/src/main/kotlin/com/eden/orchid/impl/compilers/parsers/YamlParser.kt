package com.eden.orchid.impl.compilers.parsers

import com.caseyjbrooks.clog.Clog
import com.eden.orchid.api.compilers.OrchidParser
import com.eden.orchid.utilities.SuppressedWarnings
import com.eden.orchid.utilities.logSyntaxError
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.parser.ParserException
import java.util.HashMap
import javax.inject.Inject

class YamlParser @Inject
constructor() : OrchidParser(100) {

    override fun getDelimiter(): String {
        return "-"
    }

    override fun getSourceExtensions(): Array<String> {
        return arrayOf("yml", "yaml")
    }

    @Suppress(SuppressedWarnings.UNCHECKED_KOTLIN)
    override fun parse(extension: String, input: String): Map<String, Any>? {
        try {
            val yamlData = Yaml().load<Any>(input)

            if (yamlData is Map<*, *>) {
                return yamlData as Map<String, Any>
            } else if (yamlData is List<*>) {
                val yamlObject = HashMap<String, Any>()
                yamlObject[OrchidParser.arrayAsObjectKey] = yamlData
                return yamlObject
            }
        } catch (e: ParserException) {
            input.logSyntaxError(extension, e.problemMark.line, e.problem)
        } catch (e: Exception) {
            Clog.e("Error parsing YAML:\n{}", e.message)
        }

        return null
    }

    override fun serialize(extension: String, input: Any): String {
        return Yaml().dump(input)
    }
}
