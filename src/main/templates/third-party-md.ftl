<#-- To render the third-party file in Markdown for Docusaurus.
 Available context :

 - dependencyMap a collection of Map.Entry with
   key are dependencies (as a MavenProject) (from the maven project)
   values are licenses of each dependency (array of string)

 - licenseMap a collection of Map.Entry with
   key are licenses of each dependency (array of string)
   values are all dependencies using this license
-->
<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#if result?has_content>
            <#assign result = result + ", "/>
        </#if>
        <#assign result = result + "_" + license + "_"/>
    </#list>
    <#return result>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#assign name = p.artifactId/>
    <#else>
        <#assign name = p.name/>
    </#if>
    <#if p.url??>
        <#return "**[" + name + "](" + p.url + ")**">
    <#else>
        <#return "**" + name + "**">
    </#if>
</#function>
---
id: third-party
title: Used Third-Party
sidebar_label: Used Third-Party
description: Used Third-Party Libraries.
---

# Third-Party Libraries

<!-- Auto-Generated File via License Maven Plugin -->

<#if dependencyMap?size == 0>
The project has no dependencies.
<#else>
This project uses the following ${dependencyMap?size} third-party libraries:

<#list dependencyMap as e>
    <#assign project = e.getKey()/>
    <#assign licenses = e.getValue()/>
* ${artifactFormat(project)} (`${project.groupId}:${project.artifactId}:${project.version}`) -
  ${licenseFormat(licenses)}
</#list>
</#if>
