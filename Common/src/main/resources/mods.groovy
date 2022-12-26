/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[40,)'
    issueTrackerUrl = 'https://github.com/lukebemish/defaultresources/issues'
    license = 'LGPL-3.0-or-later'

    mod {
        modId = this.buildProperties.mod_id
        displayName = this.buildProperties.mod_name
        version = this.version
        onQuilt {
            group = this.group
        }
        displayUrl = 'https://github.com/lukebemish/defaultresources'
        contact.sources = 'https://github.com/lukebemish/defaultresources'
        author 'Luke Bemish'
        description = "A tool for loading and extracting resources provided by mods or by users."

        entrypoints {
            init = 'dev.lukebemish.defaultresources.impl.quilt.DefaultResourcesQuilt'
            client_init = 'dev.lukebemish.defaultresources.impl.quilt.DefaultResourcesQuiltClient'
        }

        dependencies {
            onForge{
                forge = ">=${this.forgeVersion}"
            }
            minecraft = this.minecraftVersionRange
            onQuilt {
                quiltLoader = ">=${this.quiltLoaderVersion}"
                quilt_base = ">=${this.buildProperties.quilt_stdlib_version}"
            }
        }
    }
}