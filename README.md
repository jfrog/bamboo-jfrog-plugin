[![Introduction](src/main/resources/images/intro.png)](#readme)

<div align="center">

# Bamboo JFrog Plugin

</div>

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Installation and Configuration](#installation-and-configuration)
- [Usage](#usage)

## Overview

The **Bamboo JFrog Plugin** is designed to provide an easy integration between Bamboo and
the [JFrog Platform](https://jfrog.com/solution-sheet/jfrog-platform/).

Unlike the legacy [Bamboo Artifactory Plugin](https://plugins.atlassian.com/plugin/details/27818), the new **Bamboo
JFrog
Plugin** focuses on a single task that runs [JFrog CLI](https://jfrog.com/help/r/jfrog-cli/jfrog-cli) commands.
Worth mentioning that both JFrog plugins, can work side by side.

The advantage of this approach is that JFrog CLI is a powerful and versatile tool that integrates with all JFrog
capabilities.
It offers extensive features and functionalities, and it is constantly improved and updated with the latest enhancements
from JFrog.
This ensures that the Bamboo JFrog Plugin is always up-to-date with the newest features and improvements provided by
JFrog.

With the Bamboo JFrog Plugin, you can easily deploy artifacts, resolve dependencies, and link them to the build jobs
that created them. Additionally, you can scan your artifacts and builds for vulnerabilities
using [JFrog Xray](https://jfrog.com/xray/) and distribute your software packages to remote locations
using [JFrog Distribution](https://jfrog.com/distribution/).

## Key Features

- **Artifact Management**: Manage build artifacts with Artifactory.
- **Dependency Resolution**: Resolve dependencies from Artifactory for reliable builds.
- **Build Traceability**: Link artifacts to their corresponding build jobs for better traceability.
- **Security Scanning**: Scan artifacts and builds with JFrog Xray for vulnerabilities.
- **Software Distribution**: Distribute software packages to remote locations using JFrog Distribution.

## Installation and Configuration

1. Download the latest release of the plugin from the [Bamboo Marketplace](https://marketplace.atlassian.com/).
2. Install the plugin on your Bamboo server.
3. In the *Bamboo Administration* section, go to *Manage Apps* and select *JFrog Configuration*.

   ![Bamboo Administration - Manage Apps - JFrog Configuration](images/readme/menu.png)
4. Click on *New JFrog Platform Configuration*.

   ![New JFrog Platform Configuration](images/readme/newConfig.png)
5. Configure your credentials details and run a *Test Connection*, then click *Save*.

   ![Server Configuration](images/readme/serverConfig.png)

## Usage

Once installed and configured, you can use the JFrog CLI task in your Bamboo build plans. Follow these
steps:

1. Go to the *Tasks* section of your build plan.
2. Add the *JFrog CLI task* to your plan.

   ![Selecting JFrog CLI task](images/readme/selectTask.png)
3. Configure the JFrog CLI task by selecting the appropriate Server ID.

   ![Configuring JFrog CLI task](images/readme/task.png)
