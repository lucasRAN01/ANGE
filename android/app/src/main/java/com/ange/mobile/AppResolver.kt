package com.ange.mobile

internal sealed interface AppMatch {
    data class One(val app: LaunchableApp): AppMatch
    data class Choose(val apps: List<LaunchableApp>): AppMatch
    data object None: AppMatch
}

/** Match only installed launchable apps. Approximate names always require a choice. */
internal object AppResolver {
    private fun normalized(value: String)=JarvisVoice.withoutAccents(value)
        .replace(Regex("[^\\p{L}\\p{N} ]")," ").trim().replace(Regex("\\s+")," ")
    private fun words(value: String)=normalized(value).split(' ').filter { it.isNotBlank() }.map { if(it.length>4 && it.endsWith('s')) it.dropLast(1) else it }
    private fun result(apps: List<LaunchableApp>, approximate: Boolean = false): AppMatch {
        val unique=apps.distinctBy { it.packageName }.sortedBy { normalized(it.label) }.take(20)
        return when { unique.isEmpty() -> AppMatch.None; unique.size==1 && !approximate -> AppMatch.One(unique.single()); else -> AppMatch.Choose(unique) }
    }
    fun resolve(raw: String, apps: List<LaunchableApp>): AppMatch {
        var query=normalized(raw).replace(Regex(" (?:s il te plait|s il vous plait|stp|svp|please|maintenant)$"),"")
        if(query.isBlank()) return AppMatch.None
        val exact=apps.filter { normalized(it.label)==query || it.packageName.equals(raw.trim(),true) }
        if(exact.isNotEmpty()) return result(exact)
        query=query.replace(Regex("^(?:le|la|les|l) +"),"")
        val compact=query.replace(" ","")
        val compactMatches=apps.filter { normalized(it.label).replace(" ","")==compact }
        if(compactMatches.isNotEmpty()) return result(compactMatches)
        val queryWords=words(query)
        val tokenMatches=apps.filter { words(it.label).containsAll(queryWords) }
        if(tokenMatches.isNotEmpty()) return result(tokenMatches)
        // Compound publisher labels such as OneNote/Gmail: ask, never guess silently.
        val suggestions=apps.filter { app ->
            val label=normalized(app.label).replace(" ","")
            (compact.length>=4 && (label.startsWith(compact) || label.endsWith(compact))) ||
                (compact.length>=4 && words(app.label).any { near(it,compact) })
        }
        return result(suggestions,true)
    }
    private fun near(first: String,second: String): Boolean {
        if(kotlin.math.abs(first.length-second.length)>1) return false
        var previous=IntArray(second.length+1) { it }
        for(i in first.indices) {
            val current=IntArray(second.length+1); current[0]=i+1
            for(j in second.indices) current[j+1]=minOf(current[j]+1,previous[j+1]+1,previous[j]+if(first[i]==second[j]) 0 else 1)
            previous=current
        }
        return previous.last()<=1
    }
}
