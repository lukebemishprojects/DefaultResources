# DefaultResources

[![CodeFactor](https://www.codefactor.io/repository/github/lukebemish/defaultresources/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/lukebemish/defaultresources)
[![Version](https://img.shields.io/badge/dynamic/xml?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v&query=metadata%2F%2Flatest&url=https%3A%2F%2Fmaven.lukebemish.dev%2Freleases%2Fdev%2Flukebemish%2Fdefaultresources%2Fdefaultresources-common-1.19.4%2Fmaven-metadata.xml)](https://maven.lukebemish.dev/html/releases/dev/lukebemish/defaultresources/)

A tool for providing default "resources" (assets, world-specific data, or other data) for a mod. Data is bundled in the `defaultresources` folder of the mod jar, and is read from that folder at runtime. Data can be extracted from that folder into `globalresources` by defaultresources when a user wants to modify it, through the defaultresources config. The behavior of `defaulrresources` for a single mod is controlled by the `defaultresources.meta.json` file.

Documentation for this library is a work in progress, though it will exist eventually. For an example use case, see [Excavated Variants](https://github.com/lukebemish/excavated_variants)
