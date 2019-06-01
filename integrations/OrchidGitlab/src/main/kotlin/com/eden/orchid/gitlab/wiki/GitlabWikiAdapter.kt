package com.eden.orchid.gitlab.wiki

import com.caseyjbrooks.clog.Clog
import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.options.annotations.StringDefault
import com.eden.orchid.api.resources.resource.FileResource
import com.eden.orchid.api.resources.resource.StringResource
import com.eden.orchid.api.util.GitFacade
import com.eden.orchid.api.util.GitRepoFacade
import com.eden.orchid.utilities.OrchidUtils
import com.eden.orchid.wiki.adapter.WikiAdapter
import com.eden.orchid.wiki.model.WikiSection
import com.eden.orchid.wiki.pages.WikiPage
import com.eden.orchid.wiki.pages.WikiSummaryPage
import com.eden.orchid.wiki.utils.WikiUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import javax.inject.Inject
import javax.validation.constraints.NotBlank

class GitlabWikiAdapter
@Inject
constructor(
    val context: OrchidContext,
    val git: GitFacade
) : WikiAdapter {

    @Option
    @Description("The repository to push to, as [username/repo].")
    @NotBlank(message = "Must set the Gitlab repository.")
    lateinit var repo: String

    @Option
    @StringDefault("gitlab.com")
    @Description("The URL for a self-hosted Gitlab Enterprise installation.")
    @NotBlank
    lateinit var gitlabUrl: String

    @Option
    @StringDefault("master")
    lateinit var branch: String

    override fun getType(): String = "gitlab"

    override fun loadWikiPages(section: WikiSection): Pair<WikiSummaryPage, List<WikiPage>>? {
        val repo = git.clone(remoteUrl, displayedRemoteUrl, branch, false)

        var sidebarFile: File? = null
        val wikiPageFiles = mutableListOf<File>()

        repo.repoDir.toFile()
            .walk()
            .filter { it.isFile }
            .filter { it.exists() }
            .filter { !OrchidUtils.normalizePath(it.absolutePath).contains("/.git/") }
            .forEach { currentFile ->
                if (currentFile.nameWithoutExtension == "_Sidebar") {
                    sidebarFile = currentFile
                } else {
                    wikiPageFiles.add(currentFile)
                }
            }

        val wikiContent = if (sidebarFile != null) {
            createSummaryFileFromSidebar(repo, section, sidebarFile!!, wikiPageFiles)
        } else {
            createSummaryFileFromPages(repo, section, wikiPageFiles)
        }

        return wikiContent
    }

// Cloning Wiki
//----------------------------------------------------------------------------------------------------------------------

    private val displayedRemoteUrl: String
        get() = "https://$gitlabUrl/$repo.wiki.git"

    private val remoteUrl: String
        get() = "https://$gitlabUrl/$repo.wiki.git"

// Formatting Wiki files to Orchid's wiki format
//----------------------------------------------------------------------------------------------------------------------

    private fun createSummaryFileFromSidebar(
        repo: GitRepoFacade,
        section: WikiSection,
        sidebarFile: File,
        wikiPages: MutableList<File>
    ): Pair<WikiSummaryPage, List<WikiPage>> {
        val summary = FileResource(context, sidebarFile, repo.repoDir.toFile())

        return WikiUtils.createWikiFromSummaryFile(section, summary) { linkName, linkTarget, order ->
            val referencedFile = wikiPages.firstOrNull {
                val filePath = FilenameUtils.removeExtension(it.relativeTo(repo.repoDir.toFile()).path)
                filePath == linkTarget
            }

            if(referencedFile == null) {
                Clog.w("Page referenced in Gitlab Wiki $repo, $linkTarget does not exist")
                StringResource(context, "wiki/${section.key}/$linkTarget/index.md", linkName)
            }
            else {
                FileResource(context, referencedFile, repo.repoDir.toFile())
            }
        }
    }

    private fun createSummaryFileFromPages(
        repo: GitRepoFacade,
        section: WikiSection,
        wikiPageFiles: MutableList<File>
    ): Pair<WikiSummaryPage, List<WikiPage>> {
        val wikiPages = wikiPageFiles.map { FileResource(context, it, repo.repoDir.toFile()) }
        return WikiUtils.createWikiFromResources(context, section, wikiPages)
    }

}
