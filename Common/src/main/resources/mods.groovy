/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[40,)'
    issueTrackerUrl = 'https://github.com/lukebemishprojects/DefaultResources/issues'
    license = 'LGPL-3.0-or-later'

    mod {
        modId = this.buildProperties.mod_id
        displayName = this.buildProperties.mod_name
        version = this.version
        displayUrl = 'https://github.com/lukebemishprojects/DefaultResources'
        contact.sources = 'https://github.com/lukebemishprojects/DefaultResources'
        author 'Luke Bemish'
        description = "A tool for loading and extracting resources provided by mods or by users."

        entrypoints {
            main = 'dev.lukebemish.defaultresources.impl.fabriquilt.DefaultResourcesFabriQuilt'
            client = 'dev.lukebemish.defaultresources.impl.fabriquilt.DefaultResourcesFabriQuiltClient'
        }

        dependencies {
            mod 'minecraft', {
                def minor = this.libs.versions.minecraft.split(/\./)[1] as int
                versionRange = "[${this.libs.versions.minecraft},1.${minor+1}.0)"
            }
            onForge {
                neoforge = ">=${this.libs.versions.neoforge}"
            }
            onFabric {
                mod 'fabricloader', {
                    versionRange = ">=${this.libs.versions.fabric.loader}"
                }
                mod 'fabric-api', {
                    versionRange = ">=${this.libs.versions.fabric.api.split(/\+/)[0]}"
                }
            }
        }
    }
    onFabric {
        mixin = [
            'mixin.defaultresources.fabriquilt.json',
            'mixin.defaultresources.json'
        ]
    }
}
