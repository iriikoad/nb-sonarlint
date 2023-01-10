# nb-sonarlint
A SonarLint plugin for Netbeans from [hmvictor](https://github.com/hmvictor/nb-sonarlint) but updated for latest SonarQube, and with a view to see errors more easily.

It runs SonarLint in connected mode when a file is saved or opened and it shows the issues as editor annotations.

To see the rule details, put the caret position in the line with a issue and choose the item **Show Rule Details** in the contextual menu or hold Ctrl key and click over the annotated line.

## Global Settings ##

The SonarQube server url (default is http://localhost:9000) and security user token can be set in **Tools > Options > Miscellaneous > SonarQube**. 

## Per Project Properties ##

SonarLint can be enabled (default value) or disabled per project in **Properties > SonarLint** (for example, if the SonarQube server is down). A list of excluded files can also be defined in this section.
You can specified the project key of the SonarQube project too if not found.

## Sonar On Fly view ##

Menu **Window > Sonar On Fly** shows in the output a view to:
- turn off/on Sonar analyse
- the status of the analyse: if OK, the server was found and analyse is completed
- the projet used for analyse: you can change it if it's not the correct one, as the plugin tries to guess the project to use if not specified or exactly found
- show/hide problems in the editor, to reduce errors displayed when editing the code
- the list of the issues founds, sorted by level:
  - double click on the issue will go to the line
  - a simple click on the issue key will display the issue rule

This view is updated when the document is saved, or checkbox "Activate" or combo box "project" changes.

## Installation ##

The plan is that after some stabilization, the plugin will be in the Netbeans plugin portal and in the Update center. Meanwhile, you can compile the plugin with maven (execute **mvn install**, plugin file with the nbm extension will be generated in the target directory) and do a [manual installation](http://wiki.netbeans.org/InstallingAPlugin).

The project was made with Java 11.


