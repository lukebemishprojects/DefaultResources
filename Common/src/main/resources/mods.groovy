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
        onQuilt {
            group = this.group
        }
        displayUrl = 'https://github.com/lukebemishprojects/DefaultResources'
        contact.sources = 'https://github.com/lukebemishprojects/DefaultResources'
        author 'Luke Bemish'
        description = "A tool for loading and extracting resources provided by mods or by users."

        entrypoints {
            onQuilt {
                init = 'dev.lukebemish.defaultresources.impl.fabriquilt.quilt.DefaultResourcesQuilt'
                client_init = 'dev.lukebemish.defaultresources.impl.fabriquilt.quilt.DefaultResourcesQuiltClient'
            }
            onFabric {
                main = 'dev.lukebemish.defaultresources.impl.fabriquilt.fabric.DefaultResourcesFabric'
                client = 'dev.lukebemish.defaultresources.impl.fabriquilt.fabric.DefaultResourcesFabricClient'
            }
        }

        dependencies {
            onForge {
                forge = ">=${this.forgeVersion}"
            }
            minecraft = this.minecraftVersionRange
            onQuilt {
                quiltLoader = ">=${this.libs.versions.quilt.loader}"
                quilted_fabric_api = ">=${this.libs.versions.qfapi}"
            }
            onFabric {
                mod 'fabric-api', {
                    versionRange = ">=${this.libs.versions.qfapi.split('-')[0].split(/\+/)[1]}"
                }
            }
        }
    }
    onFabric {
        mixin = [
            'mixin.defaultresources.fabric.json',
            'mixin.defaultresources.json'
        ]
    }
    onQuilt {
        onFabric {
            mixin = [
                'mixin.defaultresources.json'
            ]
        }
    }
}
