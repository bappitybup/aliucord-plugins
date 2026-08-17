version = "1.0.1"
description = "shows the age of the most recent message on a dm row"

aliucord {
    changelog.set(
        """
        # 1.0.1
        * Fix stale dm timestamps by refreshing rows when recent message data updates.

        # 1.0.0
        * Initial release.
        """.trimIndent(),
    )

    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
    // author("Name", 0L, hyperlink = true)
    // author("Name", 0L, hyperlink = true)

    // Excludes this plugin from publishing and global plugin repositories.
    // Set this to false if the plugin is unfinished
    deploy.set(false)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}
