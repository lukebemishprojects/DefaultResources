# DefaultResources

[![CodeFactor](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources)
[![Version](https://img.shields.io/badge/dynamic/xml?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v&query=metadata%2F%2Flatest&url=https%3A%2F%2Fmaven.lukebemish.dev%2Freleases%2Fdev%2Flukebemish%2Fdefaultresources%2Fdefaultresources-common-1.20.4%2Fmaven-metadata.xml)](https://maven.lukebemish.dev/releases/dev/lukebemish/defaultresources/)

A tool for providing default "resources" (assets, world-specific data, or other data) for a mod. Data is bundled in the `defaultresources` folder of the mod jar, and is read from that folder at runtime. Data can be extracted from that folder into `globalresources` by defaultresources when a user wants to modify it, through the defaultresources config. The behavior of `defaulrresources` for a single mod is controlled by the `defaultresources.meta.json` file.

Documentation for this library is a work in progress, though it will exist eventually. For an example use case, see [Excavated Variants](https://github.com/lukebemish/excavated_variants)

## Meta File Format

The `defaultresources.meta.json` file defines how DefaultResources should treat your mod. It takes the following fields:

* `resources_path` - the path to default resources within the mod jar. Optional; defaults to `defaultresources`
* `zip` - whether, when extracted, the resources should be zipped. Optional; defaults to true.

## Detection from `resourcepacks`

DefaultResources will automatically detect resource packs that include "global" resources and attempt to configure them.
To mark that your resource pack should be detected, add the following to the `pack.mcmeta`:
```json
{
  "defaultresources": {
    "detect": true
  }
}
```
Which such resource packs are loaded as global resources can be configured in the DefaultResources config.
