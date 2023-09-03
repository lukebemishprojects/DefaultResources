# DefaultResources

[![CodeFactor](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources)
[![Version](https://img.shields.io/badge/dynamic/xml?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v&query=metadata%2F%2Flatest&url=https%3A%2F%2Fmaven.lukebemish.dev%2Freleases%2Fdev%2Flukebemish%2Fdefaultresources%2Fdefaultresources-common-1.20%2Fmaven-metadata.xml)](https://maven.lukebemish.dev/releases/dev/lukebemish/defaultresources/)

A tool for providing default "resources" (assets, world-specific data, or other data) for a mod. Data is bundled in the `defaultresources` folder of the mod jar, and is read from that folder at runtime. Data can be extracted from that folder into `globalresources` by defaultresources when a user wants to modify it, through the defaultresources config. The behavior of `defaulrresources` for a single mod is controlled by the `defaultresources.meta.json` file.

Documentation for this library is a work in progress, though it will exist eventually. For an example use case, see [Excavated Variants](https://github.com/lukebemish/excavated_variants)

## Meta File Format

The `defaultresources.meta.json` file defines how DefaultResources should treat your mod. It takes the following fields:

* `marker_path`, `extracts_by_default`, and `create_marker` - by default, DefaultResources will load resources from
within the jar but not extract them into the `globalresources` folder unless that mod is specified as a target in
DefaultResources's config. If `extracts_by_default` is true, these files will be extracted by default, unless the
specified `marker_path` exists within the config folder. If `creates_marker` is true, this marker will be created when
resources are first extracted. All these fields are optional, but if `extracts_by_default` is true, the `marker_path`
must be provided.
* `resources_path` - the path to default resources within the mod jar. Optional; defaults to `defaultresources`
* `zip` - whether, when extracted, the resources should be zipped. Optional; defaults to true.
