version = "1.0.3"
description = "Uncaps the weird timestamp text width limit so it reaches the edge of the chat and wraps to new lines."

aliucord {
    changelog.set(
        """
        # 1.0.3
        * Actually for real for real this time fix timestamp wrapping failing sometimes.

        # 1.0.2
        * Actually for real this time fix timestamp wrapping failing sometimes.

        # 1.0.1
        * Fix timestamp wrapping failing sometimes.

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
    deploy.set(true)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}
