package org.katacr.kamenu

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

/**
 * 从 CraftEngine 最终资源包的物品定义与模型中解析二维纹理。
 *
 * 这里只读取标准资源包 JSON，不依赖 CraftEngine 的内部模型注册表。
 */
object CraftEngineResourcePackSpriteResolver {
    private const val MAX_MODEL_DEPTH = 16

    /** 根据物品模型、CustomModelData 与物品 ID 查找对应的二维 Sprite。 */
    fun resolve(
        resourcePack: File,
        itemModel: String?,
        customModelData: Int?,
        itemId: String,
        preferBlocksAtlas: Boolean
    ): ItemSpriteReference? {
        if (!resourcePack.isFile) return null
        return runCatching {
            ZipFile(resourcePack).use { zip ->
                val candidates = linkedSetOf<ResourceKey>()
                itemModel?.let(ResourceKey::parse)?.let(candidates::add)
                ResourceKey.parse(itemId)?.let(candidates::add)
                for (candidate in candidates) {
                    val model = resolveItemDefinition(zip, candidate, customModelData) ?: continue
                    val texture = resolveModelTexture(zip, model) ?: continue
                    return@use resolveAtlas(zip, texture, preferBlocksAtlas)
                }
                null
            }
        }.getOrNull()
    }

    private fun resolveItemDefinition(zip: ZipFile, key: ResourceKey, customModelData: Int?): ResourceKey? {
        val json = readJson(zip, "assets/${key.namespace}/items/${key.path}.json") ?: return null
        val root = json.get("model") ?: return null
        return selectModel(root, customModelData, 0)
    }

    private fun selectModel(element: JsonElement, customModelData: Int?, depth: Int): ResourceKey? {
        if (depth >= MAX_MODEL_DEPTH || !element.isJsonObject) return null
        val model = element.asJsonObject
        return when (model.string("type")?.substringAfter(':')) {
            "model" -> model.string("model")?.let(ResourceKey::parse)
            "range_dispatch" -> {
                val entries = model.array("entries")
                val selected = if (customModelData != null && entries != null) {
                    entries.mapNotNull { entry ->
                        entry.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let {
                            val threshold = it.get("threshold")?.takeIf(JsonElement::isJsonPrimitive)?.asDouble
                            val child = it.get("model")
                            if (threshold != null && child != null && threshold <= customModelData) threshold to child else null
                        }
                    }.maxByOrNull { it.first }?.second
                } else {
                    null
                }
                selectModel(selected ?: model.get("fallback") ?: return null, customModelData, depth + 1)
            }
            "select" -> {
                val fallback = model.get("fallback")
                val firstCase = model.array("cases")?.firstOrNull()?.takeIf(JsonElement::isJsonObject)
                    ?.asJsonObject?.get("model")
                selectModel(fallback ?: firstCase ?: return null, customModelData, depth + 1)
            }
            "condition" -> selectModel(
                model.get("on_false") ?: model.get("on_true") ?: return null,
                customModelData,
                depth + 1
            )
            "composite" -> {
                val first = model.array("models")?.firstOrNull() ?: return null
                selectModel(first, customModelData, depth + 1)
            }
            "special" -> selectModel(model.get("base") ?: return null, customModelData, depth + 1)
            else -> {
                val nested = model.get("model") ?: return null
                if (nested.isJsonPrimitive) ResourceKey.parse(nested.asString)
                else selectModel(nested, customModelData, depth + 1)
            }
        }
    }

    private fun resolveModelTexture(zip: ZipFile, model: ResourceKey): ResourceKey? {
        val textures = loadModelTextures(zip, model, linkedSetOf(), 0) ?: return null
        val preferred = listOf("layer0", "particle") + textures.keys
        for (name in preferred.distinct()) {
            val value = resolveTextureVariable(textures[name] ?: continue, textures, linkedSetOf()) ?: continue
            ResourceKey.parse(value)?.let { return it }
        }
        return null
    }

    private fun loadModelTextures(
        zip: ZipFile,
        model: ResourceKey,
        visited: MutableSet<ResourceKey>,
        depth: Int
    ): LinkedHashMap<String, String>? {
        if (depth >= MAX_MODEL_DEPTH || !visited.add(model)) return null
        val json = readJson(zip, "assets/${model.namespace}/models/${model.path}.json") ?: return linkedMapOf()
        val result = linkedMapOf<String, String>()
        json.string("parent")?.let(ResourceKey::parse)?.let { parent ->
            loadModelTextures(zip, parent, visited, depth + 1)?.let(result::putAll)
        }
        json.objectValue("textures")?.entrySet()?.forEach { (name, value) ->
            if (value.isJsonPrimitive) result[name] = value.asString
        }
        return result
    }

    private fun resolveTextureVariable(
        value: String,
        textures: Map<String, String>,
        visited: MutableSet<String>
    ): String? {
        if (!value.startsWith('#')) return value
        val variable = value.substring(1)
        if (!visited.add(variable)) return null
        return resolveTextureVariable(textures[variable] ?: return null, textures, visited)
    }

    private fun resolveAtlas(zip: ZipFile, texture: ResourceKey, preferBlocks: Boolean): ItemSpriteReference {
        val matches = mutableListOf<ItemSpriteReference>()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val match = ATLAS_PATH.matchEntire(entry.name) ?: continue
            val atlas = ResourceKey(match.groupValues[1], match.groupValues[2])
            val json = readJson(zip, entry.name) ?: continue
            findSpriteInAtlas(json, atlas.namespace, texture)?.let { sprite ->
                matches += ItemSpriteReference(atlas.toString(), sprite.toString())
            }
        }
        if (matches.isNotEmpty()) {
            val preferredPath = when {
                texture.path.startsWith("block/") -> "blocks"
                texture.path.startsWith("item/") -> "items"
                preferBlocks -> "blocks"
                else -> "items"
            }
            return matches.firstOrNull { it.atlas.substringAfter(':') == preferredPath } ?: matches.first()
        }
        val atlas = if (texture.path.startsWith("block/") || preferBlocks) "minecraft:blocks" else "minecraft:items"
        return ItemSpriteReference(atlas, texture.toString())
    }

    private fun findSpriteInAtlas(json: JsonObject, defaultNamespace: String, texture: ResourceKey): ResourceKey? {
        val sources = json.array("sources") ?: return null
        for (element in sources) {
            if (!element.isJsonObject) continue
            val source = element.asJsonObject
            when (source.string("type")?.substringAfter(':')) {
                "single" -> {
                    val resource = ResourceKey.parse(source.string("resource"), defaultNamespace) ?: continue
                    if (resource == texture) {
                        return ResourceKey.parse(source.string("sprite"), defaultNamespace) ?: resource
                    }
                }
                "directory" -> {
                    val directory = ResourceKey.parse(source.string("source"), defaultNamespace) ?: continue
                    if (directory.namespace != texture.namespace) continue
                    val sourcePath = directory.path.trimEnd('/')
                    if (texture.path != sourcePath && !texture.path.startsWith("$sourcePath/")) continue
                    val suffix = texture.path.removePrefix(sourcePath).trimStart('/')
                    val prefixRaw = source.string("prefix").orEmpty()
                    val prefix = ResourceKey.parse(prefixRaw.ifEmpty { directory.path }, directory.namespace) ?: continue
                    return ResourceKey(prefix.namespace, listOf(prefix.path.trimEnd('/'), suffix).filter(String::isNotEmpty).joinToString("/"))
                }
            }
        }
        return null
    }

    private fun readJson(zip: ZipFile, path: String): JsonObject? {
        val entry = zip.getEntry(path) ?: return null
        return zip.getInputStream(entry).use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                JsonParser().parse(reader).takeIf { it.isJsonObject }?.asJsonObject
            }
        }
    }

    private data class ResourceKey(val namespace: String, val path: String) {
        override fun toString(): String = "$namespace:$path"

        companion object {
            fun parse(raw: String?, defaultNamespace: String = "minecraft"): ResourceKey? {
                val value = raw?.trim()?.lowercase()?.removeSuffix(".json") ?: return null
                if (value.isEmpty() || value.startsWith('#')) return null
                val separator = value.indexOf(':')
                val namespace = if (separator >= 0) value.substring(0, separator) else defaultNamespace
                val path = if (separator >= 0) value.substring(separator + 1) else value
                if (!KEY_PART.matches(namespace) || !PATH_PART.matches(path)) return null
                return ResourceKey(namespace, path)
            }
        }
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf(JsonElement::isJsonPrimitive)?.asString

    private fun JsonObject.array(key: String) = get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray

    private fun JsonObject.objectValue(key: String) = get(key)?.takeIf(JsonElement::isJsonObject)?.asJsonObject

    private val ATLAS_PATH = Regex("^assets/([a-z0-9_.-]+)/atlases/([a-z0-9_./-]+)\\.json$")
    private val KEY_PART = Regex("^[a-z0-9_.-]+$")
    private val PATH_PART = Regex("^[a-z0-9_./-]+$")
}
